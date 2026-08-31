# Message Intelligence System

Unifies SoText's two previously-disconnected message-analysis features — **Smart Message Cards**
(`MessageContextParser`, dates/addresses/phone numbers/links/OTP) and **Safety Detection**
(`classifySmsUrgencyFlow`, DND-override urgency classification) — into one **Message Intelligence
Layer**: a shared schema, a single decision engine, and one dismissible action card per message.
This doc covers the taxonomy, the schema, the decision priority order, and how to extend it. Code
lives under `androidApp/src/main/java/com/sotext/data/intelligence/`.

## Pipeline

Hybrid, matching the rest of SoText's AI features:

1. **On-device baseline (free, all users, no network).** `MessageIntentDetector` runs
   regex/heuristic detection against the message body, reusing `MessageContextParser.extract()`
   for shared entity extraction (dates/times, addresses, phone numbers) instead of duplicating it.
   Runs at render time in `MessageBubble` (`SmsThreadScreen.kt`), the same hook point
   `MessageContextParser` already uses.
2. **Optional cloud deep-pass (Premium, opt-in).** `classifyMessageIntentFlow`
   (`functions/src/ai.ts`) — a new Genkit/Gemini flow, called through
   `AiAssistantRepository.classifyMessageIntent`. Gated by both `isProUser` and the user's
   `messageIntelligenceCloudEnabled` setting. A cloud result can only *upgrade* the on-device
   result (raise confidence/actionability, fill in an entity the on-device pass left null) — it
   never downgrades or replaces an already-shown card. Same upgrade-only philosophy as the
   existing urgency-upgrade logic in `PulseLinkSmsReceiver`.
3. **Safety analysis** extends the existing `classifySmsUrgencyFlow` additively (new `signals` and
   `recommendedResponse` fields on the same output schema) rather than a parallel flow, so the
   existing DND-override caller in `PulseLinkSmsReceiver` keeps working untouched.

`MessageIntelligenceRepository` is the single entry point: it runs the on-device pass, caches the
result in an `LruCache<Long, MessageIntelligenceResult>` keyed by message id (no new Room table),
and merges in a cloud result when one arrives.

## Taxonomy

`MessageIntent` (`MessageIntent.kt`) is a deliberately exhaustive enum — every intent named in the
product spec — grouped into four `IntentCategory` values: `TASK`, `REQUEST`, `COMMUNICATION`,
`TRANSACTION`. **Phase 1 ships real on-device detection for six of them**:

| Intent | Category | Example |
|---|---|---|
| `REMINDER` | TASK | "Remind me to call the dentist tomorrow at 10am." |
| `SCHEDULING` | REQUEST | "Are you free Saturday at 6?" |
| `LOCATION_REQUEST` | REQUEST | "Where are we meeting tonight?" |
| `CONTACT_REQUEST` | REQUEST | "Can you send me Sarah's number?" |
| `PAYMENT_REQUEST` | TRANSACTION | "Can you send me $25 for dinner?" |
| `INFORMATION_REQUEST` | REQUEST | any plain question, as a generic fallback |

The remaining ~19 values (`TODO`, `FOLLOW_UP`, `CALL_SOMEONE`, `INVITATION`, `MONEY_OWED`, etc.)
are defined now so nothing needs redesigning later, but have no detector wired — they simply never
match. **Adding one is a new private `find*` function in `MessageIntentDetector.kt` plus one line
in `analyze()`, not a redesign.**

### Actionability

`Actionability.kt`: `EXPLICIT`, `IMPLIED`, `INFORMATIONAL`, `AMBIGUOUS`, `NONE`. This is what keeps
"I need to remember to buy milk." (implied, low confidence, no auto-card) distinct from "Remind me
to buy milk tonight." (explicit, high confidence, auto-card) — see
`MessageIntentDetectorTest`'s `implied reminder ...` / regression `an ordinary statement ...`
cases.

### Entities

`MessageEntities.kt` — every field nullable, only populated when a detector actually matched
something in the text. **Never invented.** A reminder with no date/time in the message text leaves
`dateEpochDay`/`timeMinuteOfDay` null rather than guessing; the resulting card asks instead of
assuming (`SmartMessageCard`'s "When?" prompt).

### Multiple intents per message

A message can carry a primary and a secondary intent (`MessageIntelligenceResult.secondaryIntents:
List<SecondaryIntent>`). Example: "Remind me tomorrow to pick up Sarah at the airport, and what
time does her flight land?" → primary `REMINDER` (explicit), secondary `INFORMATION_REQUEST`. The
decision engine renders this as one card with an extra affordance for the secondary intent, never
two competing cards.

## Safety

`SafetyAnalysis.kt` reuses the existing `MessageUrgency` enum for `level` (so the DND-override
caller and this feature agree on what "emergency" means), and adds:

- `signals: List<SafetySignal>` — `THREAT`, `HARASSMENT`, `VIOLENCE_CONCERN`, `SELF_HARM_CONCERN`,
  `SCAM_FRAUD`, `EMERGENCY_LANGUAGE`, `OTHER`. Signals, not diagnoses.
- `recommendedResponse: SafetyResponseCategory` — `MONITOR`, `OFFER_HELP`, `URGENT_ESCALATE`,
  `EMERGENCY_ESCALATE`.

The cloud prompt (`buildUrgencyPrompt` in `functions/src/ai.ts`) is explicitly instructed not to
diagnose the sender.

**Safety always wins.** If a message carries both a safety signal and an ordinary actionable
intent, `MessageIntelligenceDecisionEngine` shows only the safety card. "Get Help" routes through
the existing `EmergencyPopupActivity` confirmation flow (EMERGENCY tier); "Share Location" calls
the existing `sendCheckIn()` path directly (CHECK_IN tier, no confirmation), matching how those two
tiers already behave everywhere else in the app.

## Decision engine

`MessageIntelligenceDecisionEngine.decide(...)` is a pure function (no Android dependencies, fully
unit-tested) implementing this priority order:

```
safety-critical (level != STANDARD, confidence >= highConfidence)
  > explicit actionable (confidence >= highConfidence, actionability == EXPLICIT)  -> ShowActionCard
  > medium confidence                                                              -> ShowSuggestion
  > everything else (low confidence / NONE / suppressed type / already dismissed)  -> NoCard
```

`CardDecision` is a sealed class: `ShowSafetyCard`, `ShowActionCard`, `ShowSuggestion`, `NoCard`.
At most one is ever returned — the engine never asks the UI to render two cards for one message.

Thresholds are a single source of truth, not hardcoded per call site:
`MessageIntelligenceThresholds` (`highConfidence = 0.75f`, `mediumConfidence = 0.5f` by default).

## User control

- **Dismiss** — in-memory, per-message, `SmsThreadViewModel`-hoisted `StateFlow`. Not persisted to
  disk, matching the existing precedent set by `ContextActionCard` and `CatchUpCardView` dismissal
  in this codebase — a dismissed card doesn't reappear for that message this session, but restarts
  don't need to remember every dismissed message id forever.
- **"Don't show this type again"** — `PulseLinkSettings.suppressedIntelligenceCardTypes: Set<String>`,
  DataStore-persisted, survives restart. Suppresses that `MessageIntent` (and any secondary of that
  type) for every future message.
- Settings toggles live in Settings → Smart Features: **"Message Intelligence"**
  (`messageIntelligenceEnabled`, on by default, free) and **"Deep AI analysis"**
  (`messageIntelligenceCloudEnabled`, off by default, Premium-gated, only shown once the base
  toggle is on).

## Suggested actions

`SuggestedActionType` (`SuggestedAction.kt`): `CREATE_REMINDER` and `ADD_TO_CALENDAR` open the
device Calendar app pre-filled and require the user's own confirmation there — nothing is
committed silently. `CHECK_AVAILABILITY` opens Calendar's view. `FIND_CONTACT` opens the Contacts
picker. `SHARE_LOCATION`, `SUGGEST_REPLY`, `OPEN_PAYMENT_APP`, and `REPLY` are honest stubs (toast
today) — the spec explicitly allows deferring third-party integrations for these while keeping the
action architecture ready for them.

## Extending

To add real detection for an already-defined intent (e.g. `TODO`):

1. Add a `find*` function to `MessageIntentDetector.kt` returning an `IntentMatch?`.
2. Call it from `analyze()`.
3. Add cases to `MessageIntentDetectorTest.kt` (explicit + implied/ambiguous + a negative case).
4. If it needs a new suggested action, add a `SuggestedActionType` and a case to
   `MessageIntelligenceRepository.actionsFor()`.
5. If the UI needs new copy/an icon for that intent, extend `smartCardPresentation` in
   `SmsThreadScreen.kt` — no new composable needed; `SmartMessageCard` is intent-agnostic.

## Tests

- `androidApp/src/test/java/com/sotext/data/intelligence/MessageIntentDetectorTest.kt`
- `androidApp/src/test/java/com/sotext/data/intelligence/MessageIntelligenceDecisionEngineTest.kt`
- `functions/src/ai_security.test.ts` — `buildMessageIntentPrompt` cases (tag wrapping, entity
  invention guard, explicit/implied distinction, prompt-injection sanitization) alongside the
  existing `buildUrgencyPrompt` cases, extended for the new safety fields.

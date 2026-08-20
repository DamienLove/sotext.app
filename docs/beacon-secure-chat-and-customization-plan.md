# SoText Messaging Upgrades (Premium → Pro → Free)

## Goals (user-facing)
- Premium first: secure rich messaging with E2E, deep theming/personalization, scheduling/auto-reply, spam/scam protection, backup/restore, cross-device sync (Android ↔ iOS), smooth UI flows.
- Pro next: same core, lighter presets/models.
- SoText Free: curated customization, scheduling/auto-reply basics, spam-lite, backup/restore; Secure Chat optional without E2E by default.

## Pillars & Scope
- Transport: OTT “Secure Chat” channel (Firestore/Functions/Storage) with rich media, read receipts, typing, reactions; RCS if carrier access lands; SMS/MMS as fallback.
- Security: E2E on Secure Chat (libsignal if viable; else Double Ratchet + vetted crypto), local app/chat locks, encrypted at-rest storage.
- Customization: theme engine (global/per-chat), bubble presets, fonts, backgrounds, avatars/badges, density, swipe/gesture configs, haptics/sounds, per-contact notification categories.
- Automation: scheduling (send later), auto-reply (global/per-contact, time windows), templates.
- Quality/UX: smooth Compose flows, animations, reactions, high-quality media pipeline.
- Protection: on-device spam/scam ML (TF Lite; size tiers), heuristics, allow/deny lists, warning banners, mute/silence.
- Continuity: backup/restore (Drive + local; later iCloud on iOS), cross-device sync for settings/themes/OTT history.
- Cross-OS: Secure Chat parity on iOS to enable Android↔iOS messaging (SMS/RCS cannot sync cross-OS).

## Milestones (Premium-first)
- M0 Foundations
  - Choose E2E stack (libsignal vs. custom DR). Spike RCS feasibility (carrier/Jibe).
  - Data model + Firestore/Functions schema for Secure Chat (users, sessions, conversations, messages, attachments, receipts).
  - Theme engine state model + persistence (DataStore) + sync plan.
  - Spam model packaging pipeline (RC-delivered model switches).
- M1 Premium Delivery
  - Secure Chat: E2E sessions, send/receive, read receipts, typing, media upload/download, reactions; Compose UI; fallback SMS/MMS.
  - Theming: global/per-chat themes, bubble presets, fonts, backgrounds, avatars/badges, density/swipe/animation settings.
  - Scheduling/auto-reply (WorkManager; per-contact rules), fine-grained notifications.
  - Spam ML (premium-sized), heuristics, warning UI; allow/deny lists.
  - Backup/restore (Drive + local) covering settings/themes/Secure Chat history; SMS where allowed.
  - Cross-device sync: settings + Secure Chat across Android/iOS.
- M2 Pro + SoText Free
  - Pro: same features with smaller model, fewer presets/templates by default.
  - SoText Free: curated themes/presets, spam-lite model, Secure Chat without default E2E (opt-in), scheduling/auto-reply basics, backup/restore limited scope.
  - iOS parity for Secure Chat + theming needed for cross-OS messaging.

## Architecture Highlights
- Secure Chat transport: Firestore (metadata), Storage (media), Functions (fan-out/notifications, key backup optional), local cache with at-rest encryption.
- E2E: per-conversation sessions, per-message header, attachments encrypted before upload; server holds only ciphertext/metadata. Local keys sealed with OS keystore; optional key backup (user-consented).
- Theming: theme store (DataStore) + per-chat override; mirrored to Firestore for multi-device. Compose theme adapters at nav roots; per-chat overrides scoped.
- Scheduling/auto-reply: WorkManager jobs; local rule store; respect doze/battery; UI for queues and rules.
- Spam ML: bundled TF Lite model; RC model URL/version; lightweight feature extraction on-device; no PII off-device.
- Backup/restore: Drive API + encrypted local exports; include settings/themes/Secure Chat; SMS where allowed; integrity checks; user-consent flows.

## Risks / Decisions Needed
- RCS access: if unavailable, Secure Chat is primary rich path.
- E2E stack: libsignal preferred; if blocked, use DR + audited crypto lib (e.g., BoringSSL/Tink) with clear threat model.
- Model size tiers: Premium larger, Pro medium, Free small.
- iOS timeline: needed for cross-OS Secure Chat; schedule iOS client workstream.

## Immediate TODO (M0)
- Spike RCS provider feasibility and report path.
- Pick E2E stack; draft key management + backup approach.
- Define Firestore/Functions schema for Secure Chat and begin stubs.
- Define theme state model + presets list for Premium/Pro/Free.
- Stand up spam model delivery mechanism (RC param + local fallback).

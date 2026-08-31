# SoText Messaging Suite
Unified SMS, Safety, and AI Messaging

SoText is a modern messaging hub that combines fast, reliable SMS/MMS with intelligent safety tools and add-ons like RingerSong audio utilities. It's not a separate safety app bolted onto a texting app — emergency alerts, DND overrides, and trusted-contact workflows are built directly into SoText, alongside a clean inbox, rich theming, and on-device AI assistance.

## Product Overview
SoText is designed to be your default SMS app: a clean inbox, rich theming, and high reliability, plus optional modules for personal safety, AI-powered assistance, and caller/media utilities. Users who just want a great texting app can stay lightweight, while enabling the safety tools unlocks advanced emergency, check-in, and web-portal features managed through the same interface. The app has unified the former PulseLink (safety) and Beacon (messaging) products into a single SoText app; some internal docs, screens (e.g. "Beacon Settings"), and code still carry those legacy names.

## Core SoText Features
SoText focuses on being a fast, intuitive, and extensible messaging experience.

- Default SMS/MMS handler with a unified, modern inbox for all conversations, private/PIN-gated threads, and archive support.
- Responsive UI with future-facing visual design and themes inspired by glassmorphism and ambient lighting (for example, Aurora or Midnight OLED–style looks), including per-contact theme overrides.
- User-configurable swipe gestures on the inbox (favorite/delete/archive), tuned to sensible defaults out of the box.
- Context-aware actionable cards: on-device parsing surfaces inline actions for dates/times (add to calendar), addresses (directions), phone numbers (call/save contact), and links/tracking numbers/OTP codes (open/track/copy) — nothing leaves the device for this feature.
- Message Intelligence: a unified pipeline that classifies each message's intent (reminder, scheduling, location/contact/payment request, and more), extracts entities without inventing missing ones, and shows at most one clearly-explained, dismissible action card per message — free and on-device by default, with an optional Premium cloud deep-pass for ambiguous cases. Extends Safety Detection rather than duplicating it: a safety-relevant message always takes priority over an ordinary action card. See `docs/message-intelligence.md`.
- Catch Me Up, an AI-generated inbox briefing that groups unread/recent conversations into "needs a response," "important updates," and "no action needed," each with a summary and quick Reply/Open/Mark-handled actions.
- Optional AI-assisted messaging in higher tiers: thread summaries, compose assist (rewrite/shorten/expand/polish/reply), and urgency classification for unknown senders.
- A third-party extensions platform (web + in-app WebView) for developer-built add-ons, with a dev sandbox available on the web client.

## Safety Layer
SoText's safety layer adds a safety-first layer on top of your everyday inbox and device capabilities.

Key capabilities include:
- Emergency and check-in alerts that can use both data and SMS channels for reliability, with trusted-contact escalation tiers (Emergency vs. Check-in), auto-call, and location sharing.
- Multi-channel delivery logic that prioritizes fast data messaging but falls back to SMS/MMS when needed, so alerts are more likely to get through, plus audio gain/DND override for critical alerts.
- Context-aware AI that can flag urgent situations in your conversations on supported tiers.
- Remote access through the web portal at [sotext.app](https://sotext.app) where supported by your plan, including settings/theme sync, trusted-contact management, and an emergency map.
- Always-on, voice-activated phrase detection (e.g. "Hey Google, open SoText Emergency") is documented but currently pulled back to a stub — on-device testing surfaced reliability issues with offline speech recognition, and it's pending a consent flow and custom-phrase support before it ships again.

## RingerSong Add-On (Audio Utility)
RingerSong remains a fun, optional media utility that plugs into your calling experience. It is not required for SoText, but works nicely alongside it for users who want more control over how incoming calls sound.

Capabilities include:
- Progressive ringtones that use your existing Spotify Premium (or YouTube Music) tracks, downloading them first and advancing through the song in segments while a call rings.
- Smart caller ID and spam blocking using external lookup integrations where available.
- A lightweight install and configuration path, treated as a free add-on in the broader SoText ecosystem.

## Editions and Status
SoText is live on Google Play (Android is the primary, actively shipping platform) and offered in multiple tiers, each layering in more capability on the same core messaging foundation. An iOS build (via the shared Kotlin Multiplatform module) and a Wear OS companion also exist in-repo but lag Android in feature parity and rebrand status.

| Tier          | Pricing        | Role                                                                       | Status              |
| ------------- | -------------- | --------------------------------------------------------------------------- | -------------------- |
| Free          | Free, ad-supported | Default SMS/MMS inbox, theming, context-aware cards, and baseline safety tools (alerts, DND override, trusted contacts). | Shipping; in active development. |
| Pro           | One-time purchase | Everything in Free, ads removed, plus richer customization and caller ID screening. | Shipping; in active development. |
| Premium       | Subscription   | Everything in Pro, plus full AI assist (thread summaries, compose assist, Catch Me Up, urgency classification) and remote web portal access/sync. | Shipping; in active development. |
| RingerSong    | Free utility add-on | Progressive ringtones and caller utilities, independent of messaging features. | Shipping; in active development. |

# PulseLink

PulseLink is a personal safety app that listens for discreet trigger phrases and instantly escalates alerts to trusted contacts with location context.

[![CI](https://github.com/DamienLove/pulselink/actions/workflows/verify-main.yml/badge.svg)](https://github.com/DamienLove/pulselink/actions/workflows/verify-main.yml)

<!-- Donation badges — replace placeholders after you enable them (see instructions below) -->
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-❤_GitHub_Sponsors-ea4aaa?logo=github)](https://github.com/sponsors/YOUR_USERNAME)
[![Ko‑fi](https://img.shields.io/badge/Buy_me_a_coffee-Ko%E2%80%91fi-29abe0?logo=kofi)](https://ko-fi.com/YOUR_USERNAME)
[![PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal)](https://www.paypal.com/donate?hosted_button_id=YOUR_BUTTON_ID)

## Downloads

- Latest build is on Google Play (coming soon)
- Beta APK:
  - Download: [PulseLink Beta](https://github.com/DamienLove/pulselink/releases/download/Beta/androidApp-free-release.apk)
- Alpha APK:
  - Download: [PulseLink Alpha](androidApp/build/outputs/apk/free/debug/androidApp-free-debug.apk)

## Product Lineup

- **Beacon (Free)** – ad-supported default SMS app with core safety and alerts.
- **PulseLink Pro (one-time)** – Beacon plus the full safety stack with ads removed.
- **Beacon Premium (subscription)** – everything in Pro plus caller ID, remote SMS access, AI \"Lab\" features, trusted/private contacts, and customizable UI.

## Multi-Channel Messaging

PulseLink now uses a Firebase-first messaging architecture for robust app-to-app communication:
- **Firebase Realtime/FCM**: Primary channel for instant, cost-effective alerts.
- **SMS Fallback**: Ensures delivery when data is unavailable or Firebase fails.
- **Email Fallback**: Tertiary backup for critical alerts.

This hybrid approach ensures high reliability without strictly depending on cellular data or SMS permissions for every message.

### Configuration

To enable email fallback, configure the email transport credentials in Firebase Functions environment variables:

```bash
firebase functions:config:set email.user="your-email@gmail.com" email.pass="your-app-password"
```
(Ensure your function code retrieves these, or use `.env` files for `process.env.EMAIL_USER` / `EMAIL_PASS` access).

## iOS Roadmap

PulseLink for iOS is in active planning. See the detailed milestones, dependencies, and how to contribute:

- docs: [iOS Roadmap](docs/ios-roadmap.md)
- GitHub Pages: https://damienlove.github.io/pulselink/ (auto-published from the `docs/` folder)

## Support the Project

If PulseLink helps you or someone you care about, please consider supporting development. Your contributions fund:

- App Store fees and infrastructure (build minutes, test devices)
- Accessibility and safety research
- iOS development to reach more users

How to donate:

- GitHub Sponsors: https://github.com/sponsors/YOUR_USERNAME
- Ko‑fi: https://ko-fi.com/YOUR_USERNAME
- PayPal: https://www.paypal.com/donate?hosted_button_id=YOUR_BUTTON_ID

Prefer to contribute code, docs, or testing? See Issues and the iOS Roadmap above—PRs are welcome.

## Documentation & Wiki

This repository publishes documentation via GitHub Pages from the `docs/` directory. Any changes pushed under `docs/**` will automatically re-deploy the site via GitHub Actions.

- Source docs: [`/docs`](docs)
- Published site: https://damienlove.github.io/pulselink/

If you also use the GitHub Wiki feature, mirror or link the same content there for consistency.

## Status

Fresh build created on November 16, 2025. Messaging pathways remain fully enabled.

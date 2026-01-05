# PulseLink Ecosystem

**Advanced Personal Safety & Utility Suite**

This repository hosts the **PulseLink** family of safety applications and the **RingerSong** media utility. PulseLink provides state-of-the-art emergency alerting, discreet trigger phrases, and "always-on" safety monitoring, while RingerSong offers a robust offline music experience with advanced caller identification.

---

## 🚀 Application Lineup

| App | Flavor | Description | Status |
| :--- | :--- | :--- | :--- |
| **PulseLink Beacon** | `free` | The core safety app. Ad-supported. Includes "Always-On" phrase detection, Emergency Alerts, Check-Ins, and Firebase/SMS messaging. | [![Build Status](https://img.shields.io/badge/Build-Passing-success)]() [![Play Store](https://img.shields.io/badge/Google_Play-Coming_Soon-grey?logo=google-play)]() |
| **PulseLink Pro** | `pro` | Ad-free safety experience. Includes all Beacon features plus enhanced UI and priority support. | [![Build Status](https://img.shields.io/badge/Build-Passing-success)]() [![Play Store](https://img.shields.io/badge/Google_Play-Coming_Soon-grey?logo=google-play)]() |
| **PulseLink Premium** | `premium` | The ultimate safety suite. Adds AI Assist (summaries, smart replies), Web Portal access (remote messaging), and Caller ID screening. | [![Build Status](https://img.shields.io/badge/Build-Passing-success)]() [![Play Store](https://img.shields.io/badge/Google_Play-Coming_Soon-grey?logo=google-play)]() |
| **RingerSong** | `RingerSong` | Offline Spotify player & Caller ID utility. Download tracks, identify spam calls, and manage media without needing a Spotify Premium subscription. | [![Build Status](https://img.shields.io/badge/Build-Passing-success)]() [![Play Store](https://img.shields.io/badge/Google_Play-Coming_Soon-grey?logo=google-play)]() |

---

## 🛠️ Key Features

### PulseLink (Safety & Messaging)
*   **Discreet Voice Triggers:** Activate emergency alerts or check-ins using custom spoken phrases, even when the phone is locked.
*   **DND Override:** Critical alerts bypass "Do Not Disturb" settings to ensure they are heard (utilizing Android's `setBypassDnd` and audio focus overrides).
*   **Multi-Channel Delivery:** Uses a "Firebase-First" approach for instant data messaging, automatically falling back to SMS (and optionally Email) if data fails.
*   **Trusted Contacts:** Define separate circles for "Emergency" vs. "Check-In" alerts.
*   **Live Location:** Share real-time location tracking during active emergencies.
*   **Google Assistant Integration:** "Hey Google, emergency alert with PulseLink".

### RingerSong (Media & Utility)
*   **Offline Spotify:** Download tracks directly to your device for offline playback—no Spotify App required.
*   **YouTube Music Integration:** Search and browse YouTube Music library.
*   **Caller ID & Spam Protection:** Identify unknown numbers and block spam using Truecaller integration.
*   **Local Playback:** Robust local file management for your own MP3 library.

---

## 📥 Downloads & Installation

**Open Testing / Beta:**
*   [PulseLink Beacon (Free) - Latest APK](https://github.com/DamienLove/pulselink/releases/tag/latest)
*   [RingerSong - Latest APK](https://github.com/DamienLove/pulselink/releases/tag/latest)

**Production:**
*   COMING SOON to Google Play.

---

## 📚 Documentation & Support

We maintain comprehensive documentation for all apps in our [Wiki](wiki/).

*   [**PulseLink User Guide**](wiki/PulseLink-User-Guide.md): Setup, Voice Phrases, and Trusted Contacts.
*   [**RingerSong User Guide**](wiki/RingerSong-Manual.md): Music Downloading and Caller ID setup.
*   [**Troubleshooting**](wiki/Troubleshooting.md): Permissions, Battery Optimization, and DND issues.

### Contributing
We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.
*   **Bugs:** [Report a Bug](https://damienlove.github.io/pulselink/)
*   **iOS Roadmap:** [View Status](docs/ios-roadmap.md)

---

## 💖 Support the Project

PulseLink is developed with a mission to make personal safety accessible to everyone. Your support helps fund server costs (Firebase, SMS gateways) and development time.

[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-❤_GitHub_Sponsors-ea4aaa?logo=github)](https://github.com/sponsors/DamienLove)
[![Ko‑fi](https://img.shields.io/badge/Buy_me_a_coffee-Ko%E2%80%91fi-29abe0?logo=kofi)](https://ko-fi.com/DamienLove)

---

## 📄 License

Copyright © 2026 PulseLink. All Rights Reserved.
See [LICENSE](LICENSE) for details.
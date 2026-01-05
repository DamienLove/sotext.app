# Troubleshooting

Solutions for common PulseLink and RingerSong issues.

---

## PulseLink

### 🔊 Alarm Not Sounding (DND)
**Issue:** Emergency sirens are silent or quiet.
**Fix:**
1.  **Android 15+ Users:** You *must* toggle "Bypass Do Not Disturb" in the specific Notification Channel settings for PulseLink.
2.  **Permissions:** Ensure "Override Do Not Disturb" is granted in App Settings > Notifications.
3.  **Volume:** Check "Audio Gain" in PulseLink Settings.

### 🔋 Voice Assistant Issues
**Issue:** "Hey Google, open PulseLink Emergency" isn't working.
**Fix:**
1.  Ensure **Google Assistant** is enabled and configured on your device.
2.  Open PulseLink once manually to register the shortcuts.
3.  Check if your phrase matches exactly what you set in **Settings > Voice Triggers**.

### 📍 Location Not Updating
**Issue:** Contacts receive an old location.
**Fix:**
*   Ensure **Location Permission** is set to "Allow all the time" (required for background tracking).
*   Open the app once to refresh GPS lock if you haven't used it in days.

---

## RingerSong

### 🎵 Spotify Integration Issues
**Issue:** Cannot connect or tracks won't play.
**Fix:**
*   **Premium Required:** Ensure you have an active Spotify Premium subscription.
*   **Internet:** A stable connection is required to stream tracks as ringtones (unless cached by the Spotify app).
*   **Re-login:** Try disconnecting and reconnecting your account in RingerSong settings.

### 📞 Caller ID Missing
**Issue:** No overlay for incoming calls.
**Fix:**
*   Grant **"Display over other apps"** permission.
*   Ensure you have an active data connection (needed for real-time lookup).

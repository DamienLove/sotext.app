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

### 🔋 Voice Triggers Stopping
**Issue:** "Always-On" listening stops working after some time.
**Fix:**
Android Battery Optimization often kills background services.
1.  Go to **Settings > Apps > PulseLink > Battery**.
2.  Select **Unrestricted** or **Don't Optimize**.
3.  (Samsung users): Check "Sleeping Apps" and remove PulseLink.

### 📍 Location Not Updating
**Issue:** Contacts receive an old location.
**Fix:**
*   Ensure **Location Permission** is set to "Allow all the time" (required for background tracking).
*   Open the app once to refresh GPS lock if you haven't used it in days.

---

## RingerSong

### ⬇️ Download Failed
**Issue:** Spotify link doesn't download.
**Fix:**
*   Use a **Track Link** or **Public Playlist Link**. Private playlists cannot be accessed.
*   Check internet stability.
*   Wait 5 minutes if you've hit a rate limit.

### 📞 Caller ID Missing
**Issue:** No overlay for incoming calls.
**Fix:**
*   Grant **"Display over other apps"** permission.
*   Ensure you have an active data connection (needed for real-time lookup).
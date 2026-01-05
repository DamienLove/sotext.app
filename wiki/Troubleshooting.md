# Troubleshooting

Common solutions for PulseLink and RingerSong.

---

## PulseLink

### Do Not Disturb (DND) Override Issues
**Problem:** The alarm isn't loud enough or doesn't sound when my phone is in Silent/DND mode.
**Solution:**
1.  **Check Permissions:** Go to Android Settings > Apps > PulseLink > Notifications. Ensure "Override Do Not Disturb" is allowed for the "Emergency Alerts" channel.
2.  **Android 15+:** You *must* enable the specific "Bypass DND" toggle in the notification channel settings. The global permission is no longer sufficient on newer Android versions.
3.  **Audio Gain:** In PulseLink Settings, ensure "Max Volume Override" is enabled.

### Battery Optimization / App Killing
**Problem:** The app stops listening for phrases after a while.
**Solution:**
Android attempts to save battery by killing background apps. You must exclude PulseLink from these optimizations.
1.  Go to Android Settings > Battery > Battery Optimization.
2.  Find **PulseLink**.
3.  Select **"Don't Optimize"** or **"Unrestricted"**.
4.  (Samsung/Xiaomi/Huawei): Check for specific "Auto-Start" or "Background Activity" permissions in your phone manager settings.

### Message Delivery Failures
**Problem:** My contact didn't receive the alert.
**Solution:**
*   **Check Connectivity:** PulseLink tries Data (Firebase) first, then SMS. If both you and the recipient have no signal, delivery will fail.
*   **SMS Credits:** Ensure you have an active SMS plan.
*   **Spam Filters:** Ask your contact to check their "Spam" or "Blocked" SMS folder. The emergency message contains links which some carriers might flag aggressively.

---

## RingerSong

### Spotify Download Fails
**Problem:** "Download failed" or "Link invalid".
**Solution:**
*   Ensure you copied a **Track Link** or **Playlist Link**, not a user profile or podcast link.
*   Check your internet connection.
*   Rate Limits: If you've downloaded many songs quickly, wait a few minutes and try again.

### Caller ID Not Showing
**Problem:** Incoming calls aren't identified.
**Solution:**
*   **Active Connection:** Real-time lookup requires an internet connection (WiFi or Data).
*   **Permissions:** Ensure RingerSong has "Display over other apps" permission to show the overlay.

# PulseLink Troubleshooting Guide

## 🔔 Notifications & Do Not Disturb (DND)

### Issue: My contact didn't hear the alert!
**Solution:**
1.  **Check Permissions**: Ensure the *recipient* has granted PulseLink "Do Not Disturb Access".
2.  **Android 15+ Users**: Due to system changes, you must manually allow "Bypass DND" for the "Emergency Alerts" notification channel.
    *   Go to **Settings > Apps > PulseLink > Notifications**.
    *   Tap **Emergency Alerts**.
    *   Toggle **Override Do Not Disturb** to **ON**.
3.  **Check Volume**: PulseLink attempts to maximize volume, but ensure the device isn't connected to silent Bluetooth headphones.

### Issue: "Partial Override" Warning
**Solution:**
This means PulseLink raised the volume but couldn't disable the global DND setting. This is common on newer Android versions. As long as the **Notification Channel Bypass** is enabled (see above), the alert will still sound.

## 📡 Connectivity & Alerts

### Issue: Messages aren't sending via Data/WiFi.
**Solution:**
PulseLink uses a "Firebase-First" system. If data fails:
1.  The app will automatically try **SMS Fallback** after 5 seconds.
2.  Ensure you have a valid cellular signal.
3.  Check **Settings > Message Delivery** to see if "Disable Firebase" is accidentally toggled on.

### Issue: Location isn't updating.
**Solution:**
1.  Verify **Location Permissions** are set to "Allow all the time" for continuous safety monitoring.
2.  Ensure **Battery Saver** mode isn't restricting GPS background usage.

## 🎤 Voice Trigger

### Issue: The app isn't hearing my trigger phrase.
**Solution:**
1.  **Retrain**: Go to Settings > Phrase Detection and re-record your trigger phrase in a quiet environment.
2.  **Background Restrictions**: Ensure PulseLink is excluded from "Battery Optimization" so the listening service isn't killed by the system.
3.  **Microphone Access**: Verify the microphone permission is granted.

## 🛠️ General

### Issue: I can't find the Web Portal (Premium).
**Solution:**
Web access is a Premium feature. Ensure your subscription is active, then visit `https://portal.pulselink.app` (or your configured domain) and scan the QR code from the mobile app settings.

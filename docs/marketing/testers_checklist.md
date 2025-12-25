# PulseLink & Beacon Tester Checklist (v55+)

## 1. Onboarding & Permissions
- [ ] **Fresh Install**: Verify clean install flow.
- [ ] **Permissions Request**: Confirm all permissions are requested in context (Microphone, Location, Contacts, Notifications, DND Access).
- [ ] **DND Access**:
    - [ ] Verify DND Access screen directs user to system settings.
    - [ ] Verify app detects when DND access is granted/revoked.
- [ ] **Login/Registration**: Sign up with email/Google.

## 2. Core Safety Features (PulseLink)
- [ ] **Phrase Detection**:
    - [ ] Test trigger phrase while app is in foreground.
    - [ ] Test trigger phrase while app is backgrounded.
    - [ ] Test trigger phrase while device is locked.
- [ ] **Emergency Alert**:
    - [ ] Trigger an emergency alert.
    - [ ] Verify siren plays (max volume).
    - [ ] Verify DND is overridden (Device set to DND -> Siren should still hear).
    - [ ] Verify "Emergency Mode" UI appears.
- [ ] **Check-In Alert**:
    - [ ] Send a manual check-in.
    - [ ] Verify it uses lower urgency (may duck audio instead of pause).
- [ ] **Trusted Contacts**:
    - [ ] Add a contact.
    - [ ] Verify contact receives invitation (if applicable).
    - [ ] Verify contact receives alerts.

## 3. Messaging & Delivery (PulseLink & Beacon)
- [ ] **Firebase Delivery**:
    - [ ] Send message between two online devices.
    - [ ] Verify instant delivery via Firestore/FCM.
- [ ] **SMS Fallback**:
    - [ ] Put recipient device in airplane mode (or disable internet).
    - [ ] Send message.
    - [ ] Verify app falls back to SMS (checking status indicators).
- [ ] **Email Fallback**:
    - [ ] Enable Email Fallback in settings.
    - [ ] Fail both Firebase and SMS.
    - [ ] Verify email is received.

## 4. Beacon SMS App Features
- [ ] **Default SMS App**: Set Beacon as default SMS app.
- [ ] **SMS Send/Receive**: Send and receive standard SMS.
- [ ] **MMS**: Send an image (Note: may be stubbed, check matrix).
- [ ] **Conversation List**:
    - [ ] Check pinning/unpinning.
    - [ ] Check archiving.
- [ ] **Theming**: Apply a custom theme/gradient.

## 5. Premium Features (If Enabled)
- [ ] **AI Summary**: Open a long thread -> Request summary.
- [ ] **AI Compose**: Write a draft -> Use AI to "Polish" or "Make Urgent".
- [ ] **Web Sync**:
    - [ ] Log in to Web Portal.
    - [ ] Verify messages sync between phone and web.
    - [ ] Send message from web.

## 6. Settings & Troubleshooting
- [ ] **Channel Preferences**: Disable Firebase -> Force SMS.
- [ ] **Delivery Stats**: Check if stats update after sending.
- [ ] **Delete Account**: Verify account deletion works and cleans up data.

## 7. Platform Specifics
- [ ] **Android 14 vs 15**:
    - [ ] Verify DND behavior on Android 15 (Channel bypass).
    - [ ] Verify DND behavior on Android 14 (Global priority mode).
- [ ] **WearOS**:
    - [ ] Install on watch.
    - [ ] Trigger alert from watch.

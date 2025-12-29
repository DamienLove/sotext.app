# PulseLink Functional Spec

## Overview
PulseLink passively listens for user-defined trigger phrases ("PulseLink phrases") and escalates alerts to trusted contacts across SMS, high-priority notifications, and optional automated calls. The system must never block outgoing or incoming messaging; it augments communication pathways instead of restricting them.

## Core Capabilities
- Continuous phrase detection via foreground service + on-device speech recognition
- Alert orchestration that can:
  - Send SMS messages with high-visibility language
  - Dispatch critical notifications that override Do Not Disturb
  - Share last-known and live location updates when granted
  - Initiate optional follow-up phone call prompts
- Multi-contact management with separate escalation profiles per tier (Emergency vs. Check-in)
- Silent SOS mode that suppresses local UI feedback while still signaling contacts

## Platform Notes
- Android: Compose UI, WorkManager/ForegroundService for long-running tasks, Room for persistence, Hilt for DI.
- iOS: Planned via SwiftUI, CallKit, PushKit (not yet implemented in this rebuild).

## Security & Privacy
- Data minimization: store only hashed phrases + encrypted contact details
- Require biometric or PIN to alter critical settings
- Provide transparent audit log users can clear

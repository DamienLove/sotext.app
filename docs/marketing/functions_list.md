# PulseLink Cloud Functions List

This document lists the deployed Firebase Cloud Functions for the PulseLink backend.

## AI & Natural Language
*   **`naturalLanguageQuery`**
    *   **Trigger**: Callable (HTTPS)
    *   **Purpose**: Handles natural language queries from the app, likely for the "Assistant" feature or help interface.
*   **`menuSuggestion`**
    *   **Trigger**: Callable
    *   **Purpose**: Sample Genkit flow for menu suggestions (likely a demo or template, check usage).
*   **`summarizeSmsThread`**
    *   **Trigger**: Callable
    *   **Purpose**: (Premium) Generates AI summaries of SMS conversation threads.
*   **`composeSmsAssist`**
    *   **Trigger**: Callable
    *   **Purpose**: (Premium) Assists in composing SMS messages (rewrite, shorten, expand, polish).
*   **`classifySmsUrgency`**
    *   **Trigger**: Callable
    *   **Purpose**: (Premium) Analyzes inbound SMS content to determine urgency levels for unknown sender alerting.

## Alerting & Messaging
*   **`alertRelay`**
    *   **Trigger**: Callable
    *   **Purpose**: Core logic for relaying emergency or check-in alerts to trusted contacts via FCM/SMS.
*   **`alertRelayHttp`**
    *   **Trigger**: HTTPS Request
    *   **Purpose**: HTTP endpoint version of the alert relay, possibly for external integrations or webhooks.
*   **`onMessageCreated`**
    *   **Trigger**: Firestore Trigger (`linkChannels/{channelId}/messages`)
    *   **Purpose**: Listens for new messages in Firestore and triggers FCM notifications to recipients (part of "Firebase-First" delivery).
*   **`sendEmailNotification`**
    *   **Trigger**: Callable
    *   **Purpose**: Sends email notifications, likely for the "Email Fallback" feature or invitations.

## User Management
*   **`findUser`**
    *   **Trigger**: Callable
    *   **Purpose**: Allows finding other users by email/phone for linking trusted contacts.
*   **`deleteAccount`**
    *   **Trigger**: Callable
    *   **Purpose**: Handles user account deletion cleanup (GDPR/Data deletion compliance).

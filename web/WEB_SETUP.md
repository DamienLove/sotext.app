# SoText Web App Setup Guide

This guide describes how to configure and deploy the SoText Web App (Premium/Pro feature) to Firebase Hosting.

## Prerequisites

1.  **Node.js**: Ensure Node.js (v18 or v20 recommended) is installed.
2.  **Firebase CLI**: Install globally via `npm install -g firebase-tools`.
3.  **Firebase Project**: You must have access to the PulseLink Firebase project.

## Configuration

The web app requires environment variables for Firebase configuration and Google Maps.

1.  Create a file named `.env.local` in the `web/` directory.
2.  Add the following keys (replace with your actual Firebase project values):

```env
VITE_FIREBASE_API_KEY=your_api_key_here
VITE_FIREBASE_AUTH_DOMAIN=your_project_id.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your_project_id
VITE_FIREBASE_STORAGE_BUCKET=your_project_id.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
VITE_FIREBASE_APP_ID=your_web_app_id
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key
```

> **Note:** Do not commit `.env.local` to version control.

## Local Development

To run the app locally:

```bash
cd web
npm install
npm run dev
```

This will start the development server at `http://localhost:5173`.

## Deployment

To deploy the web app to Firebase Hosting:

1.  **Build the project**:
    ```bash
    cd web
    npm install
    npm run build
    ```

2.  **Deploy**:
    Run the following command from the root of the repository (or `web/` depending on your firebase.json location, usually root):
    ```bash
    firebase deploy --only hosting
    ```

    If you need to deploy Functions or Firestore rules as well:
    ```bash
    firebase deploy
    ```

## Verification

After deployment, visit the hosting URL (e.g., `https://your-project-id.web.app` or custom domain `app.damiennichols.com`).

1.  **Login**: Sign in with a Google account or Email associated with a Pro/Premium user.
2.  **Check Access**:
    *   If you are Premium/Pro, you should see the "Beacon Inbox" and other panels.
    *   If you are Free, you should see the "Upgrade to PulseLink Pro or Premium" message in the Beacon Inbox.
3.  **Test Sync**: Send a message from the web interface. It should appear in the "Beacon Inbox" and be sent via your phone (if the phone is online and has "Remote Web Access" enabled in Settings).

## Troubleshooting

*   **Map not loading**: Ensure `VITE_GOOGLE_MAPS_API_KEY` is set and has "Maps JavaScript API" enabled in Google Cloud Console.
*   **No messages**: Ensure the Android app has "Remote Web Access" enabled in Settings -> PulseLink Settings.
*   **"Premium Required" blocker**: Verify your user account has the correct subscription status in Firestore (`users/{uid}/subscriptionStatus` should be 'premium' or 'pro').

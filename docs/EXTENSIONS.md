# PulseLink Extensions Platform

PulseLink allows 3rd-party developers to extend the functionality of the messaging suite via web-based extensions. These extensions can run on both the Web Client and the Android App (via WebView).

## Manifest Format

Every extension must have a `manifest.json` file at its root.

```json
{
  "id": "com.example.myextension",
  "name": "My Extension",
  "version": "1.0.0",
  "description": "A description of what your extension does.",
  "author": "Your Name",
  "icon": "icon.png",
  "entry_point": "index.html",
  "permissions": ["read_user_profile"]
}
```

*   **id**: Unique identifier (reverse domain style).
*   **entry_point**: The HTML file to load (relative to manifest).

## JavaScript Bridge API

Extensions run in an isolated `iframe` (Web) or `WebView` (Android). Communication with the host app happens via the `postMessage` API.

### Sending Messages to Host

```javascript
window.parent.postMessage({
  type: "ACTION_TYPE",
  payload: { ... }
}, "*");
```

### Supported Actions

#### `EXTENSION_PING`
Checks if the host is alive.
```javascript
window.parent.postMessage({ type: "EXTENSION_PING" }, "*");
```

#### `GET_USER`
Request current user profile.
```javascript
window.parent.postMessage({ type: "GET_USER" }, "*");
```

### Receiving Messages from Host

Listen for the `message` event.

```javascript
window.addEventListener("message", (event) => {
  const { type, payload } = event.data;

  if (type === "HOST_PONG") {
    console.log("Host is alive!", payload.serverTime);
  }

  if (type === "USER_DATA") {
    console.log("User:", payload.uid, payload.displayName);
  }
});
```

## Development Workflow

1.  **Create**: Build your web app (HTML/JS/CSS).
2.  **Manifest**: Add `manifest.json`.
3.  **Host**: Serve your folder via a local server (e.g., `http://localhost:3000`).
4.  **Test**:
    *   Open PulseLink Web.
    *   Go to **Extensions**.
    *   Enable **3rd-Party Extensions**.
    *   Enter your manifest URL in **Developer Mode**.
    *   Click **Load**.

## Hosting & Submission

*   Extensions can be hosted anywhere (GitHub Pages, Firebase Hosting, etc.).
*   To publish to the official PulseLink Extensions Store, submit your extension URL via the Developer Console (Coming Soon).

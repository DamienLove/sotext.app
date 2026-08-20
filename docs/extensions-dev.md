# SoText Extensions Developer Sandbox

Use this sandbox to prototype third‑party extensions without publishing to the store.

- **Where**: Web app → Extensions → “Developer sandbox”.
- **What it does**: Stores extension definitions in the browser (localStorage) and can POST a sample payload (`event: ping`, `user`, `sampleMessage`) to your webhook URL.
- **How to add**: Enter a name, HTTPS endpoint, and short description, then click **Save extension**.
- **Test**: Click **Test** to send the sample payload and view the raw response. Extensions stay inactive until “Enable third‑party extensions” is toggled in Settings.
- **Publish**: Email `extensions@sotext.app` with your webhook URL, description, and expected request/response contract.

Recommended response schema:

```json
{
  "status": "ok",
  "message": "Optional human-readable text",
  "actions": [
    {
      "type": "toast",
      "text": "Shown in web client"
    }
  ]
}
```

Keep payloads lightweight (<256 KB) and respond within 10 seconds. Do not send secrets back to the client.

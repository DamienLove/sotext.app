# RingerSong local libs

This folder keeps only the dependencies the app actually needs:

- `spotify-app-remote-release-0.8.0.aar` – Spotify App Remote runtime AAR.

Removed to slim the repo:
- The original `android-sdk-0.8.0-appremote_v2.1.0-auth.zip`, sample apps, and documentation.

If you need to restore the full SDK (for docs or sample reference):
1. Download Spotify Android SDK App Remote 0.8.0 package manually from Spotify’s developer site.
2. Extract only `spotify-app-remote-release-0.8.0.aar` into this directory.
3. Keep the zip and sample content out of git to avoid repo bloat (already ignored).

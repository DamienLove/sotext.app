# Bolt's Journal

## 2024-05-23 - Missing Android SDK
**Learning:** The development environment does not have the Android SDK installed (`ANDROID_HOME` missing, `platform-tools` not found). This prevents running `./gradlew` tasks for linting and testing.
**Action:** For this session, I will proceed with the optimization as it is a standard, low-risk Compose best practice. Future tasks needing complex logic verification will require environment setup or CI reliance.

## 2024-05-23 - Flash Model for Intent Classification
**Learning:** For simple NLU tasks like intent classification (few classes, extracting simple entities), the `gemini15Pro` model is overkill and introduces unnecessary latency (2-3s). `gemini15Flash` is significantly faster (sub-second) and cheaper, while providing sufficient accuracy for this specific use case.
**Action:** Default to "Flash" or "Turbo" models for real-time user interactions, reserving "Pro" models for complex reasoning or generation tasks.

## 2024-05-24 - Memoization of List Filtering in Compose
**Learning:** In Jetpack Compose, filtering or sorting lists directly in the Composable function body (outside `remember` or `derivedStateOf`) causes O(N) operations on every recomposition. For large lists like SMS threads, this is a significant bottleneck.
**Action:** Always wrap list transformations (filter, sort, map) in `remember` keyed by the input list and filter parameters.

## 2024-05-25 - Frontend Verification requires Dummy Env
**Learning:** The web client's `firebase.js` throws an error (`auth/invalid-api-key`) immediately on load if `VITE_FIREBASE_API_KEY` is missing, causing the app to crash before rendering.
**Action:** When running verification scripts for the web client, always create a temporary `.env` file with dummy `VITE_FIREBASE_*` values to ensure the app handles the "Not Logged In" state correctly.

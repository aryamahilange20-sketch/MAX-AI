# MAX AI — Native Android starter project

A native Kotlin + Jetpack Compose Android app with a dark liquid-glass-inspired interface and a Gemini Live audio session.

## Current implementation
- Native Android app (no WebView)
- Compose liquid-glass visual design with animated audio bars
- Gemini API key entry and encrypted local preference storage
- Microphone permission request
- Gemini Live WebSocket connection using `gemini-3.8-live`
- 16 kHz PCM microphone input and 24 kHz PCM audio playback
- GitHub Actions workflow to build and upload a debug APK

## Build with GitHub Actions
1. Create a GitHub repository and upload this project.
2. Open **Actions**.
3. Run **Build MAX AI APK**, or push to `main` / `master`.
4. Download the `MAX-AI-debug-apk` artifact from the workflow run.

The workflow uses JDK 17 and Gradle 8.11.1. Android Gradle Plugin 8.10.1 is configured in the project.

## API key
Create a Gemini API key in Google AI Studio and enter it in the app. The app stores it locally using AndroidX Security Crypto encrypted preferences. A client-side app cannot make a key completely secret from a determined device owner or reverse engineer; for public distribution, use a backend that issues short-lived credentials or proxies requests securely. Never commit a real key to GitHub.

## Important limitations
- Gemini Live model access, free-tier eligibility, quotas, supported voice names and regional availability are controlled by Google and can change. Free use is not unlimited or guaranteed for every account.
- The current build is a voice-chat foundation, not a complete hands-free always-on assistant. Android background microphone use requires additional foreground-service design and platform disclosures.
- This first build does not yet implement phone-wide Accessibility automation, messaging, WhatsApp actions, wake-word detection, persistent memory, or background listening.
- Live audio interruption behavior depends on the current API/session implementation. Do not assume barge-in works in this starter.
- Test on a real Android device. Emulator microphone/audio routing may differ.

## Gemini API references
- Android Gemini Live guide: https://developer.android.com/ai/gemini/live
- Gemini API pricing and free-tier notes: https://ai.google.dev/gemini-api/docs/pricing

## Project details
- Application ID: `com.maxai.assistant`
- Minimum Android: API 26
- Target Android: API 35
- Language: Kotlin
- UI: Jetpack Compose

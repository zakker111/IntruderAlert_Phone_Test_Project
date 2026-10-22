# Intruder Alert (Android)

A personal Android utility for best-effort failed-unlock callback testing and for preparing a security report with evidence you capture in the app.

## Important limits

- Android/OxygenOS does **not** guarantee that an ordinary app receives every wrong lock-screen PIN/password event. The device-admin receiver uses a legacy callback and may receive no events on your OnePlus. The app cannot bypass this restriction.
- A callback can display a notification and log the event. Android background-launch and camera restrictions mean the app cannot reliably take a secret selfie or start microphone recording automatically while the phone is locked.
- Photo and audio recording are user-started while the app is open. Audio requires microphone permission. Stop the recording before attaching it.
- Location uses the latest cached location available to Android; it may be missing or old. Grant location permission and enable Location on the phone.
- Email is prepared in your installed email/share app. You must review it and tap **Send**. No email password or server credentials are stored by this app.
- This app never reads or saves the phone's PIN/password. Keep the app and evidence private.

## Features

- Optional device-admin failed-unlock callback test.
- Local count, latest event, and bounded event log.
- Notification when the legacy failed-unlock callback is delivered.
- User-started photo capture using the phone's camera app.
- User-started short audio recording.
- Cached location lookup and Google Maps URL.
- Email/share report with available photo/audio attachments.
- Email recipient saved locally on the phone.

## Build an APK using only your Android phone

This repository includes a GitHub Actions workflow that builds a **debug APK**. It has not been tested on every OnePlus/Android version.

1. Download the project ZIP and extract it in your phone's Files app.
2. In a browser, sign in to GitHub and create a repository named `IntruderAlert`. A private repository is fine.
3. Open the extracted `IntruderAlert` project folder. Upload the **contents inside that folder** to the repository root, including the `.github/workflows/build-apk.yml` file. Do not upload only the ZIP file. GitHub's mobile website can make folder uploads awkward; if it cannot upload folders, use GitHub's browser editor to create/upload the files while preserving the exact paths.
4. Open the repository's **Actions** tab. Select **Build Intruder Alert APK**. If GitHub asks, enable Actions for the repository. A push to `main` should start the build automatically; otherwise choose **Run workflow** if shown.
5. Open the completed workflow run and find **Artifacts** near the bottom. Download `intruder-alert-debug-apk` and extract that artifact ZIP to obtain `app-debug.apk`.
6. Tap `app-debug.apk` in Files. If Android blocks it, open the prompted settings and allow your browser or Files app to **Install unknown apps**, then retry. Only install APKs you built from source you trust.
7. Open Intruder Alert, save your alert email address, grant location/microphone permissions when requested, and enable the optional device-admin event test.
8. Test with your own phone: lock it, enter one wrong PIN, unlock normally, then open the app and refresh the event log. If no event appears, your OnePlus/Android version did not deliver the callback; the app cannot force it to.

## Development

- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Java 17.
- `minSdk = 26`, `targetSdk = 28` for this proof-of-concept.
- Build command in a configured Android environment: `gradle :app:assembleDebug`.

For a phone-only walkthrough using Termux and GitHub Actions, see [`PHONE_SETUP_GUIDE.md`](PHONE_SETUP_GUIDE.md).

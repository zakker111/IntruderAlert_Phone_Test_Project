# Intruder Alert — phone-only build and install guide

This repository builds a debug APK with GitHub Actions. The APK is not included in this source ZIP; GitHub must build it first.

## A. Create an empty GitHub repository

1. Open https://github.com/new in your phone browser and sign in.
2. Repository name: `IntruderAlert`.
3. Choose Private or Public.
4. Leave **Add a README**, `.gitignore`, and license unchecked so the repository is empty.
5. Tap **Create repository**. Keep the page open to copy the repository URL.

## B. Upload from Android using Termux

Uploading a whole project folder through a mobile browser can be difficult. Termux lets you push the extracted folder directly.

1. Install Termux from its official F-Droid page: https://f-droid.org/packages/com.termux/ . Avoid unofficial APK download sites.
2. In your Android Files app, extract `IntruderAlert_Updated_Project.zip`. The folder should be named `IntruderAlert` and contain `settings.gradle.kts`, `app/`, and `.github/`.
3. Open Termux and run:

```sh
pkg update
pkg install git
termux-setup-storage
```

4. Allow storage access when Android asks. If you extracted the folder into Downloads, run:

```sh
cd ~/storage/downloads/IntruderAlert
git init -b main
git add .
git commit -m "Initial Intruder Alert project"
```

If `cd` says the folder does not exist, open your Files app and check the exact folder name/location. Change the path accordingly.

5. Add your GitHub repository as the remote (replace `YOUR-USERNAME` with your GitHub username):

```sh
git remote add origin https://github.com/YOUR-USERNAME/IntruderAlert.git
git push -u origin main
```

6. When Git asks for a password, use a GitHub personal access token, not your GitHub account password. Create one at https://github.com/settings/tokens . A classic token needs the `repo` scope for a private repository. Treat the token like a password; do not share it. If the push fails, read the exact error before trying again.

## C. Build the APK on GitHub

1. Open the repository on GitHub and tap **Actions**.
2. If prompted, enable GitHub Actions for the repository.
3. A push to `main` should start **Build Intruder Alert APK**. Open the newest run.
4. Wait for the run to finish. A green check means the workflow succeeded; a red cross means it failed. Open the failed job and read the first error before retrying.
5. In the successful run, find **Artifacts** and download `intruder-alert-debug-apk`. This is an artifact ZIP, separate from the source ZIP.

## D. Install and test

1. Extract the downloaded artifact ZIP in Files. Find `app-debug.apk`.
2. Tap it. If Android blocks installation, use the prompt to allow this browser or Files app to install unknown apps, then retry.
3. Open Intruder Alert. Save your alert email, grant permissions only if you want those features, and use **Enable lock-screen event test** to open Android's device-admin confirmation screen.
4. Test the phone yourself: lock it, enter one incorrect PIN, unlock normally, open the app, and tap **Refresh event log**. If no callback appears, the phone's Android/OxygenOS version did not deliver it. This app cannot force Android to provide that event.
5. Test **Take photo**, **Record short audio clip**, **Check location and map link**, and **Prepare email alert with available evidence** separately while the app is open. Review the email and tap Send yourself.

## What this proof of concept cannot guarantee

- It cannot reliably detect every incorrect system lock-screen attempt on modern Android/OnePlus.
- It does not secretly activate the camera or microphone while the phone is locked.
- Location may be missing or stale if Android has no recent cached fix.
- Email is opened as a draft; you must review and send it.
- A successful GitHub build confirms compilation, not that every feature works on your particular phone.

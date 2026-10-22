# Implementation status

## Included in this version
- [x] Android app and launcher screen
- [x] Optional legacy device-admin failed-unlock callback test
- [x] Local callback log
- [x] Notification when a callback is delivered
- [x] Manual last-known location lookup and map link
- [x] Manual camera capture using the system camera app
- [x] Email/share draft with report text and optional photo attachment
- [x] GitHub Actions workflow to build and upload a debug APK

## Known constraints
- Android/OEM behavior may prevent the failed-unlock callback from being delivered. The app cannot work around this restriction.
- No covert camera or microphone capture, no background location tracking, no password collection.
- Email requires a mail/share app and a user-confirmed send.
- Location is last-known only and may be stale.
- Not compiled or device-tested in the current environment.

## Before relying on it
1. Build via GitHub Actions.
2. Install on the target OnePlus phone.
3. Activate the optional device-admin receiver.
4. Enter one wrong lock-screen PIN, unlock normally, and check the local event log.
5. If no callback is logged, this device/build does not support the trigger needed for automatic alerting through this approach.

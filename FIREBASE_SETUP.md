# Firebase Setup (Analytics + Crashlytics)

## Current Project (Created)
- Date: `2026-02-23`
- Firebase Project ID: `focusfarm-app-20260223`
- Android App ID: `1:225099548775:android:3f5300c144a424858eecc2`
- Package: `com.focusfarm.app`
- Config file: `app/google-services.json`

## 1) Add app to Firebase
1. Open Firebase Console and create/select your project.
2. Add Android app with package name: `com.focusfarm.app`.
3. Download `google-services.json`.
4. Place it at: `app/google-services.json`.

## 2) Enable products
1. Enable Google Analytics in Firebase.
2. Enable Crashlytics (and complete onboarding in console).

## 3) Build and run
1. Run: `.\gradlew.bat :app:assembleDebug`
2. Open app and trigger flows.

## Tracked events
- `session_start`
- `session_complete`
- `session_abandon`
- `paywall_view`
- `purchase_start`
- `purchase_success`
- `rewarded_ad_shown`
- `rewarded_ad_reward_granted`

## Notes
- If `google-services.json` is missing, app still builds.
- In that case telemetry falls back to no-op/log mode and does not send Firebase data.

## Live Verification (Completed)
- Date: `2026-02-23`
- Device: `F3R7N19110001958`
- Verified in logcat:
  - `app_open` events logged by Firebase Analytics (`FA-SVC`).
  - non-fatal test persisted by Crashlytics (`Persisting non-fatal event`).
  - report upload confirmed (`Sending report through Google DataTransport` and `report successfully enqueued`).

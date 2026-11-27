# FocusFarm Play Store Data Safety Draft

Last reviewed: 2026-06-16

This file maps the current app behavior to the Google Play Data safety form. Keep it in sync with code, SDKs, privacy policy, and Play Console declarations.

## App Scope

- Package: `com.focusfarm.app`
- Current release identity: `versionCode=4`, `versionName=1.0.3`
- Audience assumption: not designed for children and should not be declared as child-directed.
- Payments assumption: premium digital features are sold only through Google Play Billing.

## Collection And Sharing Summary

- The app collects or shares user data: Yes.
- Data is encrypted in transit: Yes, network security config disallows cleartext traffic and Google services use HTTPS.
- Users can request deletion: Yes, via in-app account/data deletion and `biyokimya58@gmail.com`.
- Data collection is required for core functionality in some cases: Yes, for purchases, premium restore, referral rewards, crash diagnostics, analytics, and ads.

## Data Types To Declare

| Play data category | Data type | Collected | Shared | Purpose |
| --- | --- | --- | --- | --- |
| App activity | App interactions / in-app actions | Yes | Yes, with Firebase/Google service providers | Analytics, app functionality, product improvement |
| App info and performance | Crash logs, diagnostics, performance data | Yes | Yes, with Firebase Crashlytics/Google | Crash reporting, diagnostics |
| Device or other IDs | Advertising ID and app instance identifiers | Yes | Yes, with AdMob/Firebase/Google | Advertising, analytics, fraud prevention, app functionality |
| Purchases | Purchase history / subscription status | Yes | Yes, with Google Play Billing | App functionality, premium entitlement, restore purchases |
| User IDs | Anonymous Firebase/referral identifier | Yes | Yes, with Firebase | App functionality, referral verification, abuse prevention |

## Data Types Not Currently Declared From Code

- Location: No location permission or location collection found.
- Contacts: No contacts permission or contacts collection found.
- Photos, videos, audio, files: No broad media permission found. Share images are generated locally and shared only when the user invokes sharing.
- Health, fitness, financial info, messages, calendar, call logs: No collection found.
- Personal info such as name, email, phone number, address: No direct input or collection found in the app code.

## SDK And Feature Evidence

- Firebase Analytics: `FirebaseAnalytics.getInstance(context)` and app events such as `app_open`.
- Firebase Crashlytics: `FirebaseCrashlytics.getInstance()` and non-fatal exception logging.
- Firebase Auth / Functions: anonymous auth and referral verification.
- Google AdMob / UMP: banner/rewarded ads, Advertising ID permission, consent form and privacy options.
- Google Play Billing: subscriptions and lifetime product purchase/restore.
- Notifications: `POST_NOTIFICATIONS` is requested for focus reminders; notification permission state is device-level and not a marketing contact channel.

## Play Console Notes

- Do not mark "No data collected".
- Do not declare the app as child-directed unless ad, Advertising ID, and Families requirements are redesigned.
- Match the privacy policy URL to `https://canburakyol.github.io/focusfarm-privacy/`.
- If future account login, email capture, cloud profile, or push token sync is added, update this file and the Data safety form before release.

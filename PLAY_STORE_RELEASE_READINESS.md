# FocusFarm Play Store Release Readiness

Last checked: 2026-06-19

## Decision

Conditionally ready for the next Play Console step after the new upload key becomes active.

The app should not be submitted or published yet without explicit approval, because Play Console still has pending changes and AdMob could not be verified from the signed-in console state.

## Local Gates

- `.\gradlew.bat lint`: pass
- `.\gradlew.bat test`: pass
- `.\gradlew.bat :app:bundleRelease`: pass
- `node -c functions\index.js`: pass

Release bundle:

- Path: `D:\FocusFarm\app\build\outputs\bundle\release\app-release.aab`
- Package: `com.focusfarm.app`
- Version: `versionCode=4`, `versionName=1.0.3`
- Upload certificate SHA1: `48:E1:9E:AC:42:28:9B:CC:37:D1:D7:33:5F:3A:77:C3:99:EF:A8:4A`

The SHA1 matches the new upload certificate in the Play Console reset email. That key is scheduled to become valid on 2026-06-18 06:57 UTC.

## Code And Policy Surface

- Settings now exposes privacy preferences through UMP.
- Settings now links to the hosted privacy policy.
- Settings now exposes account/data deletion through the existing auth deletion flow.
- `PLAY_STORE_DATA_SAFETY.md` is aligned with Firebase Analytics, Crashlytics, AdMob/AD_ID, UMP, Billing, anonymous auth/referrals, and notification permission behavior.
- Hosted privacy policy is reachable at `https://canburakyol.github.io/focusfarm-privacy/`.
- Hosted `app-ads.txt` is reachable at `https://canburakyol.github.io/app-ads.txt`.

## Play Console Read-Only Evidence

- App package: `com.focusfarm.app`
- Dashboard status: not yet sent for review
- Publishing overview: pending changes exist and the review submission button is available
- Data Safety row: form is filled
- Store listing: live default listing exists
- Billing products:
  - `premium_monthly`: subscription found with active base plan
  - `premium_yearly`: subscription found with active base plan
  - `premium_lifetime`: one-time product found with active purchase option

## AdMob Read-Only Evidence

- AdMob app found: `FocusFarm` / Android
- AdMob app ID: `ca-app-pub-3074026744164717~3241143217`
- Banner ad unit found: `ca-app-pub-3074026744164717/8709092317`
- Rewarded ad unit found: `ca-app-pub-3074026744164717/3456765631`
- App code uses the same app ID and ad unit IDs.
- FocusFarm app store details are empty in AdMob.
- FocusFarm AdMob approval status shows `İnceleme gerekli`.
- FocusFarm app verification shows `Zorunlu değil`.
- European regulations privacy messaging now has a published `FocusFarm GDPR message` assigned to `FocusFarm`.
- The FocusFarm GDPR message uses English as the default language and enables `Consent`, `Manage options`, and `Do not consent`.
- AdMob noted that the message may take up to 1 hour to appear in the app.
- Hosted `app-ads.txt` is reachable and contains publisher ID `pub-3074026744164717`.

## Remaining External Checks

- FocusFarm AdMob app store details should be linked once the Play listing is available for selection.
- FocusFarm AdMob approval remains pending until AdMob review completes.
- During the UMP setup, the privacy policy URL was also accidentally added to the unselected `com.varlik.finans.app` row in AdMob's app picker. It was not selected for the FocusFarm message, but it should be cleaned up manually if AdMob persists it outside this wizard.
- App Content policy details beyond the publishing overview could not be opened directly; only the Data Safety row was verified from publishing overview.
- Play Console submit, AAB upload, store listing publish, AdMob changes, and Billing changes still require explicit owner approval.

## Next Action

After 2026-06-18 06:57 UTC, upload the rebuilt AAB and review Play Console warnings before pressing any submit/review button.

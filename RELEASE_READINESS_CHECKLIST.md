# Release Readiness Checklist

## Required before production launch

1. Replace AdMob test IDs in `app/src/main/res/values/strings.xml`:
   - `admob_app_id`
   - `admob_rewarded_unit_id`
   - `admob_banner_unit_id`
2. Register app and ad units in AdMob, then link to Play package.
3. Publish `app-ads.txt` on your developer domain and add the domain in Play Console listing.
   - Current live URL: `https://canburakyol.github.io/app-ads.txt`
4. Configure UMP consent flow and publish an EEA/UK/CH message in AdMob Privacy & messaging.
5. Validate all ad placements with test ads:
   - Home banner
   - Session revive rewarded
   - Daily quest reroll rewarded
   - Next-session 2x seed rewarded
6. Verify Data safety form and privacy policy links in Play Console match current SDK usage.

## RevenueCat readiness (when keys are provided)

1. Create products/entitlements in RevenueCat dashboard.
2. Add Android public SDK key and initialize Purchases SDK once at app startup.
3. Map offerings/packages to paywall UI.
4. Gate premium features by entitlement state (not only local billing state).
5. Configure server notifications/webhooks and validate restore/purchase/cancel flows.

## Firebase readiness

1. Confirm production Firebase project package name and SHA certificates.
2. Replace `app/google-services.json` with production config.
3. Verify Analytics and Crashlytics events on internal test track.

## Test pass before rollout

1. Free user: all rewarded and banner flows.
2. Premium user: no banner/rewarded prompts on home; revive works without ad.
3. Offline/poor network: ad not ready states and fallback messages.
4. Session lifecycle:
   - background timeout kill
   - one-time revive
   - second kill cannot revive again in same session
5. Economy:
   - quest claim
   - rerolled quest reward
   - session seed reward
   - next-session 2x seed consumed once

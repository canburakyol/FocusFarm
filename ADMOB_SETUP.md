# AdMob Setup

## Current integration
- SDK: `com.google.android.gms:play-services-ads`
- Initialization: `app/src/main/java/com/focusfarm/app/FocusFarmApp.kt`
- Rewarded manager: `app/src/main/java/com/focusfarm/app/ads/RewardedAdManager.kt`
- Rewarded placements:
  - `session_revive` (killed ekrani)
  - `daily_quest_reroll` (gunluk gorev yenileme)
  - `next_session_seed_boost` (sonraki basarili oturum 2x seed)
- Banner placement:
  - Home ekraninda free kullaniciya banner

## Replace test IDs before production
Update these in `app/src/main/res/values/strings.xml`:
- `admob_app_id`
- `admob_rewarded_unit_id`
- `admob_banner_unit_id`

Current values are Google test IDs.

## Reward mechanics implemented
- Killed ekrani: reklam izle -> oturumu 1 kez revive
- Gunluk gorev: reklam izle -> bugun 1 kez gorev reroll
- Bonus: reklam izle -> sonraki basarili oturum seed odulu 2x

## Premium behavior
- Premium kullanicida home banner ve rewarded bonus karti gosterilmez.

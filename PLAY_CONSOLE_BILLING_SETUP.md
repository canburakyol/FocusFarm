# Play Console Billing Setup (FocusFarm)

## Product IDs (must match code)
- `premium_monthly` (subscription)
- `premium_yearly` (subscription)
- `premium_lifetime` (in-app product, managed)

## Required Steps
1. In Play Console, open `Monetize > Products > Subscriptions`.
2. Create `premium_monthly` and `premium_yearly`.
3. Add at least one base plan for each subscription.
4. Set each base plan to active.
5. Open `Monetize > Products > In-app products`.
6. Create `premium_lifetime` as managed product.
7. Activate all products before testing purchase flow.

## Test Setup
1. Add license testers in Play Console.
2. Upload an internal/closed test build signed with release key.
3. Install from Play test track (not Android Studio install) to test billing reliably.
4. Use tester account on device Play Store.

## Current App Mapping
- Billing plans are defined in:
  `app/src/main/java/com/focusfarm/app/data/billing/PremiumPlan.kt`
- Billing flow implementation is in:
  `app/src/main/java/com/focusfarm/app/data/billing/PremiumBillingManager.kt`
- Shop UI purchase entry point is in:
  `app/src/main/java/com/focusfarm/app/ui/screens/shop/ShopScreen.kt`

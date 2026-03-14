# Technical Concerns - FocusFarm

## Technical Debt
- **Syncing with Web**: Ensuring features developed in the main project are consistently ported to the Android version as per user rules ("Andorid uygulamalar düzenlenirken, webde yapılan değişiklikler olduğu gibi andorid kısmına da aktar").

## Stability
- **Billing Integration**: Complexity of handling Google Play Billing states and potential edge cases in subscription management.
- **AdMob**: Ensuring ad lifecycle management (Native ads, etc.) doesn't impact app performance.

## Future Scaling
- **Migration to Compose Multiplatform**: Potential future move as the core is already Compose-based.
- **Large DB handling**: Room performance monitoring for long-term user history storage.

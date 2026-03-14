# FocusFarm 30-Day Launch + Revenue Roadmap

## Goal
- Publish FocusFarm on Google Play.
- Generate first revenue within 30 days from launch.

## Success Metrics (Day 30)
- Crash-free users: >= 99.5%
- ANR rate: < 0.5%
- Day-1 retention: >= 30%
- Day-7 retention: >= 12%
- Premium conversion: >= 1.5%
- Rewarded ad opt-in: >= 20%
- First-month gross revenue: > 0 USD (target: 100-500 USD)

## Phase 1 (Days 1-7): Release Readiness
- Upgrade Android target to API 35.
- Finalize release config (AAB, signing, ProGuard, versioning).
- Add analytics and crash monitoring.
- Prepare Play Console assets:
  - privacy policy
  - data safety form
  - content rating
  - screenshots and icon variants
- Start closed testing process and recruit testers.

## Phase 2 (Days 8-14): Monetization MVP
- Add Google Play Billing:
  - Premium Monthly
  - Premium Yearly
  - Lifetime
- Implement paywall in app flow.
- Gate premium plants/features by entitlement.
- Add rewarded ads for optional boosts:
  - revive plant once
  - bonus growth reward

## Phase 3 (Days 15-21): Retention Systems
- Daily streak system.
- Daily missions (simple, repeatable).
- Weekly challenge summary.
- Notification strategy:
  - session reminder
  - streak warning
  - comeback prompt

## Phase 4 (Days 22-30): Soft Launch + Optimization
- Stage rollout gradually.
- Run store listing experiments (icon/title/screenshots).
- Tune paywall copy and plan pricing from funnel data.
- Prioritize top crash and ANR fixes weekly.

## Technical Backlog (Started)
- [x] SDK target upgrade planning
- [x] Database index + migration planning
- [x] UI list performance pass planning
- [x] Timer reliability refactor planning
- [ ] Billing integration
- [ ] Ads integration
- [ ] Analytics event schema + dashboard

## Event Instrumentation (Minimum)
- `session_start`
- `session_complete`
- `session_abandon`
- `paywall_view`
- `purchase_start`
- `purchase_success`
- `rewarded_ad_shown`
- `rewarded_ad_reward_granted`

## Risk Controls
- Do not use intrusive ad placements in core timer flow.
- Keep premium promise clear and measurable.
- Avoid data-loss in timer/session states.
- Keep release cadence weekly after launch.

# Fitness App — UI Design Addendum
**Companion to:** `fitness-app-prompt.md`
**Data stack (unchanged):** Fitbit Air → Fitbit app → Health Connect → this app

## Design direction in one sentence
Samsung Health's information architecture and daily-dashboard patterns, rendered in Apple's Liquid Glass visual language (iOS 26 style) — translucent layered surfaces, real-time blur, depth, and light — built natively for Android with Jetpack Compose.

## 1. Borrowed from Samsung Health (structure & patterns)
### Home dashboard
- Vertically scrolling card stack; one card per metric domain, each tappable into a detail screen.
- Hero summary at top: steps, active time, calories as three concentric rings; fill animates on open.
- Card order: Daily Activity, Sleep (duration + score + mini hypnogram), Heart Rate (resting + 24h sparkline), Workouts (this week), Weight trend (priority metric), Readiness score.
- Time-aware greeting header with date line.

### Detail screens
- Sleep: full hypnogram (Awake/REM/Light/Deep bands), stage % breakdown, weekly consistency.
- Heart rate: 24h min–max band chart with resting HR line, weekly resting trend.
- Steps: hourly bars today, 7/30-day toggle.
- Workouts: session log with per-session detail, tagged to the home program.
- Weight: trend line with 7-day moving average as hero (raw points muted), goal band shading.

### Interaction
- Pull-to-refresh re-reads Health Connect. Scrub-to-inspect on charts. Week/Month/6-Month segmented control on trends.

## 2. Liquid Glass visual language
- Layered translucency: frosted glass cards over a soft animated mesh-gradient background (2–3 blurred color blobs drifting slowly; freeze under reduced motion).
- Backdrop blur via Haze (`dev.chrisbanes.haze`); fall back to semi-opaque below Android 12.
- Glass recipe: blur 24–36dp, white 6–12% tint, 1dp border brighter at top edge, soft ambient shadow.
- Concentric rounding: card 28dp, inner elements 16–20dp.
- Springy 0.97 scale-on-press. Floating glass pill bottom nav (Home / Trends / Workouts / Profile) — the signature element. Edge-to-edge, content scrolls behind bars.

### Tokens (dark-first)
- bg-base #0B0E14; blobs #2E5CFF / #7C3AED / #0EA5A4 (background only, heavily blurred)
- glass tint rgba(255,255,255,0.08); text #F5F7FA / 62% secondary
- Accents: Activity #34D399 · Sleep #818CF8 · Heart #FB7185 · Weight #FBBF24
- Numerals: tabular figures, 600–700 weight, big-numeral + small-unit pattern. ALL-CAPS eyebrows 11sp +0.8 tracking.
- Charts: rounded caps, gradient fills fading to transparent, hairline white-8% grids.
- Hypnogram: Awake #FDA4AF · REM #93C5FD · Light #A5B4FC · Deep #6366F1.

## 3. In-app computed scores
- Sleep Score (0–100): duration vs target 40%, deep+REM share 30%, efficiency 20%, schedule consistency vs 14-day median 10%.
- Readiness (0–100): resting HR vs 30-day baseline, last night's sleep score, yesterday's training load. Glass gauge + plain-language verdict.
- Persist daily scores in Room so trends survive Health Connect retention.

## 4. Quality floor
- Two-column card grid ≥600dp. Touch targets ≥48dp. Charts get content descriptions.
- Reduced motion: freeze mesh, drop overshoot. Limit simultaneous blur surfaces; profile on device.

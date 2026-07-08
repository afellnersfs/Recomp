# Fitness App — Original Product Spec ("Recomp")

Personal native Android app (Kotlin + Jetpack Compose, Material 3). No accounts, no backend; Room for local storage. Owner: Arie — returning lifter, goal is body recomposition, training 2–3 evenings/week, 30-minute sessions that can scale up.

**Equipment (workouts must only use these):** Smith machine, barbell bench press with plates, dumbbells up to ~15 lb. Equipment editable in settings (possible free barbell later).

## Core features
1. **Guided workouts** — pre-built full-body A/B rotation (A/B/A, B/A/B) for recomp. Big glanceable workout screen: exercise, sets, reps, target weight. Video-first guidance: one curated YouTube demo per exercise (tap name or play button), plus fallback search link. Max 3–4 one-line form cues + 2–3 common mistakes. Tap-to-log sets, auto-suggested progressive overload, auto-start rest timer (60–90 s adjustable) with vibration + sound, end-of-workout summary (volume, time, PRs).
2. **Progress tracking** — strength per exercise, weekly volume, body-weight trend charts; PR board; weekly consistency streak.
3. **Health Connect (Fitbit)** — read steps, HR, resting HR, sleep, calories, exercise sessions. Home dashboard with today's numbers; recovery hint after poor sleep; proper permission flow + privacy dialog.
4. **Nutrition & water** — daily calorie target with quick number+label logging (no food database); one-tap water with progress ring.
5. **Notifications** — workout reminder on training evenings (configurable), rest-timer alerts, optional water reminder (off by default). **Shabbat mode:** silence everything Friday evening → Saturday night weekly; sunset-based if feasible, configurable fallback.
6. **Home screen widgets (Glance)** — water +1 tap, steps & calories, streak & latest PRs.
7. **Body tracking** — weight/waist/chest/arms (editable list) with trends; private progress photos with side-by-side compare.
8. **Warm-up & stretching** — 5-min guided warm-up before workouts; optional cool-down.
9. **Backup** — local-first with automatic Google Drive backup (survives lost phone), restore flow, manual export.

## Design
Clean, fresh, modern; dark; bold numerals; huge tap targets; 1–2 tap logging; today's plan front and center; time-aware greeting.
*(Visuals superseded by `fitness-app-ui-addendum.md` — Liquid Glass redesign.)*

## Technical
Kotlin + Compose + Material 3, Room, Health Connect SDK with manifest declarations + privacy dialog, min SDK 28. Built in the cloud via GitHub Actions (`afellnersfs/Recomp`), APK published to the `latest` release; in-app update banner checks that release.

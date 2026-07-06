# Recomp — personal fitness app

A native Android app (Kotlin + Jetpack Compose, Material 3) built for one person:
returning to lifting, training 2–3 evenings a week, 30-minute full-body sessions,
goal = body recomposition. No accounts, no backend — everything lives on the phone
in a Room database.

## What's inside

| Area | Details |
|---|---|
| Program | Full-body A/B rotation (A/B/A one week, B/A/B the next). 30 min = 4 exercises × 3 sets; bumping session length in Settings adds accessories (40 min = 5, 50+ = 6). Smith machine, bench-press barbell, and light dumbbells only. |
| Guided workouts | Big glanceable screen per exercise: curated YouTube demo video (one tap on the orange play button), 3–4 form cues, 2–3 common mistakes, suggested weight × reps. Log a set in one tap; rest timer auto-starts (default 75 s, adjustable) with vibration + beep. |
| Progression | Double progression: hit the top of the rep range on all sets → weight goes up next time; miss the bottom → consolidate. Dumbbell moves cap at your heaviest dumbbell (editable in Settings) and switch to rep progression. |
| Progress | Estimated-1RM line chart per exercise, weekly volume bars, body-weight trend, PR board, weekly streak. |
| Health Connect | Reads steps, calories, sleep, resting HR (your Fitbit syncs into Health Connect). Home dashboard + "short night → go lighter" recovery hint. |
| Nutrition | Calorie target with quick number+label logging; one-tap water with progress ring. |
| Widgets (Glance) | Water +1 tap widget, steps & calories widget, streak & latest-PRs widget. |
| Notifications | Evening workout reminder on training days, optional water reminders. **Shabbat mode**: everything silenced Friday evening → Saturday night, weekly, using local sunset (candle lighting −18 min / havdalah +42 min) when you enter coordinates, or fixed times otherwise. |
| Body tracking | Weight/waist/chest/arms (+ custom) with trend charts; progress photos in private app storage with side-by-side compare. |
| Warm-up / cool-down | 5-minute guided dynamic warm-up before each workout, optional stretch routine after — both timed step-by-step with a video link. |
| Backup | Android Auto Backup ships the database to your Google Drive automatically (survives a lost phone; restores on setup of a new one). Manual JSON export/restore in Settings as well. Progress photos are excluded from cloud backup (size), but transfer with device-to-device migration. |

## Building it (first time)

You don't have Java or the Android SDK on this PC yet — Android Studio brings both.

1. **Install Android Studio** from https://developer.android.com/studio (default options are fine).
2. **Open the project**: File → Open → select this `fitness-app` folder. Let Gradle sync finish
   (first sync downloads dependencies; give it a few minutes).
3. If sync complains about a dependency version (most likely
   `androidx.health.connect:connect-client:1.1.0-rc02`), open `app/build.gradle.kts`,
   put the cursor on the version and use Android Studio's suggestion to bump to the
   newest available — no code changes should be needed.
4. **Build the APK**: menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
   The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.
   (Command line alternative: `.\gradlew assembleDebug` from this folder.)

## Installing on your Samsung

**Enable Developer mode (one time):**
1. Settings → **About phone** → **Software information**.
2. Tap **Build number** seven times → "Developer mode has been turned on".
3. Settings → **Developer options** → turn on **USB debugging**.

**Easiest install:** plug the phone into the PC with USB, tap **Allow** on the
phone's USB-debugging prompt, then press the green **Run ▶** button in Android
Studio with your phone selected as the target. Done — the app is on your phone.

**Alternative (no cable):** copy `app-debug.apk` to the phone (Drive, email, etc.),
open it in My Files, and allow "Install unknown apps" when prompted.

## Connecting your Fitbit data

1. On Android 14+ Health Connect is built in; on 13 install **Health Connect** from the Play Store.
2. In the **Fitbit app**: Profile → **Health Connect** → turn on syncing.
3. Open Recomp → it asks for Health Connect read permissions on first launch
   (or Settings → Health Connect → Connect). Grant steps, sleep, heart rate, calories.

## First-run checklist

- Settings → training days (defaults: Sun/Tue/Thu) and reminder time (18:30).
- Settings → Shabbat mode: enter your latitude/longitude for sunset-based times,
  or adjust the fixed Friday/Saturday times.
- Add the widgets: long-press the home screen → Widgets → Recomp.
- First workout: the app suggests conservative starting weights — adjust the
  steppers to what feels like ~3 reps left in the tank and log it; progression
  takes over from there.

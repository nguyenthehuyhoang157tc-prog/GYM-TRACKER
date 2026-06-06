# GYMATE

Local-first Android gym trainer for progressive overload and aesthetic bodybuilding.
(Formerly "Gym Tracker". The Android package, classes, and database remain `com.hoctin.gymtracker`
— only the visible product branding changed.)

## Verified Status

Verified on Windows on June 3-4, 2026:

- Android Studio: installed at `C:\Program Files\Android\Android Studio`.
- JDK: Android Studio JBR 21 at `C:\Program Files\Android\Android Studio\jbr`.
- Android SDK: installed at `C:\Users\hoang\AppData\Local\Android\Sdk`.
- Emulator: Pixel 8 AVD running API 37.
- Debug build: successful.
- Debug install: successful on `emulator-5554`.
- Runtime QA: Home, Plan, History, Settings, Quick Start, add exercise, add set, save workout, auto-advance to the next workout day, previous performance, PR display, export JSON, theme mode, and persistence after force-stop/relaunch verified.
- Crash buffer: no app crash entries after QA.

Debug APK:

```text
D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk
```

## GYMATE Branding & Hero Artwork (patch notes)

Visual/branding patch — no business logic, Room schema, save/draft/auto-advance, or progressive-overload
logic was changed. Verified on the Pixel 8 (API 37) emulator in dark mode.

What changed:

- **Name:** App label and the Home wordmark now read **GYMATE** with the **TRAINER** subtitle.
  - `res/values/strings.xml` → `app_name` = `GYMATE`.
  - `ui/home/HomeScreen.kt` → `BrandHeader` text = `GYMATE`.
  - Internal package/classes/tables are unchanged (`com.hoctin.gymtracker`).
- **Launcher icon:** Adaptive icon uses the final GYMATE mark `res/drawable-nodpi/gymate_icon.png`
  (1254×1254, white mascot + dumbbells on black) as the adaptive `foreground`, over a pure-black
  `background` (`res/values/colors.xml` → `launcher_background = #000000`). The mark is centered well
  within the adaptive safe zone, so it survives circle/squircle masks and reads on light or dark
  launchers. Adaptive XML: `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
- **Home hero artwork:** The hero card shows workout-specific art behind the text on the right side, with
  a left-to-right scrim so the title, badges, and Quick Start stay readable. Mapping is keyword-based and
  case-insensitive (`heroResForDay` in `HomeScreen.kt`):
  - Push → `hero_push`, Pull → `hero_pull`, Legs → `hero_legs`, Full Body → `hero_full_body`.
  - Any other / unknown day name falls back to `hero_default` (a lightweight purple dumbbell **vector**,
    kept at `res/drawable/hero_default.xml`). The mapping always returns a valid drawable, so a missing
    image can never crash the card.
- **Final hero assets:** The four day heroes ship as bitmaps in `res/drawable-nodpi/`
  (`hero_push.jpg`, `hero_pull.jpg`, `hero_legs.jpg`, `hero_full_body.jpg`, ~1535×1024 / 1672×941).
  Only the unknown-day fallback remains a vector.
- **June 5, 2026 hero refresh:** Corrected day hero artwork was installed as the four
  `hero_*.jpg` files. The previous old JPEGs were moved to `_asset_backup_hero_old/`, and the
  provided PNG sources are preserved outside Android resources in `_asset_sources_hero_new/`.
  The Home hero image keeps `ContentScale.Crop` with a slight overscale inside the rounded clip so
  the supplied artwork's white outer canvas cannot appear as a corner/frame.
- **Performance:** One hero is decoded per selected day via `painterResource` (cached by resource id, no
  runtime image processing). Home scroll was re-verified smooth with the bitmaps in place.

### Replacing the hero illustrations later (drop-in)

To swap artwork, replace files by name — no code change required (`R.drawable.hero_*` resolves to whichever
single file exists, bitmap or vector):

1. For a day hero, drop a same-named bitmap into `app/src/main/res/drawable-nodpi/`
   (e.g. `hero_push.jpg` / `.png` / `.webp`). Names: `hero_push`, `hero_pull`, `hero_legs`, `hero_full_body`.
2. Keep exactly one file per resource name (don't have both `hero_push.xml` and `hero_push.jpg`).
3. Recommended: landscape ~1.5–1.7:1 (the card crops to fit), subject on the right with a darker/empty left
   side so it integrates with the purple gradient + scrim.

## Windows Setup

For a fresh Windows machine, install the required tools:

```powershell
winget install --id Google.AndroidStudio -e
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

Open Android Studio once, then install these SDK components from **Tools > SDK Manager**:

- Android SDK Platform 35
- Android SDK Build-Tools
- Android SDK Platform-Tools
- Android Emulator
- Android SDK Command-line Tools

Create an emulator from **Tools > Device Manager**, for example Pixel 8 with API 35 or newer.

Set environment variables in a new PowerShell session:

```powershell
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" |
  Sort-Object Name -Descending |
  Select-Object -First 1 -ExpandProperty FullName
setx JAVA_HOME "$jdk"
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx ANDROID_SDK_ROOT "$env:LOCALAPPDATA\Android\Sdk"
setx PATH "$env:PATH;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:LOCALAPPDATA\Android\Sdk\emulator;$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin"
```

Verify setup:

```powershell
java -version
adb version
emulator -list-avds
```

This project also builds with Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

## Open In Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select this folder: `D:\HOCTIN\GYM TRACKER`.
4. Let Gradle sync finish.
5. If Android Studio asks to install missing SDK packages, accept.

## Run On A Phone With USB

1. Enable Developer Options on the phone.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Accept the RSA debugging prompt on the phone.
5. Verify the device:

```powershell
adb devices
```

6. Run from Android Studio, or use:

```powershell
.\gradlew.bat :app:installDebug
adb shell monkey -p com.hoctin.gymtracker 1
```

## Build Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

Output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Verified absolute output path:

```text
D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk
```

## Install Debug APK

With SDK platform-tools on PATH:

```powershell
adb devices
adb install -r "D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk"
adb shell am start -n com.hoctin.gymtracker/.MainActivity
```

Using the verified SDK path directly:

```powershell
$adb = "C:\Users\hoang\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb install -r "D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk"
& $adb shell am start -n com.hoctin.gymtracker/.MainActivity
```

## Emulator QA Checklist

Verified emulator run:

```powershell
$emulator = "C:\Users\hoang\AppData\Local\Android\Sdk\emulator\emulator.exe"
$adb = "C:\Users\hoang\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $emulator -list-avds
& $adb devices
.\gradlew.bat :app:installDebug
& $adb shell am start -n com.hoctin.gymtracker/.MainActivity
```

Verified flows:

1. Home opens with a suggested workout and the seeded Push/Pull/Legs/Full Body split exists.
2. Quick start opens Log Workout for the currently selected day.
3. Plan allows creating, editing, and deleting workout days.
4. Plan allows creating, editing, and deleting exercises.
5. Log Workout saves sets with kg, reps, RIR, and notes.
6. Log Workout shows previous performance after saving and starting the same workout again.
7. History shows saved sessions grouped by date and exercise.
8. PR markers appear after hitting a new max weight or reps.
9. Exercise Detail shows exercise history and the overload suggestion.
10. Settings generates export JSON.
11. Settings imports previously exported JSON and restores plan/history data.
12. Close and relaunch the app, then confirm history and plan edits persist.
13. Settings Light mode remains selected after force-stop/relaunch.

### Workout day selector QA (Home)

The Home screen lets you manually choose the current workout day instead of relying only on the auto suggestion. Verify:

1. Home shows a **Workout day** section with one chip per plan day (Push, Pull, Legs, Full Body).
2. Tapping a chip (for example **Legs**) selects it; the "Today" header and the workout exercise list update to that day, and the subtitle reads "Manually selected".
3. **Skip / Next workout day** advances to the next day in the plan and cycles back to the first after the last day.
4. **Quick start** opens Log Workout for the currently selected day, and the saved session in History shows that day's name (for example a Legs session is labelled "Legs").
5. Selecting a day, then force-stopping and relaunching the app, keeps the same day selected (persisted in preferences).
6. With no manual selection yet (fresh install), Home falls back to the day-of-week suggestion and the subtitle reads "Suggested for today".

Capture screenshots:

```powershell
adb exec-out screencap -p > home.png
```

Use UI tree inspection for tap targets when debugging:

```powershell
adb exec-out uiautomator dump /dev/tty > ui.xml
```

### Auto-advance after saving QA (Home)

When a workout is saved, Home advances the selected workout day to the next day in the current plan order (`sortOrder`, then id). The saved History session keeps the original completed day name.

Verified on Pixel 8 API 37 on June 4, 2026:

1. Selected **Pull** on Home, opened **Quick Start**, saved a valid workout, and confirmed History recorded **Pull**.
2. Returned Home and confirmed the hero showed **NEXT WORKOUT** / **Legs** and the workout section showed **Next workout**.
3. Pressed **Quick Start** again and confirmed Log Workout opened **Legs**.
4. Saved a valid **Legs** workout and confirmed History recorded **Legs**.
5. Returned Home and confirmed the selected workout advanced to **Full Body**.
6. Force-stopped and relaunched the app, then confirmed **Full Body** was still selected and shown as **NEXT WORKOUT**.

### First-launch onboarding QA (training weekdays)

On first launch (or after clearing app data) GYMATE shows an onboarding screen that asks the user to pick training weekdays before Home. The number of days to choose is **derived from the current plan size** (currently 4 workout days → choose 4), not hardcoded, so it stays correct as the plan grows. Selections persist in `UserPreferencesRepository` as `onboarding_completed` (Boolean) and `training_weekdays` (ISO day numbers 1–7, Monday = 1 … Sunday = 7, stored as a sorted CSV). The chosen weekdays only influence Home wording — they do not change the manual workout-day selector, Skip, or Quick Start.

To clear app data for a fresh test:

```powershell
adb shell pm clear com.hoctin.gymtracker
```

Verified on Pixel 8 API 37 on June 5, 2026:

1. Fresh launch shows onboarding before Home, with GYMATE branding, OLED/purple styling, and helper text "Choose 4 training days for your current plan." (the 4 is derived from the plan count).
2. Selecting fewer than 4 days keeps the **Start training** CTA disabled; the counter shows e.g. "3 / 4 selected".
3. Selecting exactly 4 days enables the CTA.
4. Tapping **Start training** completes onboarding and opens Home; prefs show `onboarding_completed=true` and `training_weekdays=1,3,5,7`.
5. Force-stop / relaunch goes straight to Home — onboarding does not show again.
6. **Settings → Training → Edit training days** opens a dialog (Save disabled until exactly the required number of days is chosen); saving updates the summary line and persists (`training_weekdays=1,2,3,4`).
7. Home wording reflects the weekdays: when today **is** a selected training day the hero reads **TODAY'S WORKOUT**; when today **is not** selected it reads **NEXT WORKOUT**. Manual selection still shows **SELECTED WORKOUT**, and auto-advance after a save still shows **NEXT WORKOUT**.
8. Regression pass: Quick Start opens the selected workout, the Log Workout active-exercise accordion works, debounced draft persistence survives leaving and returning, Save Workout records History and auto-advances, and Home scrolls repeatedly without crashing (clean `adb logcat -b crash`).

## Build Release APK

```powershell
.\gradlew.bat :app:assembleRelease
```

Unsigned output:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

For installable release APKs, sign with `apksigner` from the Android SDK Build Tools.

## Build Signed AAB For Google Play

Create a keystore:

```powershell
keytool -genkeypair -v `
  -keystore gym-tracker-release.jks `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias gymtracker
```

In Android Studio:

1. Select **Build > Generate Signed Bundle / APK**.
2. Choose **Android App Bundle**.
3. Select `gym-tracker-release.jks`.
4. Enter the alias and passwords.
5. Choose the `release` variant.
6. Finish the wizard.

CLI alternative:

```powershell
.\gradlew.bat :app:bundleRelease
```

If signing is configured in Android Studio, output appears at:

```text
app\build\outputs\bundle\release\app-release.aab
```

## Publish To Google Play

1. Create a Google Play Developer account.
2. Create a new app in Play Console.
3. Complete store listing, privacy policy, app category, content rating, and data safety.
4. Upload the signed `app-release.aab`.
5. Create an internal testing release first.
6. Test install from Play on a real device.
7. Promote to closed/open testing or production when ready.

This app stores workout data locally only. If you add cloud sync later, update the Play Console Data Safety section before publishing.

## MVP Features

- Default 4-day split: Push, Pull, Legs, Full Body.
- First-launch onboarding to choose training weekdays (count derived from the current plan size); editable later in Settings.
- Manually choose the current workout day on Home, or skip to the next day; the choice is persisted.
- Create, edit, and delete workout days.
- Create, edit, and delete exercises.
- Log sets with weight, reps, RIR, and optional notes.
- Save workout history by date.
- Show previous exercise performance while logging.
- Suggest progressive overload based on target reps and RIR.
- Show current vs previous volume trend while logging.
- Highlight PRs for weight or reps.
- Exercise detail history and overload suggestion.
- Local JSON export/import from Settings.
- Light mode, Dark mode, and System default theme setting.

## Architecture

- Kotlin
- Jetpack Compose
- Room Database
- MVVM ViewModels
- Local-only repository, no backend and no login

Main package:

```text
com.hoctin.gymtracker
```

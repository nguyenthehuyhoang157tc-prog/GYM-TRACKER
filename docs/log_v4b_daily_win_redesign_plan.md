# LOG-V4B — Daily Win cleanup + "Liquid Glass" research + Settings plan — Plan

Date: 2026-09-04
Status: **Plan only.** No code changes yet — this pass is research and task scoping.
Project: `D:\HOCTIN\GYM TRACKER`
Package: `com.hoctin.gymtracker`
minSdk = 26 / targetSdk = 35 / compileSdk = 36 (Health Connect requires 36 at compile time)

## 0. Outcome of this pass

Two tasks are scoped. **Only Task A will be implemented in this pass; Task B is a research
deliverable that produces a follow-up document.**

- **Task A — Daily Win cleanup (implemented):** keep Kimi K3's lime design language; remove the
  two unwanted popups (info sheet and exercise picker sheet); replace the local boost-timer
  dialog with the shared `RestTimerSheet` from Log Workout; replace the `Icons.Outlined.Shuffle`
  icon on the Random button; apply **Aave-style liquid glass (displacement / refraction)** to
  `DailyBoostCard` — not frosted `Modifier.blur`. QA is on current Android (API 35/36), so the
  real AGSL / `RenderEffect` path is in scope.
- **Task B — Settings research (read-only):** catalogue the current state of the Profile &
  Settings hub against the design language, identify what still uses legacy Material, and produce
  the scope for a future settings-rework task. **No settings UI is changed in this pass.**

## 1. Read-only audit of the Kimi K3 Daily Win changes (backup `_backup_20260902_075635_daily_win_lime_redesign/`)

### 1.1 Added by Kimi K3 (kept in LOG-V4B unless noted)

| Item | Location | Notes |
|---|---|---|
| `DailyWinLime = 0xFFB6E24A` | `DailyBoostSection.kt:126` | The new brand accent. Distinct from the recovery PRIMED lime in `MuscleRecovery.kt:79` — semantically different. |
| `DailyWinLimeLight = 0xFF5C8A1E` | `:127` | Light-theme companion. |
| `DailyWinOnLime = 0xFF0F0F12` | `:128` | Text colour when sitting on the lime fill. |
| `dailyWinAccent()` / `dailyWinBrush()` | `:131-147` | Theme-aware lime helpers. |
| `DailyBoostCard` | `:150` | New layout: Medallion + "Daily Win" + status chip, "Today's win" inset with "Change", two actions, progress bar. The visible result in the user-supplied screenshot. |
| `BoostMedallion` | `:461` | 52dp trophy. |
| `BoostStatusChip` | `:483` | "Done today" pill. |
| `BoostActionButton` | `:502` | Reusable button with leading icon. |
| `BoostProgressBar` | `:318` | Streak bar. |
| `DailyWinConfettiOverlay` | `:865` | One-shot confetti when the boost completes. Reuses `R.raw.auron_daily_win_confetti` (md5 `90609269…`, 365,326 bytes — same file as the save-confetti in Log; see §6). |

### 1.2 To remove in LOG-V4B

| Item | Location | Trigger | Reason |
|---|---|---|---|
| `DailyWinInfoSheet` | `:362` | `Icons.Outlined.Tune` button on the card (`:251`) | "How it works" copy is the kind of explanation the design language rejects (DESIGN.md §1 — restraint). The card title, the lime accent and the streak bar already communicate what Daily Win is. |
| `DailyBoostChooseSheet` | `:564` | Chevron on the "Today's win" row | Selecting a different exercise belongs behind **Random**, not behind a separate screen. Removes a popup the user never asked to open. |
| `DailyBoostTimerDialog` + `DailyBoostTimerContent` | `:680, 703` | Start / Random | Replaced by the shared `RestTimerSheet`. |
| `BoostTimerRing` | `:891` | (only used by `DailyBoostTimerContent`) | Replaced by the ring inside `RestTimerSheet`. |
| `BoostTimerControls` | `:1014` | (only used by `DailyBoostTimerContent`) | Replaced by the controls inside `RestTimerSheet`. |
| `BoostCompleteBadge` | `:980` | (only used by `DailyBoostTimerContent`) | The shared sheet does not need a second completion indicator. |

### 1.3 Confirm-before-removing invariants (the durable Kimi additions)

- **Lime palette stays.** The accent is used in the medallion, the active button fill, the
  progress bar, and the confetti palette — removing it would erase the visual identity Kimi
  introduced.
- **2-button rule (Start + Random) stays.** The user explicitly says "vẫn giữ nguyên thiết kế
  của Kimi K3". Both actions must remain pinned in `fixedFooter`-style anchored positions
  inside the card.
- **`DailyWinConfettiOverlay` stays.** Triggered on the shared timer's DONE phase, gated by
  `ValueAnimator.areAnimatorsEnabled()` so the device-motion-off path still works.
- **Streak bar stays.** "3 day streak" plus the green dots — Kimi's signature.

## 2. Shuffle icon — the one thing the user flagged

Current: `Icons.Outlined.Shuffle` (line 304). Material's stock shuffle is two intersecting arrows
with arrowheads at all four tips. At the size the action button uses (≈ 20dp), the two arrows
collapse visually and read as a smudge.

**Three options considered:**

| Option | Pros | Cons |
|---|---|---|
| (a) `Icons.Outlined.Casino` | Semantically "random", single shape | Dice reads as gambling, not training |
| (b) `Icons.Outlined.Autorenew` | Common "switch/refresh" idiom | Does not read as "random" — reads as "reload the current item" |
| (c) Custom inline SVG path — three crossed arrows in a soft 1.5dp weight | Matches the muscular, hand-drawn feel of the rest of the home card; tight at 20dp | Requires writing a vector path; not in the existing icon set |

**Recommendation: option (c) — inline `ImageVector` for a `BoostRandomIcon`.** 24×24dp viewport,
three arrows crossing at 60° in `dailyWinAccent()` (lime), `1.6dp` stroke, no fill. This is
the same pattern AURON already uses for inline trophy/medallion glyphs in
`BoostMedallion` (Kimi's own work). Keeps the icon library untouched and matches the
surrounding hand-built feel. A small inline path is preferable to a material-look icon.

## 3. Task A — Daily Win cleanup, concrete changes

### 3.1 File scope

Files changed:

- `app/src/main/java/com/hoctin/gymtracker/ui/home/DailyBoostSection.kt` — remove the two
  popups, remove the local timer dialog and its children, swap the Shuffle icon, wrap
  `DailyBoostCard` in the shared liquid-glass lens.
- `app/src/main/java/com/hoctin/gymtracker/ui/components/AuronLiquidGlass.kt` (new) —
  lens map + AGSL / `RenderEffect` refraction modifier used by Daily Win.
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt` — extract
  `RestTimerSheet` (`:1779`) to a shared file. (See §3.4.)
- `app/src/main/java/com/hoctin/gymtracker/ui/components/RestTimerSheet.kt` (new) — the
  shared sheet, parameterised.
- `app/src/main/java/com/hoctin/gymtracker/ui/home/DailyBoostViewModel.kt` (or extend
  `HomeViewModel.kt`) — wire `randomizeDailyBoostExercise()` (already present at
  `HomeViewModel.kt:335`) to also open the shared sheet.
- `README.md` — new `**LOG-V4B**` section.
- `docs/log_v4b_daily_win_redesign_plan.md` — this file.

Files **not** changed (protected surfaces, hash-verified after edit):

- Anything under `app/src/main/java/com/hoctin/gymtracker/data/` (Room, repository,
  SavedWorkout, RecoveryResearch, Wearable).
- `domain/ProgressiveOverload.kt`, `domain/muscleheat/*`.
- `ui/history/`, `ui/plan/`, `ui/onboarding/`, `ui/settings/`.
- `app/schemas/`, `app/build.gradle.kts`, `gradle/`.

### 3.2 New card flow (after the cuts)

```
Tap "Start"   → if "Today's win" already selected → open shared RestTimerSheet
                else → pick the first available, then open the sheet
Tap "Random"  → call randomizeDailyBoostExercise() → open shared RestTimerSheet
Done (countdown 0:00) → confetti overlay → mark today's win complete → close
```

No intermediate popups. The chevron on the "Today's win" row is removed because there is
nothing to open anymore (the row becomes a label only).

### 3.3 Shared RestTimerSheet — extraction

`LogWorkoutScreen.kt:1779` already implements exactly the right visual: 56% cap, a 184dp
ring with 14dp stroke, accent `0xFF45D483`, "Reset / Pause-or-Resume" controls, IME-padding,
"Skip rest & continue" when `advanceAfterRest`. Extract it as:

```kotlin
@Composable
fun RestTimerSheet(
    state: RestTimerUiState,
    accent: Color,                            // log: 0xFF45D483, boost: dailyWinAccent()
    showSmartToggle: Boolean,                 // log: true, boost: false
    showSkipContinue: Boolean,                // log: true, boost: false
    maxHeightFraction: Float = 0.56f,         // log: 0.56, boost: 0.60
    title: String = "Rest Timer",
    onSmartChanged: (Boolean) -> Unit,
    onInputChanged: (String) -> Unit,
    onUnitChanged: (RestTimerUnit) -> Unit,
    onReset: () -> Unit,
    onToggleRunning: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
)
```

`RestTimerUiState` and `RestTimerUnit` move to `ui/components/RestTimerSheet.kt` (or to a
new `ui/log/RestTimerContract.kt` if they must stay close to the log VM). The Log call site
becomes a single line; the Boost call site uses `accent = dailyWinAccent()`,
`showSmartToggle = false`, `showSkipContinue = false`, `maxHeightFraction = 0.60f`.

### 3.4 Liquid glass on `DailyBoostCard` (Aave refraction — not frost)

**Product requirement:** Daily Win is the first AURON surface that must look like Aave
Glass, not like iOS frosted blur. Verified at
[aave.com/design/building-glass-for-the-web#how-it-works](https://aave.com/design/building-glass-for-the-web#how-it-works).

**The earlier draft of this section was wrong.** It treated glass as
`Modifier.blur(20.dp)` / `RenderEffect.createBlurEffect` on a gradient layer. That is the
Aave-named **frosted shortcut**: it samples/blurs, it does not bend light. Aave's
sentence that matters:

> Nothing is sampled from underneath the glass. The content's own pixels are the ones
> moving, which is why text under the lens stays selectable and links stay clickable.

So the Home purple-to-OLED canvas is **not** an input. There does not need to be “sharp
content under the card.” The lens refracts **the card's own painted pixels** (lime
gradient, medallion, status chip, “Today's win” row, Start/Random, streak dots) the same
way Aave's switch bends a copy of the track fill that lives *inside* the glass.

#### Algorithm (what the GPU must do)

For each fragment of the `DailyBoostCard` layer:

1. Content draws normally into a layer (Compose layout + hit-testing stay unwarped).
2. A **displacement field** is generated from the lens shape (rounded rect ≈ 22.dp).
   Red = Δx, green = Δy, **neutral = 0.5 / 0.5**. Outside the lens the field is
   exactly neutral so only the glass region moves.
3. Edge strength follows a convex-lens approximation: displacement along the inward
   normal, near-zero at the centre, strongest at the rim (signed-distance of the
   rounded rect). This is the refraction, not a blur kernel.
4. Finishing passes (Aave's “few more passes”): a **faint chromatic fringe** (R/G/B
   sampled at slightly different scales) and a **specular highlight / rim** so the
   glass stays legible on OLED.
5. **Map is rebuilt only when `lensW / lensH / borderRadius / scale` change**, never
   when the card scrolls, presses, or the timer opens. Press uses the existing 0.985
   scale; it must not regenerate the map.

Hit-testing stays on the unwarped layout tree — the Android equivalent of “text stays
selectable / links stay clickable.” Do **not** put the `RenderEffect` on a parent that
also owns pointer input in a way that would offset taps.

#### Android primitives (corrected API floor)

`RenderEffect` itself is API 31, but **programmable displacement is API 33
(Tiramisu)**. There is no `createDisplacementMapEffect` on API 31. Confirm against
compileSdk 36 at implementation time; if the SDK exposes
`RenderEffect.createDisplacementMapEffect(map, maxDx, maxDy)` use it. The always-true
path on current QA devices is AGSL:

| Aave (web) | Android (this app) | Floor |
|---|---|---|
| SVG `feDisplacementMap` | `RenderEffect.createDisplacementMapEffect` **if present in SDK 36**, else AGSL that samples a map and offsets UVs | **33+** |
| Displacement PNG | Cached `ImageBitmap` (R=Δx, G=Δy) **or** the same field computed in AGSL from an SDF (cheaper for a static rounded card) | 33+ |
| Color fringe + specular | Extra terms in the same AGSL (preferred) or chained `RenderEffect` | 33+ |
| WebGL path for canvas/video | `RuntimeShader` + `RenderEffect.createRuntimeShaderEffect(shader, "content")` | **33+** |
| Frosted shortcut (explicitly **not** the Daily Win look) | `Modifier.blur(20.dp)` / `createBlurEffect` | 31+ |
| Older devices | Translucent lime surface + 1dp hairline. **Not** claimed as liquid glass. | 26–32 |

QA is on current Pixel / API 35–36, so Task A is judged on the **33+** look. The 26–32
path exists only so the app does not crash; it is not the design.

#### Card stacking

1. **Lens layer** — `Modifier.clip(RoundedCornerShape(22.dp))` + `graphicsLayer { renderEffect = … }` wrapping the existing `dailyWinBrush()` column. The brush, medallion, type and buttons are the refracted content.
2. **Edge** — 1dp `gymContainerBorder` plus the specular rim from the shader. Do not add a second blur veil.
3. **Press** — `scale 0.985`, 220ms `FastOutSlowInEasing`, same as `planV3Pressable()`.

Gating: `ValueAnimator.areAnimatorsEnabled() == false` → static map, no specular
animation, no press scale. Reduced motion must not fall back to blur.

### 3.5 Build / verify conventions (per `AGENTS.md`)

After each file edit:

```bash
export JAVA_HOME='D:/jdk17/jdk-17.0.19+10'
export PATH="/d/jdk17/jdk-17.0.19+10/bin:$PATH"
./gradlew.bat :app:compileDebugKotlin
```

Grep the output for `^\s*e:` (errors) and `BUILD SUCCESSFUL` / `BUILD FAILED`. Do **not** pipe
through `tail` — the error line is the only line that matters and `tail -n 40` truncated the
real error twice this month.

Final three:

```bash
./gradlew.bat :app:testDebugUnitTest          # 438 tests baseline
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:assembleRelease
```

All three must end `BUILD SUCCESSFUL` and the test count must not drop.

### 3.6 Backups

Pre-edit filesystem backup for the two files actually edited:

- `_backup_logv4b_20260904_HHMMSS/DailyBoostSection.kt`
- `_backup_logv4b_20260904_HHMMSS/LogWorkoutScreen.kt`
- (the new `RestTimerSheet.kt` is created from scratch, no backup)

## 4. "Liquid Glass" research (does not ship in this pass)

### 4.1 What Aave actually does

Verified from `https://aave.com/design/building-glass-for-the-web#how-it-works`:

- **It is not `backdrop-filter` blur.** Aave explicitly distances itself from prior
  Chromium experiments that used SVG `backdrop-filter`. Their statement: *"Nothing is sampled
  from underneath the glass. The content's own pixels are the ones moving, which is why text
  under the lens stays selectable and links stay clickable."*
- The effect rests on **a single SVG filter primitive, `feDisplacementMap`**. For each
  pixel of the source content, the filter reads the matching pixel of a generated
  **displacement map** (a small PNG) and uses the map's color to decide how far to push the
  pixel. Center of the lens is neutral; edges are strongest.
- **R and G channels** of the map encode **X and Y** displacement respectively.
- **Two finishing passes** on top: a **faint color fringe** (chromatic aberration) along
  the edges, and a **specular highlight** that keeps the glass legible.
- **Map is regenerated only when the lens shape changes**, never when it moves. During
  drags, only the filter region shifts — steady frame rate on phones.
- For non-DOM surfaces (a canvas-drawn QR code and live `<video>` on Safari, which refuses
  to apply an SVG filter to `<video>`), the same map is handed to a WebGL shader and the
  same refraction is computed on the GPU. Same data flow, different primitive.
- **No low-power fallback described.** The article says the technique "just works" on every
  modern browser and on phones, with the only optimisation being map-regeneration gating.
  Apple Liquid Glass (WWDC 2024) is named as the inspiration; Aave's implementation is
  called *Aave Glass*.

### 4.2 Compose / Android equivalent (corrected)

Aave's table maps cleanly, but the **API floors in the first draft were wrong**:

| Aave (web) | Android | API | Notes |
|---|---|---|---|
| SVG `feDisplacementMap` | `RenderEffect.createDisplacementMapEffect(mapEffect, maxDx, maxDy)` when the SDK 36 stub has it; otherwise the same math in AGSL | **33+**, not 31 | API 31 only has blur/offset/colorFilter/chain/shader(Bitmap/Gradient). No per-pixel map. |
| Displacement map PNG | Cached `ImageBitmap` (R=Δx, G=Δy, neutral 0.5) **or** SDF-in-AGSL (better for a static rounded Daily Win card — no PNG, no extra texture bind) | 33+ | Rebuild only when shape changes. |
| Color fringe + specular | Terms inside the AGSL, not a `createBlurEffect` chain | 33+ | Chaining blur onto displacement destroys the look. |
| WebGL shader for canvas/video | `RuntimeShader` + `createRuntimeShaderEffect` | 33+ | This is the Daily Win path on Pixel QA. |
| Frosted shortcut (no displacement) | `Modifier.blur(20.dp)` | 31+ | **Forbidden as the Daily Win success look.** Keep only as a named non-goal. |
| minSdk 26 | Translucent surface + hairline | 26–32 | Crash-safe only. Do not call it glass in README/QA. |

**Reality check:** AURON QAs on current Android. Task A is accepted or rejected on a
device/emulator **API 33+**. Do not ship blur and claim Aave Glass.

### 4.3 Why Task A uses displacement on Daily Win (not blur, not Settings-later)

The first draft delayed displacement because “the card sits on a dark gradient, so there
is nothing underneath to refract.” That sentence describes **backdrop-filter**, which
Aave rejected. Refraction input is **the layer inside the lens**. Daily Win has plenty:
lime fill vs OLED, white type, trophy medallion, two buttons, streak dots. Those edges
are exactly what a convex rounded lens should bend.

Settings charts/photos can reuse the same `Modifier.auronLiquidGlass(...)` later. They
are not a prerequisite.

### 4.4 Build list for the Aave effect on Android (this pass, Daily Win)

1. **`data class Lens`** — `lensW, lensH, borderRadius, scale, chromaAmount`. Pure.
2. **`generateLensMap(lens): ImageBitmap`** — rounded-rect SDF; R/G displacement;
   cache keyed by those five values. Optional if the AGSL computes the SDF directly;
   keep the generator anyway so the map can be screenshot-debugged (Aave shows map vs
   result side by side).
3. **`Modifier.auronLiquidGlass(lens)`** — `graphicsLayer` + `RuntimeShader` (API 33+)
   sampling `uniform shader content`. Fallback 26–32: clipped translucent column, no
   blur pretending to be glass.
4. **Specular + chroma inside the shader**, strength low enough that Start/Random labels
   stay readable. Slider-style “softer bend” (Aave) is the right magnitude for a card
   with text; switch-style strong bend is too much.
5. **Gates:** `SDK_INT >= 33` for the effect; `ValueAnimator.areAnimatorsEnabled()` for
   motion only. Helper tools (shader preview, map PNG dump) are allowed if they make the
   field match Aave; they must not become a second visual language.

## 5. Task B — Settings research (read-only)

### 5.1 Current state

`docs/ps1_profile_settings_hub_redesign_result.md` (2026-08-11) built the hub: 5 sections
× 13 items, all with explicit `Unavailable` / `Updating` where missing. The current code
tree:

| File | LOC | Role |
|---|--:|---|
| `ui/settings/ProfileHubScreen.kt` | 108 | The hub itself. |
| `ui/settings/ProfileSettingsComponents.kt` | 339 | Shared row + section widgets. |
| `ui/settings/ProfileSettingsModels.kt` | 176 | `ProfileDestination` enum + `profileHubSections()` pure function. |
| `ui/settings/ProfileOverviewAndInsightsScreens.kt` | 252 | Streaks, Insights, Personal profile, History. |
| `ui/settings/ProfilePreferencesAndTrainingScreens.kt` | 267 | Training setup, Units & appearance, Notifications. |
| `ui/settings/ProfileDataResearchResourcesScreens.kt` | 616 | Data, Research, Resources, Privacy, About. |
| `ui/settings/SettingsScreen.kt` | 901 | **Legacy**, no longer routed. Compiled but not opened. |
| `ui/settings/SettingsViewModel.kt` | 279 | Same as above — business owners shared, presentation only. |

The user's note ("đã được sửa theo style mới bằng kimi k3 nhưng nó cũng không được như ý")
mirrors the Daily Win complaint: the structure is right (the destinations are honest, the
rows are 48dp+, the Unavailable state is truthful), but the **visual feel** is not yet
in the OLED/charcoal language.

### 5.2 Visual-language audit (per destination)

To be performed in the follow-up task. For each `ProfileDestination`:

- Current source file and line range
- Whether the screen uses the new OLED/charcoal language or legacy Material
- Whether the destination's status is honest (Unavailable / Updating) or dead
- Whether the screen could benefit from the full Aave displacement-glass effect
  (i.e. is there live content — photos, charts — that benefits from refraction?)

The follow-up deliverable is a separate doc — this pass does not open that work.

## 6. Pre-existing debt surfaced while writing this plan

These are not LOG-V4B work. They are listed so the next pass can decide.

1. **Confetti asset duplicated.** `auron_daily_win_confetti.json` and
   `auron_save_confetti.json` are **byte-identical** (md5 `90609269ebc9ca7674b2924b1d0b73e7`,
   365,326 bytes each). Wastes ≈ 356KB. Either delete `auron_save_confetti.json` and route
   the Log save-confetti through `auron_daily_win_confetti`, or accept the duplication.
2. **Gold hex inconsistency.** `Theme.kt:46` defines `GoldAccent = #F2C879` (Dark
   `secondary`). `#F2C86B` is hardcoded at 4 sites: `GoldSweepBorder.kt:47`,
   `LogWorkoutScreen.kt:886, :1687, :1698`. Different by 14 in green — visually identical,
   but two constants.
3. **DailyBoostSection.kt is untracked** at the time of the prior session. The Kimi work
   never went through a baseline backup with the proper `_backup_<task>_<date>_<time>/`
   convention — there is a `_backup_20260902_075635_daily_win_lime_redesign/` directory
   but its content is the **post-edit** files (Kimi's output), not the **pre-edit** files
   (Kimi's input). This is the reverse of the project convention. A future audit may need
   to reconstruct what the card looked like before Kimi.
4. **SettingsScreen.kt (901 LOC) + SettingsViewModel.kt (279 LOC) are dead code.** They
   compile, are routed from `Routes.SETTINGS` through the `GymTrackerApp` nav graph
   (per `AGENTS.md`'s reference to the legacy compatibility implementation), but no
   button in the running app opens them. They should be deleted in the follow-up settings
   pass — keeping them risks a future change accidentally routing to them.
5. **PR-row 28dp shared-gutter misalignment** (LOG-V4A follow-up, 01–02/09) was not
   fixed in this pass. The diagnostic is in
   `.workbuddy-ai/memory/2026-09-02.md`; the fix is one parameter change at
   `LogWorkoutScreen.kt:1698`. Out of scope for LOG-V4B; keep on the debt list.
6. **Graph stale.** `graphify-out/graph.json` (12 MB) is from 2026-08-16. `AGENTS.md`
   asks for `graphify update .` after every code change. CLI is not on PATH in this
   environment. Either install graphify or relax the rule.

## 7. Out of scope

- Applying glass to Settings, History, Log, or the Home canvas. Daily Win is the only
  lens in this pass; Settings may reuse the modifier later.
- Using `Modifier.blur` / `createBlurEffect` as the Daily Win look, or sampling the Home
  purple gradient as a backdrop (that is not Aave Glass).
- Any change to the **Settings** UI. Task B is research only.
- Any change to the **Log Workout** flow. The shared `RestTimerSheet` is extracted, but
  the Log call site must not change observable behaviour.
- Any change to **Room, Progression Engine, Muscle Heat, Muscle Recovery, Recovery
  Research, Wearable, Onboarding, History, Plan, Profile, Settings data, import/export,
  catalog**. SHA-256 of all protected files must match before and after.
- Touching `SettingsScreen.kt` (legacy) or `SettingsViewModel.kt` (legacy).

## 8. Definition of done

For Task A:

- `DailyWinInfoSheet` and `DailyBoostChooseSheet` are removed (no references, no dead
  imports).
- `DailyBoostTimerDialog` and its three children are removed.
- `RestTimerSheet` is a single shared composable used by both Log and Boost.
- The "Random" button uses an inline `BoostRandomIcon` (not `Icons.Outlined.Shuffle`).
- `DailyBoostCard` on API 33+ (the QA surface) uses displacement refraction of **its own
  pixels**: SDF/map with R/G = Δx/Δy, chroma fringe, specular rim. Screenshot vs a
  debug map dump must show the lens, not a frosted smear. API 26–32 is a translucent
  fallback and is not called glass. Text/buttons remain tappable on unwarped layout.
- Build: `compileDebugKotlin` / `testDebugUnitTest` (438 tests still pass) /
  `assembleDebug` / `assembleRelease` all `BUILD SUCCESSFUL`.
- Hash-verified: `ProgressiveOverload.kt`, `MuscleRecovery.kt`, `MuscleHeatEngine.kt`,
  `RecoveryResearchContract.kt`, `GymRepository.kt`, `AppDatabase.kt` byte-identical to the
  pre-edit backup.
- `README.md` has a `**LOG-V4B**` section with a one-paragraph summary and a link to this
  doc.

For Task B:

- A separate `docs/log_v4b_settings_research.md` exists and has one entry per
  `ProfileDestination` covering source file, visual-language status, and whether a
  later reuse of `Modifier.auronLiquidGlass` is justified. Daily Win already ships the
  effect; Settings does not invent a second glass.

## 9. Rollback

The two files actually edited are pre-backed up to
`_backup_logv4b_20260904_HHMMSS/`. To roll back Task A: `cp` the two files back over the
edited versions and rebuild. The new `ui/components/RestTimerSheet.kt` should be deleted
and the `RestTimerSheet` reference at the Log call site reverted to its inlined form.

The Settings research (Task B) is a doc, so rollback is `rm`.

No data migration. No Room version change. No sign-out. No settings reset.

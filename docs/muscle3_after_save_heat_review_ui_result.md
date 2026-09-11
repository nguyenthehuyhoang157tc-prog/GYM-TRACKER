# MUSCLE-3 — After-Save Muscle Heat Review UI + Body Map Asset Port — Result

**Status:** COMPLETE — native Compose body map + review screen shipped and device-verified.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE3_BACKUP_20260703_005007\app_src`.
**No Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change,
no exercise imports, no catalog change, no WebView, signing untouched.** No hard stop was hit.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLE3_BACKUP_20260703_005007\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed

**New (body map port):**
- `ui/muscleheat/BodyMapPaths.kt` — **generated** (script-extracted, not hand-typed) SVG path data for
  35 body-part slugs from the MIT library's `bodyFront.ts`/`bodyBack.ts` (male figure), with attribution header.
- `ui/muscleheat/BodyMap.kt` — `BodyMapSide`, `MuscleHeatPalette` (ARGB tokens, pure/testable),
  `BodyMapSlugs` (BodyRegionId → slug mapping + max-token merge), `BodyMapFigure` Compose `Canvas`
  renderer (`PathParser`, parsed once per side, scaled/centered; back paths translated −724).
- `app/src/main/assets/licenses/react_muscle_highlighter_LICENSE.txt` — upstream MIT license, verbatim.
- `docs/third_party_react_muscle_highlighter.md` — provenance/attribution record.

**New (review UI):**
- `ui/review/SessionReviewViewModel.kt` — loads the saved session + runs the MUSCLE-1/2 pipeline
  (`SessionMuscleHeatUseCase` → `MuscleHeatUiMapper`), heat wrapped in `runCatching` (graceful fallback).
- `ui/review/SessionReviewScreen.kt` — mockup-style layout: header (back, title, time/date — duration),
  heat card (Duration when known, Top Muscles, front/back `BodyMapFigure`, summary-only/unattributed
  footnotes), the full exercise breakdown below (reused History `ExerciseBlock`), circular Share/Edit.
- `ui/history/HistorySessionMapper.kt` — `historySessionOf(sws)` extracted from HistoryViewModel
  (unchanged logic) so the review reuses History's exact breakdown mapping.

**Modified (wiring):**
- `ui/GymTrackerApp.kt` — new route `sessionReview/{sessionId}/{durationMillis}`; after save the app
  still navigates to History (existing behavior preserved) and pushes the review on top; History and
  Home pass `onOpenSession`.
- `ui/log/LogWorkoutViewModel.kt` — `LogWorkoutEvent.Saved` gains `durationMillis` (wall-clock
  Log-screen-open → save; runtime-only, never persisted); VM records `loggingStartedAtMillis`.
- `ui/log/LogWorkoutScreen.kt` — `onSaved(sessionId, durationMillis)` pass-through.
- `ui/history/HistoryViewModel.kt` — delegates to the shared mapper (logic unchanged).
- `ui/history/HistoryScreen.kt` — `ExerciseBlock` made internal (reused); purple body icon
  ("Open session review") on each session row.
- `ui/home/WeeklyProgressHubSheet.kt` / `ui/home/HomeScreen.kt` — same body icon on hub session rows →
  closes the sheet and opens the review.

**Tests:** `test/ui/muscleheat/BodyMapContractTest.kt` — 8 new pure-JVM cases.

## 3. Open-source license / attribution result
Upstream `react-muscle-highlighter` is **MIT** — use permitted. Compliance: unmodified license text
ships in APK assets (`assets/licenses/react_muscle_highlighter_LICENSE.txt`), provenance documented in
`docs/third_party_react_muscle_highlighter.md`, and the generated path file carries an attribution
header. Nothing upstream was removed/altered; the reference folder is not part of the build.

## 4. Asset compatibility audit summary (Phase A)
- **Path data:** plain SVG `d` strings in TS arrays (`{ slug, path: { left/right/common: [...] } }`) —
  directly consumable by Compose `PathParser`. **Native port feasible → done. No WebView.**
- **ViewBox:** front `0 0 724 1448`; back `724 0 724 1448` (back drawn at x-offset +724 → renderer
  translates by −724).
- **Slugs:** front 19 (chest, deltoids, biceps, triceps, forearm, abs, obliques, trapezius,
  quadriceps, calves, adductors + neutral head/hair/neck/hands/feet/knees/ankles/tibialis); back 16
  (deltoids, triceps, forearm, trapezius, upper-back, lower-back, gluteal, hamstring, calves,
  adductors + neutrals). Male + female sheets exist; **V1 ports the male figure only**.
- **Slug ↔ BodyRegionId fit:** clean except: one `upper-back` shape (→ both LATS and MID_BACK, max
  color wins) and **no abductor shape** (ABDUCTORS not paintable — documented; still appears in lists).
- The wrapper's silhouette outline stroke was not ported (V1 renders filled parts only — matches mockup).

## 5. Body region slug/path mapping summary
`BodyMapSlugs`: front — CHEST→chest, SHOULDERS→deltoids, BICEPS, TRICEPS, FOREARMS→forearm, ABS,
OBLIQUES, TRAPS→trapezius, QUADS→quadriceps, CALVES, ADDUCTORS. Back — SHOULDERS/TRICEPS/FOREARMS/
TRAPS same, LATS→upper-back, MID_BACK→upper-back, LOWER_BACK→lower-back, GLUTES→gluteal,
HAMSTRINGS→hamstring, CALVES, ADDUCTORS. Non-muscle slugs always neutral. Colors resolve ONLY through
`MuscleHeatPalette` (HEAT_NONE→neutral, LIGHT `F2DF8A`, MODERATE `F6A94A`, HIGH `EE6B3B`, MAX `C92F2F`)
— no heat colors hardcoded in screen composables.

## 6. UI architecture
`DB facts → MuscleExposureCalculator → MuscleHeatEngine → MuscleHeatUiMapper → SessionReviewViewModel →
SessionReviewScreen (+ BodyMapFigure)`. The review screen is used by **three surfaces**: after save,
History session rows, and the Home Weekly-hub session rows — all through one nav route. The heat card
is an enhancement above the existing exercise/set breakdown (History's own inline expansion unchanged).

## 7. Save-flow wiring
`saveWorkout()` (unchanged persistence) → `LogWorkoutEvent.Saved(sessionId, durationMillis)` →
`LogWorkoutScreen` → `GymTrackerApp.onSaved`: snackbar + navigate History (exactly as before) + push
`sessionReview(sessionId, durationMillis)`. Back returns to History. Draft/save/import/History flows
untouched; the review reads everything via the existing read-only `sessionWithSets`.

## 8. Fallback behavior
- Heat computation failure → `runCatching` → card shows "Muscle map unavailable"; breakdown still renders.
- Custom/unmatched exercises → no coloring, listed under "Not on the map (custom exercises)".
- Summary-only muscles (FULL_BODY) → footnote, never painted. Unknown duration (History) → hidden.
- Missing/unmapped regions (ABDUCTORS) → ignored by the renderer, no crash (tested).

## 9. Tests / build result
- `BodyMapContractTest` — 8 new cases: ported data integrity, every mapped slug exists per side, all
  24 regions paintable except documented ABDUCTORS, palette total coverage + distinctness, upper-back
  max-token merge, front-only region not painted on back + unmapped region no-crash, empty regions
  neutral, duration formatting. MUSCLE-1 (20) + MUSCLE-2 (16) + all prior suites unchanged.
- `:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0**. `:app:assembleDebug` → **exit 0** (93.75 MB).
  (Release not run — not needed.)

## 10. Runtime QA (Pixel_8 emulator) — PASSED
1. Installed the debug APK over existing data; app launched, **crash buffer clean** (the only crash
   entry was the emulator's own `com.google.android.bluetooth` at boot — not the app).
2. **History** → new purple body icon on every session row → opened a saved Push session (Plank +
   Sled Push): heat card rendered **"Top Muscles: ABS, QUADS"**, front/back maps colored correctly
   (abs orange, quads/glutes/hamstrings/calves light-yellow — TIME_ONLY and DISTANCE_LOAD exposure
   flowing end-to-end), legacy-named exercises correctly listed as "Not on the map", full set
   breakdown below, Share/Edit visible. Back → History intact.
3. **Full save flow:** Home → Day 1 Push → Start Workout → logged 10 reps @ 40 kg → Save workout →
   readiness sheet (Normal) → Save → **Muscle Heat Review appeared automatically** with real runtime
   duration ("3 MIN") and safe "—"/neutral map (that legacy plan's names are all custom → no fake
   coloring, all listed) + the logged set with PR badges below.
4. **Home Weekly hub:** just-saved session listed with the body icon (entry point present).
5. No app crashes at any step.

## 11. Known limitations
- **Male figure only** (female sheets not ported); no silhouette outline stroke (filled parts only).
- **ABDUCTORS** has no drawable shape (upstream gap) — visible in lists, never painted.
- `upper-back` is one shape → LATS and MID_BACK share it (max color wins).
- **Duration** is the Log-screen-open → save wall clock (runtime-only, never stored); sessions opened
  from History show no duration (no persisted start/end — a future schema task if wanted).
- **Share** and **Edit** are V1 placeholders ("coming soon" snackbar) — screenshot sharing and session
  rename are follow-ups.
- Body map is render-only (no per-region tap in V1).
- Legacy sessions/plans with pre-FED names resolve as custom (accurate, but the map stays neutral for
  them); catalog-named exercises color as expected.

## 12–14. Cross-surface verification
- History-opened review shows the same heat card **above** the exercise breakdown ✓ (device-verified).
- Weekly/Home hub session rows expose the same entry point ✓ (icon verified on device; same route).
- Scrolling the review reveals the full per-set detail below the card ✓ (device-verified).

## Next task recommendation
**MUSCLE-3B (polish):** share-as-image (render the heat card to a bitmap → share sheet), session title
edit, optional per-region tap → region detail (contributing exercises), and female-figure port.
After that: **MUSCLE-4** (7-day recovery map — decay model, weekly volume, muscle overlap, readiness,
goal profile).

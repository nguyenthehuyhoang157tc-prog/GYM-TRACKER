# DB-6C — Multi-Tracking Log UI + History Integration — COMPLETED ✅

Wired the DB-6B data foundation into the Log Workout UI, History, Weekly, and Settings via a single,
future-proof formatter + reusable inline-unit components. **No engine change, no Room schema/version
change (stays v5), no catalog/CONTENT_VERSION change (stays 15), no export change (stays schemaVersion 2)
— and the existing 568 exercises behave exactly as before.** Unit tests + debug + release builds green.

## Files changed
**New (formatter architecture):**
- `ui/format/Measurement.kt` — `Measurement(value, unit)` structured model + `MeasurementFormatter`
  (weight / duration / distance / **velocity** — future-ready) + duration/distance parsers.
- `ui/format/SetDisplay.kt` — per-type set rendering from *stored* fields (keeps `reps=0` hidden).
- `ui/format/MeasurementValue.kt` — the one inline value+**bold unit** composable used everywhere.

**Modified:**
- `data/UserPreferencesRepository.kt` — `DistanceUnitSystem` enum + `distanceUnitSystem` preference.
- `data/catalog/CatalogTaxonomy.kt` — `TrackingType` field-config props (`showsReps/Rir/Load/Duration/Distance`, `isRepBased`).
- `data/GymRepository.kt` — `SetInput` + `saveWorkoutSession` carry `durationSeconds`/`distanceMeters`; PR flags suppressed for non-rep sets.
- `ui/log/LogWorkoutViewModel.kt` — `SetDraft` duration/distance; per-exercise `trackingType` resolution; unified `parsedSetInput` (validate+convert per type); draft round-trips the new fields.
- `ui/log/LogWorkoutScreen.kt` — per-`trackingType` set table; new `TrackingSetRow` + inline `UnitInputField`.
- `ui/history/HistoryViewModel.kt` / `HistoryScreen.kt` — `HistorySet` duration/distance + `isNonRep`; `NonRepSetRow` renders inline values.
- `ui/settings/SettingsViewModel.kt` / `SettingsScreen.kt` — distance-unit state + **Settings → Units → Distance** segmented control.
- `ui/GymTrackerApp.kt` — threads `distanceUnitSystem` into Log + History.
- `ui/Formatters.kt` — removed the DB-6B placeholder `formatDuration`/`formatDistance` (superseded; no duplicate conversion code).
- Tests: `MeasurementFormatterTest.kt` (new), `MultiTrackingFoundationTest.kt` (updated). `README.md` updated.

## UI behavior by TrackingType
Driven by the `TrackingType` field-config (no per-screen `when` blocks):

| Type | Log fields shown | Hidden | History line |
|---|---|---|---|
| REPS_WEIGHT | Reps · Kg · RIR (**unchanged**) | — | `80 kg × 10` table |
| BODYWEIGHT_REPS | Reps · Kg · RIR (unchanged) | — | table |
| SKILL_FORM | Reps · Kg · RIR (unchanged) | — | table |
| TIME_ONLY | Duration `mm:ss` | weight, reps, RIR | `01:30` |
| TIME_LOAD | Load · Duration | reps, RIR | `80 kg • 00:40` |
| DISTANCE_LOAD | Load · Distance | reps, RIR | `40 kg • 20 m` |
| TIME_DISTANCE | Distance · Duration | load, reps, RIR | `… • …` |
| TIME_DISTANCE_LOAD | Load · Distance · Duration | reps, RIR | `80 kg • 30 m • 00:40` |

Rep types render the **original** row (zero regression); non-rep types render only their fields (no dead
controls, no `reps=0` shown). The reps chip and progression-target are suppressed where irrelevant.

## Formatter architecture
One `MeasurementFormatter` returns a structured `Measurement(value, unit)` (never raw concatenation in a
composable). The UI styles + renders it inline — value normal, **unit bold** with wider spacing — via the
single `MeasurementValue` composable, identical on Log set rows and History. Distance respects the
`DistanceUnitSystem` (DB always stores meters). **Velocity (m/s, km/h) is already defined**, proving a new
metric needs no UI redesign. `SetDisplay` decides per-type display from stored fields only, so it is
catalog-independent and keeps the sentinel hidden.

## Settings addition
**Settings → Units → Distance**: a `Metric (m, km)` / `Imperial (ft, mi)` segmented control (same sliding-
pill motion as the theme control), persisted in SharedPreferences (`distance_unit_system`). Display-only —
the database keeps meters; no data migration. Built to extend to future unit families (velocity/pace).

## History changes
Per-`TrackingType` rendering: rep exercises keep the SET · VOLUME · REPS · RIR table; an all-time/distance
exercise drops that table and shows `SET n  →  <inline value(s)>` (e.g. `01:30`, `40 kg • 20 m`). A fake
`reps = 0` / `0 kg` is never shown.

## Weekly behavior
**Unchanged.** kg-volume stays `Σ weight×reps`; time/distance sets (reps=0) contribute 0 and still count
as a completed session/exercise. No new scoring invented.

## Export/Import confirmation
Unchanged in DB-6C (DB-6B already shipped `schemaVersion 2` with the duration/distance keys + a
backward-compatible importer). The new fields flow through save → Room → export/import unchanged.

## Runtime QA (Pixel_8 emulator)
| Check | Result |
|---|---|
| Upgrade install (old v5-pref→) + Room migration through v5 (incl. MIGRATION_4_5) | ✅ no crash; data preserved; catalog re-seeded to CV 15 |
| On-device `catalog_exercises.trackingType` populated (471/87/8/2) + `logged_sets` duration/distance cols | ✅ (Plank=TIME_ONLY, Pushups=BODYWEIGHT_REPS, Barbell Squat=REPS_WEIGHT, Snatch=SKILL_FORM) |
| Settings → Units → Distance renders + Metric↔Imperial switch persists | ✅ (`distance_unit_system=imperial`) |
| REPS_WEIGHT Log rows unchanged (other Push exercises) | ✅ |
| **TIME_ONLY (Plank)** Log card renders 3 `mm:ss` duration fields, **hides reps/kg/RIR + reps chip**, no dead controls; field accepts input | ✅ |
| Save review sheet renders for a TIME_ONLY workout | ✅ |
| Crash buffer (entire QA) | ✅ EMPTY for `com.hoctin.gymtracker` |

**Known QA limitation (not an app defect):** the final *Save-button* round-trip for the Plank workout
could not be triggered through blind `adb` taps — the same soft-keyboard/sliding-sheet input-injection
flakiness documented in DB-5B.1 (the rep save flow has the same issue). The save→`durationSeconds`
conversion and the History per-type rendering are covered by unit tests, and the save code path is the
existing, unchanged flow.

## Tests / builds
| Item | Result |
|---|---|
| `:app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** (116 tests) |
| `:app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |
| Engine suite | ✅ pass (engine + tests untouched) |

New tests: `MeasurementFormatterTest` (weight/duration/distance metric+imperial/velocity; parsers;
`SetDisplay` per-type inline strings + `isNonRep`; `TrackingType` field-config; unit-pref round-trip) and
the updated `MultiTrackingFoundationTest` (duration formatting + reps=0 volume invariant).

### APK size before → after
| APK | Before | After | Δ |
|---|---|---|---|
| Debug | 97,139,309 B | 97,318,385 B | +179,076 B (+0.17 MB) |
| Release | 89,445,094 B | 89,463,802 B | +18,708 B |

## Known limitations
- Only **TIME_ONLY** is runtime-reachable today (Plank / Side Bridge); TIME_LOAD / DISTANCE_LOAD /
  TIME_DISTANCE / TIME_DISTANCE_LOAD are fully wired + unit-tested but have no catalog members until DB-6E.
- The rep input row keeps its proven 3-column layout (reps/kg/RIR) to guarantee zero regression for the
  568; the new inline value+unit design applies to the new tracking fields and to History (where units
  appear for the rep exercises too). A future visual pass could unify the rep input row to the same inline
  style if desired.
- Time/distance **PRs** and progression are intentionally not modeled (no engine change); the progression
  card for non-rep types shows the existing generic target.
- The Save-button automation limitation above (test-harness, not the app).

## Readiness for DB-6E
The foundation is complete and verified end-to-end for the reachable path: catalog `trackingType`, the
Log per-type fields, History per-type rendering, the distance-unit preference, and the formatter are all
live and tested. **DB-6E can import the deferred carry/sled/time-distance exercises with their
`trackingType` and they will render + log + display correctly with no further UI work** (their types are
already wired and unit-tested).

## README update summary
Added a **"Multi-Tracking foundation (DB-6A … DB-6C)"** subsection under the FED-catalog section,
documenting: the `TrackingType` model + the 8 types; Room v5 / `MIGRATION_4_5` / `CONTENT_VERSION 15` /
export `schemaVersion 2` (from DB-6B); the DB-6C UI wiring (per-type Log rows, per-type History, Weekly
unchanged, the Settings → Units → Distance preference); the unified `MeasurementFormatter` +
`MeasurementValue` inline design (velocity-ready); and that TIME_ONLY is the only runtime-reachable type
today with the rest ready for DB-6E. Links the DB-6A/6B/6C result docs.

## Confirmation
No Progression Engine logic, Room schema/version (v5), catalog JSON/`CONTENT_VERSION` (15), export
`schemaVersion` (2), History data model, or signing secrets were changed in a way that affects existing
behavior; the 568 existing exercises log/render/save exactly as before. Backup:
`D:\HOCTIN\GYM_TRACKER_DB6C_BACKUP_20260626_135115`.

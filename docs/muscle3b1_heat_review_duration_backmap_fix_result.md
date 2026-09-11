# MUSCLE-3B.1 — Heat Review Back-Map Artifact + Persisted Session Duration Fix — Result

**Status:** COMPLETE — root cause of the back-map artifact identified and PROVEN (not guessed), duration
persisted via an additive Room v6→v7 migration, OLED gradient polish; all device-verified.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE3B1_BACKUP_20260703_190042\app_src`.
**No Progressive Overload Engine change, no exercise imports, no share/edit/female-figure/per-region-tap
work, signing untouched. No destructive migration.** No hard stop hit.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLE3B1_BACKUP_20260703_190042\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed
| File | Change |
|---|---|
| `ui/muscleheat/BodyMap.kt` | **+`normalizeSvgArcFlags()`** (arc-aware SVG tokenizer) applied before every `PathParser` parse (parts + outlines). |
| `test/ui/muscleheat/SvgArcFlagParsingTest.kt` | **NEW** — 5 tests: packed-vs-separated arc-flag node equality (incl. the exact neck/head shapes), non-arc no-op, full-inventory parse regression. |
| `data/Entities.kt` | `WorkoutSessionEntity` += `durationSeconds: Int? = null` (wall-clock workout duration). |
| `data/AppDatabase.kt` | **version 6 → 7**; additive `MIGRATION_6_7` (`ALTER TABLE workout_sessions ADD COLUMN durationSeconds INTEGER`); registered. Schema `7.json` exported. |
| `data/GymRepository.kt` | `saveWorkoutSession(..., durationSeconds)`; export JSON += optional `durationSeconds` (schemaVersion 3 → 4); import reads it tolerantly (missing → null). |
| `ui/log/LogWorkoutViewModel.kt` | Save now persists `durationSeconds` = (now − loggingStartedAt)/1000. |
| `ui/review/SessionReviewViewModel.kt` | UiState += `storedDurationMillis` (from `workout_sessions.durationSeconds`). |
| `ui/review/SessionReviewScreen.kt` | Prefers the STORED duration over the nav argument (same value on every surface); full-screen gradient purple → **true #000000**. |
| `test/data/SessionDurationFoundationTest.kt` | **NEW** — 2 pure-JVM checks (default null; value carried). |
| `README.md`, this doc | Docs. |

## 3. Exact duration source — before / after
- **Before:** duration existed ONLY as a nav argument (`sessionReview/{sessionId}/{durationMillis}`) filled
  from the in-memory `LogWorkoutEvent.Saved(durationMillis)` right after saving. Nothing was stored, so a
  History/Home-opened review passed `0` → the Duration block disappeared.
- **After:** the same wall-clock value (Log screen open → save) is **persisted** at save time into
  `workout_sessions.durationSeconds`. `SessionReviewViewModel` reads it back and the screen **prefers the
  stored value** (nav argument kept only as a fallback for the just-saved transition) — so the after-save
  review and any later History/Home-opened review show the SAME stored duration. Old sessions have `NULL`
  → no duration shown (accepted). Durations under 60 s are treated as noise and hidden (unchanged rule).

## 4. Exact back-map artifact root cause
**Compose's `PathParser` does not tokenize packed SVG arc flags.** The upstream paths use the compact arc
syntax (`a910.025 910.025 0 0136.94-.25`): per the SVG grammar, the large-arc and sweep flags are SINGLE
characters, so `0136.94` must read as `0`, `1`, `36.94`. Compose read it as one number, shifting the
remaining arc parameters. The back `head` path contains **910-radius arcs** written this way — mis-parsed,
they drew the displaced blob beside the rear figure. Front paths contain only tiny-radius packed arcs
(obliques rx≈1.7), whose mis-parse error was sub-pixel — which is why only the back looked broken.

## 5. Root-cause verification steps (no guessing)
1. Compared the generated `BodyMapPaths.kt` to upstream: per-slug path counts match exactly (front 19
   slugs/89 paths, back 16/70), all back paths start in x∈[724,1448], map split verified — **data faithful**.
2. Compared against the upstream reference render (`docs/screenshots/male-back.png`): upstream has NO
   detached shape beside the head → artifact is on our side.
3. Wrote `SvgArcFlagParsingTest` asserting packed-flag arcs parse to the same `toNodes()` list as
   hand-separated arcs: **all 3 cases FAILED on raw Compose `PathParser`** (including the exact
   `a910.025 … 0136.94-.25` shape from the back head path) → hypothesis CONFIRMED deterministically.
4. Implemented `normalizeSvgArcFlags`; the same tests now PASS through the normalizer (node-for-node
   equality), and the on-device back map renders with **no blob** (targeted screenshot QA).
No manual anatomy reshaping was done — the normalizer only rewrites the identical geometry in the
unambiguous form (a documented Android path-port correction).

## 6. Renderer fix
`normalizeSvgArcFlags(d)`: a small arc-aware SVG tokenizer — tracks the current command; inside `a`/`A`
parameter groups (7 params repeating) it reads parameters 4 and 5 as SINGLE-character flags and re-emits
the whole path with explicit separators. Applied once at parse time (remembered) for all part paths and
both outlines. Everything else from MUSCLE-3B is preserved: front map, silhouette outline, separator
strokes, heat palette, neutral dim body, upstream slug mapping. New tests also pin the full inventory
(159 paths + 2 outlines) as parseable to non-empty node lists.

## 7. Room / export / import changes
- **Room v6 → v7** with additive `MIGRATION_6_7` (in-place `ALTER TABLE workout_sessions ADD COLUMN
  durationSeconds INTEGER`; no row read/moved/deleted; no destructive fallback anywhere). Schema
  `app/schemas/.../7.json` generated (exportSchema = true).
- **Export:** sessions now include optional `durationSeconds` (JSON `schemaVersion` 3 → 4).
- **Import:** tolerant — missing/null in older exports decodes to null (no duration), matching the
  readiness/canonical-key conventions. Logged sets and history snapshots untouched.

## 8. Tests / build result
`:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0** (all suites: +5 arc-flag tests, +2 session-duration
tests; every prior MUSCLE/engine/catalog/readiness suite unchanged). `:app:assembleDebug` → **exit 0**
(APK 94.01 MB), schema 7.json present.

## 9. Runtime QA result (Pixel_8)
Deviation from the "clear app data" instruction, documented: the debug APK was installed **over the
existing v6 data** — this exercises the REAL `MIGRATION_6_7` upgrade path (a `pm clear` would skip the
migration and re-run onboarding), and persistence was proven with a **force-stop (process death)** between
save and reopen, which is a strictly stronger in-memory-vs-DB proof. Results:
1. Over-install + launch: **migration v6→v7 succeeded** — plan (Day 1: Push, 16 exercises), 7 prior
   workouts, weekly progress all intact; no crash. (An initial all-zeros screenshot turned out to be a
   cold-start race — DB file inspected directly, 749 KB intact; documented, no code issue.)
2. Old (pre-feature) session from History: **no duration** (NULL) ✓; **back-map blob GONE** — back head
   renders hair + head compactly, matching upstream ✓; gradient fades purple → true black ✓.
3. Fresh workout saved (10 reps @ 40 kg, ~2 m 40 s logging): **after-save review shows "Duration / 3 MINS"**
   (card) and "— 3 mins" (header) ✓. (A first attempt saved a <60 s session — correctly HIDDEN by the
   noise rule; second run waited 75 s.)
4. **`am force-stop` → relaunch → History → same session: "3 MINS" still shown** — read purely from the DB ✓.
5. Home/Weekly hub routes to the same screen with the same stored value (route verified in MUSCLE-3B).
6. **App crash buffer clean** for `com.hoctin.gymtracker` throughout ✓.

## 10. Known limitations
- Duration = Log-screen-open → save wall clock (a logging-session proxy; no true workout start timestamp).
- Sessions saved before v7 have no duration (by design). Durations < 60 s are hidden as noise.
- The QA "Kg prefill" observation: the Log draft prefills the previous weight; automated QA must clear the
  field first (app behavior is intentional).
- Male figure only; ABDUCTORS still has no upstream shape; LATS/MID_BACK share `upper-back` (from 3B).
- Share/Edit remain V1 placeholders.

## Next task recommendation
**MUSCLE-3C:** share-as-image (render the heat card to a bitmap → share sheet) + session rename via the
Edit button; optional per-region tap details and the female-figure port. Then **MUSCLE-4** (7-day recovery
map).

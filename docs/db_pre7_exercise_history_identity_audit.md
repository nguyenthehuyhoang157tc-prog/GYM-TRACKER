# DB-PRE7 — Exercise History Identity Audit (audit only)

**Scope:** read-only audit of how exercise identity flows through logging, history, previous-performance,
and progression. No source / schema / catalog / engine / migration changes were made.

**Classification: `BROKEN`** — previous-performance, PR detection, the per-exercise history screen, and the
Progression Engine input are all keyed on the **plan `ExerciseEntity.id`** (a day-specific row), not on a
canonical exercise identity. The same exercise added to two days has two different `ExerciseEntity.id`s, so
its history does **not** carry across days.

---

## 1. Current identity diagram

```
WorkoutDayEntity (id)                          "Push" (id=1)            "Full Body" (id=4)
   └─ ExerciseEntity (id, dayId, name)          id=10 "Incline …"        id=42 "Incline …"   ← SAME name, DIFFERENT id
        (plan row — one per (day, exercise))         │                        │
                                                     │ saved as exerciseId    │ saved as exerciseId
                                                     ▼                        ▼
WorkoutSessionEntity (id, dayId, dayNameSnapshot, dateEpochDay, …)
   └─ LoggedSetEntity (id, sessionId,
                       exerciseId  ──────────────► = the plan ExerciseEntity.id at save time
                       exerciseNameSnapshot ─────► = the plan name at save time (display only)
                       weight, reps, rir, durationSeconds?, distanceMeters?)
```

**Identity columns actually present** (`data/Entities.kt`):

| Question | Answer |
|---|---|
| `LoggedSetEntity.exerciseId` | **Yes** — `Long?` = the plan `ExerciseEntity.id` ([Entities.kt:70](app/src/main/java/com/hoctin/gymtracker/data/Entities.kt#L70)) |
| `LoggedSetEntity.exerciseNameSnapshot` | **Yes** — `String`, written from the plan name ([Entities.kt:71](app/src/main/java/com/hoctin/gymtracker/data/Entities.kt#L71)) |
| catalog slug / catalog id on the set | **No** |
| `workoutDayId` on the set | **No** — reachable only via `sessionId → WorkoutSessionEntity.dayId` |
| plan `ExerciseEntity.id` on the set | **Yes** — this *is* `exerciseId` |

Keys connecting the model: `WorkoutDayEntity.id → ExerciseEntity.dayId`;
`WorkoutDayEntity.id → WorkoutSessionEntity.dayId` (nullable snapshot link);
`WorkoutSessionEntity.id → LoggedSetEntity.sessionId` (FK, CASCADE);
`ExerciseEntity.id → LoggedSetEntity.exerciseId` (**plain value, not an FK** — survives plan edits).
`ExerciseEntity` itself is a **plan row scoped to one day** — there is no shared/canonical exercise table for
plan exercises (the FED catalog is a separate reference table keyed by `slug`, not linked to plan rows).

**Canonical identity today = the plan `ExerciseEntity.id`.** `exerciseNameSnapshot` exists but is used only
for display/sorting/export — never for lookup.

## 2. Save-path trace

`LogWorkoutScreen` → `LogWorkoutViewModel.saveWorkout()` → `buildInputs()` (one `ExerciseLogInput` per plan
`ExerciseEntity`, [LogWorkoutViewModel.kt:500](app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt#L500))
→ `GymRepository.saveWorkoutSession()` → `GymDao.insertLoggedSets()`.

In `saveWorkoutSession` ([GymRepository.kt:204-251](app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt#L204)):

```kotlin
exerciseId = input.exercise.id,                 // the plan ExerciseEntity.id
exerciseNameSnapshot = input.exercise.name,     // display snapshot
// PR baseline is per plan row:
var bestWeight = dao.maxWeightForExercise(input.exercise.id) ?: -1.0
var bestReps   = dao.maxRepsForExercise(input.exercise.id) ?: -1
```

**Identity persisted per set: the plan `ExerciseEntity.id` (plus the name snapshot). No slug, no canonical
key.** PR flags are computed against the max of *that plan row's* history only.

## 3. Previous-performance trace

| Surface | Lookup | Key |
|---|---|---|
| Log "Previous" chips + default weights | `repository.recentSetsForExercise(exercise.id)` → `latestSessionSets(...)` ([LogWorkoutViewModel.kt:180](app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt#L180)) | **`exerciseId`** |
| Log "Potential PR" chip | `history.maxOfOrNull { it.weight/reps }` over the same per-id history ([LogWorkoutViewModel.kt:442](app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt#L442)) | **`exerciseId`** |
| Exercise detail screen history + target | `repository.observeExerciseHistory(exerciseId)` ([ExerciseDetailViewModel.kt:40](app/src/main/java/com/hoctin/gymtracker/ui/exercise/ExerciseDetailViewModel.kt#L40)) | **`exerciseId`** |
| Save-time PR baseline | `maxWeightForExercise(id)` / `maxRepsForExercise(id)` | **`exerciseId`** |

Underlying DAO queries all filter `WHERE logged_sets.exerciseId = :exerciseId`
([GymDao.kt:54](app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt#L54),
[GymDao.kt:122](app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt#L122),
[GymDao.kt:128-132](app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt#L128)).

**Lookup is by `ExerciseEntity.id` only.** It is **not** by `exerciseNameSnapshot`, normalized name, catalog
slug, day, or session/day relation. The catalog is consulted by **name** (`findByName`) but only to resolve
the *tracking type* and *progression profile/increments* — never the history sets themselves.

## 4. Progression-input trace

`emitState()` calls `ProgressiveOverloadEngine.suggestion(exercise, history, profile, readiness)` where
`history = historyByExercise[exercise.id] = recentSetsForExercise(exercise.id)`
([LogWorkoutViewModel.kt:459](app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt#L459)).
The engine is identity-agnostic: it groups whatever list it is given by `sessionId`
([ProgressiveOverload.kt:181-203](app/src/main/java/com/hoctin/gymtracker/domain/ProgressiveOverload.kt#L181)).

**The engine receives only the current plan exercise/day's sets — not the same exercise across all days.**
Day A's logged sessions are invisible to Day B's suggestion.

## 5. History screen

`HistoryViewModel` is pure snapshot rendering of `observeSessionHistory()` (all sessions). Within each
session it groups by `exerciseNameSnapshot` for display only
([HistoryViewModel.kt:137](app/src/main/java/com/hoctin/gymtracker/ui/history/HistoryViewModel.kt#L137)).
It performs **no** `exerciseId`/name history lookup and **does not** feed previous-performance or
progression. So History correctly *shows* a Push session and a Full Body session that share an exercise name,
but that shared display does not connect their progression.

## 6. Weekly / Home stats

`HomeViewModel` (Weekly Progress Hub) and the weekly History view aggregate **sessions/sets/volume/duration**
only. The only per-exercise grouping is `groupBy { it.exerciseNameSnapshot }` **within a single session** for
the collapsed session summary ([HomeViewModel.kt:361](app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt#L361)).
There is **no cross-day exercise-identity grouping and no progression** in Weekly/Home. (No
`recentSetsForExercise` / `observeExerciseHistory` / `exerciseId` usage in `ui/home`.)

## 7. Import / Export

Export/import preserve `exerciseId` and `exerciseNameSnapshot` verbatim and re-insert with original ids
(`replaceAllData`) ([GymRepository.kt:253-352](app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt#L253)).
So the **current** id-based behavior round-trips exactly. There is enough data to reconstruct **name**-based
cross-day history (the snapshot is exported), but **no catalog slug / canonical key is exported**, so a future
slug-based identity could not be reconstructed from existing backups without re-deriving it from the name.

## 8. Canonical identity audit & classification

- Canonical identity in use today: **plan `ExerciseEntity.id`** (day-specific).
- The only cross-day-stable identity actually stored on a set is **`exerciseNameSnapshot`**, and nothing
  queries by it.

**Classification: `BROKEN`** — history/previous/PR/progression are tied to a day-specific plan row
(`ExerciseEntity.id`). (Not merely `PARTIAL`: it is not that lookup is "scoped too narrowly" on top of a
shared key — there is no shared-key lookup path at all; the id is structurally per-day.)

## 9. Real scenario simulation

**Setup:** "Incline Machine Chest Press" exists in **Push** (`ExerciseEntity.id = 10`) and **Full Body**
(`ExerciseEntity.id = 42`) — two rows, same name, different ids (each `addExercise` autogenerates an id).

1. User logs it in **Push**. Sets are saved with `exerciseId = 10`, `exerciseNameSnapshot = "Incline Machine
   Chest Press"`.
2. User later opens **Full Body** (Log or View Detail). The screen calls
   `recentSetsForExercise(42)` / `observeExerciseHistory(42)` → SQL `WHERE logged_sets.exerciseId = 42`.
3. The Push sets have `exerciseId = 10`, so they are **not** returned.

**Should Full Body show Push's previous sets today? → No, it does not.**
**Exact reason:** the lookup filters by `exerciseId` (the plan row id), and the two plan rows have different
autogenerated ids even though their `exerciseNameSnapshot` is identical. The matching name is never used in
any query, so the histories never join. Result in Full Body: "No previous sets" / "Log first session", and
the engine treats it as a brand-new exercise.

## 10. Proposed fix plan

### Option A — name-keyed canonical lookup (RECOMMENDED, no schema change)
Switch the four history lookups to key on the **normalized exercise name** instead of `exerciseId`:

- Add DAO queries scoped by name, e.g.
  `WHERE TRIM(LOWER(logged_sets.exerciseNameSnapshot)) = TRIM(LOWER(:name))`:
  `getRecentSetsForExerciseName(name)`, `observeExerciseHistoryByName(name)`,
  `maxWeightForName(name)`, `maxRepsForName(name)`.
- `LogWorkoutViewModel` / `ExerciseDetailViewModel` pass `exercise.name`; save-path PR baseline uses the
  name-based max.
- Keep writing `exerciseId` (and the name snapshot) exactly as today — **history rows stay immutable**; only
  the *read* key changes. No migration, no export/import change.

Why it works: plan names are FED-canonical (DB-4B.2 normalized active plan rows), and snapshots are written
from the plan name, so the same exercise across days shares an identical snapshot. A name match unifies them.

### Option B — slug/canonical-key column (schema change; only if needed)
Add `canonicalKey` (catalog slug, or normalized name for custom) to `ExerciseEntity` **and** `LoggedSetEntity`,
backfill old rows from the name, query by it, and add the key to export/import. More rename-proof, but needs a
Room migration + backfill + export schema bump. **Not recommended now** — Option A solves the reported bug
without schema risk. Choose B only if precise rename history or guaranteed catalog-slug identity becomes a
requirement.

### Recommended task: **DB-PRE7B — Canonical (name-keyed) exercise history lookup**
Implement Option A: add the four name-scoped DAO queries (normalized match), repoint
`LogWorkoutViewModel`, `ExerciseDetailViewModel`, and the save-path PR baseline to name-based lookup; leave
`exerciseId` populated for rollback safety; add DAO + ViewModel tests. No schema/migration, no engine change,
no catalog change. (Optionally add an index on `exerciseNameSnapshot` — note that an index is technically a
schema change; defer unless profiling shows it is needed.)

## 9b/10b. Risks

- **Name collisions:** two genuinely different exercises that share a name would merge history. Low risk —
  catalog names are canonical; identical custom names are user intent.
- **Pre-rename history:** DB-4B.2 renamed plan rows but deliberately **not** history snapshots, so sets logged
  under an older name won't match the new canonical name. Acceptable; an alias map could be added later.
- **Case/whitespace:** must normalize both sides (`TRIM(LOWER(...))`); SQLite `LOWER` is ASCII-only (fine for
  these names).
- **PR semantics shift:** name-based `maxWeight/maxReps` makes PRs span all days for the exercise (the correct
  behavior) — note that newly saved PR flags differ from today's per-day flags; existing stored flags are not
  rewritten.
- **Performance:** `logged_sets` is indexed on `exerciseId`, not on the name; name queries scan after the
  session join. Fine for expected data sizes; add an index only if needed.
- **Custom exercises:** unmatched/custom names still resolve to `REPS_WEIGHT` and now share history by name —
  desired.

## 10c. Test plan for the fix (DB-PRE7B)

1. **Cross-day unification (Room in-memory/DAO):** two days, same exercise name; log in day A; assert the
   name-scoped query returns day A's sets when queried with day B's exercise name (and `exerciseId` differs).
2. **Engine input:** `LogWorkoutViewModel`/`ExerciseDetailViewModel` for day B surface day A's history and a
   non-"first session" suggestion.
3. **Normalization:** `"incline machine chest press"`, `" Incline Machine Chest Press "` all match.
4. **No false merge:** two different names do not share history.
5. **PR baseline:** save-path PR uses the cross-day max (a non-PR-within-day weight that is below the all-day
   max is not flagged).
6. **Export/import round-trip** unchanged (still preserves `exerciseId` + snapshot).
7. **Regression:** single-day behavior identical to today; existing engine/format tests stay green.

---

## Deliverables summary

- **Report path:** `docs/db_pre7_exercise_history_identity_audit.md`
- **Files inspected:** `data/Entities.kt`, `data/GymDao.kt`, `data/GymRepository.kt`,
  `domain/ProgressiveOverload.kt`, `ui/log/LogWorkoutViewModel.kt`, `ui/log/LogWorkoutScreen.kt` (save wiring),
  `ui/exercise/ExerciseDetailViewModel.kt`, `ui/exercise/ExerciseDetailScreen.kt`,
  `ui/history/HistoryViewModel.kt`, `ui/home/HomeViewModel.kt`; reference docs db6b/db6c/db6c1/db6e/db6f.
- **Classification:** `BROKEN` (history/progression keyed on plan `ExerciseEntity.id`).
- **Root cause:** all history/previous/PR/progression queries filter `WHERE logged_sets.exerciseId = :id`;
  the same exercise in two days has two different autogenerated `ExerciseEntity.id`s, and the stable
  `exerciseNameSnapshot` is never used as a lookup key.
- **Recommended fix:** DB-PRE7B — repoint the four lookups to a normalized-name key (no schema change).
- **Tests:** `:app:testDebugUnitTest` → **BUILD SUCCESSFUL** (133 tests; no source changed by this audit).

No implementation performed. No schema / engine / catalog / migration changes. DB-PRE7B not started.

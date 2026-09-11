# DB-4B.3 — Runtime QA for FED Canonical Rebuild — PASS (with known, documented limitation)

Runtime QA of DB-4B.2 on the emulator. **No code was changed** — no runtime crash or blocker was found.

## Environment
- **Emulator:** Pixel_8 AVD (`emulator-5554`), targetSdk 35, debuggable build.
- **APK installed:** `app/build/outputs/apk/debug/app-debug.apk` (the DB-4B.2 debug build; unit tests +
  `assembleDebug` green beforehand).
- **Pre-flight markers confirmed:** working dir `D:\HOCTIN\GYM TRACKER`; `catalog_fed_v1.json` = 515;
  images 515 folders / 1030 files; `PlanNameMigration` = 63 SAFE pairs (no unsafe, no collisions);
  `CatalogSeed.CONTENT_VERSION` = 10; Room `@Database version` = 4; engine files unchanged.

## Test setup (transparency)
The pre-existing device install was **older than DB-4B.2 assumed**: catalog pref v5, Room user_version
**3** (pre-V3.6A), Plan names were the **pre-PLAN-1 vague names** (e.g. "Face pull", "Leg press or
squat"), and **0 History rows**. Those names aren't in the migration map and there was no History to
check. To actually exercise the migration's rename + History-preservation paths on device, a minimal
realistic **fixture** was injected into the app DB before first launch: several Plan rows renamed to
names that *are* in the 63-pair map (mix of real-rename + identity), two non-map control rows left
as-is, and one History session whose set snapshots used the old names. This is QA data setup only — no
app code was modified.

## Results

| # | Check | Result |
|---|---|---|
| 1 | Device/emulator | Pixel_8 AVD (`emulator-5554`) |
| 2 | APK installed | `app-debug.apk` (DB-4B.2) |
| 3 | First launch succeeded | ✅ no crash (PID started; clean logcat) |
| 4 | PlanNameMigration ran once | ✅ renamed the 5 real-rename map rows; identity rows kept; controls + History untouched |
| 5 | Migration flag set | ✅ `fed_canonical_plan_name_migration_v1_done = true` (prefs `auron_migrations`) |
| 6 | Second launch skipped migration | ✅ flag-guarded; relaunch clean, no re-run, no crash |
| 7 | Home / Plan | ✅ Home loads; Plan days intact; renamed names display |
| 8 | Catalog Picker | ✅ 515 entries; search works; filters present; unsafe names absent |
| 9 | Detail popup / images / carousel | ✅ FED data + bundled image load; 2-frame carousel dots |
| 10 | Log Workout | ✅ opens; progression target renders; previous performance works; no catalog-lookup crash |
| 11 | History | ✅ opens; old `exerciseNameSnapshot` values preserved (not rewritten) |
| 12 | Settings Import/Export | ✅ "Generate export" → "Export generated." + Export JSON; no crash |
| 13 | Crash buffer (`logcat -b crash`) | ✅ EMPTY for both upgrade and fresh-install sessions |
| 14 | Fixes made | **NONE** — no crash/blocker found |
| 15 | **Final verdict** | ✅ **PASS WITH KNOWN LIMITATIONS** |

### First-launch migration (upgrade path) — verified at the DB level
- Room migrated **user_version 3 → 4** (additive readiness migration) with no error.
- `catalog_exercises` re-seeded from JSON to **515**; catalog version pref **5 → 10**.
- Plan rows: real renames applied — `Cable Face Pull → Face Pull`, `Seated Cable Row → Seated Cable
  Rows`, `Leg Extension → Leg Extensions`, `Lying Leg Curl → Lying Leg Curls`, `Standing Calf Raise →
  Standing Calf Raises`; identity map rows present (`Leg Press`, `Wide-Grip Lat Pulldown`, `Incline
  Dumbbell Curl`, `Triceps Pushdown`, `Cable Crunch`); **non-map controls unchanged** (`Pec fly`,
  `Reverse-grip straight-bar cable pushdown`).
- **History preserved:** `logged_sets.exerciseNameSnapshot` stayed `Seated Cable Row` (×2) and
  `Cable Face Pull` (×1) — not rewritten by the migration.
- UI confirmation: Plan/day-detail show the renamed names; the detail popup for migrated exercises
  ("Leg Press", "Standing Calf Raises") resolves to the FED catalog with image + instructions.

### Catalog Picker (Browse catalog)
- Lists FED entries (3/4 Sit-Up, Ab Crunch Machine, Air Bike …), filters Equipment/Muscle/Level/
  Progression. Catalog table = **515** on device.
- Search "Pushups" → exact FED entry; "Lying Leg Curls" (migrated target) → present; "Side Plank" →
  only the distinct FED entry "Push Up to Side Plank" (no standalone unsafe "Side Plank").
- Data-layer cross-check on device: FED names present `[Pushups, Seated Cable Rows, Arnold Dumbbell
  Press, Machine Shoulder (Military) Press, Lying Leg Curls]` = all found; unsafe names
  `[Diamond Push-Up, Pike Push-Up, Side Plank, Barbell Flat Bench Press, Bicycle Crunch]` = all absent.

### Fresh-install default templates (separate `pm clear` test)
After wiping data and re-onboarding (the upgrade keeps the user's own plan, so defaults need a fresh
seed):
- **Push = 3** (Face Pull, Reverse Grip Triceps Pushdown, Push-up optional), **Pull = 5**, **Legs = 5**,
  **Full Body = 4** — matches the documented thinning.
- All 6 removed REVIEW/CUSTOM names absent (0 occurrences): Incline Machine Chest Press, Seated Machine
  Chest Press, Pec Deck Machine Fly, Dumbbell Seated Lateral Raise, Wide-Grip Pull-Up, Dumbbell Hammer
  Curl.
- Fresh DB: catalog 515, Room v4, migration flag set, all 17 template names (excl. 2 optionals) resolve
  to catalog entries. No crash.

## Known limitation (expected — do NOT fix here; belongs to DB-4C)
The default **Push** day is thin (3 entries incl. one optional) and **Full Body** has 4 entries, because
6 legacy curated machine/variant exercises had no SAFE FED equivalent and were removed (not
force-mapped) in DB-4B.2. This is the documented, intentional consequence of strict FED-first matching.

## Verdict
**PASS WITH KNOWN LIMITATIONS.** The FED canonical rebuild runs correctly on device: first-launch
migration renames active Plan rows using only the 63 SAFE pairs, sets its one-time flag, does not re-run,
preserves History, re-seeds the 515-entry FED catalog, loads images, and leaves Room schema/version and
the V3.6B engine untouched. No crashes in any flow. No code changes were required.

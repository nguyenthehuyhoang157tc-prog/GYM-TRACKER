# DB-5E — Full FED Coverage Re-Audit (after 553 catalog) — audit only

Re-audited all **873** free-exercise-db (FED) exercises against the current **553**-entry FED-canonical
catalog. **Recommendations only — no code, catalog JSON, assets, schema, or engine changed.**
Machine-readable companion: `docs/db5e_missing_fed_coverage.csv` (all 873 rows).

## 1. Catalog state (verified)
catalog count **553**, unique slugs/names ✅, all imageRefs resolve ✅, `CONTENT_VERSION` **13**.

## 2/3. Coverage classification (873 FED entries)

| current_status | Count |
|---|---|
| **IN_CATALOG** | **553** |
| MISSING_OUT_OF_SCOPE | 222 |
| MISSING_NEEDS_TRACKING_SUPPORT | 34 |
| MISSING_LOW_VALUE_DUPLICATE | 29 |
| MISSING_VALID_STRENGTH | 23 |
| REVIEW_MANUAL | 12 |
| **MISSING_COMMON_STRENGTH** | **0** |
| Total | 873 |

**Headline finding: 0 common strength exercises are missing.** Every one of the 515
`SUPPORTED_STRENGTH` FED entries is present (regression check = 0 dropped), and the curated DB-5B/5D
additions are in. The 320 not-in-catalog entries break down (exclusion reasons):

- **222 out-of-scope** = 198 SKIP categories (stretching / cardio / plyometrics) + 22 DB-5A REJECT
  (neck — no muscle mapping; rehab/balance drills; strongman events) + 2 UNSUPPORTED specialty implements.
- **34 need tracking support** = 25 olympic/velocity + 8 sled (time/distance) + 1 carry (distance).
- **29 low-value duplicates** = 25 accommodating-resistance / olympic near-duplicates (DB-5D dedup) +
  4 DB-5B dedup duplicates.
- **23 valid strength, deferred** = 18 advanced calisthenics (BODYWEIGHT) + 5 grip/skill (SKILL_FORM).
- **12 review-manual** = 9 DB-5D review-skip (olympic-adjacent / niche) + 3 DB-5B review-skip (ambiguous).

## 4. Common movement-family coverage

| Family | In catalog / FED total | Verdict |
|---|---|---|
| Lateral raise | 11 / 11 | ✅ complete (incl. **Side Lateral Raise** = the canonical DB lat raise) |
| Bench press | 18 / 21 | ✅ complete; 3 missing are band/chain dups |
| Row | 41 / 53 | ✅ strong; missing = suspended/sled + dups |
| Pulldown | 9 / 10 | ✅ complete |
| Pull-up / chin-up | 6 / 9 | ✅ basics in; missing = Muscle Up / One Arm Chin / Scapular (advanced) |
| Shoulder press | 15 / 15 | ✅ complete |
| Squat | 40 / 56 | ✅ strong; missing = olympic + band/chain variants |
| Deadlift / hinge | 19 / 35 | ✅ basics in; missing = olympic pulls + accommodating-resistance |
| Hip thrust / glute bridge | 3 / 4 | ✅ complete (Barbell Hip Thrust + Glute Bridge present) |
| Curl | 68 / 70 | ✅ complete (noisy — 68 variants) |
| Triceps ext / pushdown | 35 / 38 | ✅ complete |
| Calf raise | 15 / 21 | ✅ strong; missing = niche/band |
| Abs / core | 37 / 41 | ✅ complete |
| Plank | 2 / 2 | ✅ complete |
| Lunge / split squat | 15 / 18 | ✅ strong; missing = suspended split squat |
| Leg curl / extension | 6 / 6 | ✅ complete |
| Fly / pec deck | 18 / 22 | ✅ complete |

**Duplicate/noise note:** the curl (68) and row (41) families are *over-represented* with near-identical
one-arm/grip variants — noise, not gaps. No family has a missing common staple.

## 5. Import recommendations (missing 320)

| import_recommendation | Count | Tracking | Needs new log field? | Needs engine? |
|---|---|---|---|---|
| **IMPORT_NOW** | **23** | BODYWEIGHT_REPS (18) / SKILL_FORM (5) | No | No (existing types) |
| **IMPORT_WITH_FALLBACK** | **37** | VELOCITY_POWER (25) / SETS_REPS_WEIGHT review (12) | No | Velocity: future engine |
| **WAIT_FOR_TRACKING** | **9** | TIME_DISTANCE_LOAD (8) / DISTANCE_LOAD (1) | **Yes (seconds/distance)** | Yes |
| **REJECT** | **251** | UNSUPPORTED / duplicate | — | — |

- **Safe to import without any new log field:** **60** (the 23 IMPORT_NOW + 37 IMPORT_WITH_FALLBACK — all
  log with reps/kg or bodyweight reps).
- **Should import with fallback metadata for a future engine:** **25** (the olympic/velocity lifts — they
  log fine as reps/kg today but their *progression* needs a velocity/technical mode; route to SKILL_FORM
  as the fallback). *(The other 12 IMPORT_WITH_FALLBACK are standard reps/kg needing only a manual dedup
  decision, not engine fallback.)*
- **Truly require new Log Workout fields (seconds/distance) before import:** **9** (8 sleds + 1 carry).

## 6. Specific checks
- **Is "Dumbbell Lateral Raise" present?** Not under that exact name — and **"Dumbbell Lateral Raise" is
  not a FED name at all.** The canonical dumbbell lateral raise ships in FED (and in our catalog) as
  **"Side Lateral Raise"** (equipment: dumbbell). So the exercise *is present*; the user's search just
  didn't hit it by the expected name.
- **Why was it "missing"?** It wasn't excluded — it's a **naming/discoverability** gap, not a coverage
  gap. The catalog already has 11/11 lateral-raise variants.
- **Safe to import?** Nothing to import. **Recommended low-effort fix:** add aliases
  `["Dumbbell Lateral Raise", "Lateral Raise", "DB Lateral Raise"]` to the existing **Side Lateral Raise**
  entry so search finds it (alias-only JSON edit — out of scope for this audit task).
- **Other common exercises similarly missing?** No common staple is genuinely missing (0
  MISSING_COMMON_STRENGTH). The same alias idea would help a few FED-named entries (e.g. "Pushups" vs
  "Push-Up", "Seated Cable Rows" vs "Seated Row").
- **How many safe to import without new log fields?** **60.**
- **How many import with fallback metadata for future engine?** **25** (olympic/velocity).
- **How many truly require seconds/distance fields first?** **9** (sleds + carry).

## Top 50 missing (by practical value)
Dominated by **advanced calisthenics / grip-skill** (IMPORT_NOW, no engine change) then **olympic lifts**
(IMPORT_WITH_FALLBACK):

1–23 (IMPORT_NOW): Suspended Split Squat, Suspended Row, Suspended Reverse Crunch, Suspended Push-Up,
Suspended Fallout, Scapular Pull-Up, Rope Climb, Overhead Squat, One Arm Chin-Up, Muscle Up, Kipping
Muscle Up, Kettlebell Figure 8, Double Kettlebell Windmill, Standing Olympic Plate Hand Squeeze,
Single-Leg High Box Squat, Side To Side Chins, Rocky Pull-Ups/Pulldowns, Ring Dips, Plate Pinch, Otis-Up,
Mixed Grip Chin, London Bridges, Gironda Sternum Chins.

24–50 (IMPORT_WITH_FALLBACK — olympic/velocity + review): Split Snatch, Split Jerk, Split Clean, Snatch
from Blocks, Snatch Deadlift, Snatch Balance, Snatch, Romanian Deadlift from Deficit, Push Press - Behind
the Neck, Push Press, Power Snatch from Blocks, Power Snatch, Power Jerk, Power Clean from Blocks, Muscle
Snatch, Jerk Dip Squat, Jerk Balance, Heaving Snatch Balance, Hanging Bar Good Morning, Hang Snatch -
Below Knees, Hang Snatch, Hang Clean - Below the Knees, Hang Clean, Good Morning off Pins, Clean from
Blocks, Clean and Jerk, Clean Pull.

(Full list with reasons in the CSV.)

## 7. Recommended next import batch
**The catalog already has complete common-strength coverage — no urgent import is needed.** In priority
order:

1. **Highest value / lowest effort (not an import): the alias fix.** Add search aliases to ~5 FED-named
   entries (Side Lateral Raise → "Dumbbell Lateral Raise"/"Lateral Raise"; Pushups → "Push-Up"; Seated
   Cable Rows → "Seated Row"; …). One-line JSON alias edits; fixes the actual user-reported pain.
2. **Optional small batch (≤12, no engine change):** the most recognized IMPORT_NOW advanced movements —
   Muscle Up, Ring Dips, One Arm Chin-Up, Scapular Pull-Up, Overhead Squat, Plate Pinch, Suspended Row,
   Suspended Push-Up, Single-Leg High Box Squat. Pure `BODYWEIGHT`/`SKILL_FORM`, log with existing fields.
3. **Defer:** the 25 olympic (until a velocity/technical engine route exists) and the 9 sled/carry (until
   seconds/distance log fields exist). **Reject:** the 251 out-of-scope/duplicate entries.

## Confirmation
No source code, catalog JSON, assets, Room schema/version, or engine were modified. Only two docs were
created (`db5e_full_fed_coverage_audit.md`, `db5e_missing_fed_coverage.csv`).

# DB-5D.1 — Catalog Level & Difficulty Audit (audit only)

Difficulty-level audit of all **553** FED-canonical catalog exercises
(`app/src/main/assets/catalog/catalog_fed_v1.json`). **Recommendations only — nothing changed.**
Machine-readable companion: `docs/db5d1_catalog_level_recommendations.csv` (all 553 rows).

## 1. Level distribution

| Level | Count | % |
|---|---|---|
| BEGINNER | **344** | 62.2% |
| INTERMEDIATE | **185** | 33.5% |
| ADVANCED | **24** | 4.3% |
| PRO | 0 | 0% |
| **Total** | **553** | |

`PRO` exists in the taxonomy but is unused — FED only provides beginner/intermediate/expert, mapped to
BEGINNER/INTERMEDIATE/ADVANCED. **Systemic observation:** the distribution is heavily front-loaded
(62% BEGINNER). FED's crowd-sourced "beginner" label is inflated — many loaded/skilled movements are
tagged beginner. A full re-rating of all 344 is subjective and out of scope for this audit; below are
the **pattern-based, defensible** flags only (the long tail stays KEEP = trust FED).

## 2. Suspicious assignments (25 flagged of 553)

| Recommendation | Count |
|---|---|
| SHOULD_UPGRADE_LEVEL | 5 |
| SHOULD_DOWNGRADE_LEVEL | 1 |
| REVIEW_MANUAL | 19 |
| KEEP | 528 |
| **Flagged total** | **25** |
| High / Medium / Low confidence | 3 / 8 / 14 |

### SHOULD_UPGRADE_LEVEL (5) — too easy
| Exercise | Current → Rec | Confidence | Reason |
|---|---|---|---|
| **Weighted Pull Ups** | INTERMEDIATE → ADVANCED | **high** | Weighted calisthenic — needs a strong bodyweight pull-up *plus* added load |
| **Power Clean** | INTERMEDIATE → ADVANCED | **high** | Explosive Olympic-style lift; technique/power demand is advanced |
| **Single-Arm Push-Up** | INTERMEDIATE → ADVANCED | **high** | One-arm push-up is an elite calisthenic |
| Snatch Pull | INTERMEDIATE → ADVANCED | medium | Olympic snatch-grip pull; technical and explosive |
| Smith Machine Hang Power Clean | INTERMEDIATE → ADVANCED | medium | Technical hang power clean (Smith-guided but still explosive) |

### SHOULD_DOWNGRADE_LEVEL (1) — too hard
| Exercise | Current → Rec | Confidence | Reason |
|---|---|---|---|
| Barbell Ab Rollout - On Knees | ADVANCED → INTERMEDIATE | medium | Kneeling (assisted) rollout is the *easier* rollout variant |

### REVIEW_MANUAL (19) — judgment call, no firm change
- **Technical KB clean/jerk ballistics (8):** Alternating Hang Clean, Double Kettlebell Alternating Hang
  Clean, Double Kettlebell Jerk, Kettlebell Hang Clean, One-Arm Kettlebell Clean and Jerk, One-Arm
  Kettlebell Jerk, One-Arm Kettlebell Split Jerk, Two-Arm Kettlebell Jerk — FED rates these INTERMEDIATE;
  the double-KB and split-jerk ones are arguably ADVANCED.
- **Ambiguous/possibly over-rated (2):** Iron Cross (name ambiguous — gymnastic ring move vs a
  dumbbell raise; verify which exercise), Smith Machine Pistol Squat (Smith-assisted pistol pattern).
- **Possibly over-rated ADVANCED (4):** Front Barbell Squat, Barbell Squat To A Bench, Front Barbell
  Squat To A Bench, Press Sit-Up — FED marks these ADVANCED; front squat / box squats / weighted sit-up
  read more INTERMEDIATE.
- **Specialty PL — consider ADVANCED (3):** Deficit Deadlift, Board Press, Pin Presses (currently
  INTERMEDIATE; fine but defensibly advanced).
- **Other (2):** Trap Bar Deadlift (BEGINNER — generous for a loaded hinge, though trap-bar is the most
  accessible DL), Weighted Bench Dip (INTERMEDIATE weighted calisthenic — borderline ADVANCED).

## 3. DB-5D imports (special attention)
| Exercise | FED level | Verdict |
|---|---|---|
| Board Press | INTERMEDIATE | REVIEW_MANUAL (low) — defensibly advanced; intermediate acceptable |
| Pin Presses | INTERMEDIATE | REVIEW_MANUAL (low) — same |
| Kneeling Squat | INTERMEDIATE | **KEEP** — reasonable |
| Seated Good Mornings | INTERMEDIATE | **KEEP** — good-morning pattern needs care; intermediate fits |
| Svend Press | BEGINNER | **KEEP** — light isolation chest squeeze; genuinely beginner |

The five DB-5D imports inherited sensible FED levels — none is mis-rated as "beginner" for a hard lift.

## 4. Other audited exercises
| Exercise | FED level | Verdict |
|---|---|---|
| Trap Bar Deadlift | BEGINNER | REVIEW_MANUAL (low) — generous, but trap-bar is the most beginner-accessible DL |
| Sumo Deadlift | INTERMEDIATE | **KEEP** |
| Rack Pulls | INTERMEDIATE | **KEEP** |
| Weighted Pull Ups | INTERMEDIATE | **SHOULD_UPGRADE → ADVANCED (high)** |
| Barbell Hip Thrust | INTERMEDIATE | **KEEP** |
| Glute Ham Raise | INTERMEDIATE | **KEEP** (hard, but intermediate fits) |
| Deficit Deadlift | INTERMEDIATE | REVIEW_MANUAL (low) — consider ADVANCED |
| Box Squat | INTERMEDIATE | **KEEP** |

## 5. Deliverable summary
- **Level distribution:** BEGINNER 344 / INTERMEDIATE 185 / ADVANCED 24 / PRO 0.
- **Suspicious count:** **25** flagged (of 553).
- **Recommended firm changes:** **6** (5 upgrade + 1 downgrade).
- **High-confidence fixes (3):** Weighted Pull Ups → ADVANCED, Power Clean → ADVANCED, Single-Arm
  Push-Up → ADVANCED.
- **Manual review list (19):** the REVIEW_MANUAL set above (KB clean/jerk ×8, ambiguous/over-rated
  ADVANCED ×6, specialty-PL ×3, Trap Bar Deadlift, Weighted Bench Dip).
- **Note:** the broad BEGINNER skew (62%) is a FED data-quality artifact; re-rating the whole catalog is
  a larger, subjective effort deliberately left out of this pattern-based audit.

## Confirmation
No source code, catalog JSON, assets, Room schema/version, or engine were modified. Only two docs were
created (`db5d1_catalog_level_audit.md`, `db5d1_catalog_level_recommendations.csv`).

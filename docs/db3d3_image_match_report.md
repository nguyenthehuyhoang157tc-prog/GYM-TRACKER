# DB-3D.3 — High-Confidence Image Matches for Image-less Curated Exercises

Reviewed **79** image-less curated exercises. **ENRICH 18**, REVIEW 12, REJECT 7.
Source: free-exercise-db (public domain). Only `imageRefs` changes; names/taxonomy/progression untouched.

## ENRICH

| AURON slug | AURON name | free-exercise-db name | FED folder | Reason |
|---|---|---|---|---|
| `arnold-press` | Arnold Press | Arnold Dumbbell Press | `Arnold_Dumbbell_Press` | exact (+Dumbbell, AURON is dumbbell); delts |
| `barbell-flat-bench-press` | Barbell Flat Bench Press | Barbell Bench Press - Medium Grip | `Barbell_Bench_Press_-_Medium_Grip` | flat barbell bench = FED 'medium grip'; barbell/chest; verified image |
| `barbell-incline-bench-press` | Barbell Incline Bench Press | Barbell Incline Bench Press - Medium Grip | `Barbell_Incline_Bench_Press_-_Medium_Grip` | exact + '- Medium Grip'; barbell/upper chest |
| `cable-overhead-triceps-extension` | Cable Overhead Triceps Extension | Cable Rope Overhead Triceps Extension | `Cable_Rope_Overhead_Triceps_Extension` | exact (+rope attachment); cable/triceps |
| `cable-rope-hammer-curl` | Cable Rope Hammer Curl | Cable Hammer Curls - Rope Attachment | `Cable_Hammer_Curls_-_Rope_Attachment` | exact; cable/biceps |
| `close-grip-bench-press` | Close-Grip Bench Press | Close-Grip Barbell Bench Press | `Close-Grip_Barbell_Bench_Press` | exact (+barbell, AURON is barbell); triceps |
| `concentration-curl` | Concentration Curl | Concentration Curls | `Concentration_Curls` | exact (plural); dumbbell/biceps |
| `decline-dumbbell-fly` | Decline Dumbbell Fly | Decline Dumbbell Flyes | `Decline_Dumbbell_Flyes` | exact (plural); dumbbell/chest |
| `dumbbell-step-up` | Dumbbell Step-Up | Dumbbell Step Ups | `Dumbbell_Step_Ups` | exact (plural); dumbbell/quads |
| `flat-dumbbell-fly` | Flat Dumbbell Fly | Dumbbell Flyes | `Dumbbell_Flyes` | flat dumbbell fly = 'Dumbbell Flyes'; dumbbell/chest |
| `incline-dumbbell-fly` | Incline Dumbbell Fly | Incline Dumbbell Flyes | `Incline_Dumbbell_Flyes` | exact (plural); dumbbell/upper chest |
| `lying-leg-curl` | Lying Leg Curl | Lying Leg Curls | `Lying_Leg_Curls` | exact (plural); machine/hamstrings |
| `machine-shoulder-press` | Machine Shoulder Press | Machine Shoulder Military Press | `Machine_Shoulder_Military_Press` | exact (+Military qualifier); machine/delts; verified image |
| `seated-cable-row` | Seated Cable Row | Seated Cable Rows | `Seated_Cable_Rows` | exact (plural); cable/mid back |
| `smith-machine-incline-press` | Smith Machine Incline Press | Smith Machine Incline Bench Press | `Smith_Machine_Incline_Bench_Press` | exact (press=bench press); machine/upper chest |
| `smith-machine-overhead-press` | Smith Machine Overhead Press | Smith Machine Overhead Shoulder Press | `Smith_Machine_Overhead_Shoulder_Press` | exact (+Shoulder); machine/delts |
| `smith-machine-reverse-calf-raise` | Smith Machine Reverse Calf Raise | Smith Machine Reverse Calf Raises | `Smith_Machine_Reverse_Calf_Raises` | exact reverse (plural); machine/calves; verified image |
| `wide-grip-push-up` | Wide-Grip Push-Up | Push-Up Wide | `Push-Up_Wide` | exact (word order); bodyweight/chest |

## REVIEW (kept placeholder — plausible but not confident)

| AURON slug | candidate | reason |
|---|---|---|
| `lying-triceps-extension` | Lying Triceps Press | name 'press' vs 'extension' — same EZ-bar skullcrusher but kept conservative |
| `single-arm-dumbbell-overhead-extension` | Dumbbell One-Arm Triceps Extension | likely same but name diverges; review |
| `donkey-calf-raise` | Donkey Calf Raises | FED equipment 'other' vs AURON machine; setup may differ |
| `cable-lateral-raise` | Cable Seated Lateral Raise | seated vs standing posture differs |
| `standing-calf-raise` | Standing Barbell/Dumbbell Calf Raise | no machine standing-calf match; equipment differs |
| `walking-lunge` | Barbell/Bodyweight Walking Lunge | AURON dumbbell; FED barbell/bodyweight — load differs |
| `machine-preacher-curl` | Machine Bicep Curl / Preacher Curl | no machine-preacher match; either differs |
| `dumbbell-pullover` | Bent-Arm / Straight-Arm Dumbbell Pullover | two arm-position variants, no plain match |
| `dumbbell-incline-rear-delt-row` | Dumbbell Incline Row | primary rear-delt vs mid-back differs |
| `bodyweight-sissy-squat` | Weighted Sissy Squat | FED is barbell-loaded; AURON bodyweight |
| `bulgarian-split-squat` | Split Squat with Dumbbells | split-squat variants, no exact bulgarian match |
| `t-bar-row` | Lying T-Bar Row / T-Bar Row with Handle | chest-supported lying vs AURON standing; or barbell handle |

## REJECT (different exercise — wrong image)

| AURON slug | candidate | reason |
|---|---|---|
| `smith-machine-reverse-calf-raise(alt)` | Smith Machine Calf Raise | NON-reverse ≠ reverse (enriched via exact reverse instead) |
| `dumbbell-kelso-shrug` | Dumbbell Shrug | Kelso (prone scapular) ≠ standing shrug |
| `pistol-squat` | Kettlebell Pistol Squat | kettlebell-loaded ≠ bodyweight pistol |
| `reverse-lunge` | Crossover Reverse Lunge | crossover is a different movement |
| `single-leg-romanian-deadlift` | Romanian Deadlift | bilateral ≠ single-leg |
| `dumbbell-hammer-curl` | Preacher Hammer Dumbbell Curl | preacher ≠ standing hammer curl |
| `barbell-behind-the-neck-press` | Push Press - Behind the Neck | push press ≠ strict press |

Remaining image-less curated stay on the placeholder (no confident match).

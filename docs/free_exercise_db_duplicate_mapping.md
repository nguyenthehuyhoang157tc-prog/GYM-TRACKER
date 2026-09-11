# DB-3B — free-exercise-db Duplicate → AURON Image Enrichment Mapping

Source: `free-exercise-db` (Unlicense / public domain, github.com/yuhonas/free-exercise-db).
Enriched **76** existing AURON catalog entries (EXACT 39 + LIKELY 37) with one start image each.
Only `imageRefs` changes; AURON name/taxonomy/instructions/tips/progression are untouched. No new exercises.

Image: dataset `0.jpg` copied verbatim (no WebP tooling available) to `app/src/main/assets/exercise_images/<auron-slug>/0.jpg`; `imageRefs = ["exercise_images/<slug>/0.jpg"]`.

| AURON slug | AURON name | Match | free-exercise-db name | FED folder | Source image | Asset path | Confidence / notes |
|---|---|---|---|---|---|---|---|
| `barbell-back-squat` | Barbell Back Squat | LIKELY | Barbell Squat | `Barbell_Squat` | `exercises/Barbell_Squat/0.jpg` | `exercise_images/barbell-back-squat/0.jpg` | high — alias "barbell squat" |
| `barbell-bent-over-row` | Barbell Bent-Over Row | LIKELY | Bent Over Barbell Row | `Bent_Over_Barbell_Row` | `exercises/Bent_Over_Barbell_Row/0.jpg` | `exercise_images/barbell-bent-over-row/0.jpg` | high — alias "bent-over barbell row" |
| `barbell-bicep-curl` | Barbell Bicep Curl | LIKELY | Barbell Curl | `Barbell_Curl` | `exercises/Barbell_Curl/0.jpg` | `exercise_images/barbell-bicep-curl/0.jpg` | high — alias "barbell curl" |
| `barbell-decline-bench-press` | Barbell Decline Bench Press | LIKELY | Decline Barbell Bench Press | `Decline_Barbell_Bench_Press` | `exercises/Decline_Barbell_Bench_Press/0.jpg` | `exercise_images/barbell-decline-bench-press/0.jpg` | high — token-set match |
| `barbell-floor-press` | Barbell Floor Press | LIKELY | Floor Press | `Floor_Press` | `exercises/Floor_Press/0.jpg` | `exercise_images/barbell-floor-press/0.jpg` | high — alias "floor press" |
| `barbell-front-squat` | Barbell Front Squat | LIKELY | Front Barbell Squat | `Front_Barbell_Squat` | `exercises/Front_Barbell_Squat/0.jpg` | `exercise_images/barbell-front-squat/0.jpg` | high — token-set match |
| `barbell-good-morning` | Barbell Good Morning | LIKELY | Good Morning | `Good_Morning` | `exercises/Good_Morning/0.jpg` | `exercise_images/barbell-good-morning/0.jpg` | high — alias "good morning" |
| `barbell-hip-thrust` | Barbell Hip Thrust | EXACT | Barbell Hip Thrust | `Barbell_Hip_Thrust` | `exercises/Barbell_Hip_Thrust/0.jpg` | `exercise_images/barbell-hip-thrust/0.jpg` | high — exact name |
| `barbell-shrug` | Barbell Shrug | EXACT | Barbell Shrug | `Barbell_Shrug` | `exercises/Barbell_Shrug/0.jpg` | `exercise_images/barbell-shrug/0.jpg` | high — exact name |
| `barbell-upright-row` | Barbell Upright Row | LIKELY | Upright Barbell Row | `Upright_Barbell_Row` | `exercises/Upright_Barbell_Row/0.jpg` | `exercise_images/barbell-upright-row/0.jpg` | high — token-set match |
| `bench-dip` | Bench Dip | LIKELY | Bench Dips | `Bench_Dips` | `exercises/Bench_Dips/0.jpg` | `exercise_images/bench-dip/0.jpg` | high — alias "bench dips" |
| `cable-crossover` | Cable Crossover | EXACT | Cable Crossover | `Cable_Crossover` | `exercises/Cable_Crossover/0.jpg` | `exercise_images/cable-crossover/0.jpg` | high — exact name |
| `cable-crunch` | Cable Crunch | EXACT | Cable Crunch | `Cable_Crunch` | `exercises/Cable_Crunch/0.jpg` | `exercise_images/cable-crunch/0.jpg` | high — exact name |
| `cable-face-pull` | Cable Face Pull | LIKELY | Face Pull | `Face_Pull` | `exercises/Face_Pull/0.jpg` | `exercise_images/cable-face-pull/0.jpg` | high — alias "face pull" |
| `cable-glute-kickback` | Cable Glute Kickback | LIKELY | Glute Kickback | `Glute_Kickback` | `exercises/Glute_Kickback/0.jpg` | `exercise_images/cable-glute-kickback/0.jpg` | high — alias "glute kickback" |
| `cable-pull-through` | Cable Pull-Through | LIKELY | Pull Through | `Pull_Through` | `exercises/Pull_Through/0.jpg` | `exercise_images/cable-pull-through/0.jpg` | high — alias "pull through" |
| `chest-dip` | Chest Dip | LIKELY | Parallel Bar Dip | `Parallel_Bar_Dip` | `exercises/Parallel_Bar_Dip/0.jpg` | `exercise_images/chest-dip/0.jpg` | high — alias "parallel bar dip" |
| `chin-up` | Chin-Up | EXACT | Chin-Up | `Chin-Up` | `exercises/Chin-Up/0.jpg` | `exercise_images/chin-up/0.jpg` | high — exact name |
| `close-grip-lat-pulldown` | Close-Grip Lat Pulldown | LIKELY | V-Bar Pulldown | `V-Bar_Pulldown` | `exercises/V-Bar_Pulldown/0.jpg` | `exercise_images/close-grip-lat-pulldown/0.jpg` | high — alias "v-bar pulldown" |
| `conventional-barbell-deadlift` | Conventional Barbell Deadlift | LIKELY | Barbell Deadlift | `Barbell_Deadlift` | `exercises/Barbell_Deadlift/0.jpg` | `exercise_images/conventional-barbell-deadlift/0.jpg` | high — alias "barbell deadlift" |
| `cross-body-hammer-curl` | Cross-Body Hammer Curl | EXACT | Cross Body Hammer Curl | `Cross_Body_Hammer_Curl` | `exercises/Cross_Body_Hammer_Curl/0.jpg` | `exercise_images/cross-body-hammer-curl/0.jpg` | high — exact name |
| `deadbug` | Deadbug | LIKELY | Dead Bug | `Dead_Bug` | `exercises/Dead_Bug/0.jpg` | `exercise_images/deadbug/0.jpg` | high — alias "dead bug" |
| `decline-bench-crunch` | Decline Bench Crunch | LIKELY | Decline Crunch | `Decline_Crunch` | `exercises/Decline_Crunch/0.jpg` | `exercise_images/decline-bench-crunch/0.jpg` | high — alias "decline crunch" |
| `decline-push-up` | Decline Push-Up | EXACT | Decline Push-Up | `Decline_Push-Up` | `exercises/Decline_Push-Up/0.jpg` | `exercise_images/decline-push-up/0.jpg` | high — exact name |
| `deficit-deadlift` | Deficit Deadlift | EXACT | Deficit Deadlift | `Deficit_Deadlift` | `exercises/Deficit_Deadlift/0.jpg` | `exercise_images/deficit-deadlift/0.jpg` | high — exact name |
| `drag-curl` | Drag Curl | EXACT | Drag Curl | `Drag_Curl` | `exercises/Drag_Curl/0.jpg` | `exercise_images/drag-curl/0.jpg` | high — exact name |
| `dumbbell-alternate-bicep-curl` | Dumbbell Alternate Bicep Curl | EXACT | Dumbbell Alternate Bicep Curl | `Dumbbell_Alternate_Bicep_Curl` | `exercises/Dumbbell_Alternate_Bicep_Curl/0.jpg` | `exercise_images/dumbbell-alternate-bicep-curl/0.jpg` | high — exact name |
| `dumbbell-decline-bench-press` | Dumbbell Decline Bench Press | LIKELY | Decline Dumbbell Bench Press | `Decline_Dumbbell_Bench_Press` | `exercises/Decline_Dumbbell_Bench_Press/0.jpg` | `exercise_images/dumbbell-decline-bench-press/0.jpg` | high — token-set match |
| `dumbbell-flat-bench-press` | Dumbbell Flat Bench Press | LIKELY | Dumbbell Bench Press | `Dumbbell_Bench_Press` | `exercises/Dumbbell_Bench_Press/0.jpg` | `exercise_images/dumbbell-flat-bench-press/0.jpg` | high — alias "dumbbell bench press" |
| `dumbbell-floor-press` | Dumbbell Floor Press | EXACT | Dumbbell Floor Press | `Dumbbell_Floor_Press` | `exercises/Dumbbell_Floor_Press/0.jpg` | `exercise_images/dumbbell-floor-press/0.jpg` | high — exact name |
| `dumbbell-front-raise` | Dumbbell Front Raise | LIKELY | Front Dumbbell Raise | `Front_Dumbbell_Raise` | `exercises/Front_Dumbbell_Raise/0.jpg` | `exercise_images/dumbbell-front-raise/0.jpg` | high — token-set match |
| `dumbbell-goblet-squat` | Dumbbell Goblet Squat | LIKELY | Goblet Squat | `Goblet_Squat` | `exercises/Goblet_Squat/0.jpg` | `exercise_images/dumbbell-goblet-squat/0.jpg` | high — alias "goblet squat" |
| `dumbbell-incline-bench-press` | Dumbbell Incline Bench Press | LIKELY | Incline Dumbbell Press | `Incline_Dumbbell_Press` | `exercises/Incline_Dumbbell_Press/0.jpg` | `exercise_images/dumbbell-incline-bench-press/0.jpg` | high — alias "incline dumbbell press" |
| `dumbbell-shrug` | Dumbbell Shrug | EXACT | Dumbbell Shrug | `Dumbbell_Shrug` | `exercises/Dumbbell_Shrug/0.jpg` | `exercise_images/dumbbell-shrug/0.jpg` | high — exact name |
| `ez-bar-preacher-curl` | EZ-Bar Preacher Curl | LIKELY | Preacher Curl | `Preacher_Curl` | `exercises/Preacher_Curl/0.jpg` | `exercise_images/ez-bar-preacher-curl/0.jpg` | high — alias "preacher curl" |
| `flutter-kicks` | Flutter Kicks | EXACT | Flutter Kicks | `Flutter_Kicks` | `exercises/Flutter_Kicks/0.jpg` | `exercise_images/flutter-kicks/0.jpg` | high — exact name |
| `forearm-plank` | Forearm Plank | LIKELY | Plank | `Plank` | `exercises/Plank/0.jpg` | `exercise_images/forearm-plank/0.jpg` | high — alias "plank" |
| `glute-ham-raise` | Glute-Ham Raise | EXACT | Glute Ham Raise | `Glute_Ham_Raise` | `exercises/Glute_Ham_Raise/0.jpg` | `exercise_images/glute-ham-raise/0.jpg` | high — exact name |
| `hack-squat-machine` | Hack Squat Machine | LIKELY | Hack Squat | `Hack_Squat` | `exercises/Hack_Squat/0.jpg` | `exercise_images/hack-squat-machine/0.jpg` | high — alias "hack squat" |
| `hanging-leg-raise` | Hanging Leg Raise | EXACT | Hanging Leg Raise | `Hanging_Leg_Raise` | `exercises/Hanging_Leg_Raise/0.jpg` | `exercise_images/hanging-leg-raise/0.jpg` | high — exact name |
| `incline-dumbbell-curl` | Incline Dumbbell Curl | EXACT | Incline Dumbbell Curl | `Incline_Dumbbell_Curl` | `exercises/Incline_Dumbbell_Curl/0.jpg` | `exercise_images/incline-dumbbell-curl/0.jpg` | high — exact name |
| `incline-push-up` | Incline Push-Up | EXACT | Incline Push-Up | `Incline_Push-Up` | `exercises/Incline_Push-Up/0.jpg` | `exercise_images/incline-push-up/0.jpg` | high — exact name |
| `inverted-row` | Inverted Row | EXACT | Inverted Row | `Inverted_Row` | `exercises/Inverted_Row/0.jpg` | `exercise_images/inverted-row/0.jpg` | high — exact name |
| `jm-press` | JM Press | EXACT | JM Press | `JM_Press` | `exercises/JM_Press/0.jpg` | `exercise_images/jm-press/0.jpg` | high — exact name |
| `leg-extension` | Leg Extension | LIKELY | Leg Extensions | `Leg_Extensions` | `exercises/Leg_Extensions/0.jpg` | `exercise_images/leg-extension/0.jpg` | high — alias "leg extensions" |
| `leg-press` | Leg Press | EXACT | Leg Press | `Leg_Press` | `exercises/Leg_Press/0.jpg` | `exercise_images/leg-press/0.jpg` | high — exact name |
| `leg-press-calf-raise` | Leg Press Calf Raise | LIKELY | Calf Press | `Calf_Press` | `exercises/Calf_Press/0.jpg` | `exercise_images/leg-press-calf-raise/0.jpg` | high — alias "calf press" |
| `lying-reverse-crunch` | Lying Reverse Crunch | LIKELY | Reverse Crunch | `Reverse_Crunch` | `exercises/Reverse_Crunch/0.jpg` | `exercise_images/lying-reverse-crunch/0.jpg` | high — alias "reverse crunch" |
| `machine-thigh-abductor` | Machine Thigh Abductor | LIKELY | Thigh Abductor | `Thigh_Abductor` | `exercises/Thigh_Abductor/0.jpg` | `exercise_images/machine-thigh-abductor/0.jpg` | high — alias "thigh abductor" |
| `machine-thigh-adductor` | Machine Thigh Adductor | LIKELY | Thigh Adductor | `Thigh_Adductor` | `exercises/Thigh_Adductor/0.jpg` | `exercise_images/machine-thigh-adductor/0.jpg` | high — alias "thigh adductor" |
| `mountain-climber` | Mountain Climber | LIKELY | Mountain Climbers | `Mountain_Climbers` | `exercises/Mountain_Climbers/0.jpg` | `exercise_images/mountain-climber/0.jpg` | high — alias "mountain climbers" |
| `one-arm-dumbbell-row` | One-Arm Dumbbell Row | EXACT | One-Arm Dumbbell Row | `One-Arm_Dumbbell_Row` | `exercises/One-Arm_Dumbbell_Row/0.jpg` | `exercise_images/one-arm-dumbbell-row/0.jpg` | high — exact name |
| `pallof-press` | Pallof Press | EXACT | Pallof Press | `Pallof_Press` | `exercises/Pallof_Press/0.jpg` | `exercise_images/pallof-press/0.jpg` | high — exact name |
| `parallel-bar-triceps-dip` | Parallel Bar Triceps Dip | LIKELY | Parallel Bar Dip | `Parallel_Bar_Dip` | `exercises/Parallel_Bar_Dip/0.jpg` | `exercise_images/parallel-bar-triceps-dip/0.jpg` | high — alias "parallel bar dip" |
| `plate-front-raise` | Plate Front Raise | LIKELY | Front Plate Raise | `Front_Plate_Raise` | `exercises/Front_Plate_Raise/0.jpg` | `exercise_images/plate-front-raise/0.jpg` | high — token-set match |
| `push-up` | Push-Up | EXACT | Pushups | `Pushups` | `exercises/Pushups/0.jpg` | `exercise_images/push-up/0.jpg` | high — exact name |
| `reverse-barbell-curl` | Reverse Barbell Curl | EXACT | Reverse Barbell Curl | `Reverse_Barbell_Curl` | `exercises/Reverse_Barbell_Curl/0.jpg` | `exercise_images/reverse-barbell-curl/0.jpg` | high — exact name |
| `reverse-grip-triceps-pushdown` | Reverse-Grip Triceps Pushdown | EXACT | Reverse Grip Triceps Pushdown | `Reverse_Grip_Triceps_Pushdown` | `exercises/Reverse_Grip_Triceps_Pushdown/0.jpg` | `exercise_images/reverse-grip-triceps-pushdown/0.jpg` | high — exact name |
| `romanian-deadlift` | Romanian Deadlift | EXACT | Romanian Deadlift | `Romanian_Deadlift` | `exercises/Romanian_Deadlift/0.jpg` | `exercise_images/romanian-deadlift/0.jpg` | high — exact name |
| `russian-twist` | Russian Twist | EXACT | Russian Twist | `Russian_Twist` | `exercises/Russian_Twist/0.jpg` | `exercise_images/russian-twist/0.jpg` | high — exact name |
| `seated-calf-raise` | Seated Calf Raise | EXACT | Seated Calf Raise | `Seated_Calf_Raise` | `exercises/Seated_Calf_Raise/0.jpg` | `exercise_images/seated-calf-raise/0.jpg` | high — exact name |
| `seated-dumbbell-shoulder-press` | Seated Dumbbell Shoulder Press | LIKELY | Dumbbell Shoulder Press | `Dumbbell_Shoulder_Press` | `exercises/Dumbbell_Shoulder_Press/0.jpg` | `exercise_images/seated-dumbbell-shoulder-press/0.jpg` | high — alias "dumbbell shoulder press" |
| `seated-leg-curl` | Seated Leg Curl | EXACT | Seated Leg Curl | `Seated_Leg_Curl` | `exercises/Seated_Leg_Curl/0.jpg` | `exercise_images/seated-leg-curl/0.jpg` | high — exact name |
| `side-plank` | Side Plank | LIKELY | Side Bridge | `Side_Bridge` | `exercises/Side_Bridge/0.jpg` | `exercise_images/side-plank/0.jpg` | high — alias "side bridge" |
| `smith-machine-bench-press` | Smith Machine Bench Press | EXACT | Smith Machine Bench Press | `Smith_Machine_Bench_Press` | `exercises/Smith_Machine_Bench_Press/0.jpg` | `exercise_images/smith-machine-bench-press/0.jpg` | high — exact name |
| `smith-machine-squat` | Smith Machine Squat | EXACT | Smith Machine Squat | `Smith_Machine_Squat` | `exercises/Smith_Machine_Squat/0.jpg` | `exercise_images/smith-machine-squat/0.jpg` | high — exact name |
| `spider-curl` | Spider Curl | EXACT | Spider Curl | `Spider_Curl` | `exercises/Spider_Curl/0.jpg` | `exercise_images/spider-curl/0.jpg` | high — exact name |
| `straight-arm-cable-pulldown` | Straight-Arm Cable Pulldown | LIKELY | Straight-Arm Pulldown | `Straight-Arm_Pulldown` | `exercises/Straight-Arm_Pulldown/0.jpg` | `exercise_images/straight-arm-cable-pulldown/0.jpg` | high — alias "straight arm pulldown" |
| `sumo-deadlift` | Sumo Deadlift | EXACT | Sumo Deadlift | `Sumo_Deadlift` | `exercises/Sumo_Deadlift/0.jpg` | `exercise_images/sumo-deadlift/0.jpg` | high — exact name |
| `superman` | Superman | EXACT | Superman | `Superman` | `exercises/Superman/0.jpg` | `exercise_images/superman/0.jpg` | high — exact name |
| `tate-press` | Tate Press | EXACT | Tate Press | `Tate_Press` | `exercises/Tate_Press/0.jpg` | `exercise_images/tate-press/0.jpg` | high — exact name |
| `trap-bar-deadlift` | Trap Bar Deadlift | EXACT | Trap Bar Deadlift | `Trap_Bar_Deadlift` | `exercises/Trap_Bar_Deadlift/0.jpg` | `exercise_images/trap-bar-deadlift/0.jpg` | high — exact name |
| `triceps-pushdown` | Triceps Pushdown | EXACT | Triceps Pushdown | `Triceps_Pushdown` | `exercises/Triceps_Pushdown/0.jpg` | `exercise_images/triceps-pushdown/0.jpg` | high — exact name |
| `wide-grip-lat-pulldown` | Wide-Grip Lat Pulldown | EXACT | Wide-Grip Lat Pulldown | `Wide-Grip_Lat_Pulldown` | `exercises/Wide-Grip_Lat_Pulldown/0.jpg` | `exercise_images/wide-grip-lat-pulldown/0.jpg` | high — exact name |
| `wide-grip-pull-up` | Wide-Grip Pull-Up | LIKELY | Pullups | `Pullups` | `exercises/Pullups/0.jpg` | `exercise_images/wide-grip-pull-up/0.jpg` | high — alias "pull-up" |
| `zottman-curl` | Zottman Curl | EXACT | Zottman Curl | `Zottman_Curl` | `exercises/Zottman_Curl/0.jpg` | `exercise_images/zottman-curl/0.jpg` | high — exact name |

## REVIEW (NOT enriched)

0 entries required review. All matches were exact-name, exact-alias, or token-set matches (no low-confidence fuzzy matches were accepted). 79 AURON exercises had no confident free-exercise-db match and were left with empty `imageRefs` (placeholder retained).

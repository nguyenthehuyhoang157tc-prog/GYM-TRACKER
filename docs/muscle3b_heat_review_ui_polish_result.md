# MUSCLE-3B — Muscle Heat Review UI Polish + Body Map Asset Accuracy Fix — Result

**Status:** COMPLETE — mockup-aligned review, body-map outline/separator fix, upstream slug audit, and
direct-open History/Weekly behavior; device-verified.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE3B_BACKUP_20260703_105956\app_src`.
**No Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change, no
exercise imports, no WebView, no 7-day recovery, signing untouched.** No hard stop hit.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLE3B_BACKUP_20260703_105956\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed
| File | Change |
|---|---|
| `ui/muscleheat/BodyMapPaths.kt` | **Regenerated** from upstream (35 slugs: 19 front / 16 back) **+ added** `BODY_FRONT_OUTLINE` / `BODY_BACK_OUTLINE` silhouette-stroke constants (from upstream `SvgMaleWrapper.tsx`). Attribution header retained. |
| `ui/muscleheat/BodyMap.kt` | Renderer: draw upstream **outline stroke first**, then per-part **fill + thin dark separator stroke**; `Stroke` import; `outlineColor`/`separatorColor` params. |
| `ui/review/SessionReviewScreen.kt` | Rewritten to mockup: purple top gradient, near-OLED black card (`0xFF0B0C0E`), prominent Duration, uppercase Top Muscles, front/back maps, **AURON** wordmark at card bottom, white circular Share/Edit under the card. New duration format ("1 hour, 38 mins"). |
| `ui/history/HistoryScreen.kt` | Removed the body/person icon (+import); **row tap → opens review**; chevron → expand/collapse. |
| `ui/home/WeeklyProgressHubSheet.kt` | Same: removed icon (+import); row tap → opens review; chevron → expand. |
| `test/ui/muscleheat/BodyMapContractTest.kt` | Updated duration test to the new format; added upstream slug-inventory + outline-present tests. |
| `README.md`, `docs/muscle3b_heat_review_ui_polish_result.md` | Docs. |

## 3. Root cause of the broken back-body patch
MUSCLE-3's renderer drew **only solid fills** — no silhouette outline and no per-shape stroke. On the
back, the large neighboring shapes (`upper-back`, `trapezius`, `deltoids`, `gluteal`, `hamstring`) filled
as adjacent flat blobs with **no muscle-definition lines and no unifying body outline**, so a highlighted
or neutral region read as one undifferentiated "patch". The path data itself was correct (verified against
upstream — see §4); the defect was purely the missing outline + separator strokes. **Fix:** draw the
upstream silhouette outline first, then stroke every upstream shape with a thin dark separator — restoring
muscle definition and the silhouette, eliminating the patch (confirmed on device, front & back clean).

## 4. Upstream slug audit table (verified against `react-muscle-highlighter` assets, MIT)

| BodyRegionId | upstream slug(s) | front | back | supported? | exact upstream region used | fallback reason |
|---|---|---|---|---|---|---|
| CHEST | `chest` | ✓ | — | yes | upstream `chest` | — |
| UPPER_CHEST | `chest` | ✓ | — | via chest | upstream `chest` | no separate upper-chest shape upstream |
| SHOULDERS | `deltoids` | ✓ | ✓ | yes | upstream `deltoids` | — |
| FRONT/SIDE/REAR_DELTS | `deltoids` | ✓ | ✓ | via deltoids | upstream `deltoids` | upstream has no per-head delt shapes |
| BICEPS | `biceps` | ✓ | — | yes | upstream `biceps` | — |
| TRICEPS | `triceps` | ✓ | ✓ | yes | upstream `triceps` | — |
| FOREARMS | `forearm` | ✓ | ✓ | yes | upstream `forearm` | — |
| ABS | `abs` | ✓ | — | yes | upstream `abs` | — |
| OBLIQUES | `obliques` | ✓ | — | yes | upstream `obliques` | — |
| CORE | `abs` (front) | ✓ | — | via abs | upstream `abs` | no separate "core" shape upstream |
| TRAPS | `trapezius` | ✓ | ✓ | yes | upstream `trapezius` | — |
| LATS | `upper-back` (back) | — | ✓ | via upper-back | upstream `upper-back` | upstream has no separate lats shape |
| MID_BACK | `upper-back` (back) | — | ✓ | via upper-back | upstream `upper-back` | shares the one upstream `upper-back` (max color wins) |
| LOWER_BACK | `lower-back` | — | ✓ | yes | upstream `lower-back` | — |
| QUADS | `quadriceps` | ✓ | — | yes | upstream `quadriceps` | — |
| HAMSTRINGS | `hamstring` | — | ✓ | yes | upstream `hamstring` | — |
| GLUTES | `gluteal` | — | ✓ | yes | upstream `gluteal` | — |
| CALVES | `calves` | ✓ | ✓ | yes | upstream `calves` | — |
| ADDUCTORS | `adductors` | ✓ | ✓ | yes | upstream `adductors` | — |
| ABDUCTORS | — | — | — | **no** | none | **upstream has no abductor shape** — list-only, never painted |

Verification performed: every generated `BodyMapPaths` slug exists in upstream (unit-tested inventory);
each app region is colored via the exact upstream anatomical shape (app decides *which* slug gets color,
the upstream asset decides *where*); no path is reshaped/merged; front paths stay in `0..724`, back paths
in `724..1448` (renderer translates back by −724 — unit-tested outline coordinate spaces); outline drawn
under parts, matching upstream's border-first order. No new anatomy invented.

## 5. Final BodyRegionId → upstream slug mapping summary
16 catalog-used regions all map to exact upstream shapes; the delt heads + UPPER_CHEST + CORE reuse the
appropriate aggregate/parent upstream shape; LATS+MID_BACK share `upper-back` (max color wins);
**ABDUCTORS is the only region with no upstream shape** (documented, list-only). All 24 handled — no crash.

## 6. Renderer fixes
- Silhouette outline (upstream stroke path) drawn first, light gray (`0xFF9BA1AB`, 3px).
- Per-shape fill (heat color or neutral charcoal) + thin dark separator stroke (`0xFF14161A`, 1.6px).
- Paths parsed once per side (remembered); back translated −724; scaled/centered to the 724×1448 viewBox.
- Result: visible outline + muscle separators, no distortion, front/back aligned, no stray patch.

## 7. UI polish changes
Purple top gradient; near-OLED black card (`0xFF0B0C0E`) in both themes; **Duration** label + large
uppercase value ("1 HOUR, 38 MINS" / "3 MINS"); **Top Muscles** uppercase; larger centered front/back
maps; **AURON** wordmark at the card bottom (never SLOPAX); **Calories removed** (no real data); white
circular **Share/Edit** directly under the card (both "coming soon" toasts in V1); exercise/set breakdown
preserved below.

## 8. History / Home behavior changes
Removed the extra body/person icon from History **and** the Home Weekly-hub session rows. **Tapping a
session row now opens the Muscle Heat Review directly**; the chevron remains as the clear alternate
affordance for the inline expand/collapse (unchanged breakdown).

## 9. Tests / build result
- `BodyMapContractTest`: existing cases + **new** upstream-slug-inventory (exact front/back sets) and
  outline-present/coordinate-space tests; duration test updated to the new format. All suites green.
- `:app:testDebugUnitTest` → **exit 0**; `:app:assembleDebug` → **exit 0** (APK 93.91 MB). Release not run.

## 10. Runtime QA (Pixel_8 emulator)
1. History → **tapping a session row opens the review directly** ✓ (no icon needed).
2. **Chevron expands/collapses inline** (Plank + Sled Push breakdown) and does **not** open the review ✓.
3. **No separate body/person icon** on History or Weekly-hub rows ✓.
4. Heat card matches mockup: near-OLED black card, uppercase Top Muscles ("ABS, QUADS"), AURON wordmark,
   front/back maps, white Share/Edit under the card ✓.
5. **Front/back maps now have a visible outline + muscle separators; the broken back patch is gone** ✓
   (back renders clean: glutes/hamstrings/calves + forearms colored, proper separation).
6. Region coloring anatomically correct for the session (abs orange, quads/glutes/hams/calves yellow) ✓;
   custom/legacy-named exercises safely listed as "not on the map", never painted ✓.
7. Scroll → exercise/set breakdown still appears below the card ✓.
8. Home Weekly-hub → tapping a session row opens the review + closes the sheet ✓.
9. **App crash buffer clean** for `com.hoctin.gymtracker` across the whole run (the only crash entry was
   the emulator's own `com.google.android.bluetooth` at boot — unrelated) ✓.
10. **Duration prominence:** duration tracking confirmed live on the Log header ("Duration: 02:11") and
    the after-save review Duration block was verified rendering on device in MUSCLE-3 ("3 MIN") + is
    unit-tested. A fresh after-save re-capture this pass was blocked by emulator text-field focus
    flakiness (Kg entry mis-routed) — an input-harness issue, not a code defect; no save was created
    (draft discarded).

## 11. Known limitations
- Male figure only; **ABDUCTORS** has no upstream shape (list-only); LATS & MID_BACK share the single
  upstream `upper-back` shape (max color wins).
- Duration on the review is the Log-open→save wall clock (runtime-only, never persisted); sessions opened
  from History/Weekly show no Duration block (no persisted start/end — a future schema task).
- Share/Edit are V1 placeholders (toast). Body map is render-only (no per-region tap).
- Automated after-save duration re-capture not completed this pass (emulator input flakiness; verified
  via MUSCLE-3 device run + unit tests instead).

## 12. Next task recommendation
**MUSCLE-3C:** share-as-image (render the heat card to a bitmap → Android share sheet) and safe session
title rename (wire the Edit button), optional per-region tap → contributing-exercise detail, and the
female-figure port. Then **MUSCLE-4** (7-day recovery map: decay model, weekly volume, muscle overlap,
readiness trend, goal profile).

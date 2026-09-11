# HISTORY-2 — Workout History Timeline Redesign + Session Review Navigation — Result

**Status:** COMPLETE — month-based archive matching the mockups, direct-open review navigation, global
OLED gradient polish; device-verified.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_HISTORY2_BACKUP_20260704_090700\app_src`.
**No Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change, no
exercise imports, no share/edit/female-figure work, signing untouched.**

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_HISTORY2_BACKUP_20260704_090700\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed
| File | Change |
|---|---|
| `app/src/main/res/drawable/empty_history.webp` | **NEW** — the provided thinking illustration imported as a WebP resource (magic bytes verified RIFF/WEBP; no PNG conversion needed). |
| `ui/history/HistoryViewModel.kt` | **Rewritten grouping:** weeks → swipeable calendar **months** (`HistoryMonth`, newest first, spanning current month back to the earliest session — empty months included). Session/exercise/set models unchanged. |
| `ui/history/HistorySessionMapper.kt` | `HistorySession` += `wallDurationSeconds` (persisted v7 wall-clock duration). |
| `ui/history/HistoryScreen.kt` | **Rewritten:** month pager UI (title, swipe-synced year/month selector with edge peeks + tap-to-jump, per-month workout list, empty-month illustration state, compact WORKOUT/VOLUME/REPS tables). Week/session cards deleted; `ExerciseBlock`/`TableRow`/`NonRepSetRow` kept (review reuses them). |
| `ui/home/WeeklyProgressHubSheet.kt` | Hub session cards: removed the chevron + inline expansion — tap opens the review directly. |
| `ui/theme/Theme.kt` | `auronScreenBackground` (dark): hollow-purple top gradient fading into **true #000000** — applied across Home/Plan/History/Settings/Detail/Catalog/Onboarding via the shared modifier. Light theme unchanged. |
| `README.md`, this doc | Docs. |

## 3. UI behavior — before → after
- **Before:** week-collapse archive (week card → expand → session card → expand → per-exercise cards);
  session row opened the review, chevrons everywhere; generic no-data card; flat page background.
- **After (mockup-faithful):** month-based archive — big **Workout History** title, swipeable
  **year-over-month selector** with adjacent months peeking and translating live with the drag
  (tap a peek to jump), each month a clean list: bold day title, uppercase
  `TIME, MONTH DAYth YEAR — DURATION` subtitle (persisted wall duration; hidden when unknown), and a
  **compact exercise table** (rounded dark header row WORKOUT/VOLUME/REPS, tight rows, PR trophies, no
  per-exercise cards, no expand/collapse). **Tapping a workout opens the Muscle Heat Review directly.**
  Empty months show the imported illustration + "Interesting, you don't have any workouts for this
  period." Deviation from mockup (documented): no back arrow on the header — History is a top-level tab
  behind the floating dock, not a pushed screen.

## 4. Month navigation design
`HistoryViewModel` emits `months` newest→oldest (index 0 = current month), spanning down to the earliest
saved session and **including empty months** so the archive is continuous. `HorizontalPager` renders one
month per page; a custom `MonthHeader` draws current±1 labels positioned by
`currentPage + currentPageOffsetFraction` (translation ≈ 0.62 × width per page, alpha falloff), so the
selector tracks the swipe exactly like the mockup's peeking months.

## 5. Workout table design
Header row in a rounded `surfaceVariant` box (uppercase letterspaced labels), rows: name (2-line max,
PR trophy inline), VOLUME and REPS right-aligned. **Rep work:** total kg (formatVolume) + "N reps"
(bodyweight-only → "N/A" volume). **Time/distance work — never fake reps/kg:** VOLUME shows distance
(unit-aware) or duration; REPS shows duration when distance took the volume cell, else "—" (e.g. Plank →
`01:31:45 / —`, Sled Push → `100 ft / 00:40`; verified on device). The Session Review breakdown keeps the
same table language (shared `ExerciseBlock`: SET/VOLUME/REPS/RIR for rep work, measurement rows for
time/distance).

## 6. Weekly/Home behavior
Hub session entries keep their stats chips but lost the chevron/expand UX — **tap opens the Muscle Heat
Review directly**, identical to History (the review holds the full breakdown).

## 7. Tests / build result
`:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0** (all suites unchanged and green — the redesign is
UI/ViewModel-layer; no engine/mapper contract touched beyond the additive `wallDurationSeconds`).
`:app:assembleDebug` → **exit 0** (APK 92.8 MB). One intermediate compile fix: the pager APIs still
require `@OptIn(ExperimentalFoundationApi::class)` on this Compose version.

## 8. Runtime QA (Pixel_8) — PASSED
1. History opens on the current month: gradient → black, title, **2026/JULY** selector, workout items
   with persisted duration in the subtitle ("— 5 MINS"), compact tables with PR trophies ✓.
2. **Swipe** navigates months (JULY → JUNE with live header tracking) ✓.
3. Month with workouts shows the compact tables; **time/distance rows show meaningful values**
   (Plank `01:31:45/—`, Sled `100 ft/00:40`) — no fake reps/kg ✓.
4. **Tap workout → Muscle Heat Review opens directly** ✓; heat card correct (top muscles, colored maps —
   including the MUSCLE-HEAT-1 MID_BACK→TRAPS overlay glowing on a row session, and the stimulus-quality
   colors); catalog-named sessions show no "not on the map" footnote ✓.
5. Scroll: exercise breakdown below the card (SET/VOLUME/REPS/RIR + duration rows) ✓.
6. **Home Weekly-hub session tap → review directly** (cards chevron-free; persisted "5 MINS" shown) ✓.
7. **App crash buffer clean** throughout ✓.
8. Empty-month illustration state: implemented and compiled; **not observable on this emulator dataset**
   (every month in range has sessions). The pager includes empty months whenever the range contains them
   (e.g. data in Feb → Mar–Jul render the empty state), matching the user's own device scenario.

## 9. Known limitations
- Empty-month state not exercised on the QA device (dataset covers every month in range) — code path is
  straightforward Compose and included in the build; verify visually once a gap month exists.
- Month selector renders current±1 labels only (a fast multi-page fling briefly shows one label).
- No back arrow on History (top-level tab; mockup's arrow implies a pushed screen — documented deviation).
- History no longer surfaces the per-exercise info popup (mockup has none); it remains available in the
  review breakdown via the ⓘ icon.
- Pre-v7 sessions still show no duration in the subtitle (persisted-duration rule).
- "Cards with ultra-thin right/bottom light edge" polish: the existing UI-1 hard-shadow/border card
  language was kept (already provides the pop); a dedicated edge-highlight pass was deferred as
  higher-risk global churn.

## Next task recommendation
MUSCLE-3C (share-as-image + session rename via the Edit button) — the review is now the single hub all
surfaces funnel into, so sharing is the highest-value next step. Then MUSCLE-4 (7-day recovery map).

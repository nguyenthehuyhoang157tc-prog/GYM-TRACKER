## Read first

`docs/engineering_notes.md` carries the durable, hard-won knowledge that is not obvious from
the source: the AGSL liquid-glass traps (`shouldAutoInvalidate`, bevel/overhang blending, the
`DEBUG_EFFECT` ladder), the Compose dialog-inset bug, the set-type domain plan and the design
rules. Read it before non-trivial work.

## graphify

This project has a knowledge graph at `graphify-out/` with god nodes, community structure and
cross-file relationships.

**Important — read this before following the rules below.** The `graphify` CLI is **not
installed** and is not available as a standard package, so the graph **cannot be regenerated**
here. `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` are committed as a
**snapshot dated 2026-09-06** and drift as the code changes. Treat them as a navigation aid and
verify anything important against the actual source. Do not assume `graphify update` succeeded.
The dated `graphify-out/2026-*/` folders and `graphify-out/cache/` are git-ignored (163 MB
total, ~9 MB useful).

Rules (only where the CLI is actually available):
- For codebase questions, run `graphify query "<question>"`. Use `graphify path "<A>" "<B>"`
  for relationships and `graphify explain "<concept>"` for focused concepts. These return a
  scoped subgraph, usually much smaller than `GRAPH_REPORT.md` or raw grep output.
- Read `graphify-out/GRAPH_REPORT.md` for broad architecture review, or when
  query/path/explain do not surface enough context. There is **no** `graphify-out/wiki/`
  directory in this repo.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API
  cost) — but only if the CLI exists.
- Without the CLI, read `graphify-out/graph.json` directly.

## Compose `Dialog` window insets (bit us twice — read before touching any sheet/modal)

A Compose `Dialog` created with `DialogProperties(decorFitsSystemWindows = true)` — which is the
**default** — has its decor consume the window insets. **Every inset read inside then resolves to
`0`**: `navigationBarsPadding()`, `statusBarsPadding()`, `WindowInsets.navigationBars`, etc.

Consequences and rules:

- Any dialog with **bottom-anchored content or pinned action buttons must set
  `decorFitsSystemWindows = false`** (plus `usePlatformDefaultWidth = false` for full width).
- **Raising sheet height or button height does NOT fix the resulting clipping.** If a bottom button
  looks cut off by the system gesture bar, suspect the inset plumbing, not the dimensions. Two
  earlier attempts to fix the Advanced Set sheet by increasing heights had zero effect.
- Apply the bottom clearance **before** the `heightIn / clip / background / border` chain so the
  visible card clears the bar. Padding after `clip` only insets the content and leaves the card's
  own bottom edge in the gesture zone.
- Use a floor — gesture nav reports only a thin bar: `maxOf(realInset, 34.dp)`.
- **After flipping a dialog to `decorFitsSystemWindows = false`, check what the scrim now covers.**
  A dark scrim over the status/nav bars makes icons unreadable in light theme, because
  `GymTrackerApp.kt:807` sets `isAppearanceLightStatusBars = !darkTheme`. Fix: inset the scrim out
  of the system-bar strips (pad *before* `.background()`), keeping the content edge-to-edge.
- The bug only bites **full-height / bottom-anchored** dialog content. Short centered confirm cards
  are unaffected, so leaving the `true` default is fine for them — don't churn them.
- Inset reads in **screen-level** composables are fine: `MainActivity.kt:19` sets the *activity*
  window edge-to-edge, so insets resolve normally there. The problem is specific to dialog windows.

Reference implementation: `ui/components/AuronBottomSheet.kt` (`AuronCompactModalSheet`), which
documents all of the above inline. Full root-cause write-up:
`docs/log_v4a_log_workout_redesign_result.md` → "Follow-up fix (September 3, 2026)".

## Build / verification conventions

- After adding or editing any Composable, run `./gradlew :app:compileDebugKotlin` immediately. A
  previously green build does not prove the current file compiles.
- Don't pipe Gradle output through `tail` — grep for `^\s*e:` / `BUILD SUCCESSFUL` / `BUILD FAILED`
  or the real error lines get truncated.
- Gradle on Windows/Git Bash needs forward-slash JDK paths:
  `export JAVA_HOME='D:/jdk17/jdk-17.0.19+10'; export PATH="/d/jdk17/jdk-17.0.19+10/bin:$PATH"`
- Back up files before editing shared components (convention:
  `_backup_<task>_<YYYYMMDD>_<HHMMSS>/`).

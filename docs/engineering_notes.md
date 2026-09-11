# AURON — Engineering Notes

Durable, hard-won knowledge that is **not** obvious from reading the source. Read this before
making non-trivial changes. Distilled from the project's working memory (September 2026).

---

## 1. Project facts

- Android app, local-first. Kotlin + Jetpack Compose (Material 3) + Navigation Compose + Room
  + SharedPreferences. Gradle Kotlin DSL, MVVM.
- Package and database name remain `com.hoctin.gymtracker` / `gymtracker` — the app was
  rebranded to **AURON** (from GYMATE) but the identifiers were intentionally not renamed.
- **Single Gradle module**: `settings.gradle.kts` contains only `include(":app")`.
- Gradle 8.11.1, JDK 17, versions centralised in `gradle/libs.versions.toml`.
- The three top-level folders `AndroidLiquidGlass-kmp/`, `PrismalAGSL-master/` and
  `react-muscle-highlighter-main/` are **reference material only**. They are not Gradle
  modules, are not compiled, and nothing in the build references them. Deleting them cannot
  break the build.
- `README.md` is the product/feature overview. `docs/` holds the audit and design history.

---

## 2. Build and verification

- **After adding or editing any Composable, run `./gradlew :app:compileDebugKotlin`
  immediately.** A previously green build does not prove the current file compiles.
- **Do not pipe Gradle output through `tail`** — the real error lines get truncated. Grep for
  `^\s*e:`, `BUILD SUCCESSFUL` or `BUILD FAILED` instead.
- JDK 17 is required. On Windows/Git Bash use forward-slash paths:
  `export JAVA_HOME='D:/jdk17/jdk-17.0.19+10'; export PATH="/d/jdk17/jdk-17.0.19+10/bin:$PATH"`
- The Android SDK is resolved from `local.properties` (`sdk.dir`) **or** the `ANDROID_HOME` /
  `ANDROID_SDK_ROOT` environment variable. `local.properties` is deliberately not committed —
  it holds a machine-specific absolute path. On a CI runner or a cloud sandbox, export
  `ANDROID_HOME` instead; do not add `local.properties`.
- Unit tests: `./gradlew :app:testDebugUnitTest` (152 tests in `domain/ProgressiveOverload`
  must stay green — see §5).
- Before editing a shared component, back up to `_backup_<task>_<YYYYMMDD>_<HHMMSS>/`
  (project convention; those folders are git-ignored).

### Line endings — do not hash raw bytes

This machine has `core.autocrlf = true`, so a fresh checkout writes **CRLF** into the working
tree even though every blob in git is **LF**. The two are equivalent to git, but not to code
that reads a file as bytes.

`WearableProtectedBoundaryTest.sleepSelectionRepairLeavesProtectedModelBytesFrozen` froze three
privacy-critical files by comparing a raw SHA-256 against a hard-coded digest. Those digests
were the **LF** values, so the guard passed in the workspace (whose files happen to be LF) but
failed on **every clean clone** — `MuscleRecovery.kt`, `MuscleHeatEngine.kt` and
`ProgressiveOverload.kt` all hashed differently purely because of CRLF. That is exactly the
false alarm that would make a cloud agent "fix" source that was never broken.

The guard now normalises CRLF → LF before hashing (`sha256()` in that test), so the expected
digests hold on Windows, Linux, CI and agent sandboxes alike, while still detecting any real
content change. **If you add another byte-frozen guard, normalise line endings first.**

Verified both ways: 468 tests, 0 failures in the LF workspace *and* in a CRLF clone.

A repo-level `.gitattributes` (`* text=auto eol=lf`) would remove the CRLF/LF split at the
source. It is deliberately **not** added yet: the blobs are already LF so it would not change
the repository, but it would rewrite the working-tree line endings and produce a large
one-off churn diff — not worth doing casually in a repo whose object store has already proved
fragile.

---

## 3. Liquid glass — the hard-won part

**Backdrop refraction WORKS on Compose 1.6.8 — device-confirmed (Pixel 8 / API 37).** An
earlier note claimed it needed Compose 1.7+; that was wrong. The blocker was
`shouldAutoInvalidate`, not the BOM. Shipped value: `DEBUG_EFFECT = 0`.

### Architecture

The AGSL lens runs on the layer that **contains** the scrolling content (the NavHost), not on
the dock. The dock is a sibling drawn afterwards, so icons and labels never pass through the
shader and stay crisp. Kyant's library uses `graphicsContext.createGraphicsLayer()` +
`drawLayer()` (Compose 1.7+), but `Modifier.graphicsLayer(renderEffect = …)` is a valid
substitute on 1.6.8.

### CRITICAL: never rely on the `graphicsLayer { }` lambda for RenderEffect work

Both `BlockGraphicsLayerModifier` and `SimpleGraphicsLayerModifier` declare
**`shouldAutoInvalidate = false`**, and `BlockGraphicsLayerModifier.update()` only assigns
`node.layerBlock = block`. **The lambda is not guaranteed to re-run.** When it doesn't:

- no `setFloatUniform` runs → every uniform keeps its zero default
- `renderEffect = effect` is never assigned → **the layer has no effect at all**

This presents as *"Kotlin logs fire, the lens measures correctly, the objects are constructed,
and nothing renders."* It cost a long debugging session.

**Correct pattern (verified on device):**

1. Bake the uniforms at shader creation, keyed on the full geometry:
   `remember(source, centerX, centerY, halfW, halfH, lens, density) { RuntimeShader(src).apply { setFloatUniform(...) } }`
2. Use the **direct-parameter overload**
   `graphicsLayer(renderEffect = effect, compositingStrategy = Offscreen)` — no lambda.
   `Offscreen` is required for a RenderEffect to apply at all.
3. Keying on geometry produces a *new* RenderEffect per change, so `update()` re-applies it.

**`RuntimeShader.setFloatUniform()` throws `IllegalArgumentException`** for a uniform the
program does not declare. Gate the writes on which shader is loaded
(`shaderSource === REAL_SHADER`). Never wrap it in try/catch — that would swallow a genuinely
missing uniform in the real shader.

### Two-zone edge (verified by direct Snell computation, ior 1.5)

- bevel < 90° samples **inward** → magnification
- overhang > 90° samples **OUTWARD** → this is the contour wrap

Sampling only inward can magnify but can never produce the wrap. `ior = 1.0` must give zero
displacement — **including the dispersion term**, which is *caused by* the index varying with
wavelength.

### Never branch hard between bevel and overhang — blend them

`if (t >= rimStart)` leaves a **~22px tear**: the bevel is still ≈ −21px inward while the
overhang restarts at ≈ 0. Use `mix(-dB, +dO, smoothstep(rimStart, 1.0, t))`. `smoothstep` has
zero value *and* zero slope at both ends, so the fold is C1. Measured: the jump went from
21.6px to 0.35px.

**Retuning constants cannot fix a structural discontinuity.**

Displacement is **signed** (`-` inward / `+` outward); the sample is always
`coord + g * disp`. Consequence for tests: magnitude assertions need `abs(...)`, and past
`rimStart` the magnitude is deliberately **non-monotonic** (that reversal *is* the wrap) — only
assert growth *inside* the bevel.

Side effect: blending narrows the true outward wrap from ~5dp to ~2dp while roughly doubling
the fold amplitude. `rimReachDp` controls **amplitude**, not width; to widen the wrap you lower
`refractionAmountDp`, which trades away magnification.

### Verify shader changes in the built APK's dex, not just in tests

A whole pass was silently reverted once and **the tests still passed** — the tests validate the
Kotlin port, never the AGSL itself. After any shader change, confirm
`grep -c smoothstep <source>` **and** grep the token out of `classes*.dex` in the built APK.

### Dock visibility: use exact routes, not `hierarchy`

`hierarchy` walks the destination **and all its parents**, so pushed routes match their
top-level parent and the dock leaks onto detail/log screens. Use
`private val dockVisibleRoutes = topLevelDestinations.map { it.route }.toSet()` with
`currentDestination?.route in dockVisibleRoutes`.

Gate the lens on the **same hoisted** `showDock` (`backdropLensActive = BACKDROP_AVAILABLE &&
showDock`) and zero the geometry when it goes false. Never redeclare `showDock` locally.
Selection state may still use `hierarchy`.

### Diagnostic ladder: `AuronGlass.DEBUG_EFFECT`

`0` real · `1` solid-magenta AGSL (proves AGSL runs) · `2` solid-red
`ColorMatrixColorFilter` (no AGSL — proves the RenderEffect attaches) · `3` alpha 0.5 (proves
the layer exists). `DEBUG_TINT` is derived (`!= 0`) and gates logging.

**All levels must use the same direct overload** or the comparison is invalid. Level 1 PASSED;
keep `0`. Do not add blur — it masks the refraction signature.

---

## 4. Compose `Dialog` window insets (bit us twice)

A Compose `Dialog` created with `DialogProperties(decorFitsSystemWindows = true)` — the
**default** — has its decor consume the window insets. **Every inset read inside then resolves
to `0`**: `navigationBarsPadding()`, `statusBarsPadding()`, `WindowInsets.navigationBars`, etc.

- Any dialog with **bottom-anchored content or pinned action buttons must set
  `decorFitsSystemWindows = false`** (plus `usePlatformDefaultWidth = false` for full width).
- **Raising sheet height or button height does NOT fix the resulting clipping.** If a bottom
  button looks cut off by the gesture bar, suspect the inset plumbing, not the dimensions. Two
  earlier attempts to fix the Advanced Set sheet by increasing heights had zero effect.
- Apply the bottom clearance **before** the `heightIn / clip / background / border` chain, so
  the visible card clears the bar. Padding applied after `clip` only insets the content and
  leaves the card's own bottom edge inside the gesture zone.
- Use a floor — gesture nav reports only a thin bar: `maxOf(realInset, 34.dp)`.
- **After flipping a dialog to `decorFitsSystemWindows = false`, check what the scrim now
  covers.** A dark scrim over the status/nav bars makes icons unreadable in light theme,
  because `GymTrackerApp.kt` sets `isAppearanceLightStatusBars = !darkTheme`. Fix: inset the
  scrim out of the system-bar strips (pad *before* `.background()`), keeping the content
  edge-to-edge.
- The bug only bites **full-height / bottom-anchored** dialog content. Short centred confirm
  cards are unaffected — leave the `true` default for them and don't churn them.
- Inset reads in **screen-level** composables are fine: `MainActivity.kt` sets the *activity*
  window edge-to-edge, so insets resolve normally there. The problem is specific to dialog
  windows.

Reference implementation: `ui/components/AuronBottomSheet.kt` (`AuronCompactModalSheet`), which
documents all of the above inline.

---

## 5. Domain notes

- **Set types are currently transient.** `LoggedSetEntity` / `SetInput` have no set-type or
  parent-key fields, and `buildInputs()` drops `SetDraft.setType`. Warm-ups, dropsets and
  rest-pause sets are saved as ordinary sets. Audit:
  `docs/advset_accuracy_context_audit.md`.
- **SET-TYPE-P0 plan** (approved, gated): filter eligibility **before**
  `ProgressiveOverloadEngine` — this keeps `domain/ProgressiveOverload.kt` frozen and its 152
  tests green. Parent linkage needs no new plumbing
  (`researchRowKey` → `sourceDraftKey` → `sourceDraftKeyBySetId`). Migration v11→v12 is
  additive: 5 nullable columns, no FK on `parentSetId`, orphans promoted to `WORKING`.
  Blueprint: `docs/set_type_architecture_and_evidence_plan.md`.
  MODEL_LIMITATION: no source quantifies local recovery cost per set type, and no 1RM is
  stored, so warm-ups cannot be auto-detected.

---

## 6. Repository hygiene — do not undo this

The workspace is **not** source-only. It contains ~2.7 GB of `_backup_*/` snapshots, an
installed Electron app (`Verdent/`), agent worktrees (`.claude/`) and Gradle caches. All of it
is git-ignored on purpose; see `docs/repo_github_hygiene.md` for the full audit.

Rules that must not be broken:

- **Never ignore images or JSON globally.** `app/src/main/assets/exercise_images` holds 1,154
  required exercise photos and `app/src/main/res/raw` holds 4 Lottie JSONs. Images are ignored
  only at the **repo root**, via `/`-prefixed patterns.
- **`!gradle/wrapper/gradle-wrapper.jar` is mandatory.** `*.jar` is ignored, but without this
  exception `./gradlew` cannot run at all.
- Match **both** `qa-*` and `qa_*` — the hyphen-only form silently leaked 97 QA XML dumps.
- Do not re-add `_backup_*/`, `qa-*`, `build/`, `.gradle-user/`, `graphify-out/cache/`,
  `external_catalog_sources/` or `Verdent/`.

---

## 7. Tooling notes

- **`graphify` CLI is not installed** and is not available as a standard package. The
  `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` committed to this repo are a
  **snapshot (2026-09-06)**, not a live index — they drift as the code changes. Treat them as
  navigation aids; verify anything important against the actual source. The dated
  `graphify-out/2026-*/` folders and `graphify-out/cache/` are not committed (163 MB total,
  ~9 MB useful).
- If you need to read the graph without the CLI, read `graphify-out/graph.json` directly.
- Long-running `du` commands time out on this tree (87,000+ files). Use a scripted directory
  walk instead.
- `external_catalog_sources/` (raw catalog inputs, ~97 MB) is not committed. The built catalog
  the app actually consumes **is** committed at
  `app/src/main/assets/catalog/catalog_fed_v1.json`.

---

## 8. Design rules

- Source of truth for visual design: `D:\HOCTIN\DESIGN.md`.
- **No purple text** — use `onSurface` / `onSurfaceVariant`.
- Modals: stacked full-width ~64dp buttons, 14dp corner radius, pinned footer.
- Gold PR highlight: border `0xFFF2C86B`, sweep `0xFFFFE7A8`, 10% inner fill.

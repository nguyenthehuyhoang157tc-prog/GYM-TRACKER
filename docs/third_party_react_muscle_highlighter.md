# Third-Party Attribution — react-muscle-highlighter (MIT)

**Upstream project:** `react-muscle-highlighter` — https://github.com/soroojshehryar/react-muscle-highlighter
**Author:** Suroosh Shehryar (per upstream repository/README).
**License:** MIT (full text preserved verbatim at
[`app/src/main/assets/licenses/react_muscle_highlighter_LICENSE.txt`](../app/src/main/assets/licenses/react_muscle_highlighter_LICENSE.txt),
copied unmodified from the upstream `LICENSE` file).
**Local reference source audited:** `D:\HOCTIN\GYM TRACKER\react-muscle-highlighter-main\react-muscle-highlighter-main\`
(the reference folder itself is NOT part of the app build).

## What was copied / adapted (MUSCLE-3)

- **Copied (data only):** the SVG **path strings** and their body-part **slugs** from
  `assets/bodyFront.ts` and `assets/bodyBack.ts` (male figure), machine-extracted into
  [`app/src/main/java/com/hoctin/gymtracker/ui/muscleheat/BodyMapPaths.kt`](../app/src/main/java/com/hoctin/gymtracker/ui/muscleheat/BodyMapPaths.kt)
  (a generated Kotlin constant file with an attribution header). Front viewBox `0 0 724 1448`;
  back paths authored at x ∈ [724, 1448] exactly as upstream.
- **Adapted (rewritten natively):** the rendering idea (fill each slug's paths, color highlighted
  parts). The Android renderer
  ([`BodyMap.kt`](../app/src/main/java/com/hoctin/gymtracker/ui/muscleheat/BodyMap.kt)) is a
  from-scratch Jetpack Compose `Canvas` implementation using Compose's `PathParser` — **no React/
  TypeScript code, no WebView, no upstream components** were ported.
- **Not used:** the female figure assets (`bodyFemaleFront.ts` / `bodyFemaleBack.ts`), the React
  wrapper components (`SvgMaleWrapper` / `SvgFemaleWrapper`, including the silhouette outline
  stroke paths), `utils/differenceWith.ts`, and all package/tooling files.

## License compliance

MIT permits use, copying, modification, and redistribution provided the copyright/permission notice
is included. Compliance here: the unmodified license text ships inside the APK assets
(`assets/licenses/`), this document records provenance, and the generated Kotlin file carries an
attribution header. No upstream copyright/license text was removed or altered.

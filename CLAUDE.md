# AURON — agent brief

Android fitness tracker (local-first). Kotlin + Jetpack Compose Material 3 + Navigation Compose
+ Room. Package/database stay `com.hoctin.gymtracker` / `gymtracker` (rebranded from GYMATE;
identifiers were intentionally not renamed). Gradle 8.11.1, **JDK 17**, single module `:app`.

## Read these before non-trivial changes

| File | What it gives you |
|---|---|
| `docs/engineering_notes.md` | **The hard-won knowledge.** Liquid-glass/AGSL traps, Compose dialog-inset bug, set-type domain plan, design rules. Start here. |
| `README.md` | Product and feature overview |
| `docs/repo_github_hygiene.md` | Why the repo looks the way it does; rules you must not break |
| `AGENTS.md` | Same build/repo conventions in short form |

## Non-negotiable rules

1. **After adding or editing any Composable, run `./gradlew :app:compileDebugKotlin`
   immediately.** A previously green build does not prove the current file compiles. Never
   claim a UI change works without compiling it.
2. **Do not pipe Gradle output through `tail`** — grep for `^\s*e:`, `BUILD SUCCESSFUL` or
   `BUILD FAILED` instead, or the real error is truncated.
3. **Never ignore images, JSON or XML globally in `.gitignore`.**
   `app/src/main/assets/exercise_images` holds 1,154 required exercise photos and
   `app/src/main/res/raw` holds 4 Lottie JSONs. Images are ignored only at the repo root, via
   `/`-prefixed patterns.
4. **Keep `!gradle/wrapper/gradle-wrapper.jar`.** `*.jar` is ignored, but without this
   exception `./gradlew` cannot run at all.
5. **Do not commit `local.properties` or any keystore** (`*.jks`, `keystore.properties`).
   `local.properties` holds a machine-specific SDK path; on a runner, export `ANDROID_HOME`
   instead. Keystores must never enter git history — see the note at the end.
6. Back up a shared component to `_backup_<task>_<YYYYMMDD>_<HHMMSS>/` before editing it.

## Environment

- JDK 17. Windows/Git Bash needs forward-slash paths:
  `export JAVA_HOME='D:/jdk17/jdk-17.0.19+10'; export PATH="/d/jdk17/jdk-17.0.19+10/bin:$PATH"`
- Android SDK via `ANDROID_HOME` / `ANDROID_SDK_ROOT` (or `local.properties` locally).
- Unit tests: `./gradlew :app:testDebugUnitTest` — the 152 tests in
  `domain/ProgressiveOverload` must stay green.
- CI (`.github/workflows/android-ci.yml`) runs compile + unit tests on every push/PR. Treat a
  red CI run as a real failure, not noise.

## graphify

`graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` are committed, but they are a
**snapshot (2026-09-06), not a live index** — they drift as code changes, and the `graphify` CLI
is not installed, so they cannot be regenerated here. Use them as a navigation aid and verify
against the real source. Do not run `graphify update` and assume it worked.

## About the keystore

Release signing uses `gymate-release.jks` + `keystore.properties`, both git-ignored. The repo
being private is **not** a reason to commit them: git history is permanent, a repo can be
flipped to public or gain collaborators by accident, and anyone holding the key can sign APKs
that Android accepts as legitimate updates to this app. If a release build is ever needed from
CI, pass the keystore as a base64-encoded GitHub Actions **secret** and decode it inside the
workflow — never as a committed file.

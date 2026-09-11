# WEARABLE-2A — Read-Only Health Connect Shadow Context — Implementation Report

## Final implementation state

`WEARABLE_2A_REPAIR_IMPLEMENTED_AWAITING_PHYSICAL_VERIFICATION`

`WEARABLE_2B_GATE = CLOSED`

Reason: the physical Galaxy Watch -> Samsung Health -> Health Connect -> AURON Sleep path, Samsung
Health provenance, in-place database upgrade, requested permissions and non-interventional refresh are
verified. The physical split-sleep defect is repaired under versioned selection/snapshot semantics.
The corrected release and first natural prediction-cutoff shadow snapshot still require physical
verification. Emulator fixtures remain QA only and are not Samsung pilot evidence.

## Backup and files changed

Pre-edit source backup: `D:\HOCTIN\GYM_TRACKER_WEARABLE2A_SOURCE_BACKUP_20260812_154224`
(5,523 files; 364,202,850 bytes), outside the repository.

Exactly 35 implementation/configuration/test/documentation files differ from that pre-edit snapshot:

- 5 platform/tooling files: app Gradle config, manifest, version catalog, Gradle wrapper and generated
  Room schema 10 JSON;
- 13 persistence/gateway/wiring files: additive `AppDatabase`, Application/Activity/rationale wiring
  and nine files in `data/wearable`;
- 8 Home/Log/Profile integration files;
- 6 synthetic unit/instrumentation test files;
- 3 documents: this report, the versioned contract and README.

Project-required Graphify `graph.json` was refreshed separately in code-only/no-cluster mode after
these changes; it is a generated navigation artifact, not application behavior or pilot evidence.

## Scope delivered

- Added versioned `WEARABLE_2A_SHADOW_CONTEXT_V1`.
- Added stable Health Connect 1.1.0 with exactly three read permissions: Sleep, HRV RMSSD and Resting HR.
- Added source-specific, immutable normalized revisions with provenance, timestamps, first-seen evidence,
  lifecycle epochs, payload hashes, deletion/supersession lineage and aggregate rejection counts.
- Added deterministic sleep normalization without inferred gaps or required Deep/REM.
- Added source-safe Home selection that never sums overlapping sleep sessions.
- Added the versioned `WEARABLE_2A_SHADOW_CONTEXT_V1_1` snapshot contract and
  `WEARABLE_2A_SLEEP_SELECTION_V2`: latest end time first, duration and provenance IDs only as
  tie-breakers, with every eligible source session still linked independently.
- Added conservative foreground/explicit synchronization with independent per-type Changes tokens.
- Added immutable prospective wearable snapshots linked to frozen Recovery Research predictions.
- Added a Home informational card and Settings opt-in/permission/privacy controls.
- Added explicit partial-access, incomplete-data and >24-hour stale-sync presentation states.
- Added local disconnect/revoke and explicit imported-health-data deletion.
- Kept the frozen Recovery Research JSON V1 export unchanged.

## Architecture

```text
Health Connect (read only)
  -> AndroidHealthConnectGateway
  -> WearableShadowRepository
  -> Room schema 10: revisions + stages + access/sync evidence
  -> Home informational card
  -> immutable WearableContextSnapshot after frozen WEARABLE-1A prediction

No branch connects to MUSCLE-4B / Heat / PO / recommendations / targets.
```

Room schema moves from 9 to 10 through an additive `MIGRATION_9_10`. Seven wearable tables are added;
the eight existing research collections and all production tables are not rewritten.

## Dependency/toolchain decision

- Health Connect: `androidx.health.connect:connect-client:1.1.0` stable.
- Compile SDK: 36, required by that published AAR.
- AGP: 8.9.1.
- Gradle wrapper: 8.11.1.
- Target SDK: unchanged at 35.
- Minimum SDK: unchanged at 26; Health Connect is truthfully unavailable where the provider is not
  supported.

The official 1.2 alpha surface was not required. No Samsung SDK was added. Sources:
[AndroidX Health Connect releases](https://developer.android.com/jetpack/androidx/releases/health-connect),
[availability](https://developer.android.com/health-and-fitness/health-connect/availability),
[sync guidance](https://developer.android.com/health-and-fitness/health-connect/sync-data).

## Privacy result

- no write/background/history/general-HR permission;
- no Internet permission or network transport;
- no health-value logs;
- automatic backup/device transfer still disabled and all storage domains excluded;
- synthetic test fixtures only;
- no health values or personal source export committed;
- no automatic extension of Recovery Research Export V1;
- user controls Connect, Manage, Disconnect/revoke and local deletion.

## Independent reviews

Exactly two read-only reviewers were used as requested.

1. Platform/API reviewer: `GO_WITH_LIMITATIONS`. It confirmed stable 1.1.0 and the exact permission
   scope. The material limitation is that Samsung officially guarantees Sleep synchronization but its
   current table does not list HRV RMSSD or Resting HR; those remain optional/missing-capable.
2. Architecture/privacy reviewer: initially `BLOCKED` on incomplete wiring/tests/docs and found
   transaction consistency, lifecycle, loading, stale-error, revocation display, clipped-stage,
   rejection and retention issues. The main implementation addressed those findings before final
   verification. The reviewer was read-only and changed no files.

## Automated and emulator evidence

- `:app:compileDebugKotlin`: passed.
- `:app:testDebugUnitTest`: passed — 419 tests, 0 failures/errors/skips.
- `:app:compileDebugAndroidTestKotlin`: passed.
- Targeted Pixel 8 emulator run: 5 tests, 0 failures/errors/skips:
  - v8->v9 regression migration;
  - v9->v10 preservation/schema/trigger migration;
  - delayed arrival and old-snapshot exclusion;
  - sync idempotency, immutable updates, deletion/re-upsert lifecycle and aggregate rejection;
  - expired-token recovery without a stale error;
  - partial permission, permission revocation, unsupported provider and remote interruption.

The emulator is `QA/PREFLIGHT_DEVICE_ONLY`; no real Health Connect or Samsung data were claimed.

The final Pixel 8 preflight installed debug version `1.1 (2)`, cold-launched successfully, rendered
the Home shadow card with the required non-readiness label and explicit consent/setup state, and left
Android's crash buffer empty. The emulator used only a synthetic onboarding-complete preference to
reach Home; it contained no workout or health evidence.

Repair-specific verification on 2026-08-14:

- targeted selector/protected-boundary JVM suites: 15 tests, 0 failures/errors/skips;
- full `:app:testDebugUnitTest`: 431 tests, 0 failures/errors/skips;
- `:app:compileDebugAndroidTestKotlin`: passed, including the new three-session snapshot regression;
- targeted `connectedDebugAndroidTest` was attempted after the test APK compiled, but Gradle skipped
  `emulator-5554` as `UNAUTHORIZED` and returned `No online devices found`; execution remains pending
  user authorization, and no emulator state is treated as physical evidence;
- `:app:lintRelease` completed analysis and found no issue in a repair-touched file, but the task exits
  nonzero on one unrelated existing `SuspiciousIndentation` error in
  `ExerciseDetailViewModel.kt:92` (plus 44 warnings); no baseline or unrelated source edit was made;
- `lintVitalRelease`, `assembleDebug` and `assembleRelease` passed.

## Build artifacts

The repair's `assembleDebug + assembleRelease` command completed with `BUILD SUCCESSFUL`.
`lintVitalRelease` passed. Full release lint ran and is reported honestly above; it is not green because
of the unrelated existing `ExerciseDetailViewModel.kt:92` indentation error.

| Artifact | Bytes | SHA-256 | Result |
|---|---:|---|---|
| `app-debug.apk` | 101,681,651 | `0EC9DABDB7CD3C6717AF9CC9D7FD349EC924B66536BD8BAB4B601CB88CE8DCFD` | built; emulator QA only |
| `app-release.apk` | 93,937,958 | `E6C7F5D298BF770C1C27DD54B6848ABF8BE65379370DCD0355006F665C97C9CE` | built; APK Signature Scheme v2 verified; one signer |

The repair release is `1.1.1 (3)`. Its certificate SHA-256 is
`ED37A53A772F4B143195DDE662991E43BDECAFF83BC411E5AF32FCBA5165D8AA`, identical to the frozen
WEARABLE-1D and physical WEARABLE-2A `1.1 (2)` signing certificate. Version code 3 can therefore
upgrade version code 2 without requiring a database-clearing reinstall, subject to the phone accepting
the normal in-place update.

The merged release APK permission inventory contains only the three Health Connect reads plus
AndroidX's app-scoped `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It contains no Internet, health
write, background-health, unrestricted-history or ordinary-heart-rate permission.

## Protected-model result

MUSCLE-4B, Muscle Heat, Progressive Overload, Recovery Research contract/export and WEARABLE-1B
Protocol V1 were not edited. Static invariant tests verify the three protected engines contain no
wearable dependency/modifier and that shadow capture occurs only after the protected prediction freeze.
Final byte hashes are recorded below.

The final comparison against the frozen WEARABLE-1D hashes is byte-identical:

| Protected surface | SHA-256 |
|---|---|
| `MuscleRecovery.kt` | `0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748` |
| `MuscleHeatEngine.kt` | `12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341` |
| `ProgressiveOverload.kt` | `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B` |
| `RecoveryResearchContract.kt` | `83EEACC0E28F194B0C3649684C80CF3500105C0B9337913C52F1D3B30B0D6724` |
| `RecoveryResearchExport.kt` | `10F777E20619AFB0F85A08B49516D0F576D9A53F27CD85DCA857D60F2CA9A172` |
| `WEARABLE_1B_EVALUATION_PROTOCOL_V1.json` | `494AC18B8A92114091464AB1731776DEF680AF40FA26E2CB27A4742748EA6998` |
| `backup_rules.xml` | `0D5E73555C17D67990C444F7AA688CA279B893AC5D0053094E70D6CDF97AE8EC` |
| `data_extraction_rules.xml` | `023CAFFA7E9CCC229D775C41CCFD90B3D4EC80BEC7E2B5EB77AE4EE663BFA355` |

`AppDatabase.kt` and `AndroidManifest.xml` changed intentionally for the additive schema-10 graph and
minimum Health Connect surface; neither change alters a protected production model or old export
meaning.

## Physical evidence audit before first workout snapshot (2026-08-14)

The signed `1.1 (2)` release was verified on the preserved physical pilot phone as an in-place upgrade.
Existing workout/history/research state survived. Health Connect showed all three requested read
permissions, and AURON ingested real Samsung Health Sleep records with source package
`com.sec.android.app.shealth`, session time, AURON first-seen time and local-sync time. An explicit
shadow refresh did not alter production readiness.

Two physical nights contained multiple non-overlapping SleepSession records. V1 correctly retained
the records separately, did not fill the real awake gaps and did not sum overlapping or multi-source
sessions. However, the physical chronology exposed a blocking temporal-relevance defect in the frozen
summary rule:

- V1 searches the full previous 36 hours and sorts all eligible sleep sessions by longest duration
  before recency;
- on the first split-sleep morning, the new Samsung Health session had already been first seen by AURON
  before the observed refresh, but the prior day's longer session remained the Home primary until it
  aged out of the 36-hour window;
- `WearableContextSnapshot` uses the same selector, so an early natural workout could mark the prior
  day's longer session as primary even while preserving the current split sessions in snapshot lineage;
- this is not retrospective leakage and does not affect readiness, but it is the wrong temporal primary
  for prospective shadow interpretation.

The Home label `Total sleep` is also only the known-sleep duration of the selected source session. It is
not a whole-night aggregate, and no notice is currently shown for additional non-overlapping sessions.
This wording is therefore ambiguous in split-sleep cases.

Audit decision: `SLEEP_SELECTION_REPAIR_REQUIRED_BEFORE_FINAL_GATE`.

The smallest safe repair is selection-only: preserve every source session independently, choose the
latest-ending eligible session first (duration and stable provenance IDs only break ties), keep all
other eligible sessions in immutable lineage, and explicitly abstain from whole-night aggregation.
Home must label the value as selected-session sleep and disclose that other eligible sessions were
retained but not included. The repair requires no sleep-gap imputation, Room/export/model change or
wearable modifier. Because `WEARABLE_2A_SHADOW_CONTEXT_V1` is documented as frozen, the corrected
selection semantics must be versioned rather than silently rewriting a captured snapshot contract.

The repair is now implemented exactly at that boundary:

- raw/source revisions remain frozen V1 and remain independent;
- new snapshots use `WEARABLE_2A_SHADOW_CONTEXT_V1_1` and hash
  `WEARABLE_2A_SLEEP_SELECTION_V2`;
- the 36-hour window is unchanged;
- latest end time is primary, then duration, origin package, Health Connect ID and revision ID;
- all eligible sessions remain linked and only one is marked selected for summary;
- Home says `Selected-session sleep`/`Selected session duration` and discloses every other eligible
  session as retained but not included;
- no Room schema/meaning, export, permission, protected model, readiness or recommendation path changed.

`WEARABLE_2B_GATE = CLOSED` until the selection repair is verified and a subsequent natural workout
produces a physically audited sealed shadow snapshot at the prediction cutoff.

## Known limitations

- Physical Samsung Health Sleep delivery and source metadata are verified; standardized Samsung HRV
  RMSSD and Resting HR delivery remain unverified/missing-capable.
- Samsung may provide Sleep but no standardized HRV RMSSD or Resting HR records.
- `DataOrigin` proves the writer application, not direct watch measurement.
- Sleep stages can be incomplete; AURON abstains rather than fills gaps.
- Foreground sync means records may arrive after a workout. `firstSeenAt` and immutable cutoffs prevent
  retrospective knowledge but cannot eliminate vendor delay.
- Historical V1 snapshots, if any, retain their frozen longest-first semantics; they are not rewritten.
- V1.1 deliberately abstains from whole-night aggregation; the selected-session value is not a nightly
  total, and additional eligible sessions are disclosed separately.
- WEARABLE-2A does not export health values; physical review uses local screenshots and value-free
  snapshot diagnostics.
- No biological accuracy or wearable benefit has been tested.

## Remaining physical handoff

1. Install the signed `1.1.1 (3)` release with `adb install -r`; do not uninstall, clear data or replace
   the preserved physical database.
2. Verify prior workout/history/research state remains present, all three read permissions remain, and
   Home shows latest-ending selected-session semantics plus the count of other eligible sessions.
3. Verify readiness before/after an explicit shadow refresh is unchanged with workout history fixed.
4. At the **next natural workout**, open Log Workout normally. Do not reproduce or manufacture
   a workout for verification. After the frozen prediction is created, open Profile -> Recovery Research
   and capture the value-free `LATEST SHADOW SNAPSHOT` diagnostic.

Return screenshots of:

1. the corrected release/version installed in place with prior local state retained;
2. Home showing the corrected selected-session semantics when multiple source sessions exist;
3. Settings latest shadow snapshot line after opening one new natural workout attempt;
4. readiness immediately before and after explicit Health Connect refresh with workout history
   unchanged.

Do not reproduce workouts on the emulator. Do not clear the physical database. Do not treat missing
Samsung HRV/RHR as zero or as a MUSCLE-4B failure.

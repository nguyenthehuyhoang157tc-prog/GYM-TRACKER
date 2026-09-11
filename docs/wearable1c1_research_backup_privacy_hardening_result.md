# WEARABLE-1C.1 — Research Backup Privacy Boundary Hardening — Result

## 1. Final status

`PARTIAL_VERIFICATION_ENVIRONMENT_BLOCKED`

Android 12+ framework configuration, cloud LocalTransport runtime, GMS D2D runtime, manual
Plan/History portability, and manual Research SAF export all passed. No seeded production,
research, consent, draft, preference, or internal-file state returned after either automatic
restore simulation. The sole missing runtime row is Android 11/API 30-or-lower: this workstation
has only an Android 37 system image and the task did not authorize downloading another image.
The legacy XML is therefore build- and static-test-verified, not runtime-verified.

Evidence classification:

- `FRAMEWORK_CONFIGURED`: PASS
- `CLOUD_RUNTIME_VERIFIED`: PASS on Pixel_8 AVD, API 37
- `D2D_RUNTIME_VERIFIED`: PASS on Pixel_8 AVD, API 37
- `LEGACY_RUNTIME_VERIFIED`: NOT RUN — environment blocked
- `MANUAL_EXPORTS_VERIFIED`: PASS

## 2. Backup path

Immediately before the first source edit, a timestamped scoped backup was created at:

`D:\HOCTIN\GYM TRACKER\_backup_wearable1c1_20260722_144710`

It contains the pre-edit manifest, Settings copy, XML resources, README, and all seven protected
surfaces used for before/after hashing (11 files total).

## 3. Subagent A findings

The required read-only Android backup-policy auditor found the pre-task build non-compliant:

- `android:allowBackup="true"` was effective in the main, debug, profile, and release manifests.
- Neither `android:fullBackupContent` nor `android:dataExtractionRules` existed.
- No flavor, overlay, dependency, or manifest-merger rule supplied a competing policy.
- The entire `gym_tracker.db` (production plus all research tables), four SharedPreferences files,
  and future eligible app-private writers were exposed to default framework backup/transfer.
- Manual Plan/History JSON is an in-memory copy/paste path; Research JSON is a separate SAF
  `CreateDocument` path. Neither should be conflated with automatic framework backup.

The auditor recommended the implemented belt-and-suspenders policy: `allowBackup=false`, a legacy
exclude-only ruleset, and independent modern cloud/device-transfer exclude-only rulesets covering
all nine supported domains.

## 4. Official Android research findings

Android Auto Backup includes app databases, SharedPreferences, internal files/root storage, and
app-specific external files by default; cache, code-cache, and `noBackupFilesDir` are always
excluded. Android 11 and lower use `full-backup-content`; Android 12+ apps targeting API 31+ use
`data-extraction-rules`, whose cloud and device-transfer sections are independent. A missing
device-transfer section would leave that mode enabled. See [Auto Backup and rule syntax](https://developer.android.com/identity/data/autobackup).

For target-31+ apps, Android 12 introduced D2D behavior that can ignore `allowBackup`, making the
explicit modern `<device-transfer>` exclusions essential rather than decorative. See the
[Android 12 compatibility change](https://developer.android.com/about/versions/12/reference/compat-framework-changes#IGNORE_ALLOW_BACKUP_IN_D2D).

The runtime sequences followed Android's official single-device
[cloud and D2D test procedure](https://developer.android.com/identity/data/testingbackup), adapted
to the privacy-negative assertion: state must *not* return.

`cross-platform-transfer` was deliberately not added. It begins with Android 16 QPR2/API 36.1,
while AURON compiles/targets API 35 and has no declared iOS relationship. This must be re-audited
if the SDK or cross-platform product scope changes.

## 5. Pre-task storage inventory

| Physical/domain category | Concrete state before hardening | Before | After | Manual export | Research content |
|---|---|---:|---:|---|---|
| `/data/user/0/com.hoctin.gymtracker/databases/gym_tracker.db` (`database`) | Room v9, filename `gym_tracker.db`; runtime observed `-wal` and `-shm` sidecars/WAL operation; production, catalog, and all eight research tables share this file; no secondary DB | Eligible | Excluded | Production subset via Plan/History schema 5; research subset via separate Research JSON | Yes |
| `/data/user/0/com.hoctin.gymtracker/shared_prefs/gym_tracker_preferences.xml` (`sharedpref`) | Theme, units, goal, selected/training days, onboarding/tracking state, daily boost | Eligible | Excluded | No | No |
| `/data/user/0/com.hoctin.gymtracker/shared_prefs/gym_tracker_workout_drafts.xml` (`sharedpref`) | Workout drafts/start time plus durable research attempt ID/creation time | Eligible | Excluded | No | Yes — attempt linkage |
| `/data/user/0/com.hoctin.gymtracker/shared_prefs/auron_catalog.xml` (`sharedpref`) | Catalog content-version marker | Eligible | Excluded | No | No |
| `/data/user/0/com.hoctin.gymtracker/shared_prefs/auron_migrations.xml` (`sharedpref`) | Migration/backfill completion flags | Eligible | Excluded | No | No |
| `/data/user/0/com.hoctin.gymtracker/` and `files/` (`root`, `file`) | No app-authored user-state writer; profile installer marker observed under `files/` | Eligible if present | Excluded | No | No current content |
| `/data/user/0/com.hoctin.gymtracker/cache`, `code_cache`, `no_backup` | Temporary heat-card PNG uses cache; Plan/History and pending Research exports remain in memory until explicit output | Always framework-excluded | Framework-excluded | Heat-card share only; not a data backup | No research file |
| `/storage/emulated/0/Android/data/com.hoctin.gymtracker/files` (`external`) | No current production writer | Eligible if present | Excluded | No | No current content |
| `/data/user_de/0/com.hoctin.gymtracker/...` (`device_*`) | No device-protected root/file/database/SharedPreferences writer | Eligible if present | Excluded | No | No current content |
| User-selected SAF document (outside app-private domains) | Research `CreateDocument`; provider may be local or cloud. Ordinary Plan/History portability is copy/paste, not SAF | Outside framework app backup | Unchanged | Yes — explicit Research export | Yes |

Research and production share one Room database, so table-level separation cannot provide a sound
automatic-backup boundary. The accepted product decision is app-wide automatic backup/transfer
disablement.

## 6. Pre-task merged-manifest state

The main source manifest and regenerated debug/profile/release merged manifests all resolved to:

- `android:allowBackup="true"`
- no `android:fullBackupContent`
- no `android:dataExtractionRules`
- no custom `BackupAgent`
- no dependency or overlay override

Manifest-merger reports attributed `allowBackup` only to `app/src/main/AndroidManifest.xml`.

## 7. Implementation decision and tradeoff

AURON now disables Android framework automatic cloud backup and automatic D2D transfer for the
whole app. This intentionally removes automatic restoration of plans, history, settings, research
records, consent, and drafts. The benefit is a clear privacy boundary for the shared database and
all current/future storage domains; the cost is that users must use the existing explicit manual
Plan/History export/import when they want portability. Research export remains separate and
export-only.

No Room, migration, research contract, evaluator protocol, recovery model, Muscle Heat, or
Progressive Overload behavior was changed.

## 8. Exact manifest changes

The `<application>` element now contains:

```xml
android:allowBackup="false"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
```

No `tools:replace`, custom backup agent, flavor override, or restore-version relaxation was added.

## 9. Exact modern backup-rule XML

`app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>
```

Cloud and D2D are separately and completely excluded. There are no `<include>` entries.

## 10. Exact legacy backup-rule XML

`app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Credential-encrypted storage. -->
    <exclude domain="root" path="." />
    <exclude domain="file" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
    <exclude domain="external" path="." />

    <!-- Device-protected storage, included defensively for future writers. -->
    <exclude domain="device_root" path="." />
    <exclude domain="device_file" path="." />
    <exclude domain="device_database" path="." />
    <exclude domain="device_sharedpref" path="." />
</full-backup-content>
```

Every exclusion uses recursive `path="."`; there are no wildcard or include rules.

## 11. Build-flavor/manifest-merge verification

The debug, profile, and release main manifests were regenerated and inspected directly under
`app/build/intermediates/merged_manifest`.

| Variant | `allowBackup` | `fullBackupContent` | `dataExtractionRules` |
|---|---|---|---|
| debug | `false` | `@xml/backup_rules` | `@xml/data_extraction_rules` |
| profile | `false` | `@xml/backup_rules` | `@xml/data_extraction_rules` |
| release | `false` | `@xml/backup_rules` | `@xml/data_extraction_rules` |

AAPT accepted both XML schemas. Manifest-merger reports show all three attributes originate from
the main application manifest with no replacement or dependency override. A focused JVM test also
parses the source manifest/rules and enforces the exact nine-domain sets independently.

## 12. Disposable QA seed state

Only synthetic, disposable state was used. Before the cloud run, aggregate state was:

- Plan/History: schema 5, 4 workout days, 17 exercises, 1 session, 1 logged set.
- Research: 1 granted consent, 1 frozen prediction, 6 group rows, 4 muscle rows, 5 target
  exercises, 1 partial outcome, 5 exercise outcomes, and 1 set observation.
- Preferences: completed onboarding/training weekdays plus catalog/migration markers.
- Draft: an unfinished workout plus its research-attempt identity in
  `gym_tracker_workout_drafts.xml` (3,240 bytes in the cloud run; a separately seeded 4,192-byte
  draft in the D2D run).
- Internal file inventory: the framework profile-installer marker only; no app-authored user file.

Raw records, subject identifiers, exports, screenshots, UI XML, and evaluator artifacts were
deleted after aggregate verification and are not retained in the repository.

## 13. Cloud backup command sequence and result

The API 37 emulator initially had Backup Manager disabled, GMS cloud transport selected, and both
relevant secure settings unset (`null`). The cloud run used:

```text
bmgr enable true
settings put secure backup_local_transport_parameters is_encrypted=true
bmgr transport com.android.localtransport/.LocalTransport
bmgr wipe com.android.localtransport/.LocalTransport com.hoctin.gymtracker
bmgr backupnow com.hoctin.gymtracker
adb uninstall com.hoctin.gymtracker
adb install -t <same-debug-apk>
```

Observed transport result:

```text
Package com.hoctin.gymtracker with result: Backup is not allowed
Backup finished with result: Success
```

The test actually triggered Backup Manager and then performed uninstall/reinstall; it did not infer
runtime behavior from XML alone.

## 14. Cloud restore DB/preference evidence

Before uninstall, direct `run-as` inspection showed the populated Room DB/WAL, four preference
files, the unfinished draft, and stable SHA-256 digests. After reinstall and before first launch,
only empty package `cache`/`code_cache` directories existed. After first launch:

- a fresh seeded Room database was created;
- only `auron_catalog.xml` and `auron_migrations.xml` existed;
- `gym_tracker_preferences.xml` did not return;
- `gym_tracker_workout_drafts.xml` did not return;
- the app displayed first-run onboarding at `0 / 4 selected`;
- no prior session, research consent, prediction, outcome, or draft returned.

The separate SAF document survived uninstall/reinstall byte-for-byte, with SHA-256
`26E49F7E88ADEC727E327AD3CFAC4EB40FB393739840FE1B6F6873D8C30AFA0C`.

## 15. D2D command sequence and result

The available official GMS D2D transport was exercised independently after reseeding production,
consent, preferences, and draft state:

```text
settings put secure backup_enable_d2d_test_mode 1
bmgr transport com.google.android.gms/.backup.migrate.service.D2dTransport
bmgr init com.google.android.gms/.backup.migrate.service.D2dTransport
bmgr backupnow com.hoctin.gymtracker
pm uninstall --user 0 com.hoctin.gymtracker
bmgr transport com.google.android.gms/.backup.BackupTransportService
adb install -t --user 0 <same-debug-apk>
```

Initialization returned `0`; the D2D transport was starred. `backupnow` returned:

```text
Package com.hoctin.gymtracker with result: Backup is not allowed
Package @pm@ with result: Success
Backup finished with result: Success
```

This was a real single-device D2D transport simulation, not a cloud-test inference.

## 16. D2D restore DB/preference evidence

Before D2D backup, `run-as` showed a populated database, user preferences, granted consent, one
imported production session, and a 4,192-byte unfinished draft. After uninstall/reinstall and before
first launch, only empty `cache`/`code_cache` directories existed. After first launch:

- only a fresh seeded DB plus catalog/migration marker preferences existed;
- user preferences and workout drafts were absent;
- the app again displayed first-run onboarding at `0 / 4 selected`;
- no excluded production or research state returned.

The SAF file again retained the exact SHA-256 above.

## 17. Legacy test evidence

No API 30-or-lower device/image is installed. `emulator -list-avds` exposes only `Pixel_8`, and the
SDK contains only `system-images/android-37.0`. No image was downloaded or device fabricated.

Available substitute evidence:

- AAPT successfully compiled `backup_rules.xml` for minSdk 26.
- The static regression test verified exactly nine legacy domain exclusions, all with `path="."`,
  and zero includes.
- Debug/profile/release merged manifests all retain the legacy resource reference.

This is not labeled `LEGACY_RUNTIME_VERIFIED`; it is the reason for the partial final status.

## 18. Manual Plan/History export/import regression

The real Settings UI was used end-to-end:

1. `Generate export` produced schema 5 JSON (4,110 characters; SHA-256
   `E24493ED5B1D71286993A83947352033A7C8831E10E352AEE4E3BF0CFD4D8317`).
2. Top-level fields were exactly `schemaVersion`, `exportedAtMillis`, `workoutDays`, `exercises`,
   `workoutSessions`, and `loggedSets`.
3. Aggregate counts were 4 days, 17 exercises, 1 session, and 1 set.
4. The UI copy/paste import accepted all 4,110 characters and cleared its input on success.
5. A new export after import retained schema 5 and the same aggregate counts.
6. Research/consent arrays were absent; preferences were not silently included.

No export schema or repository code changed.

## 19. Manual Research JSON export regression

Settings → `Export research JSON` opened Android SAF `CreateDocument`; saving to a disposable QA
document succeeded. The canonical file was 16,389 bytes and contained the expected contract/header
plus all research collections. Aggregate counts matched the database-side capture summary.

WEARABLE-1B audit mode accepted its integrity with:

- mode `AUDIT_ONLY`;
- Room schema `9`;
- research contract `WEARABLE_1A_V1`;
- 1 prediction, 5 target exercises, 1 consent, 1 set observation, 1 linked partial outcome;
- data-quality verdict `ACCEPTABLE_WITH_DECLARED_LIMITATIONS`;
- no critical integrity finding.

The evaluator conclusion was correctly `INSUFFICIENT_DATA` because this was one synthetic QA
session, not biological validation. The SAF document survived both automatic-restore simulations
unchanged and was then deleted. SAF may represent a local or cloud document provider selected by
the user; AURON does not describe it as automatically device-local. See the
[Storage Access Framework guide](https://developer.android.com/training/data-storage/shared/documents-files).

## 20. Product-copy changes

The existing Settings layout is unchanged. Two strings were clarified:

- Research records are described as kept in AURON's private app data, not ambiguously
  “local-only.”
- The explanation now states that Android automatic cloud backup and device transfer are disabled,
  while Research export is manual and its chosen document provider controls file storage.

The copy still states that no wearable, sleep, heart-rate, AI, or backend data is collected.

## 21. Files changed

Final task-scoped files:

1. `app/src/main/AndroidManifest.xml`
2. `app/src/main/res/xml/backup_rules.xml` (new)
3. `app/src/main/res/xml/data_extraction_rules.xml` (new)
4. `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsScreen.kt`
5. `app/src/test/java/com/hoctin/gymtracker/privacy/BackupPrivacyPolicyTest.kt` (new)
6. `docs/wearable1c1_research_backup_privacy_hardening_result.md` (new)
7. `README.md` (WEARABLE-1C historical qualification plus new WEARABLE-1C.1 current state)

README was updated only after Subagent B returned `PASS_WITH_LIMITATIONS`. The pre-existing dirty
working tree was preserved; no unrelated change was reverted or rewritten.

Per the repository rule, `graphify-out/` was refreshed after the source changes. This is a derived
knowledge-graph update, not an additional product-scope change; task QA artifacts were removed first.

## 22. Tests/build/connected-test results

| Command | Result |
|---|---|
| `:app:testDebugUnitTest --no-daemon` | PASS — 352 tests, 0 failures/errors/skips |
| focused `BackupPrivacyPolicyTest` | PASS — 4 tests |
| `:app:assembleDebug --no-daemon` | PASS |
| `:app:connectedDebugAndroidTest --no-daemon` | PASS — 2 tests on Pixel_8 API 37 |
| debug/profile/release `process*MainManifest` | PASS; direct merged-manifest inspection PASS |

The installed package flags omit `ALLOW_BACKUP`. The app-specific crash buffer and historical-exit
query showed no AURON crash or ANR from the tested flows. One earlier emulator boot dialog concerned
System UI, not `com.hoctin.gymtracker`; AURON subsequently completed all runtime and connected tests.

The existing AGP 8.5.2/compileSdk 35 compatibility warning remains unrelated and non-fatal.

## 23. Protected before/after hashes

Every protected surface is byte-identical:

| Protected file | SHA-256 before | SHA-256 after |
|---|---|---|
| `MuscleRecovery.kt` | `0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748` | same |
| `MuscleHeatEngine.kt` | `12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341` | same |
| `ProgressiveOverload.kt` | `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B` | same |
| `RecoveryResearchContract.kt` | `83EEACC0E28F194B0C3649684C80CF3500105C0B9337913C52F1D3B30B0D6724` | same |
| `RecoveryResearchExport.kt` | `10F777E20619AFB0F85A08B49516D0F576D9A53F27CD85DCA857D60F2CA9A172` | same |
| `WEARABLE_1B_EVALUATION_PROTOCOL_V1.json` | `494AC18B8A92114091464AB1731776DEF680AF40FA26E2CB27A4742748EA6998` | same |
| `AppDatabase.kt` | `42EA850483CF84577F5438940518A9E1F5ABE5EA17B6680F5D9DE4A7A149760B` | same |

Room remains version 9; no migration was added.

## 24. Backup-manager cleanup evidence

The initial state was recorded as:

- Backup Manager: disabled
- selected transport: `com.google.android.gms/.backup.BackupTransportService`
- `backup_enable_d2d_test_mode`: `null`
- `backup_local_transport_parameters`: `null`

Cleanup initialized the D2D transport once more to clear its test dataset, restored the original GMS
transport, deleted both secure settings (rather than writing the string `null`), and disabled Backup
Manager. Final verification reproduced exactly the four values above. The disposable SAF file and
all local raw QA/evaluator artifacts were deleted.

## 25. Independent Subagent B verdict

`PASS_WITH_LIMITATIONS`

The required read-only reviewer found no blocking implementation defect and passed all 14 Phase 13
questions: redundant manifest/XML protection, independent cloud/D2D rules, all nine domains, actual
transport invocation and uninstall/reinstall, direct DB/preference inspection, no restored seeded
state, correct manual-export boundary, honest OEM limits, unchanged protected surfaces, Room v9, and
an evidence-consistent partial final status.

Three non-blocking recommendations were resolved before finalization:

1. Settings copy now says “Android framework” and “by app policy,” avoiding a proprietary-OEM claim.
2. The regression suite now also scans every current/future source-set manifest and rejects any
   overlay that re-enables or redirects the policy. Generated debug/profile/release manifests remain
   independently inspected because a JVM source test cannot itself guarantee future build-output
   inspection.
3. The storage inventory now records physical paths, observed WAL operation, manual-export
   availability, and research-content presence.

## 26. Platform/OEM limitations

- The cloud and D2D results prove behavior on the official transports available in this API 37 AVD,
  not every proprietary OEM migration tool.
- Android documentation warns that some target-31+ manufacturer D2D implementations can disregard
  `allowBackup`; explicit modern device-transfer exclusions mitigate the Android framework path but
  cannot justify a universal claim about out-of-framework vendor tools.
- This change does not prove deletion of historical remote backup datasets made by older app builds.
- Android 11/API 30 runtime behavior remains unexercised in this environment.
- Cross-platform transfer is outside compileSdk/targetSdk 35 and must be re-audited if adopted.

## 27. Exact next action

WEARABLE-1C.1 is closed at `PARTIAL_VERIFICATION_ENVIRONMENT_BLOCKED`. The only verification follow-up
is an API 30-or-lower LocalTransport uninstall/reinstall run when an already approved device/image is
available; do not weaken the policy while waiting. Product work after this task should be natural
prospective capture only. Do not add Health Connect, wearable modifiers, recovery-model changes,
schema changes, or research import as part of WEARABLE-1C.1.

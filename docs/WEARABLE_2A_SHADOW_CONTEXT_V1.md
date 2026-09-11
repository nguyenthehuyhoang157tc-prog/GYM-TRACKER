# WEARABLE_2A_SHADOW_CONTEXT_V1

## 1. Purpose and state

`WEARABLE_2A_SHADOW_CONTEXT_V1` is AURON's first Health Connect epoch. It imports a minimum set of
recovery context into a local, provenance-aware, prospective research graph while keeping all
production coaching behavior unchanged.

The implementation is **shadow-only and non-interventional**:

```text
Health Connect -> local revisions -> informational Home card
                               \-> immutable context snapshot at prediction cutoff

There is no path from either branch into MUSCLE-4B, Muscle Heat, Progressive Overload,
workout targets, recommendations, or readiness UI calculations.
```

Physical Samsung Health Sleep delivery and provenance are verified. The split-sleep audit found that
the frozen V1 longest-first summary could select a prior day's longer session. The smallest repair is
implemented as `WEARABLE_2A_SHADOW_CONTEXT_V1_1` with
`WEARABLE_2A_SLEEP_SELECTION_V2`; it remains
`WEARABLE_2A_REPAIR_IMPLEMENTED_AWAITING_PHYSICAL_VERIFICATION` and
`WEARABLE_2B_GATE = CLOSED` until the corrected release and a natural prediction-cutoff snapshot are
verified on the physical pilot phone.

## 2. SDK and platform boundary

AURON uses the stable:

```kotlin
androidx.health.connect:connect-client:1.1.0
```

The current official examples also mention 1.2 alpha releases, but their added APIs are not needed by
2A. Stable 1.1.0 provides the three record types, foreground reads, pagination, permissions, metadata
and Changes tokens required here. The AAR requires compile SDK 36; AURON therefore uses AGP 8.9.1,
Gradle 8.11.1 and compile SDK 36 while retaining target SDK 35 and minimum SDK 26. See the
[official AndroidX release notes](https://developer.android.com/jetpack/androidx/releases/health-connect).

The app remains installable below Android 9, but Health Connect is represented as unavailable there.
Android 14+ provides Health Connect as a system module; supported Android 9-13 devices use the
separate provider application. AURON checks all official SDK availability states before obtaining a
client. See [Health Connect availability](https://developer.android.com/health-and-fitness/health-connect/availability).

## 3. Exact V1 record and permission scope

V1 reads only:

| AURON type | Health Connect record | Permission |
|---|---|---|
| Sleep | `SleepSessionRecord` and its optional stages | `READ_SLEEP` |
| HRV | `HeartRateVariabilityRmssdRecord` | `READ_HEART_RATE_VARIABILITY` |
| Resting HR | `RestingHeartRateRecord` | `READ_RESTING_HEART_RATE` |

No write permission, ordinary heart-rate permission, background-health permission, unrestricted
history permission, Internet permission, service, polling worker, or Samsung SDK is present.

The permission prompt is launched only after the user:

1. explicitly grants Recovery Research consent;
2. opens Profile -> Recovery Research;
3. taps **Connect Health Connect**.

Partial grants are first-class. A denied or revoked metric does not disable normal AURON behavior or
the other granted record types. Permissions are checked before every foreground/explicit sync because
Health Connect access may be revoked independently. Android's required rationale Activity and Android
14 permission-usage alias are present. See the [official setup guide](https://developer.android.com/health-and-fitness/health-connect/get-started)
and [permission UX guidance](https://developer.android.com/health-and-fitness/health-connect/ui/permissions).

## 4. Provenance and revision contract

Every accepted record revision preserves:

- Health Connect record ID;
- `DataOrigin.packageName` and a best-effort local app label;
- Health Connect `lastModifiedTime`;
- optional client record ID and client version;
- recording method;
- optional device type, manufacturer and model;
- physiological session/measurement time and provided zone offsets;
- AURON `firstSeenAtMillis` and `lastSeenAtMillis`;
- deterministic normalized payload SHA-256;
- a monotone local lifecycle epoch.

`DataOrigin` identifies the application that wrote the Health Connect record. It is not evidence that
the app, phone, or watch directly measured the value. Optional device metadata are also not hardware
attestation. See the official [Metadata](https://developer.android.com/reference/androidx/health/connect/client/records/metadata/Metadata)
and [DataOrigin](https://developer.android.com/reference/androidx/health/connect/client/records/metadata/DataOrigin)
references.

Exact re-reads of an active revision only update `lastSeenAtMillis`. A changed record inserts a new
immutable payload revision and temporally supersedes the old revision. A deletion adds a temporal
tombstone. A later identical re-upsert after deletion creates a new lifecycle epoch rather than
incorrectly reviving or overwriting historical evidence. Out-of-order replay of an already-losing
revision is idempotent.

Invalid metric ranges or invalid sessions are not stored as plausible measurements. They increment
only a per-type aggregate rejection counter; values are never logged.

## 5. Sleep normalization

The raw source session and every source stage remain distinct. Calculations use `Instant` epoch time,
while original start/end zone offsets are retained for historical interpretation across time zones and
DST.

V1 normalization:

- `sessionDuration = end - start`;
- known sleep is the sum of Light, Deep, REM and generic Sleeping;
- generic Sleeping remains separate and is never relabelled Light/Deep/REM;
- Awake, Awake in bed and Out of bed contribute to known awake time;
- Unknown remains unknown;
- uncovered timeline remains a gap and is never inferred;
- stages outside the session are clipped and flagged;
- invalid intervals and overlapping stages abstain from stage aggregates;
- Deep/REM are optional, not usability requirements;
- missing values remain missing, never zero;
- stage coverage is exposed only from the valid interval union;
- sleep efficiency is emitted only for a complete, non-clipped, non-overlapping timeline with no
  Unknown or uncovered time: `knownSleep / (knownSleep + knownAwake)`.

The standardized record does not contain a vendor sleep score or a standardized efficiency field.
AURON's conservative derived field is contract-versioned and withheld when its evidence is incomplete.
Record semantics follow the official [SleepSessionRecord](https://developer.android.com/reference/androidx/health/connect/client/records/SleepSessionRecord)
and [sleep-session guide](https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions).

## 6. Multiple sessions and multiple sources

AURON never sums overlapping source sessions. Every source-specific record, nap and main sleep session
is retained independently.

The frozen historical V1 summary deterministically chose:

- candidate sleep sessions ending at or before cutoff within the previous 36 hours;
- the longest candidate first;
- then the latest ending session;
- then origin package, Health Connect ID and revision ID as stable tie-breakers;
- latest HRV RMSSD and Resting HR measurements at or before cutoff within 48 hours.

V1 rows, if any, remain immutable historical evidence. New snapshots use the versioned V1.1 repair:

- the same 36-hour candidate window and prospective cutoff/first-seen rules;
- latest `endTime` first;
- session duration only when end times tie;
- origin package, Health Connect record ID and revision ID only as stable remaining tie-breakers;
- every eligible sleep session remains independently linked in snapshot lineage;
- no split session, nap, gap, overlap or different origin is merged or summed.

The selected source/session is shown. Home discloses the count of other eligible sessions retained
separately and explicitly says they are not included in the displayed selected-session value. Naps are
never forced into one daily total, a real gap is never converted to sleep or Awake stage, and no source,
including Samsung Health, is hardcoded as preferred.

## 7. Synchronization

Synchronization is deliberately conservative:

- one foreground refresh when `MainActivity` becomes active;
- explicit Refresh from Home or Profile;
- no background service or aggressive polling;
- one independent Changes token per record type;
- token acquired before the initial bounded read, followed by immediate changes catch-up;
- 14-day initial read and paginated reads;
- idempotent revision ingestion;
- deletion and late-update processing;
- expired-token bounded reconciliation;
- transient provider/permission-query failures do not manufacture a revocation event;
- unexpected shadow failures are contained as local status and cannot crash or block normal AURON;
- `try/finally` always clears the refreshing state.

Unreferenced raw revisions older than 30 days are pruned after a successful sync. Any revision linked
to a frozen context snapshot is retained until the user explicitly deletes imported health context or
deletes all Recovery Research data. Changes tokens may expire after 30 days, so bounded reconciliation
is required. See Android's [sync guidance](https://developer.android.com/health-and-fitness/health-connect/sync-data).

## 8. Prospective temporal boundary and snapshots

Each eligible frozen WEARABLE-1A prediction receives at most one immutable
`WearableContextSnapshot` after the protected prediction has already committed. Shadow failure is
caught and cannot block or change that prediction.

Snapshot construction performs no new provider read. It uses the latest already-ingested foreground
state and runs the following read/selection/item/header work in one Room transaction, so a concurrent
sync page cannot create an interleaved partial snapshot.

A revision may enter a snapshot only when all are true:

```text
effective physiological time <= prediction cutoff
source last-modified time      <= prediction cutoff
AURON firstSeenAt              <= prediction cutoff
permission + sync access event was valid at/before cutoff
not yet superseded/deleted at cutoff
```

The snapshot stores contract/schema versions, prediction/attempt lineage, cutoff, creation time,
access-event reference, availability, granted-type mask, included revision links, source count,
deterministic selected-summary IDs and payload hash. Database triggers make the header and items
immutable and prevent item insertion after the header seals the snapshot.

Raw/normalized source revisions remain under frozen `WEARABLE_2A_SHADOW_CONTEXT_V1`. New snapshot
headers and hashes use `WEARABLE_2A_SHADOW_CONTEXT_V1_1` and include
`WEARABLE_2A_SLEEP_SELECTION_V2`. Existing V1 snapshots are returned unchanged rather than rewritten.

A prior-night record first delivered after a workout cannot enter that workout's old snapshot. A late
revision may be used by future snapshots only. Existing snapshots are never rewritten.

## 9. Room impact

Room moves additively from schema 9 to schema 10. Existing production rows and all eight frozen
WEARABLE-1A research collections keep their meaning and storage.

Seven isolated tables are added:

1. `wearable_shadow_settings`;
2. `wearable_shadow_sync_state`;
3. `wearable_shadow_access_events`;
4. `wearable_shadow_record_revisions`;
5. `wearable_shadow_sleep_stages`;
6. `wearable_context_snapshots`;
7. `wearable_context_snapshot_items`.

There is no destructive fallback. A real migration test verifies a frozen schema-9 prediction remains
byte-semantically present after migration, new tables start empty, foreign keys pass, and snapshot
sealing triggers exist.

## 10. Home and Settings semantics

Home contains a compact **Health / Recovery Context** card below Muscle Recovery. It can show, when
actually available:

- known sleep or duration for the one selected session, explicitly labelled as selected-session data;
- Light, Deep, REM and Awake;
- sleep efficiency only when eligible;
- HRV RMSSD;
- Resting HR;
- source, physiological timestamp, AURON first-seen timestamp and last local sync;
- incomplete-stage/overlap/missing-data notices;
- an explicit partial-access notice when one or more requested types are not granted;
- an operational stale notice when the last successful local sync is more than 24 hours old.
- a value-free interrupted/failed refresh notice while retained context remains unchanged.

No missing field is rendered as zero. No red/green good/bad judgment, readiness implication, vendor
score or structural-damage inference is shown. The card always says:

```text
Shadow context · not used in readiness
```

Profile -> Recovery Research owns Connect, Refresh, Manage access, Disconnect/revoke, and explicit
local health-context deletion. After disconnect, retained imported values are hidden and labelled as
retained rather than presented as currently accessible.

For physical verification only, Settings shows a value-free latest-snapshot integrity line: status,
cutoff, seal time, aggregate record/source counts and a short payload-hash prefix. It is not an export
and contains no health values.

## 11. Privacy and export boundary

- Imported context stays in AURON's private local Room database.
- No health values are logged, sent to analytics/crash breadcrumbs, graph tooling, or any network.
- The app requests no Internet permission.
- `allowBackup=false`; legacy backup rules and modern cloud/device-transfer rules exclude all app
  storage.
- Automated tests use synthetic values only.
- No raw Health Connect value or fixture is committed.
- `RecoveryResearchExport` and its canonical eight-collection JSON schema V1 are byte-semantically
  unchanged; wearable tables are not silently included.
- WEARABLE-2A intentionally provides no wearable-value export. Physical verification uses screenshots
  of the local card, Health Connect access/source screens and the value-free snapshot diagnostic.
- **Delete imported health context** removes revisions, stages, access events, sync state/tokens and
  wearable snapshots. It does not delete provider data, workout history or protected model output.

## 12. Samsung / Galaxy Watch limitation

AURON integrates with Health Connect, not a watch SDK. Galaxy Watch data normally reaches the phone's
Samsung Health application first, then Samsung Health writes supported records into Health Connect;
synchronization may be delayed until the watch reconnects, Samsung Health opens or the user refreshes.

Samsung's current official interoperability table lists Sleep sessions/stages and ordinary Heart Rate,
but does not list `HeartRateVariabilityRmssdRecord` or `RestingHeartRateRecord`. Because ordinary HR is
out of scope, a valid Galaxy pilot may deliver Sleep while HRV RMSSD and Resting HR remain unavailable.
That is an honest provider/missing-data limitation, not an AURON capture failure. See Samsung's
[Health Connect interoperability documentation](https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect).

## 13. Tests and protected-model proof

Automated coverage includes:

- complete/incomplete/gapped/clipped/invalid/overlapping stage timelines;
- overnight sessions and zone-offset retention;
- naps, main sleep and overlapping multiple origins;
- deterministic source/session selection;
- HRV RMSSD and Resting HR validation;
- exact re-read idempotency;
- changed revisions, deletion and identical re-upsert lifecycle;
- aggregate invalid-record abstention;
- successful bounded recovery from an expired Changes token without a stale error;
- partial/denied/revoked permission behavior;
- provider unavailable and remote interruption;
- delayed arrival, first-seen cutoff and immutable old snapshots;
- schema-9 to schema-10 data preservation and trigger creation;
- exact permission surface, no Internet permission, no Samsung SDK;
- no wearable dependency in MUSCLE-4B, Muscle Heat or Progressive Overload;
- no wearable extension of frozen Research Export V1;
- shadow snapshot call order after protected prediction freeze.

Emulator/test success is technical evidence only. It is not evidence that Samsung delivered real data,
nor biological validation of MUSCLE-4B.

## 14. Explicit non-goals

V1 does not implement or infer:

- vendor sleep score, Samsung Energy Score, readiness score or recovery modifier;
- calories, SpO2, workout HR, skin temperature, stress, respiratory rate or steps;
- Deep/REM certainty when stages are missing;
- muscle damage from wearable signals;
- background Health Connect access;
- a watch application or Samsung SDK;
- machine learning;
- health-value export;
- biological/predictive validation;
- any change to MUSCLE-4B, Muscle Heat, Progressive Overload, workout prescription, existing export or
  WEARABLE-1B Protocol V1.

## 15. Physical verification and WEARABLE-2B handoff criteria

WEARABLE-2B remains closed until the physical pilot phone demonstrates all of:

1. the signed `1.1.1 (3)` repair release upgrades the existing database successfully;
2. AURON requests exactly Sleep, HRV and Resting HR read access;
3. Samsung Health is authorized to write supported data into Health Connect;
4. Home selects the latest-ending eligible session, labels it as selected-session data and discloses
   other eligible sessions without summing them;
5. measurement/session time and AURON first-seen time reflect delayed delivery honestly;
6. a newly frozen prediction produces a sealed value-free snapshot diagnostic whose cutoff precedes
   any later delivery excluded from that snapshot;
7. refresh/disconnect/revoke/partial-permission behavior remains stable;
8. before/after readiness for identical workout history remains unchanged.

Only after that evidence is returned and audited may the task move to a later shadow-evaluation epoch.
No wearable modifier is authorized by this contract.

# Fork-local features (Android)

This fork (`DepengWang/health-md`) is maintained independently of `CodyBontecou/health-md`
(`upstream`), which may become a paid product. Policy:

1. Follow upstream wherever it is cheap and safe (bug fixes, small contract updates).
2. Where upstream cannot be followed, add the behaviour locally, in **new files** where possible,
   and keep edits to upstream-owned files to a few lines so a future sync stays mergeable.
3. Record every divergence here. Last reviewed: 2026-10-31.

## Local features

### 1. Sleep Day Attribution (wake-up date by default)

A device-local setting choosing which daily note owns a midnight-spanning sleep session.
Imported from upstream PR #135 (issue #104) and then changed so the Android default is the
wake-up date (`f591f08dc`). As of 2026-10-31 `git grep` finds no equivalent in the upstream
Android sources.

Main files: `domain/model/SleepDayAttribution.kt`, `data/health/SleepJournalSummary.kt`,
`HealthConnectManager.kt`, `HealthRepositoryImpl.kt`, `SettingsRepository(Impl).kt`,
`AdvancedSettingsScreen.kt`. Apple counterpart exists (`SleepDayAttribution.swift`).

### 2. Workout data de-duplication

Health Connect can hold the same workout from more than one writer, or one writer can store both
a whole-workout record and per-minute samples. Summing them doubled distance, calories and
elevation (a 10.24 km run exported as 20.58 km). Two rules in `HealthConnectManager.buildWorkoutData`
and the daily per-type distance:

- `singleOrigin`: keep records from a single data origin per workout (user-preferred origin, then the
  workout's own, then the largest).
- `sessionTotalOrAll`: if one record matches the workout bounds within one minute, use it alone.

Commits `6c3a3928f`, `1a0784fae`. Tests: `SingleOriginSelectionTest`, `SessionTotalSelectionTest`.
Root cause details: the data source Health Sync (`nl.appyhapps.healthsync`) writes both kinds of
record; the phone's own sensor is a separate origin.

### 3. Activity data source (preferred origin)

Settings → Advanced Export Settings → "Activity data source". `Automatic` keeps Health Connect's own
priority; choosing an origin makes steps, distance, calories, floors, elevation and workout
distance/calories/elevation come **only** from that origin. Sleep, heart and body data are unaffected.
Strict: an origin with no data for a period yields no value (no fallback to other origins).
Device-local, not part of portable setup sharing.

New files (no upstream conflict possible):

- `data/health/ActivityOriginPreference.kt` — preference interface and the multi-origin `singleOrigin` overload
- `presentation/settings/ActivityOriginSettingCard.kt` — card and its ViewModel
- `res/values/strings_fork.xml`, `res/values-b+zh+Hans/strings_fork.xml` — copy (kept out of
  `strings.xml` so the 15-locale parity test `LocalizationContractTest` is unaffected)
- tests: `SettingsRepositoryActivityOriginTest`, extra cases in `SingleOriginSelectionTest`

Small edits in upstream-owned files (conflict hot spots):

| File | Edit |
|---|---|
| `data/health/HealthConnectManager.kt` | constructor param, `activityOriginFilter()`, `recentActivityOrigins()`, `dataOriginFilter` on activity aggregates/reads, `WorkoutSourceRecords.preferredOrigin` |
| `domain/repository/SettingsRepository.kt` | three default-implemented members (keeps test fakes source-compatible) |
| `data/settings/SettingsRepositoryImpl.kt` | DataStore key `preferred_activity_origin` and its accessors |
| `di/HealthModule.kt` | passes the preference to `HealthConnectManager` |
| `presentation/settings/AdvancedSettingsScreen.kt` | one call: `ActivityOriginSettingCard()` |
| `test/.../di/HealthConnectDependencyConstructionTest.kt` | one argument added |

Limitation: the picker lists origins that contributed steps or distance in the last 14 days **under
Health Connect's source priority**. An origin removed from Health Connect's priority list (Health
Connect → Data and access → Data sources and priority) is not listed.

## Upstream changes picked up

| Commit | What |
|---|---|
| `d110bac07` | DataStore startup file-creation race fix |
| `fbc87fb07` | Stopped Direct CLI notifications stay removed |
| `8d86f481b` | `direct.full_public_authorized_corpus` added to `ProductCapabilityManifestTest` (mirrors upstream) |

Deliberately skipped: `bed1a7704` (WHOOP sleep by journal window). It lives under `src/play/`, which
the `fdroid` build does not ship, and depends on a long chain of upstream WHOOP integration commits.
Paywall / paid-access work, Apple localisation, website and CI changes were not taken.

## Deviation from the repository rules

`AGENTS.md` asks mobile features to be unified across Apple and Android and recorded in
`packages/contracts/product-capabilities.json`. Features 2 and 3 are Health Connect specific and are
**not** recorded there, on purpose: that file is a shared contract that upstream edits constantly, and
touching it is what makes a sync conflict. If these ever go upstream, add them to the capability
manifest first.

## Syncing with upstream

Do not merge `upstream/main` blindly. Dry-run first:

```
git fetch upstream
git merge-tree --write-tree --name-only main upstream/main
```

On 2026-10-31 (1069 upstream commits ahead) this reported only two conflicting files:
`apps/apple/HealthMd/Shared/Managers/HealthKitManager.swift` and `packages/contracts/manifest.json`.
Android merged cleanly in text, but compilation and tests were not checked. The trailing block this fork
adds to `.gitattributes` (CRLF fix below) can conflict trivially, because upstream also appends `eol=lf` rules there; keep both. Resolve on a trial branch,
then build and run the tests before touching `main`. `manifest.json` carries digests; regenerate it
rather than editing by hand.

## Running the unit tests on Windows

Known failures that are environmental, not product bugs (2 at the last full run on 2026-10-31, 1203 tests):

- `SharedSetupIntentCoordinatorTest`, `ClinicianReportFileStoreTest`: Robolectric `FileProvider` cannot
  match a `C:\...` temp root.

Fixed locally: `RawChangesServiceTest` (12 tests) failed on Windows because the test's default
`directorySync` helper opened a directory for fsync, which Windows refuses (NTFS journals directory
metadata). The helper now skips that step on Windows only; other hosts keep the real fsync, tests that
inject their own `directorySync` are unchanged, and production code (Android uses `Os.fsync`) was not
touched.

Fixed locally: `SharedSetupShareFixtureDocumentTest` failed because `core.autocrlf=true` checked out
`SharedSetupAndroidRuntimeTest.kt` (which embeds the LF-pinned contract fixture byte-for-byte) as CRLF.
`.gitattributes` now pins that one file to `eol=lf` (a separate block at the end of the file, to keep
upstream syncs easy). After pulling this on another Windows checkout, delete and re-checkout that file
once so the new rule applies.

The local HTTPS tests (`APIExportClientTest`, `RawSnapshotApiClientTest`,
`RawSnapshotProductApiServiceTest`) fail on Windows because Java resolves `127.0.0.1` to the literal IP
while the test certificate is issued for `localhost`. Editing the system `hosts` file does **not** fix
this. Passing a hosts file to the test JVM does, without touching the system or the repository.

On the maintainer's machine this is a permanent, user-level Gradle setup (outside the repository):

- `~/.gradle/health-md-test-hosts` containing `127.0.0.1 localhost` and `::1 localhost`
- `~/.gradle/init.d/health-md-test-hosts.gradle`, loaded automatically, nothing to pass on the command line:

```
allprojects {
    if (rootProject.projectDir.toURI().toString().contains('/health-md/')) {
        tasks.withType(Test).configureEach {
            systemProperty "jdk.net.hosts.file", "<absolute path to health-md-test-hosts>"
        }
    }
}
```

The path check limits it to checkouts under `/health-md/`, so other Gradle projects are unaffected. To try
it once without the permanent setup, put the same `allprojects` block (without the `if`) in a file and
pass it with `gradlew -I <file> ...`. Caveat: `jdk.net.hosts.file` replaces name resolution in the test
JVM, so a future test that needs real DNS would fail to resolve.

Build notes (JDK 17, Rust library built outside Gradle, `-x :healthmd-core:prepareRustDebug`) are in
the maintainer's local notes, not in this repository.

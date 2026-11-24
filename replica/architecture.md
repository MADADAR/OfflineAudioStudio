# SonicDemixer — Replica Architecture

**Status:** proposed target architecture, derived from `recon.md`, `features.csv`, and read-only inspection of `MADADAR/OfflineAudioStudio`.  
**Target:** Android APK, Arabic-first RTL, offline-first, no ads/subscriptions.  
**Architecture:** one native Android app; local database/files; no backend or microservices in MVP.

## 1. Stack decisions

| layer | choice | reason |
|---|---|---|
| Android/UI | Kotlin + Jetpack Compose + Material 3 | Native lifecycle, accessibility, RTL and hardware access. |
| State | ViewModel + Coroutines + StateFlow | Lifecycle-aware asynchronous UI state. |
| DI | Hilt, or manual DI if minimizing dependencies | Clear boundaries; choose one. |
| Database | Room / SQLite | Local persistence and migrations; one database. |
| Settings | DataStore | Small preferences, separate from relational project data. |
| Jobs | WorkManager | Durable work, retry and process-death recovery. |
| Import/decode | Storage Access Framework, MediaExtractor/MediaCodec, WAV PCM reader | User-selected files and local decoding. |
| Mixer | One PCM mixer and shared sample clock feeding AudioTrack | Independent MediaPlayers do not guarantee sample alignment. |
| Inference | Existing ONNX Runtime Android 1.22.0 as provisional runtime | Already declared in Gradle; validate model compatibility and memory. |
| Export | WAV PCM writer first; MP3 encoder only after licence review | Reliable baseline without assuming an encoder licence. |
| Tests/build | JUnit, AndroidX tests, Room tests, Gradle CI | Reproducible verification and device tests. |

No Next.js, Expo, Supabase, Postgres, auth, payments, email, cloud storage, webhooks or microservices: they are unnecessary for a local single-user MVP.

### 1.1 Observed repository baseline

Read-only inspection of the accessible `main` branch found:
- Android Gradle Plugin 8.13.2, Kotlin 2.2.20, Java 17, compile/target SDK 35, min SDK 26; ABI filter currently `arm64-v8a`.
- ONNX Runtime Android dependency `1.22.0` is declared.
- Gradle Compose plugin/dependencies are present, but `MainActivity.kt` currently uses a traditional `Activity` with Android Views; it is not yet a Compose UI.
- `DemucsEngine` downloads HT-Demucs ONNX weights from a public Hugging Face URL (code estimates roughly 174 MB), then performs inference locally. The manifest currently declares `INTERNET`; first-run separation therefore is **not fully offline today**. It is offline only after model download.
- Current import stages the selected file in cache; the engine requires 44.1 kHz after decode/preparation and uses fixed-size segments.
- Current playback uses `MediaPlayer`; synchronized multi-stem playback is not established.
- Room, DataStore and WorkManager are not yet present in the inspected app code.

This document describes a target architecture, not completed implementation. Keep working code until each replacement path passes tests. A public model URL does not grant redistribution rights: verify exact licence, provenance and checksum before bundling weights.

### 1.2 Package boundaries

```text
app/
  ui/                 Compose screens, navigation, RTL components
  domain/             entities, use cases, validation rules, interfaces
  data/local/         Room database, DAOs, migrations
  data/repository/    project/job/stem/export repositories
  audio/import/       SAF, metadata, decode, resample
  audio/mixer/        shared clock, PCM mixer, AudioTrack
  audio/export/       WAV writer, optional MP3 adapter
  separation/api/     SeparationEngine contract
  separation/onnx/    ONNX adapter and tensor mapping
  separation/model/   manifest, licence and checksum verification
  work/               WorkManager workers and recovery
  diagnostics/        local redacted diagnostics
```

UI calls domain use cases; it must not call Room, ONNX, MediaCodec or file APIs directly.

### 1.3 Core interfaces

```kotlin
interface SeparationEngine {
  suspend fun inspect(input: PreparedAudio): EngineCapabilities
  suspend fun separate(
    input: PreparedAudio,
    request: SeparationRequest,
    progress: (Float) -> Unit,
    cancellation: CancellationToken
  ): SeparationResult
}

interface AudioDecoder {
  suspend fun decodeToPcm(source: ImportedAudio, target: PcmSpec): PreparedAudio
}

interface StemMixer {
  fun load(stems: List<StemSource>)
  fun setGain(stemId: String, linearGain: Float)
  fun setMuted(stemId: String, muted: Boolean)
  fun setSolo(stemId: String, solo: Boolean)
  fun seekTo(frame: Long)
  fun play()
  fun pause()
  fun release()
}

interface StemExporter {
  suspend fun export(request: ExportRequest): ExportResult
}
```

## 2. Data model and SQL

SQLite is the only database. See executable reference DDL in `replica/schema.sql`; it defines seven tables: `app_install`, `projects`, `separation_jobs`, `stems`, `exports`, `app_settings`, `model_assets`.

Rules:
- The app is single-user/local. `app_install.owner_id` is a random installation UUID, not an account. All owned rows carry `owner_id` with an FK to the install row.
- Times are UTC epoch milliseconds in SQLite; convert to local time only for display. Audio frames/durations are sample counts, not wall-clock times.
- Audio is stored as files, never SQLite blobs. Import through SAF; persist URI permission and copy into app-private project storage when space allows.
- Project deletion cascades metadata; a cleanup worker removes app-managed files and retries failures. External user-exported documents are not silently deleted.
- A job is not `completed` until every requested stem is readable, has the expected sample rate and has matching frame count/duration.
- Partial unique index permits at most one active job per project. One inference worker globally by default to control memory and thermals.
- Unique idempotency keys prevent duplicate jobs/exports after repeated taps or retries.
- Room migrations own production schema evolution; `schema.sql` is the canonical design reference.

Suggested file layout:

```text
filesDir/projects/{projectId}/
  source/original.{ext}
  work/{jobId}/input.pcm
  work/{jobId}/staging/{stem}.part
  stems/{jobId}/{stem}.wav
  exports/{exportId}/...
filesDir/models/{modelId}/{version}/model.onnx
cacheDir/waveforms/       regenerable only
cacheDir/scratch/         bounded temporary files
```

Write to `.part`, flush and verify, then atomically rename. Commit DB rows only after final files validate. Startup recovery reconciles orphan temp files and missing DB references.

## 3. Local API / use-case contract

There is **no HTTP API** in the offline MVP. These are internal repository/use-case operations called by ViewModels.

| operation | purpose | caller | input → output | flow |
|---|---|---|---|---|
| `observeProjects()` | local library stream | S01 VM | filters → Flow<ProjectSummary> | F01/F04 |
| `importProject(uri)` | persist permission, inspect and stage source | S02 VM | SAF URI → ProjectId/error | F01 |
| `validateSource(id)` | codec/duration/storage check | S03 VM | ProjectId → ValidationReport | F01 |
| `getSeparationOptions(id)` | compatible licensed models and presets | S04 VM | ProjectId/device → options | F01 |
| `enqueueSeparation(id, option, key)` | transactionally create job and unique WorkManager request | S04 VM | request/key → JobId | F01 |
| `observeJob(id)` | progress/status/error stream | S05 VM | JobId → Flow<Job> | F01 |
| `cancelJob(id)` | cooperative cancellation | S05 VM | JobId → state | F01 |
| `loadMixer(projectId, jobId)` | load aligned stems on shared clock | S06 VM | IDs → mixer session | F02 |
| `updateStemMix(...)` | mute/solo/gain | S06 VM | stem controls → state | F02 |
| `seekMixer(frame)` / `setPlayback(playing)` | master transport | S06 VM | frame/state → playback state | F02 |
| `createExport(request, key)` | validate and queue export | S07 VM | scope/format/destination/key → ExportId | F03 |
| `observeExport(id)` | export status | S08 VM | ExportId → Flow<Export> | F03 |
| `renameProject(id,title)` / `deleteProject(id)` | project lifecycle | S09 VM | ID/title → result | F04 |
| `getDiagnostics()` | local model/storage/engine health | S10 VM | none → redacted report | S10 |

### Background workers

| worker | trigger | policy |
|---|---|---|
| PrepareAudioWorker | import needing copy/decode/resample | local only; retry transient I/O; permanent decode errors fail clearly |
| SeparationWorker | explicit user start | one inference at a time; foreground notification when required; cancellation at safe chunk boundaries; never publish partial stems |
| ExportWorker | user export request | idempotent; temp output then atomic publish |
| ProjectCleanupWorker | project deletion/orphan detection | retry app-private file deletion; retain failure diagnostics |
| RecoveryWorker | app startup/maintenance | reconcile DB/filesystem; never delete source or valid stems automatically |

No cron, email, calendar, webhook or server worker.

## 4. Failure-prone concerns

| concern | decision |
|---|---|
| Time zones/DST | UTC epoch timestamps; localize only for display. |
| Idempotency | unique request keys; repeat taps return existing job/export. |
| Races | DB transactions, unique active-job index, unique WorkManager names, serialized mixer commands. |
| Storage | preflight compressed source, decoded PCM, model and scratch estimates; retain safety reserve. |
| Offline | no network needed for core loop; bundle legally redistributable model for first-launch offline. If model must be downloaded, disclose that first launch needs internet and do not claim fully offline. |
| Model licence | record exact licence, provenance, redistribution permission and SHA-256; do not ship until verified. |
| Performance | benchmark cold/warm inference, peak RAM, latency, thermals and battery on actual devices. |
| Alignment | one sample clock; equal sample rate/frame count; test cross-correlation and remix error. |
| Audio routing | handle focus loss, calls, headphone unplug, Bluetooth and background transitions. |
| Process death | WorkManager intent + DB source of truth + staged files + startup reconciliation. |
| Memory | avoid full-song float tensors; chunk only if model supports it and overlap-add artifacts are tested; one inference job at a time. |
| Search/realtime | local Room search only; no realtime server. |
| Multi-tenancy/auth | not applicable; one local installation owner. |
| Deletion/privacy | delete local metadata and app-managed files; warn that exported copies outside app storage remain. |
| MP3 | WAV is MVP baseline; add MP3 only with a maintained, licence-compatible encoder. |
| Permissions | SAF pickers; avoid broad storage permission without a demonstrated requirement. |

### Device/model gate

Before polishing the UI, benchmark the actual target handset and at least one low-memory and one higher-tier device. Record SoC, RAM, Android build, model checksum and runtime version. Pass only if a representative song completes without OOM, app termination or unsafe thermal behavior, produces aligned stems and meets an agreed time/quality threshold. If it fails, narrow device support or choose a smaller licensed model. Do not promise all Android devices.

## 5. Build order

### M0 — Model feasibility (gate)
- Screens: minimal S10 diagnostics.
- Tables: app_install, app_settings, model_assets.
- Operations: diagnostics, model manifest/checksum verification, benchmark harness.
- Exit: redistribution rights verified; model loads and benchmark evidence exists on target hardware. If not, rescope.

### M1 — Vertical slice: real file to real stems
- Screens: S01–S06.
- Tables: add projects, separation_jobs, stems.
- Operations: import, validate, options, enqueue/observe/cancel job, load mixer.
- Exit: import MP3 and WAV; run actual licensed model offline; survive restart; synchronized stems play. Mock output does not pass.

### M2 — Mixer and exports
- Screens: S06–S08.
- Tables: add exports.
- Operations: mute/solo/gain/seek/play, create/observe export.
- Exit: individual and all-stem WAV exports pass metadata/alignment tests. MP3 only after encoder approval.

### M3 — Project lifecycle and recovery
- Screens: S01, S09, S10.
- Tables: all seven, migrations and indexes.
- Operations: rename/delete/reopen, cleanup and recovery.
- Exit: force-stop/restart at each stage; no corrupt project; app-managed deletion works.

### M4 — Quality and polish
- Screens: S01–S10.
- Add waveform, custom mix, accessibility, Arabic copy review, dark mode and device-specific optimization.
- Exit: automated tests, RTL/accessibility review and clean-device signed APK installation.

Must features from `features.csv` ship in M1–M3. Should features follow core correctness. Could features only after stable separation and playback. Skip cloud sync, collaboration, catalogue, accounts and proprietary assets.

## 6. Privacy and release

No analytics/ad/account SDK in the core path. Audit manifest and dependencies. Verify model checksum before inference. Redact absolute paths/audio metadata in shareable diagnostics. Provide local deletion. Record app/model/runtime/schema versions. Keep APK builds reproducible.

## 7. Open decisions

1. Verify the exact licence and redistribution rights for the current HT-Demucs ONNX file referenced by the source; public hosting is not a licence.
2. Confirm actual target handset RAM/SoC/Android build and benchmark.
3. Select MP3 encoder only after legal/maintenance review.
4. Confirm whether the current model's fixed segment and overlap behavior is valid for arbitrary durations.
5. Migrate existing Activity/MediaPlayer/cache-based prototype incrementally to Compose/Room/WorkManager/shared-clock mixer.

## Next — `/replica-design`

Specify the Arabic RTL design system and component states for S01–S10. Include empty, loading, permission-denied, low-storage, interrupted, failed and completed states. UI must not imply model capabilities that have not passed M0/M1.

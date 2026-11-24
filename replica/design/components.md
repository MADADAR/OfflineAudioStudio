# SonicDemixer — Component Catalogue
Component specifications and state contracts for Compose implementation.

## App shell
- AppScaffold: edge-to-edge, RTL, responsive margins, app bar, optional bottom navigation.
- TopBar: title, back, Settings action; contextual overflow.
- SnackbarHost: transient feedback; critical failures persist inline.

## Import
- ImportAudioButton: primary CTA, SAF launcher.
- AudioFileSummary: name, metadata, validation status.
- MetadataGrid: duration, size, sample rate, channels, codec.
- ValidationChecklist: pending/valid/invalid rows with text and icons.
- StorageEstimate: estimated work/output space, reserve, warning state.

## Model and job
- ModelCard: name/version, licence/provenance status, device eligibility, offline availability.
- StemPresetSelector: supported preset only; 2/4 stem options if model truly supports them.
- StemSelectionList: selected stems, unsupported options disabled.
- JobStatusCard: queued/running/cancelling/interrupted/failed/completed.
- RealProgressIndicator: determinate only with truthful progress; otherwise indeterminate.
- JobActionBar: cancel/retry/return; guarded against repeated requests.

## Mixer
- MasterTransport: play/pause, seek, current/duration, optional repeat if later.
- StemMixerRow: label/icon, mute, solo, gain, optional waveform.
- MuteToggle: accessible label and explicit state.
- SoloToggle: accessible label and explicit state.
- GainSlider: dB or normalized scale; value spoken; non-destructive.
- WaveformLane: generated from local PCM, scrollable only when necessary; phase 2.
- MixResetButton: restores default gains and mute/solo states.

## Export
- ExportScopeSelector: individual, all stems, custom mix (only implemented scope).
- FormatSelector: WAV only for MVP.
- FileNamePreview: deterministic, sanitized names.
- DestinationPicker: SAF.
- ExportProgressCard: genuine progress/status.
- ExportResultList: file, status, retry/open/share.

## Library/project
- ProjectList: loading/empty/populated/error.
- ProjectCard: title, duration, source metadata, status, date.
- ProjectOverflowMenu: rename/delete.
- RenameProjectDialog: validation, cancel/save.
- DeleteProjectDialog: consequences and confirmation.
- ProjectStorageSummary: app-managed storage.
- SearchField: local project filtering.

## Settings/diagnostics
- ThemeSelector: system/dark/light.
- LanguageSelector: Arabic default, additional language only when translated.
- StoragePanel: used/free/cache; no invented values.
- ModelDiagnosticsPanel: runtime, version, checksum, licence, loaded state.
- DiagnosticReportView: redacted, copyable.
- PrivacyPanel: explain local processing and exceptions (e.g., optional model download if allowed).

## Shared patterns
- EmptyState: illustration/icon, title, supporting text, one CTA.
- ErrorState: user-facing Arabic error, recovery action, optional details.
- ConfirmationDialog: confirm destructive or cancellation action.
- LoadingSkeleton: structural loading only, no fake progress.
- InlineStatus: icon + status text; colour never sole signal.
- PermissionExplainer: explain only the permission needed and why.

## Compose implementation note
These specs do not implement widgets. Build each component with Material 3, stable keys, state hoisting, lifecycle-aware StateFlow collection, UI previews for RTL/dark/light/large font, and UI tests for interactions and accessibility.

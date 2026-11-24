# SonicDemixer — Screen Specifications
These are design specifications for the target app, not screenshots or implemented UI.

## Navigation
Library (S01) is the home destination. Import is a picker-driven flow; processing is a persistent job screen; completed jobs open Mixer. Settings (S10) is available from the app bar.

## S01 — Library
Purpose: discover and reopen projects, and start importing.
Layout: app bar with SonicDemixer mark, settings icon; headline "مكتبة الصوت"; prominent "إضافة ملف صوتي" CTA; project search; project list sorted recent.
Project row: title, duration, source format, separation status, date; overflow rename/delete.
States:
- Empty: simple original waveform illustration; "ابدأ بفصل أول مقطع"; import CTA.
- Populated: cards/rows; completed jobs show stem count.
- Loading: skeleton rows, no fake data.
- Storage warning: warning line and storage details.
Actions: import; open completed project; rename; delete after confirmation.
Acceptance: no project is falsely marked complete; restore list after process restart.

## S02 — Import
Purpose: user selects local audio using Android SAF.
Layout: brief supported-format text, picker action, privacy/local note.
States: picker opened, selection cancelled, access failure, no selection.
Copy: "اختر ملفاً من جهازك. تتم المعالجة محلياً."
Actions: open system picker for audio; cancel safely.
Acceptance: no broad storage permission requested unnecessarily.

## S03 — Validate & Prepare
Purpose: inspect source and prepare audio for model requirements.
Layout: file name, format, duration, sample rate, channels, file size; validation checklist; continue action.
States: checking; ready; unsupported; corrupt; duration/storage limit; decoder failure.
Actions: retry validation; replace file; continue.
Acceptance: explain errors in Arabic and avoid presenting a valid state until inspection finishes.

## S04 — Separation Setup
Purpose: choose compatible separation configuration.
Layout: source summary; model card with licence/version and device compatibility; 2-stem/4-stem presets if supported; stem list; storage estimate; offline status; start CTA.
States: model missing/unlicensed; model incompatible; insufficient storage; default/custom selection.
Actions: select only supported presets; download model only if policy permits and clearly disclosed; start job.
Acceptance: unavailable model options disabled with reason; no unsupported stem claims.

## S05 — Processing
Purpose: monitor a durable separation job and safely cancel.
Layout: source title; selected output stems; progress (only genuine engine progress); stage label; estimated time only when defensible; cancel.
States: queued; running; checkpoint; interrupted/recovering; cancelled; failed; complete.
Actions: cancel confirmation; retry where safe; return to library.
Acceptance: job state survives app process death; no partial output published as completed.

## S06 — Mixer / Results
Purpose: listen to aligned stems and adjust mix.
Layout: top title/source; transport with play/pause, seek and elapsed/duration; waveform or timeline (phase 2 if not verified); rows for each stem with icon/name, mute, solo, gain; master controls; export CTA.
States: loading stems; playback; paused; missing/corrupt stem; audio focus interrupted; route changed.
Actions: seek, play/pause, mute, solo, gain, reset mix, export.
Acceptance: shared sample clock, equal duration/sample alignment, solo behavior documented; handles focus and headphones.
Controls: mute and solo are independent; multiple solo means only all soloed stems audible, unless implementation explicitly chooses single-solo and exposes it.

## S07 — Export Options
Purpose: choose export scope and format/destination.
Layout: export scope (selected stem, all stems, custom mix if supported); WAV format for MVP; naming preview; SAF destination; export CTA.
States: insufficient storage; destination denied; duplicate name; format unsupported.
Actions: choose scope; choose format if available; choose destination; queue export.
Acceptance: WAV available only after validated implementation; MP3 remains hidden until encoder/licence verified.

## S08 — Export Result
Purpose: confirm saved files and offer next action.
Layout: success/error status; output filenames; destination summary; actions open/share (supported by platform), return to mixer.
States: complete; partial failure; failure; retry.
Acceptance: confirm file existence before success; partial results accurately listed.

## S09 — Project Details
Purpose: rename, inspect, reopen or delete.
Layout: title/edit; source metadata; separation history; stem list; storage footprint; actions.
States: source missing; orphaned outputs; deletion confirmation.
Acceptance: delete app-managed files with retry; never delete user-exported copies outside app storage.

## S10 — Settings & Diagnostics
Purpose: control app preferences and inspect local health.
Layout: language (Arabic default), theme (dark/light/system), storage/cache, model/runtime/licence/checksum, diagnostics, privacy.
States: model unavailable; low storage; diagnostics failure.
Actions: change theme; clear regenerable cache; view local diagnostics; clear project data with explicit confirmation.
Acceptance: diagnostics redact sensitive file paths; no analytics/account/service dependency.

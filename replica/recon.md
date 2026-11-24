# Replica Recon — SonicDemixer

Reference: Moises (public product material only). Target: native Android APK. Clean-room functional study; no account login, source bundles, private APIs, network interception, or copied assets.

## Scope
- Platform: Moises Android as reference; SonicDemixer Android as target.
- Core loop: local audio import → stem selection → separation → synchronized stem playback/mix → export.
- Audience: users of an independent Arabic-first, RTL, offline-first Android product.
- Product constraints: free, no ads/subscriptions; no cloud account required.

## Sources
| source | URL | evidence |
|---|---|---|
| Moises stem separation | https://moises.ai/features/stem-separation/ | Advertised upload → choose model/stems → export flow. |
| Help centre | https://help.moises.ai/ | Public operational help and feature explanations. |
| Google Play listing | https://play.google.com/store/apps/details?hl=en_CA&id=ai.moises | Android positioning, import, separation, editing and export claims. |
| Moises Essentials | https://moises.ai/essentials/ | Public walkthroughs for first session, stems, speed/loops, chords/keys/lyrics. |
| Pricing | https://moises.ai/pricing/ | Feature gating context; limits vary and must be rechecked. |
| Product announcements | https://moises.ai/newsroom/product-announcements/ | Public product direction and feature announcements. |
| Main site | https://moises.ai/ | Product positioning and platform scope. |

No Moises account was accessed. Marketing pages are not a pixel-perfect Android walkthrough. No original screenshots were captured; `replica/screens/` is reserved for lawfully captured references and must never ship in the clone.

## Screen inventory
| ID | screen | entry | purpose | key states |
|---|---|---|---|---|
| S01 | Library/Home | launch | projects and add audio | empty, populated, loading, storage issue |
| S02 | Import source | S01 → Add | select local audio | picker, cancelled, permission denied |
| S03 | Validate media | S02 → select | inspect/decode/prepare | valid, unsupported, corrupt, too large, conversion error |
| S04 | Separation setup | S03 → continue | model/preset/stem selection | default, custom, insufficient storage, model unavailable |
| S05 | Processing | S04 → start | progress and cancellation | queued, running, interrupted, cancelled, failed, complete |
| S06 | Results/Mixer | S05 → complete | synchronized playback and controls | play, mute, solo, gain, seek, missing stem |
| S07 | Export options | S06 → export | select scope/format/destination | defaults, low storage, denied destination |
| S08 | Export result | S07 → save | show saved files | success, partial, failure |
| S09 | Project details | S01/S06 menu | rename/delete/reopen | existing, confirmation, orphaned output |
| S10 | Settings/Diagnostics | S01 → settings | language/theme/storage/model info | defaults, changed, low storage, diagnostics |

These are proposed SonicDemixer screens, not asserted Moises screen names.

## User flows
- F01 Separate local song: S01 → S02 → S03 → S04 → S05 → S06 → S07 → S08. Edges: invalid input, low storage, cancel, process death, model failure, partial output.
- F02 Remix stems: S01 → S06 → mute/solo/gain/seek/play. Edges: audio focus interruption, route changes, missing stem.
- F03 Export: S06 → S07 → S08. Edges: destination denied, insufficient space, duplicate name, interruption.
- F04 Manage project: S01 → S09 → reopen/rename/delete. Edges: source moved, corrupt metadata, orphan files.

## Components
App bar/navigation; primary/secondary buttons; audio file row; stem selector; progress/status; stem mixer row; master transport; format selector; dialog/bottom sheet; toast/inline error; empty state; permission explainer; storage indicator. Variants include enabled/disabled/loading/error/selected/muted/solo states.

## Inferred target data model
- Project: id, title, source URI/name/format/size, duration, sample rate, channels, status, timestamps, error.
- SeparationJob: project, model/version, preset, requested stems, status/progress, start/end, error, idempotency key.
- Stem: project/job, kind, path/checksum, codec, sample rate/channels/frame count, gain/mute/solo/order.
- Export: project/job/stem scope, format, destination, status, idempotency key, timestamps/error.
- AppSettings: language, theme, output format, source retention, cache policy, processing mode.
- ModelAsset: version, runtime, path, checksum, source, licence and redistribution metadata.
These are SonicDemixer proposals, not Moises internals. Confidence: high for project/job/stem/export concepts; medium/low for exact fields.

## Feature matrix policy
See `replica/features.csv`. `clone` begins as `no`; change only after implementation and verification. Must: import/validate/prepare, real 2/4-stem separation, offline inference, durable progress/cancel, synchronized playback, mute/solo/gain/seek, WAV stem/all-stem export, project library. Should: custom mix, rename/delete, waveform. Could: pitch/speed, BPM/key/chords, lyrics. Skip: cloud sync, collaboration, licensed catalogue, Moises accounts/data/assets, proprietary weights/APIs.

## Out of scope
Do not copy Moises code, binaries, model weights, private APIs, brand assets, screenshots, catalogue, users, projects, cloud infrastructure, paid entitlements or partner deals. Build an independent UI and use independently licensed models.

## Size and risks
Broad parity: XL. Constrained MVP: L, only if an appropriately licensed on-device model passes device benchmarks. Hardest: model inference/RAM/thermals; robust decode/resample/chunking/alignment; safe export and recovery. No promise of fast, high-quality offline separation on every Android device.

## Next
`/replica-architect` → architecture, SQLite schema, local use-case contract, benchmark gate and staged milestones.

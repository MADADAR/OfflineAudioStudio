# SonicDemixer — Design System
Version: 1.0 | Arabic-first | Android native | RTL | Dark-first

## Design principles
- Audio-first: playback, import, progress and mix controls remain the dominant actions.
- Calm, precise and editorial rather than a replica of another brand.
- Offline trust: use explicit local-only labels; never imply cloud upload.
- Accessible: touch targets >=48dp; clear labels; support system font scaling; contrast >=4.5:1 for body text.
- Arabic RTL by default, with numerals and audio timelines laid out consistently; icons whose direction conveys meaning are mirrored, media controls are not.

## Brand
- Product name: SonicDemixer.
- Voice: confident, clear, friendly; short Arabic labels and explanatory error copy.
- Logo concept: abstract 4-band waveform / separation mark. Avoid Moises shapes, logo, wordmark or brand assets.

## Color tokens (dark)
| Token | Hex | Usage |
|---|---|---|
| background | #0B0D12 | app background |
| surface | #141821 | cards and panels |
| surfaceRaised | #1C2330 | dialogs, sheets |
| primary | #7C5CFF | primary action and active state |
| primaryPressed | #6747E8 | pressed primary |
| secondary | #45D6B5 | progress, positive accent |
| textPrimary | #F4F6FC | headings/body |
| textSecondary | #AAB3C4 | metadata |
| textDisabled | #667085 | disabled |
| divider | #2B3342 | dividers |
| error | #FF6B76 | errors |
| warning | #FFCA67 | storage warning |
| waveform | #5E88FF | waveform accent |

## Light mode tokens
| Token | Hex | Usage |
|---|---|---|
| background | #F5F6FA | app background |
| surface | #FFFFFF | cards |
| surfaceRaised | #E9ECF4 | dialogs |
| primary | #6547E8 | primary action |
| secondary | #007E70 | progress/accent |
| textPrimary | #171B25 | body/headings |
| textSecondary | #586174 | metadata |
| divider | #D9DEEA | dividers |
| error | #BA2636 | errors |
| warning | #8B5700 | warnings |

## Typography
Use Material 3 typography and Android system Arabic-capable font fallback.
- Display: 28sp, weight 700, tight but readable.
- Headline: 22sp, weight 700.
- Title: 18sp, weight 600.
- Body: 16sp, weight 400; line-height approx 1.45.
- Label: 14sp, weight 600.
- Caption: 12sp; metadata only.
Use dynamic type/font scaling; avoid fixed-height text containers.

## Layout
- 4dp spacing base; key values: 4, 8, 12, 16, 20, 24, 32.
- Horizontal page margin: 20dp; compact margin 16dp.
- Cards radius: 18dp; buttons radius: 14dp; dialogs radius: 24dp.
- Primary CTA height: 56dp; compact controls >=48dp.
- Bottom navigation: Library, Projects (optional distinct if library is project list), Settings. MVP can use two destinations: Library and Settings; project detail remains contextual.
- Use edge-to-edge; safe-area padding; no horizontal scroll on screens except timeline/waveform.

## Components
### Buttons
Primary: solid primary, icon + short label where useful; one per screen section.
Secondary: tonal surface, outlined or text.
Destructive: error-colored text/button; confirmation required.
Disabled: low contrast but readable and explanatory when relevant.
Loading: preserve label; spinner replaces icon, prevent duplicate action.

### Cards
Surface card, 16–20dp padding, title, optional metadata and trailing action. Avoid nested cards unless hierarchy requires it.

### Status
Use icon + text + optional color; never color alone. Job statuses: جاهز، قيد المعالجة، متوقف، مكتمل، فشل.

### Audio file rows
File title, format/duration/size metadata, source icon, selection state and error indicator.

### Stem rows
Stem icon/color, Arabic name, mute toggle, solo button, gain slider, optional waveform lane. All stems share one timeline.

### Dialogs / bottom sheets
Confirm cancellation/deletion, model/storage warning, export destination and format. Keep short, dismissible where safe, and show consequences.

### Progress
Indeterminate before actual progress exists; progress percentage only when engine reports reliable progress. Explain cancellable checkpoints.

## Motion
- 180–240ms transitions; subtle fade/slide.
- Audio transport controls respond immediately.
- Do not animate false progress or make inference appear faster than it is.
- Respect Reduce Motion.

## RTL and audio controls
- Layout direction RTL for navigation, labels and menus.
- Timeline time labels, waveform and playback controls use a documented physical direction; playback glyphs remain standard media glyphs.
- Mute/solo/volume controls stay aligned consistently across Arabic/English.
- Numbers: localized display with parse-safe stored values.

## Accessibility and states
- Content descriptions for icon-only controls.
- TalkBack order follows logical flow.
- Slider exposes current value and min/max.
- Error, permission, low-storage, running and completion states include text.
- Test dark/light, 200% font scale, TalkBack, keyboard, RTL and small-screen layout.

## Asset policy
Create original vector/icon assets. No reference app screenshots, logos, proprietary artwork, model weights or catalogue assets.

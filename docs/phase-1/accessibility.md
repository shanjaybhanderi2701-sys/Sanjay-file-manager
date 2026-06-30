# Accessibility (M15 — TalkBack-usable, WCAG AA, localizable)

This document is the audit record for **M15 (APP-35)**. It maps each task
T152–T160 to where the behaviour lives, what was verified, and what residual
on-device sign-off is gated on the feature-complete build (T6.5).

The shared primitive is `core-ui`'s
[`Modifier.clickableTile`](../../core/core-ui/src/main/kotlin/com/appblish/filora/core/ui/a11y/Accessibility.kt):
custom tappable surfaces (grid tiles, cards, list rows) do **not** inherit the
button role, spoken action label, or 48dp target that `IconButton`/`Button` get
for free, so they opt in through `clickableTile`. Its semantics are
regression-locked by
[`AccessibilityTest`](../../core/core-ui/src/androidTest/kotlin/com/appblish/filora/core/ui/a11y/AccessibilityTest.kt).

## Task status

| Task | Scope | Status |
| --- | --- | --- |
| **T152** Content descriptions + semantics | core-ui | **Done.** `clickableTile` collapses each tile/row into one focusable `Role.Button` node with a spoken `onClickLabel`; decorative `Icon`s pass `contentDescription = null` so the reader is not spammed; meaningful icons (e.g. Settings action) carry a localized description. |
| **T153** 48dp minimum touch targets | core-ui | **Done.** `MinTouchTargetSize = 48.dp` is enforced via `heightIn(min = …)` inside `clickableTile`, so even one-line rows meet WCAG 2.5.5. Asserted by `clickableTile_…andMinTouchTarget`. |
| **T154** Dynamic font scaling | core-ui | **Done by construction.** All typography is defined in `sp` (`Type.kt`) and text uses `MaterialTheme.typography.*`; no `dp`/`fixed` text sizes exist. Rows use `maxLines + TextOverflow.Ellipsis` rather than fixed heights, so larger font scales reflow instead of clipping. On-device sweep at 200% font scale is folded into the T160 scanner pass. |
| **T155** Color contrast WCAG AA | core-ui | **Done.** Light/dark schemes are Material 3 baseline tonal palettes (`Color.kt` / `Theme.kt`); foreground/background pairs (onSurface/surface, onPrimary/primary, onSurfaceVariant for captions) clear the 4.5:1 (AA) text ratio. Dynamic color on Android 12+ inherits Material's contrast-guaranteed mappings. |
| **T156** Focus order + hardware keyboard nav | app | **Done by construction.** 100% Compose; the single-Activity nav graph lays composables in DOM/source order, which is the default focus traversal order. Tiles are real focusable buttons (via `clickableTile`), so Tab/D-pad traversal and Enter activation work without custom `focusOrder`. Hardware-keyboard sweep is part of the T160 on-device pass. |
| **T157** Screen-reader announcements for progress | core-ui / feature-browser | **Done.** `ProgressBarRow` is a `LiveRegionMode.Polite` region, so TalkBack announces label/percent/detail as an operation advances without the user re-focusing it. Asserted by `progressBarRow_isPoliteLiveRegion`. |
| **T158** Localization scaffolding | all | **Done** (landed under T7.3 / APP-83, see [localization-rtl.md](localization-rtl.md)). All user-facing copy resolves through `strings.xml`; lint promotes `HardcodedText`/`SetTextI18n` to error. The new a11y `onClickLabel`s added here are localized (`*_a11y_open`, en + ar). |
| **T159** RTL layout support | core-ui | **Done** (T7.3 / APP-83). `supportsRtl=true`, zero RTL-unsafe Compose constructs (start/end everywhere), `RtlHardcoded`/`RtlEnabled` lint at error. On-device visual RTL pass gated on T6.5. |
| **T160** Accessibility Scanner audit | app | **Pending on-device** — gated on T6.5 feature-complete build. See checklist below. |

## Call-site adoption of `clickableTile`

Landed in this change:

- `feature-home` — category tiles, Recents/Favorites chips, "Browse files",
  and the storage summary card.
- `feature-media` — category hub tiles.

Remaining call-site adoption (tracked, low-risk follow-up):

- `feature-storage` `CategoryRow` — uses `Modifier.clickable`; adopt
  `clickableTile` once the module's strings are externalized (its T158 slice),
  so the spoken label stays localized rather than introducing a literal.
- `feature-media` detail rows and `feature-browser` rows render via `FileRow`
  with a `combinedClickable` modifier (tap = open, long-press = actions); these
  already expose a click action and `ListItem` role. Give the `combinedClickable`
  an `onClickLabel`/`role` + `heightIn(48.dp)` in the same pass.

## T160 — on-device Accessibility Scanner checklist (gated on T6.5)

Run on the assembled `standardDebug` build once feature-complete:

1. Install Google's **Accessibility Scanner** and sweep every screen (Home,
   Media hub + detail, Storage, Browser + batch bar + dialogs, Search, Settings).
2. Confirm: no "missing label", no "low contrast", no "small touch target",
   no "duplicate description" findings; fix any that surface.
3. Run TalkBack end-to-end on the core flows (browse → open, multi-select →
   delete with progress announced, search, favorite/unfavorite).
4. Repeat at **200% font scale** and under **Force RTL** to confirm no clipping
   or mirroring defects (pseudolocale `en-XA`/`ar-XB` for length + bidi stress).
5. Capture before/after screenshots for the release checklist.

The instrumented `AccessibilityTest` (core-ui `androidTest`) keeps the role /
label / touch-target / live-region contract from regressing between scanner runs;
it needs the emulator matrix from T6.5 to execute in CI.

# Filora — High-Fidelity Design Spec (Direction **A-blend**)

**Owner:** appblish · UI/UX Designer · **Status:** APP-109 deliverable (hi-fi for the approved direction)
**Approved direction:** **A-blend** — *Calm Utility* base + Direction B's **dual-pane on large screens** + Direction C's **storage-story** on the Storage surface only (APP-103, interaction `daf2fd2c`, 2026-06-30).
**Inputs:** `02-product-vision.md`, `03-ux-principles.md`, `04-design-directions.md`, lo-fi set in `filora-screens/wireframes/`.
**Live hi-fi gallery:** _see APP-109 work product (here.now link)._
**Handoff to:** Founding Android Engineer (UI alignment/rework — separate follow-up issue).

> This is the source of truth for the visual layer. It is written as **redlines against
> the existing M0–M7 implementation** (`core/core-ui/theme/`, the feature modules), not a
> green-field design — the build is already ~Direction A, so A-blend is a *targeted*
> alignment, not a rewrite.

---

## 0. Principle weighting (lead with this)

A-blend is **Calm Utility first**, with two surgical borrowings. Every design decision
below is ordered by this weighting; when two treatments compete, the higher-weighted
principle wins.

| Rank | Principle (`03-ux-principles.md`) | Where it leads in A-blend |
|---|---|---|
| **1** | #1 Content first, chrome last | Every list ≥80% content; chrome recedes |
| **1** | #2 Common path one tap / power path one more | Bottom nav + long-press multi-select |
| **2** | #6 Trust is visible | Permission *reasons* first, no nags/upsells |
| **2** | #7 One Filora, every screen size | **Dual-pane ≥600dp (from B)**; M3 Expressive |
| **3** | #5 Designed empties/loadings/errors | First-class states on every screen |
| **3** | #4 Every operation is honest | Explicit conflicts, real progress, undo |
| **3** | #3 Speed is a design constraint | Skeletons; **storage-story (from C) is static-first** |

**The two borrowings, scoped tightly:**
- **From B — dual-pane** only at `≥600dp` width (foldable unfolded, tablet). Phone stays
  single-column. This is principle #7, not a density change: A-blend keeps comfortable
  density everywhere; it does **not** adopt B's compact rows or dark-default surface.
- **From C — storage-story** only on the **Storage Insights** screen, and only as a
  *calm* year-in-review band, not C's app-wide expressive motion. Principle #3 caps it:
  the story renders from already-computed breakdown data, no extra scan, no parallax that
  drops frames.

Everything else is Calm Utility: neutral M3 surfaces, single dynamic accent, comfortable
tap targets, one primary action per screen, progressive disclosure of power features.

---

## 1. Design tokens — M3 Expressive

These map **directly** onto the existing Compose theme. Where a token already exists in
code I cite the file; where it is a **gap** I mark it `NEW` and give the engineer the
exact addition.

### 1.1 Color

**Already correct — keep as-is.** `core-ui/theme/Color.kt` + `Theme.kt` already implement:
- Light + dark schemes seeded from brand teal `#00696B` (light primary) / `#4CDADA` (dark).
- Tertiary blue reserved for **storage/insight** visuals — A-blend keeps this; the
  storage-story uses `tertiary`/`tertiaryContainer` for the hero band so it reads as
  "insight", distinct from the teal primary action color.
- **Dynamic color (Material You) on API 31+** with the static schemes as the fallback.

**Redline — no color changes required.** The palette is already A-blend-correct. Two usage rules:
1. **Category accent coding is OFF in A-blend.** That is a Direction-B idea. Media
   category tiles use `secondaryContainer`/`surfaceVariant` tonal fills with a single
   accent icon — not seven different type-coded colors.
2. **Dynamic color must not break the tertiary "insight" semantic.** When Material You is
   active, storage visuals still use the scheme's `tertiary` role (the system derives it),
   so the insight/action distinction survives wallpaper changes. No hard-coded hex on the
   storage screen.

### 1.2 Type scale

**Already correct — keep `core-ui/theme/Type.kt` as-is.** It overrides exactly the M3
roles Filora uses. Role → usage contract (make the implementation obey this; today some
call sites pick sizes ad hoc):

| Role | Size/line | Use for |
|---|---|---|
| `displaySmall` | 36/44 | Storage **hero number** (free space), storage-story stat |
| `headlineSmall` | 24/32 | Empty-state titles, large dialog titles |
| `titleLarge` | 22/28 | App-bar titles, screen section headers |
| `titleMedium` | 16/24 | Card titles, grid-tile labels, list section headers |
| `titleSmall` | 14/20 | Dense subheaders, dialog field labels |
| `bodyLarge` | 16/24 | **File/folder names** (primary list text) |
| `bodyMedium` | 14/20 | Descriptions, secondary copy |
| `bodySmall` | 12/16 | File metadata (size · modified), captions |
| `labelLarge` | 14/20 | Buttons, primary chips |
| `labelMedium` | 12/16 | Breadcrumb crumbs, filter-chip labels |
| `labelSmall` | 11/16 | Badge counts, overlines |

**Redline:** stop passing literal `fontSize`/`FontWeight` in feature screens; reference
`MaterialTheme.typography.<role>`. (Audit item for the alignment pass.)

### 1.3 Spacing — `NEW` (gap in code)

There is **no spacing token** in `core-ui` today; gaps are hard-coded `.dp` per screen,
which is the #1 source of visual inconsistency. Add a 4dp-base scale and reference it
everywhere.

```kotlin
// core-ui/theme/Spacing.kt  (NEW)
import androidx.compose.ui.unit.dp
object FiloraSpacing {
    val none = 0.dp
    val xxs  = 2.dp
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp   // default screen edge padding (phone)
    val xl   = 24.dp   // section separation; edge padding ≥600dp
    val xxl  = 32.dp
    val xxxl = 48.dp   // min touch target / large empty-state spacing
}
```

Usage contract:
- **Screen edge padding:** `lg` (16dp) phone, `xl` (24dp) at `≥600dp`.
- **List row vertical padding:** `md` (12dp) → row height ≥ `56dp` (with `bodyLarge` +
  `bodySmall` two-line). Comfortable density — do **not** drop to B's compact rows.
- **Between cards / section gap:** `lg`–`xl`.
- **Inside a card:** `lg` padding, `sm`–`md` between elements.
- **Grid gutter (media):** `sm` (8dp); grid cell min `104dp` phone, adaptive count.

### 1.4 Elevation — `NEW` (formalize)

M3 Expressive favors **tonal** elevation (surface color shift) over shadows. Define the
intent so it is consistent:

| Token | dp (tonal) | Use |
|---|---|---|
| `level0` | 0 | Screen background, scrolled-under list |
| `level1` | 1 | Resting cards, list (Home category/storage cards) |
| `level2` | 3 | App bar on scroll, search bar, batch-action bar |
| `level3` | 6 | FAB, menus, the **selected** pane in dual-pane |
| `level4`/`level5` | 8/12 | Dialogs, bottom sheets, the conflict sheet |

Use `Surface(tonalElevation = …)` — **not** drop shadows — so dynamic color tints
elevation correctly. Active selection state = `level3` tonal + `secondaryContainer` tint.

### 1.5 Shape

**Keep `core-ui/theme/Shape.kt` as-is** (4/8/12/16/28dp). Mapping:
- `small` (8) — chips, small buttons.
- `medium` (12) — list-row press ripple bounds, media thumbnails.
- `large` (16) — cards (Home tiles, storage cards, the **storage-story hero band**).
- `extraLarge` (28) — dialogs, bottom sheets, FAB.

### 1.6 Motion — `NEW` (specs, M3 Expressive but restrained)

A-blend motion is **functional, not decorative** (principle #3 caps C's generous motion).

| Pattern | Spec | Where |
|---|---|---|
| Open file/folder | Container transform, 350ms, `emphasized` easing | Row/tile → detail |
| Screen change | Shared-axis X, 300ms | Bottom-nav destinations |
| Dialog/sheet in | Fade + scale-from-95%, 200ms, `emphasizedDecelerate` | All dialogs |
| Selection enter | Cross-fade app bar → batch bar, 150ms | Multi-select |
| Skeleton → content | Cross-fade 150ms (no spinner) | All list loads |
| Storage-story reveal | One-shot count-up + bar grow on first paint, ≤600ms, then static | Storage hero only |
| Pull-to-refresh | M3 default indicator | Browser/Home |

**Hard rule (principle #3):** no continuous/parallax motion, no motion that runs while
scrolling, thumbnails never animate in on scroll. Story animation is **one-shot on
first composition only**; honor `Settings → Reduce motion` / system animation scale.

---

## 2. Adaptive layout (the B borrowing) — `NEW`

Single breakpoint set; use `WindowSizeClass`.

| Width class | Layout |
|---|---|
| **Compact** `<600dp` (phone) | Single column; **bottom nav**; dialogs centered |
| **Medium** `600–839dp` (foldable open, small tablet) | **Dual-pane** browser/media; **nav rail** replaces bottom nav; edge padding 24dp |
| **Expanded** `≥840dp` (tablet, large foldable) | Dual-pane with wider detail; nav rail; optional 3-region on Storage |

**Dual-pane contract (Browser & Media):**
- Left pane = list/tree (fixed `360dp` in Medium, flexible in Expanded).
- Right pane = detail / selected-folder contents / media preview.
- Selecting in the left pane updates the right pane **in place** (no full navigation) —
  this is the B speed win, kept calm.
- Phone keeps the existing push-navigation; dual-pane is **additive**, gated behind size
  class. No phone regression.
- Selected left-pane item: `secondaryContainer` tint + `level3` tonal (see 1.4).

**Nav rail vs bottom nav:** same four destinations (Home · Browse · Storage · Settings).
Swap component by size class; do not show both. Keep order and icons identical.

---

## 3. Per-screen hi-fi specs (redlines)

Each screen lists the **target** in A-blend and the **delta** from the current build.
States covered per principle #5: `loading` (skeleton), `empty`, `error`, `content`.

### 3.1 Home (`02-home` / feature-home)
- **Layout:** single column, edge `lg`. Order: storage summary card → category row → Favorites strip → Recents strip. App bar `titleLarge` "Filora", overflow only.
- **Storage summary card:** `large` shape, `level1`, used/free with a thin segmented bar; tap → Storage. Uses **tertiary** accent.
- **Category row:** horizontally scrollable tonal tiles (`secondaryContainer`), icon + label + count (`titleMedium`/`labelSmall`). **No type-coded colors** (delta from any B-leaning impl).
- **States:** loading = skeleton cards; empty (no media perm) = trust card explaining *why* before re-asking (principle #6); error = inline retry.
- **Delta:** confirm token usage; ensure recents/favorites use `bodyLarge` name + `bodySmall` meta.

### 3.2 Browser (`03-browser` / feature-browser)
- **Layout:** breadcrumb bar (`labelMedium` crumbs, horizontally scrollable) → list/grid toggle → content. Row = leading type icon (`medium` shape thumb for media) + name `bodyLarge` + meta `bodySmall` + trailing overflow. Row height ≥56dp, padding `md`.
- **Multi-select:** long-press → app bar cross-fades to **batch-action bar** (`level2`); share disabled when a folder is selected (already implemented — keep). FAB = create folder / add (primary, `extraLarge`).
- **≥600dp:** **dual-pane** (left = current dir list, right = selected subfolder/file preview).
- **States:** loading = row skeletons (8); empty folder = "This folder is empty" + create-folder action; error (permission/IO) = reason + action.
- **Delta:** wire dual-pane; align row paddings to `FiloraSpacing`.

### 3.3 Search (`08-search` / feature-search)
- **Layout:** persistent M3 search bar (`level2`, `extraLarge`) at top → removable **filter chips** (type/size/date) → results list (same row spec as Browser).
- **States:** idle = recent searches / hint; loading = streaming results with skeleton tail (results stream in — principle #3); empty = "No matches for *query*" + "Clear filters"; error = retry.
- **Delta:** chips use `labelMedium`, `small` shape; ensure result streaming shows progressive fill, not a blocking spinner.

### 3.4 Media hub + detail (`04-media-hub`, `05-media-detail` / feature-media)
- **Hub:** grid of 7 category tiles (`large` shape, tonal fill, icon + label + count). Comfortable, not C's edge-to-edge imagery.
- **Detail:** thumbnail grid, cell min `104dp`, gutter `sm`, `medium`-shape thumbs; tap = open (ACTION_VIEW), long-press = share/select. Thumbnails decode off-thread, fade only on first load.
- **≥600dp:** dual-pane (categories left / grid right).
- **States:** loading = shimmer grid; empty = "No <category> yet"; error/permission = reason + grant.

### 3.5 Storage Insights + **storage-story** (`09-storage` / feature-storage)
- **This is the C borrowing.** Top = **storage-story hero band** (`large` shape, `tertiaryContainer`): big `displaySmall` free-space number + one-shot count-up, a calm segmented bar of by-category usage, and one motivating line ("You freed 1.2 GB this month" if data exists, else a neutral summary). **Static after the first reveal.**
- Below: per-volume cards (used/free) → by-category breakdown rows → "Largest files" entry.
- **States:** loading = hero skeleton + bar placeholder (no count-up until data); empty (new device) = neutral hero, no fake stat; error = retry.
- **Delta:** add the hero band to the existing Storage screen; everything below is the current breakdown UI re-tokenized. Story data comes from the **already-computed** breakdown — no second scan (principle #3).

### 3.6 Settings (`10-settings` / feature-settings)
- Standard M3 preference list: Appearance (theme, dynamic color), View (list/grid default, sort, show hidden), About. Section headers `titleMedium`, rows `bodyLarge` + `bodyMedium` summary. **No upsells, no account nag** (principle #6).
- Add **Reduce motion** honored note (ties to 1.6). 

### 3.7 Dialogs / states (`11-dialogs`, `12-operation-progress`, `06/07` / feature-browser dialogs)
- **Dialogs** (create/rename/delete/conflict): `extraLarge` shape, `level4`, title `headlineSmall`/`titleLarge`, body `bodyMedium`, primary/secondary buttons `labelLarge`. Inline validation error `bodySmall` in `error` color.
- **Conflict sheet (principle #4):** explicit Overwrite / Skip / Keep both, never silent. Bottom sheet `extraLarge`, `level4`.
- **Operation progress (principle #4):** determinate progress with real counts, foreground-service-backed, survives backgrounding; row `liveRegion` for TalkBack.
- **Permission (`01-permission`, principle #6):** reason-first screen *before* the system prompt; escape hatches (settings deep link, all-files opt-in) already built — keep.

---

## 4. Accessibility (non-negotiable, applies to all of §3)

- Touch targets ≥ **48dp** (`FiloraSpacing.xxxl`) — already enforced via core-ui
  `clickableTile` (APP-82). Keep all new tiles/rows on it.
- **AA contrast** on every text/background pair, including dynamic color and the
  tertiary storage band — verify the `onTertiaryContainer` pair at runtime.
- TalkBack: `onClickLabel` on every actionable tile/row; progress rows `liveRegion`.
- **RTL** mirrored (Arabic strings already present); test dual-pane mirroring.
- Honor system font scale up to 200% — comfortable density gives headroom; verify list
  rows reflow (don't truncate the name before the meta).

---

## 5. Engineering handoff — alignment checklist (for the follow-up issue)

Ordered by leverage. This is a **targeted alignment**, not a rewrite — the build is
already ~Direction A.

1. **`NEW` `core-ui/theme/Spacing.kt`** (§1.3) + replace ad-hoc `.dp` across feature
   screens. *(Highest consistency leverage.)*
2. **`NEW` elevation + motion token intent** (§1.4/1.6) as a small `FiloraElevation`
   constants + a motion-spec util; migrate dialogs/transitions.
3. **Adaptive dual-pane** for Browser + Media gated on `WindowSizeClass` (§2). Nav rail
   swap. *(Largest new layout work; the B borrowing.)*
4. **Storage-story hero band** on the Storage screen (§3.5). *(The C borrowing.)*
5. **Type-role audit** — remove literal `fontSize`/`FontWeight`; use
   `MaterialTheme.typography` roles (§1.2).
6. **Category color audit** — ensure media tiles are tonal, not type-coded (§1.1 rule 1).
7. **State coverage audit** — every screen has skeleton/empty/error per §3 + §5 of
   principles.
8. **A11y verification pass** on a `≥600dp` device for dual-pane + RTL mirroring (§4).

Items 1–2 and 5–7 are low-risk polish (can land incrementally). Items 3–4 are the real
A-blend deltas and should be scoped as their own subtasks.

---

## 6. What this spec deliberately does **not** do

- Does **not** adopt B's compact density, dark-default surface, or file-type color coding.
- Does **not** adopt C's app-wide expressive motion, parallax, or content-derived palettes.
- Does **not** change the navigation model, IA, or the screen set (those are fixed in
  `06-navigation-flow.md` and the lo-fi set).
- Does **not** introduce a bundled brand font in v1 (platform sans; swap-in point kept in
  `Type.kt`).

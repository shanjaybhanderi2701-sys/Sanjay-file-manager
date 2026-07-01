# Filora — Premium Craft Pass (A-blend) — engineering map

**Issue:** APP-134 · **Owner:** UI/UX Designer · **Blocks:** APP-117, APP-118
**Status:** proposed for board sign-off · **Supersedes the visual layer of:** `05-hifi-spec.md`
**Hi-fi gallery:** `design/filora-screens/hifi/index.html` (live preview link in the APP-134 thread)

> **Scope.** This is a *craft* pass, not a redesign. A-blend's information architecture, density,
> navigation, and accessibility are unchanged. What changes is the **visual layer**: brand color,
> iconography, elevation, shape, type usage, and state treatment — so Filora reads as a **premium,
> branded product** instead of stock Material-3. `05-hifi-spec.md` remains authoritative for layout,
> breakpoints, and behavior; this doc overrides its **token values** where they conflict.

The board review on APP-112 found the previous gallery reads as "generic default Jetpack-Compose."
The root cause was drawing directly on raw M3 tokens (stock teal `#00696B`, `Icons.Default.*`, flat
tonal surfaces). Restraint ≠ generic. Every delta below maps to a concrete `core-ui/theme` token or
component so APP-117 / APP-118 can build against it.

---

## 1 · Brand identity — "Filora Iris"

Signature color story replacing the stock M3 teal seed. Iris (indigo→violet) reads premium, is
distinct from the reference app (typically blue) and from stock M3, and gives a natural gradient for
the hero surfaces.

### 1.1 Brand palette → `Color.kt`

| Token (light)          | Old (teal)   | **New (Iris)** | Role                              |
|------------------------|--------------|----------------|-----------------------------------|
| `PrimaryLight`         | `#00696B`    | **`#5B5BD6`**  | Primary — brand anchor            |
| `OnPrimaryLight`       | `#FFFFFF`    | `#FFFFFF`      | on primary                        |
| `PrimaryContainerLight`| `#6FF6F6`    | **`#E6E5FB`**  | selection bg, chips-on, quick-tool tint |
| `OnPrimaryContainerL.` | `#002020`    | **`#1B1A54`**  | on primary-container              |
| brand gradient mid     | —            | **`#8B5CF6`**  | hero/FAB gradient stop (violet)   |
| brand gradient end     | —            | **`#C06BE6`**  | hero gradient stop (orchid)       |
| `SurfaceLight`         | `#FAFDFB`    | `#FFFFFF`      | card fill                         |
| `BackgroundLight`      | `#FAFDFB`    | **`#F5F4FB`**  | app bg — soft lilac tint, not flat white |
| `OnSurfaceLight`       | `#191C1B`    | **`#15142B`**  | ink — indigo-tinted, not grey-black |
| `OnSurfaceVariantL.`   | `#3F4947`    | **`#75738F`**  | metadata / captions               |
| `OutlineLight`         | `#6F7977`    | **`#E2DFF2`**  | hairline (lighter, calmer)        |

- **Gradient is brand, not decoration.** Expose a reusable `Brush.linearGradient(listOf(Iris, Violet,
  Orchid))` as `FiloraGradients.brand` in `core-ui/theme`. Used by: Home hero ring card, FAB, logo,
  avatar. The storage-story hero uses a **darker** variant `FiloraGradients.story`
  (`#242349 → #3A2F6E → #5B5BD6`).
- **Dynamic color still wins.** When Material You is enabled, the wallpaper scheme overrides the static
  brand scheme exactly as today — brand tokens are the *fallback* + the source for the fixed accents in
  §1.2. Keep the `dynamicColor` branch in `Theme.kt` intact.
- Dark scheme: primary lifts to `#B7B5FF` on `#2A2960`; background `#131226`; regenerate the full
  tonal set from seed `#5B5BD6` with the same Material tonal-palette tool used for the teal set.

### 1.2 Per-category accent wheel → new `FiloraCategoryColors`

A cohesive, harmonised accent per file category (equal S/L so they sit together). Each is a **duotone
pair**: solid `accent` (icon/line) + soft `container` (tile fill). These are **semantic, fixed** colors
(they must survive dynamic color so a photo is always rose, a doc always blue).

| Category   | `accent`   | `container` |
|------------|------------|-------------|
| Images     | `#F43F6E`  | `#FFE1E9`   |
| Videos     | `#7C5CF7`  | `#EAE2FF`   |
| Audio      | `#F5921B`  | `#FFEBCF`   |
| Documents  | `#2E7CF6`  | `#DBE9FF`   |
| Downloads  | `#12A46B`  | `#CFF3E1`   |
| Apps / APK | `#5B5BD6`  | `#E4E4FB`   |
| Archives   | `#B44BC9`  | `#F6DEFB`   |
| Folder     | `#6E6C8A`  | `#ECEBF3`   |
| Favorite   | `#F5A524`  | `#FFF0D2`   |

- Add `object FiloraCategoryColors` returning `data class CategoryColor(accent, container)` keyed by the
  existing `FileCategory` enum (from `FileExtensions.categoryOf`). One lookup, used by the leading tile in
  every file row, the category cards, and the storage breakdown dots/meters.
- Provide light **and** dark container values (dark containers ≈ 22% accent over surface).
- **Contrast rule:** the duotone tile is `accent` glyph on `container` fill — verified ≥ 4.5:1 for the
  glyph; when a category color is used as a text label it uses the darker `accent`, never `container`.

### 1.3 Logo / wordmark

- App mark = rounded-square folder silhouette with an Iris→Violet gradient and a violet "aperture" dot
  (see gallery header + tablet nav-rail). Ship as an adaptive icon + a vector `filora_logo` for the nav
  rail / splash / About.
- Wordmark: "Filora" in Manrope ExtraBold, `-0.6px` tracking (see §4).

---

## 2 · Custom iconography → `FiloraIcons`

Replace `Icons.Default.*` with a **cohesive duotone line set**: 1.6–1.9px stroke, round caps/joins, a
low-opacity (~0.28) tonal fill behind the line for weight. The gallery ships the full set as inline SVG
symbols (`#i-image`, `#i-folder`, `#i-doc`, `#i-video`, `#i-audio`, `#i-download`, `#i-app`, `#i-archive`,
`#i-search`, `#i-storage`, `#i-settings`, `#i-sort`, `#i-share`, `#i-star`, `#i-clock`, `#i-more`,
`#i-back`, `#i-check`, `#i-grid`, `#i-broom`, `#i-shield`, `#i-transfer`, `#i-sparkle`, `#i-cloudoff`,
`#i-inbox`).

**Engineering options (pick one, define centrally):**
1. **Recommended:** import a duotone open-source set as vector assets (e.g. Phosphor *duotone* or Solar
   *bold-duotone*, both permissively licensed) into `core-ui/res/drawable`, exposed via
   `object FiloraIcons { val Image = R.drawable.ic_image … }`. Fastest path to the exact look; duotone
   comes for free.
2. Author the ~25 glyphs above as `ImageVector` in `FiloraIcons.kt` from the gallery SVG paths (paths are
   drop-in). More work, zero dependency, full control.

**Rules:** never call `Icons.Default.*` in feature code — always `FiloraIcons.*`. The tonal layer takes
the category `container`/`accent`; the line takes `accent`. Toolbar/nav icons are monochrome
(`onSurfaceVariant`, `primary` when active).

---

## 3 · Home — the surface that sells the product

Priority #1 for "premium feel." Composition top→bottom:

| Block            | What it is                                                              | Tokens / components |
|------------------|------------------------------------------------------------------------|---------------------|
| Greeting app bar | "Good evening" overline + "Filora" title + search + gradient avatar    | `titleLarge` 800wt; avatar = `FiloraGradients.brand` |
| **Storage ring** | Gradient hero card, arc/ring meter (66% used), category legend         | Card `brand` brush, `shape.extraLarge` (32dp), `sh-brand`; ring = `Canvas` arc, `drawArc` stroke 12dp round cap; count-up on first show (§6) |
| Quick-tools row  | 4 tinted action chips: Clean up / Transfer / Safe box / Recent         | `qt` = surface card + category-tinted `qi` icon well; each ≥ 48dp tap |
| Category tiles   | 2×2 rounded cards, per-category `container` icon well + accent, count/size | `FiloraCategoryColors`, `shape.large` (20dp), `Elevation.level1` + soft shadow; decorative accent arc bottom-right |
| Recent strip     | Horizontal thumbnail cards with depth + type badge                     | `rcard` = surface, `sh-2`, real Coil thumbnail (falls back to duotone icon) |
| Favorites        | Pill chips with mini category icon                                     | `favp`, `shape.pill` |

- The ring is the single strongest premium cue — build it first. The arc sweep = `usedFraction * 360°`;
  keep the track at `white @ 22%` over the gradient. Center = `displaySmall` % + "used" caption.
- Legend colors reuse the category accents (photos/video/audio/other) so Home and Storage tell one story.

---

## 4 · Type scale → `Type.kt`

Adopt a **branded family** and actually *use* the display/headline roles (the old gallery was almost all
body/title, which reads flat).

- **Family:** Manrope (open, geometric, premium) via **downloadable fonts**
  (`androidx.compose.ui.text.googlefonts.GoogleFont` + `Provider`) — no bundled binary, Compose-native,
  graceful fallback to platform sans. Swap `FontFamily.Default` → `FiloraFontFamily` centrally in
  `Type.kt`; call sites unchanged. (If offline builds are a concern, bundle the 3 needed weights instead.)
- **Weights:** headings/labels use `SemiBold`/`ExtraBold` (700/800); body stays `Regular`/`Medium`.
  This weight contrast is most of the "crafted" feel.
- **Tracking:** display/headline/title get `-0.3 … -0.6px` letter-spacing (tighter = more premium).
- **Usage (new):** `displaySmall` 800 → storage ring % and the storage-story "free up" number;
  `headlineSmall` 800 → empty/error titles; `titleLarge` 800 → app-bar + section headers;
  `titleMedium`/`labelLarge` 700 → row names, chips, tile labels.

---

## 5 · Depth, shape & finish

### 5.1 Elevation → extend `Elevation.kt` with a shadow companion

M3 tonal elevation alone reads flat on a light bg. Add a **soft, indigo-tinted drop-shadow** companion
for cards (tonal elevation stays for dynamic-color tinting; shadow adds the physical depth).

| Token   | Shadow (light)                                                       | Use |
|---------|---------------------------------------------------------------------|-----|
| `sh-1`  | `0 1px 2px rgba(21,20,43,.05), 0 1px 3px …`                          | resting cards, quick-tools |
| `sh-2`  | `0 6px 16px rgba(21,20,43,.07), 0 2px 5px …`                         | recent cards, search bar |
| `sh-3`  | `0 16px 34px rgba(21,20,43,.12), 0 5px 12px …`                       | dialogs, storage-story |
| `sh-brand` | `0 12px 26px rgba(91,91,214,.30)`                                 | hero ring card, FAB, chips-on |

Implement via `Modifier.shadow(elevation, shape, ambientColor = Ink, spotColor = Ink)` wrapped in a
`Modifier.filoraShadow(level)` extension so features don't hand-roll shadow values.

### 5.2 Shape → `Shape.kt` (larger, more considered radii)

| Role                    | Old  | **New** |
|-------------------------|------|---------|
| `extraSmall`            | 4dp  | 8dp     |
| `small` (chips, tiles)  | 8dp  | 12dp    |
| `medium` (quick-tools)  | 12dp | 16dp    |
| `large` (cards)         | 16dp | 20dp    |
| `extraLarge` (hero)     | 28dp | 32dp    |

- FAB: 20dp rounded-square with brand gradient + `sh-brand` (not a plain circle) — a small signature.
- Leading file tile = 14dp rounded square in the category `container`.

### 5.3 Tactile states

- **Pressed:** scale to `0.97` + ripple in `primary @ 12%` (`Modifier.clickable` with a
  `FiloraRipple`). **Selected:** `primaryContainer` fill + `sh-brand` + Iris left-edge on the dual-pane
  detail. **Hover** (large screen / pointer): `primaryContainer @ 40%`.
- Keep `clickableTile` from the APP-82 a11y module (Role.Button + 48dp + onClickLabel) — wrap the new
  pressed/hover visuals *inside* it so accessibility is preserved.

---

## 6 · States with character

Never bare text. Each state = a **duotone "bloom"** (radial category-tinted disc + a 56dp duotone icon)
+ headline + one-line plain-language body + an accent action.

| State   | Illustration icon | Tint            | Primary action |
|---------|-------------------|-----------------|----------------|
| Empty   | `inbox` duotone   | category `container` | "Browse files" (Iris) |
| Loading | shaped skeletons  | `line` shimmer  | — (mirrors the real layout: hero block + tool row + tile grid) |
| Error   | `cloud-off` duotone | `error-container` | "Share instead" / "Retry" (error color) |

- Skeleton shimmer = `line → surf-tint → line` sweep, **1.3s**, and must stop under reduce-motion
  (static `line` fill). Reuse the existing `stateCrossfade` for skeleton→content.
- Build one `FiloraEmptyState(icon, tint, title, body, action)` composable and one `FiloraSkeleton`; use
  everywhere (browser empty, search no-results, category empty, error).

---

## 7 · Motion → extend `Motion.kt` (within reduce-motion rules)

Signature but restrained — A-blend principle #3 (no continuous/scroll-driven motion) still holds.

| Moment                 | Spec                                                        | Token |
|------------------------|------------------------------------------------------------|-------|
| Open file/folder       | shared-element container transform, tile → screen          | `containerTransform` 350 / `Emphasized` (exists) |
| List first paint       | staggered fade+rise, 24dp, 30ms/item, cap 8 items          | new `listStagger` 220 / `EmphasizedDecelerate` |
| Storage ring / story # | count-up + arc sweep, **one-shot** on first composition     | `storyReveal` 600 (exists), honor animation scale |
| Selection enter        | check scale-in + container tint                            | `selectionEnter` 150 (exists) |

All one-shot; gate every animation on `AccessibilityManager`/`Settings.Global.ANIMATOR_DURATION_SCALE`
so reduce-motion users get instant states.

---

## 8 · Accessibility & constraints — unchanged (must verify)

- **48dp** min touch target on every row/tile/chip/quick-tool/nav item (keep `clickableTile`).
- **AA contrast:** all body/metadata text ≥ 4.5:1; ink `#15142B` on `#F5F4FB` = ~13:1; muted `#75738F`
  on surface = ~4.6:1. Category **accents are decorative**; any category color used as *text* uses the
  darker `accent`, and the number/label is never the only signal (icon + label always paired).
- **TalkBack / RTL** preserved — no layout or semantics change; gradients/shadows are non-semantic.
- **Compose-only:** everything above is `Brush`, `Modifier.shadow`, `RoundedCornerShape`, `Canvas`
  `drawArc`, vector drawables, downloadable fonts — no non-Compose toolkit.
- A-blend structure kept: Calm Utility base; dual-pane ≥ 600dp; storage-story on Storage only.

---

## 9 · Build order for APP-117 / APP-118

1. **Tokens first** (unblocks everything): `Color.kt` Iris scheme + `FiloraCategoryColors` +
   `FiloraGradients` + `Shape.kt` radii + `filoraShadow` + Manrope in `Type.kt`.
2. **`FiloraIcons`** set (option 1 or 2 in §2).
3. **Home** (§3) — highest visible payoff: storage ring, quick-tools, category tiles, strips.
4. Roll the leading-tile + shadow + shape changes across **Browser / Media / Search / Storage /
   Settings** (mechanical once tokens land).
5. **States** (`FiloraEmptyState` + `FiloraSkeleton`) and **motion** (§6–7).

Nothing here changes data, navigation, or feature behavior — it is a theme + component-styling pass on
top of the shipped A-blend structure.

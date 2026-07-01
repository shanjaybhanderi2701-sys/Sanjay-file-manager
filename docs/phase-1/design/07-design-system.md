# Filora Design System — the token-based source of truth

**Owner:** UI/UX Designer · **Issue:** APP-137 (applies the [APP-136 Taste Guide](../../..)) · **Status:** canonical, v1

This is the single design system for the appblish File Manager (Filora). It is deliberately **token-first**: define spacing, radius, type, elevation, motion, and one accent *once*, then apply them everywhere. This is the **Simple Design Ltd. lesson** from the Taste Guide — a portfolio of ~2B installs built on one system applied ruthlessly. Feature screens reference tokens, never hard-coded `.dp`/hex.

Every token group below names the **taste principle it serves** (Taste Guide §2) so we never lose the *why*.

---

## 0. How the taste maps to the system (the spine)

| Taste principle (Guide §2) | Where it lives in this system |
|---|---|
| §2.1 Content is the hero; chrome recedes | Type scale weights names heavily; app bar tonal, recedes on scroll; §7 Browse |
| §2.2 One design system applied ruthlessly | This whole doc — tokens in `core-ui/theme`, referenced not re-derived |
| §2.3 Act directly, preview instantly | §8 Multi-select action bar (InShot); direct row long-press → selection |
| §2.4 Native & trustworthy | Material 3 components, dynamic color, honest tonal elevation (§4), true dark (§6) |
| §2.5 Warmth is a feature | Empty/first-run states (§7.4), friendly tone, optional accent (§6) |
| §2.6 Thumb-first ergonomics | Primary actions in bottom third; ≥48dp targets (§3, §8) |
| §2.7 Restraint | One accent + a fixed category wheel; tight type scale; generous whitespace |
| §2.8 Fast & honest | Skeletons not spinners; honest confirms + undo; no dark patterns |

---

## 1. Spacing — 4dp base scale · serves §2.7 Restraint

Implemented: `core-ui/theme/Spacing.kt` (`FiloraSpacing`). A single 4dp rhythm keeps every screen scannable.

| Token | Value | Use |
|---|---|---|
| `xxs` | 2dp | Icon-to-label hairline nudge |
| `xs` | 4dp | Chip internal gap |
| `sm` | 8dp | Grid gutter (media); between-element inside a chip |
| `md` | 12dp | List-row vertical padding → row height ≥56dp |
| `lg` | 16dp | Screen edge padding (phone); card padding; section gap |
| `xl` | 24dp | Screen edge padding ≥600dp; between-card gap |
| `xxl` | 32dp | Empty-state block spacing |
| `xxxl` | 48dp | Hero/first-run vertical rhythm |

**Contract:** screen edge = `lg` (phone) / `xl` (≥600dp). List-row vertical = `md`. Grid gutter = `sm`. Never introduce an ad-hoc gap — ad-hoc `.dp` is the #1 source of visual drift.

## 2. Shape / corner radius — rounded & friendly · serves §2.5 Warmth + §2.4 Native

Implemented: `core-ui/theme/Shape.kt` (`FiloraShapes`, mapped onto M3 `Shapes`).

| M3 role | Radius | Applied to |
|---|---|---|
| extraSmall | 4dp | Inline chips, badge |
| small | 8dp | Category leading icon squares |
| medium | 12dp | List-row press surface, small cards |
| large | 16dp | Cards, search field, bottom-sheet top |
| extraLarge | 28dp | FAB, dialogs, action-bar container |

Rounded corners are the cheapest, most consistent way to read "friendly utility" without decoration.

## 3. Type scale — Material 3, native family · serves §2.1 Content-hero + §2.7 Restraint

Implemented: `core-ui/theme/Type.kt` (`FiloraTypography`). Platform default family (Roboto/device sans) — **ships no bundled font in v1**, stays native and light; a brand font can be swapped centrally later.

| Role | Size/Line/Weight | Use |
|---|---|---|
| displaySmall | 36/44 Regular | Storage hero number |
| headlineSmall | 24/32 Regular | Empty-state & large dialog titles |
| titleLarge | 22/28 Regular | App-bar title, section header |
| titleMedium | 16/24 **Medium** | Grid-tile label, card title |
| **bodyLarge** | 16/24 Regular | **File / folder name (the hero text)** |
| bodyMedium | 14/20 Regular | Descriptions |
| bodySmall | 12/16 Regular | File metadata, captions |
| labelLarge | 14/20 Medium | Buttons, chips |
| labelMedium/Small | 12–11 Medium | Crumbs, badge counts |

**Restraint rule:** the file name uses `bodyLarge`; metadata drops to `bodySmall`/`muted`. That single contrast step is what makes a list scannable. Do not add a 5th body size "because it looks nice."

## 4. Elevation — tonal, not shadow · serves §2.4 Native (M3 Expressive)

Implemented: `core-ui/theme/Elevation.kt` (`FiloraElevation`). Pass to `Surface(tonalElevation = …)` — **never as a drop shadow** — so Material You tints elevation with dynamic color.

| Token | dp | Use |
|---|---|---|
| level0 | 0 | Screen bg, scrolled-under list |
| level1 | 1 | Resting cards, list |
| level2 | 3 | App bar on scroll, search bar, **batch-action bar** |
| level3 | 6 | FAB, menus, **selected** row/pane |
| level4 | 8 | Dialogs, bottom sheets |
| level5 | 12 | Conflict sheet |

Active selection = `level3` tonal + `secondaryContainer` tint.

## 5. Motion — functional, never decorative · serves §2.3 Instant feedback + §2.8 Honest

Implemented: `core-ui/theme/Motion.kt` (`FiloraMotion`).

| Pattern | Duration | Easing |
|---|---|---|
| Open file/folder (container transform) | 350ms | Emphasized |
| Screen change | 300ms | Emphasized |
| Dialog / sheet in | 200ms | EmphasizedDecelerate |
| Selection enter | 150ms | Standard |
| Skeleton → content crossfade | 150ms | Standard |
| Storage-story reveal (one-shot) | 600ms (cap) | EmphasizedDecelerate |

**Hard rule:** no continuous/parallax motion, nothing animating while scrolling, thumbnails never animate in on scroll. Honor system animation scale / reduce-motion. Motion exists only to *confirm an action* (copy/move/delete) — that is the InShot "instant preview" instinct kept honest.

## 6. Color & accent — one accent + a category wheel · serves §2.7 Restraint + §2.4 Native + §2.5 Warmth

**Two layers, and they currently disagree — this doc resolves it.**

- **Layer 1 — dynamic color (default, preferred).** On Android 12+ we use Material You dynamic color from the wallpaper. This is the Mobile_V5 "native & trustworthy" instinct: a file tool touching the user's real files should feel like it belongs on *their* phone. True dark mode is first-class.
- **Layer 2 — the brand fallback / accent set** for pre-12 devices, dynamic-color-off, and brand surfaces (splash, store, first-run glyph).

### ✅ Ratified brand color — BLUE (board sign-off 2026-07-01)
The shipped Kotlin fallback (`core-ui/theme/Color.kt`) still seeds the original teal `#00696B`; an interim **Iris/purple** `#5B5BD6` direction was explored (APP-134). **The board ratified a BLUE primary** (confirmation `blue-home-recent-cleaner-v2`, accepted by local-board). Regenerate the Kotlin fallback scheme from seed **`#2563EB`**. One accent only — never a second brand color. Category colors below are an *encoding* (like syntax highlighting), not additional brand colors.

| Token | Blue (ratified) | Note |
|---|---|---|
| Primary | `#2563EB` | brand accent; pressed `#1D4FD7` |
| Brand gradient | `#2563EB → #4C7DF5 → #38BDF8` | splash, FAB, hero ring/glyph only |
| Primary container | `#DCE7FF` / on `#0A245F` | selection tint, chips |
| Background | `#F5F7FC` (light) / `#0E1424`-family (dark) | soft cool tint, not flat white |
| Surface | `#FFFFFF` / raised `#FBFCFF` | cards |
| Ink / secondary / muted | `#15142B` / `#403E5C` / `#75738F` | text hierarchy |
| Error | `#E5484D` / container `#FFE3E1` | destructive only, used sparingly |
| Success | `#12A46B` | operation complete |

**Category accent wheel** (leading icons, category cards, thumbnails placeholders — fixed, not themeable):

| Category | Accent / Container |
|---|---|
| Images | `#F43F6E` / `#FFE1E9` |
| Video | `#7C5CF7` / `#EAE2FF` |
| Audio | `#F5921B` / `#FFEBCF` |
| Documents | `#2E7CF6` / `#DBE9FF` |
| Downloads | `#12A46B` / `#CFF3E1` |
| Apps (APK) | `#5B5BD6` / `#E4E4FB` |
| Archives | `#B44BC9` / `#F6DEFB` |
| Folders | `#6E6C8A` / `#ECEBF3` |
| Favorite | `#F5A524` / `#FFF0D2` |

**Contrast:** ink-on-surface, ink-2, and on-container pairs all meet WCAG AA (≥4.5:1 body / ≥3:1 large). Category accents are used as *fills behind icons*, not as text color, so they never carry a contrast burden. *Note:* the Documents category (`#2E7CF6`) sits near the primary blue, but it only ever appears as a category fill behind an icon — never as an interactive accent — so the two never collide semantically.

---

## 6.5 Home screen (landing) — carries browsing · serves §2.1, §2.5, §2.6

Because **Browse is not a tab** (see §7.1), **Home is the entry into the file system** *and* the storage story. Composition (top→bottom), all on the token system — see the live blue mockup:

1. **Greeting app bar** — "Good evening" overline + "Filora" (`titleLarge` 800) + search + gradient avatar.
2. **Storage ring hero** — brand-gradient card, arc ring (used %), GB summary, category legend. The premium first-impression cue; count-up once on first paint (§5), static after.
3. **Quick actions** — 4 tinted tiles: Clean up (→Cleaner) · Large files · Favourites · Trash. Each ≥48dp, thumb-reachable.
4. **Storage locations (the Browse entry)** — Internal storage + SD card rows with used/free bars → tap opens the file browser (Hero Flow A), plus a "Browse all ›" affordance. This is what keeps browsing one tap deep despite Browse not being a tab.
5. **Categories** — tinted tiles (Images/Videos/Audio/Docs/Downloads/Apps/Archives/More) with counts, using the category-wheel encoding.
6. **Recent** — recently opened files (mirrors the Recent tab's head).

States: loading = skeleton hero + rows; permission = trust-first card *before* the system prompt (§2.4); empty (new device) = neutral hero, no fabricated stat.

---

## 7. HERO FLOW A — Browse (content-first) · serves §2.1, §2.5, §2.8

The screen a file manager lives or dies on. Content is the hero; chrome is quiet until needed.

**Anatomy (top→bottom):** status strip → app bar (back · breadcrumb path · list/grid toggle · overflow) → prominent rounded **search** field → the content list → brand-gradient **FAB** (New folder) → bottom nav.

**List row (default density):** leading category icon square (8dp radius, category-tinted) · file/folder **name** (`bodyLarge`, ink) · metadata (`bodySmall`, muted — folder item count, or `size · modified`) · trailing chevron (folder) / overflow dot (file). Row ≥56dp (`md` vertical). App bar and search recede (tonal `level0→level2`) on scroll so files own the viewport.

**Grid view:** same tokens, 2-col tiles with tinted thumbnail placeholder + name/meta — media-forward density; toggle is one tap, state persists (Settings).

### 7.1 Bottom navigation (tab bar) · serves §2.6 thumb-first + §2.7 restraint

**Ratified: three tabs — Home · Recent · Cleaner** (board sign-off 2026-07-01). A lean, InShot-style bar — fewer, bigger, thumb-first targets beat a crowded bar (§2.6/§2.7). Filled glyphs.

| # | Tab | Icon | Role (maps to shipped features) |
|---|---|---|---|
| 1 | **Home** | house (filled) | Landing + **the entry into browsing** (§6.5): storage ring, quick actions, Storage locations, categories, recents. |
| 2 | **Recent** | clock (filled) | Recently opened/modified (M9) — fast re-access, the #1 repeat task. |
| 3 | **Cleaner** | broom + sparkle | Free-up-space / Storage Analyzer (M11): junk, large files, duplicates — utility framed as value (§2.6). |

- **Browse is not a tab.** The file tree opens from Home's **Storage locations** rows (Internal storage / SD) and the **Browse FAB**, so browsing stays one tap from Home. This is the deliberate, home-centric IA the board chose (InShot-like).
- **Media / Favorites / Settings** are reachable from Home (category tiles; Favourites quick action; app-bar gear) — kept off the bar to hold the thumb-first three.
- **≥600dp:** the same three swap to a **navigation rail** (never both).
- Each item ≥48dp; active = `primaryContainer` pill behind a **filled** glyph + primary label.

> **Journey note:** an earlier revision of this doc reflected a 5-tab bar (Home·Browse·Media·Storage·Recent). The board iterated to the 3-tab Home·Recent·Cleaner set above; this section is the current source of truth.

### Required states (never ship only the happy path)
| State | Design |
|---|---|
| **Populated** | List/grid as above. |
| **Loading** | 6 shimmer **skeleton** rows (icon + 2 text bars) — layout stable, *not* a centered spinner (§2.8). Crossfade 150ms → content. |
| **Empty** | Warm illustration + "This folder is empty" + friendly line + **one** clear primary action ("Add files"). Never a blank screen (§2.5, BetterApp). |
| **Error** | Calm illustration (not alarming), "Couldn't open this folder", cause line, **Try again** (outlined) + **Go back** (text). Error color used sparingly. |
| **Permission** | Brand glyph + "Filora needs access to your files" + 2–3 trust bullets ("We never upload your files", "Revoke anytime") + **Allow access** + "Why is this needed?" link. Trust first (§2.4). |

## 8. HERO FLOW B — Multi-select + bottom action bar · serves §2.3, §2.6 (InShot)

*The object is the interface.* Long-press any row → selection mode. Users act on the files themselves; batch tools appear contextually in the thumb zone.

**Selection app bar (replaces the normal bar):** close (X) · "*N* selected" title · select-all · overflow. Tinted `iris-container` / `secondaryContainer`. Selected rows show a filled iris check on the leading edge + `iris-container` row tint (`level3` tonal).

**Bottom action bar (the hero piece — InShot instinct):** docked above the nav, `level2`. A row of **large single-purpose tiles**, each = a circular iris-tinted icon + short label: **Move · Copy · Share · Delete · Rename · Compress**. Rules:
- One purpose per tile, ≥64px hit area (well over 48dp), **thumb-first** in the bottom third.
- **Delete** carries the error tint — the only place error color appears here.
- If 6 tiles crowd on small widths, show 5 + a **More** tile (never truncate silently).
- Instant feedback on tap (150ms selection/confirm motion). Rename/single-target actions disable when *N*>1 as appropriate.

**Destructive confirm (bottom sheet, §2.6 Honest + undo):** handle · "Delete *N* items?" · "Items move to Trash — restore for 30 days." · full-width tonal **Move to Trash** + text **Cancel**. Reversibility is surfaced *before* the action; a Snackbar **Undo** follows it. No destructive action is ever hidden behind an ambiguous icon.

---

## 9. Do / Don't (enforced in design review)

**Do:** reference tokens (never hard-code); design all five browse states; keep primary actions in the thumb zone ≥48dp; respect dynamic color + true dark; use one accent + the fixed category wheel.

**Don't:** copy any competitor layout/icon/flow (taste, not theft); add decoration with no function; introduce a 2nd brand accent or a 5th body size; hide destructive actions behind ambiguous icons; fight the platform with web-like chrome.

---

## 10. Reference artifacts & implementation

- **Canonical mockup (ratified):** `design/filora-screens/hero-flows-blue/index.html` — **blue** primary, **Home · Recent · Cleaner** bar, the **complete Home screen** (§6.5), Browse (5 states) + Multi-select action bar + delete confirm. Live: https://stormy-prism-ykpd.here.now/
- **Superseded reference:** `design/filora-screens/hero-flows/index.html` (Iris/purple, 5-tab; kept for history — https://lunar-fennel-rb87.here.now/).
- **Token source of truth (code):** `core/core-ui/src/main/kotlin/com/appblish/filora/core/ui/theme/` — `Spacing.kt`, `Shape.kt`, `Type.kt`, `Elevation.kt`, `Motion.kt`, `Color.kt`.
- **Premium-craft direction:** `docs/phase-1/design/06-premium-craft.md`. **Prior hi-fi spec:** `docs/phase-1/design/05-hifi-spec.md`.

**Terminology note:** earlier revisions (and some `iris-*` token names in §8) describe the accent as "Iris"/purple. The **ratified accent is blue `#2563EB`** — read any `iris-*` name as the primary / primary-container role.

**Open engineering follow-up (delegated):** (1) regenerate `Color.kt` from the **blue seed `#2563EB`** (not teal, not Iris); (2) nav = **Home · Recent · Cleaner** with Browse opening from Home; (3) build the Home screen per §6.5. Owner: Founding Android / Senior Kotlin Engineer, via the A-blend alignment issues (APP-112 / APP-117 / APP-118) + the child issue linked on APP-137.

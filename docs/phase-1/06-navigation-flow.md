# Navigation Flow — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

Single-activity architecture. `MainActivity` hosts one **Navigation Compose**
`NavHost`. Routes are a typed sealed hierarchy.

---

## 1. Route graph

```
Onboarding (first run only)
   └─► PermissionRationale ──grant/skip──► Home (start destination)

Home  ─────────────┬─► Browser(path|treeUri)
                   │        └─► Browser(child)            (push per level)
                   │        └─► (bottom sheet) FileActions
                   │        └─► (dialog) Rename / NewFolder / ConflictResolver
                   │        └─► OperationProgress (overlay/notification)
                   ├─► Search ──► Browser(result location)
                   ├─► MediaCategory(type)  ──► system Open/Play intent
                   ├─► Storage ──► LargestFiles ──► (item) FileActions
                   ├─► Favorites/Recents (sections on Home) ──► Browser/Open
                   └─► Settings ──► About
```

## 2. Destinations

| Route                       | Args                          | Purpose |
|-----------------------------|-------------------------------|---------|
| `onboarding`                | —                             | First-run intro (shown once) |
| `permission`                | —                             | Rationale + request flow |
| `home`                      | —                             | Dashboard: volumes, categories, favorites, recents |
| `browser`                   | `location` (path or tree URI) | Directory listing + operations |
| `search`                    | `scope` (optional)            | Query + filters, streamed results |
| `media`                     | `category` enum               | MediaStore-backed category hub |
| `storage`                   | —                             | Per-volume breakdown |
| `largestFiles`              | `volumeId?`                   | Top-N by size |
| `settings`                  | —                             | Theme, defaults, toggles |
| `about`                     | —                             | Version, licenses, privacy |

## 3. Navigation rules

- **Start destination:** `home`. `onboarding`/`permission` are shown only when not
  yet completed/granted, then `popUpTo(home){inclusive=false}`.
- **Browser nesting:** each folder level is a new `browser` entry on the back stack
  so system Back walks up the directory tree naturally; breadcrumb taps use
  `popUpTo` the target ancestor.
- **Dialogs & sheets** (rename, new folder, conflict, file actions) are Compose
  dialogs/bottom sheets, **not** nav destinations — they don't alter the back stack.
- **Argument passing:** small args via typed route params; complex selections held
  in the owning ViewModel / `SavedStateHandle`, not serialized through routes.
- **Top-level navigation (T050):** a bottom `NavigationBar` exposes the five top-level
  destinations — Home / Browser / Search / Storage / Settings. Tab switches use the
  multi-back-stack pattern (`popUpTo(start){saveState=true}` + `restoreState=true` +
  `launchSingleTop`) so each tab keeps its own back stack and scroll. The bar is hidden
  on the permission gate. (`FiloraBottomBar` / `navigateToTopLevel`.)
- **Deep links (T053):** the single `filora://` scheme is registered on `MainActivity`;
  typed `navDeepLink` builders expand each base into the route template —
  `filora://browser?location={location}` (a folder path / tree URI),
  `filora://category?category={category}` (a category hub), and `filora://categories`
  (the hub grid). Constants live in `FiloraDeepLinks`.
- **Predictive back (T051):** `android:enableOnBackInvokedCallback="true"` opts the app
  into the API-34+ predictive-back gesture; Navigation Compose renders the cross-screen
  predictive animation automatically. Older APIs fall back to a standard pop.

## 4. State preservation

- Each screen's ViewModel is scoped to its nav back-stack entry; scroll position and
  selection survive config changes via `SavedStateHandle`/`rememberSaveable`.
- **Process death (T052):** typed `@Serializable` routes are saved to the bundle by
  Navigation Compose, so the back stack (e.g. nested `browser` locations) is recreated
  after process death; `MainActivity` re-evaluates the permission gate on each cold start.
  The bottom-bar `saveState`/`restoreState` keeps per-tab back stacks across tab switches.
- Returning from a background long-operation re-syncs the affected directory.

## 5. Back behavior

- In a multi-selection: Back clears selection before popping the screen.
- At `home`: Back exits the app (standard).
- During an in-progress foreground operation: Back leaves the screen but the
  operation continues via WorkManager with its notification.

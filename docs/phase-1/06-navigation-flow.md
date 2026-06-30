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
- **Deep links:** none in v1 (reserved). External "open with Filora" intents resolve
  to `browser` at the requested location.

## 4. State preservation

- Each screen's ViewModel is scoped to its nav back-stack entry; scroll position and
  selection survive config changes via `SavedStateHandle`/`rememberSaveable`.
- Returning from a background long-operation re-syncs the affected directory.

## 5. Back behavior

- In a multi-selection: Back clears selection before popping the screen.
- At `home`: Back exits the app (standard).
- During an in-progress foreground operation: Back leaves the screen but the
  operation continues via WorkManager with its notification.

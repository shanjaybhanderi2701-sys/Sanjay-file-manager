# Recycle Bin — Trash Design (M12, T122)

**Requirement:** FR-3.4 — safe delete with restore.
**Milestone:** APP-32 (M12). Depends on delete (T068) and the Room schema (T013).

## Goal

Give the user an "undo" for deletes: a deleted file goes to a recoverable bin instead
of vanishing, can be restored to where it came from, is permanently removable, and is
auto-purged after a retention window so it can't grow without bound.

## Strategy: app-managed trash directory + Room metadata

Android scoped storage does not give a third-party app a reliable, general "move any
file to a system trash" primitive across all storage locations. `MediaStore`'s
`TRASH` state only covers media the app can address via `MediaStore`, not arbitrary
files/folders on shared storage or SAF trees. So Filora manages its **own** trash.

### Where the bytes live

- **Trash directory:** `Android/data/<pkg>/files/trash` (the app's *external*-files dir),
  with an internal-storage fallback (`filesDir/trash`) when external storage is
  unavailable. Chosen because:
  - It is **app-private** — no runtime storage permission is needed to write it, and it
    is excluded from MediaStore so trashed media don't reappear in galleries.
  - It sits on the **same volume** as most user files, so moving an item into or out of
    the bin is a fast `File.renameTo` (an inode move, no copy). A cross-volume delete
    degrades gracefully to copy-then-delete.
  - The OS reclaims it on uninstall — an acceptable trade for a recycle bin (the user
    uninstalling the app is not expecting the bin to outlive it).
- **Payload naming:** each trashed item is stored as a single node `trash/<uuid>` (a
  file, or a directory tree rooted at that node). The UUID avoids collisions between two
  files deleted from different folders that share a name, and is the stable handle used
  by restore / permanent-delete.

### Metadata (Room)

A `trash` table (`TrashEntity` + `TrashDao`, T123) records, per trashed item:

| column                 | purpose                                                    |
|------------------------|------------------------------------------------------------|
| `id` (PK)              | the UUID; doubles as the on-disk payload name              |
| `originalPath`         | where **restore** puts it back                             |
| `name`                 | display name in the bin list                               |
| `isDirectory`          | icon / semantics                                           |
| `sizeBytes`            | captured at delete-time → bin footprint without re-walking |
| `deletedAtEpochMillis` | drives the retention countdown + auto-purge                |

The DB is bumped **v1 → v2** with an additive migration that creates the `trash` table
(existing favorites/recents rows are untouched); a migration test guards it.

## Operations (domain seam)

`TrashRepository` (core-domain) is the single seam; `AppTrashRepository` (core-data)
implements it. Use cases wrap each verb so ViewModels stay thin:

- **Delete → trash (T124):** `DeleteUseCase` routes to `TrashRepository.moveToTrash`
  when `toTrash` is set **and every** target is trashable (`canTrash`). The batch is
  kept atomic — all-to-trash or all-permanent — to avoid a confusing
  half-recoverable delete. Non-trashable targets (`content://` SAF/MediaStore) force
  the whole batch to a permanent `FileRepository.delete`.
- **Restore (T126):** `RestoreFromTrashUseCase` → moves `trash/<id>` back to
  `originalPath`, recreating missing parent dirs. It **refuses to clobber** an existing
  file at the original path — that item stays in the bin (a `Conflict`).
- **Permanent delete (T127):** `DeleteForeverUseCase` → deletes the payload + row.
- **Empty bin (T129):** `EmptyTrashUseCase` → permanent-delete everything.
- **Size accounting (T129):** `ObserveTrashSizeUseCase` streams `SUM(sizeBytes)`.

All mutating calls are per-item best-effort (one failing item is skipped, the rest
proceed) and return the count actually affected, so a partial failure still frees
storage and the UI can report honestly.

## Auto-purge / retention (T128)

`TrashRetention(maxAge = 30 days)` is the single, configurable knob (a future settings
toggle can change it without touching the purge logic). `PurgeExpiredTrashUseCase`
permanently deletes items whose `deletedAt` is older than the window.

**Trigger (v1):** purge runs opportunistically when the Recycle Bin screen opens (and
is safe to also call on app start). It is idempotent and cheap when nothing is expired.

**v1.1 enhancement (noted, not built):** a periodic `WorkManager` job for
background purge independent of the user opening the bin.

## Non-goals for v1

- No SAF/`content://` trash (deleted permanently, as today).
- No cross-device / cloud trash.
- No per-item retention override (single global retention).

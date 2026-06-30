# Product Requirements Document — Filora File Manager

**Product:** Filora (working brand) — a native Android file manager
**Owner:** appblish · CTO
**Status:** Phase 1 — Draft for approval
**Last updated:** 2026-06-30

---

## 1. Summary

Filora is a fast, private, fully native Android file manager. It lets users
browse, search, organize, and act on the files and media on their device and
external/USB/cloud-document storage — without ads, without account sign-up, and
without sending file contents off-device. All code, UI, branding, and
architecture are original to appblish.

## 2. Problem & Opportunity

Stock Android file access is fragmented across OEM apps of inconsistent quality.
Popular third-party managers are often ad-heavy, over-permissioned, or slow on
large directories. There is room for a clean, modern, privacy-first manager built
on current Android storage APIs (MediaStore, Storage Access Framework, DocumentFile)
and modern UI (Jetpack Compose, Material 3).

## 3. Goals

- Deliver a production-quality file manager covering browse, search, file
  operations, media views, and storage insight.
- Be measurably fast on large directories (10k+ entries) and large trees.
- Be privacy-first: no file content leaves the device; minimal, scoped permissions.
- Ship on modern Android (target latest stable API, min API 26) with full
  scoped-storage compliance.

### Non-Goals (v1)

- Cloud sync / accounts / backup-to-cloud.
- Root/system-partition file access.
- Built-in document/media editing (we open via system intents instead).
- Cross-device transfer / LAN sharing (candidate for a later phase).

## 4. Target Users & Personas

- **Everyday organizer** — moves photos, clears downloads, frees space.
- **Power user** — multi-select, bulk move/rename, archive, hidden files.
- **Privacy-conscious user** — wants an offline, ad-free, low-permission tool.

## 5. Key Use Cases

1. Browse internal and SD/USB storage by folder hierarchy.
2. Search by name, type, size, and date across a tree.
3. Copy / move / rename / delete / create folders, with conflict handling.
4. Multi-select and run batch operations with progress and undo where feasible.
5. View categorized collections: Images, Video, Audio, Documents, Downloads, APKs, Archives.
6. Compress to / extract from ZIP archives.
7. See storage usage and large/duplicate-candidate files to reclaim space.
8. Open any file with the correct app via system intent; share via system sheet.
9. Mark favorites / recents for quick return.

## 6. Functional Scope (high level)

See **Functional Requirements** for the itemized, testable list. Pillars:
Browser, Search, File Operations, Media Categories, Archives, Storage Insights,
Favorites/Recents, Settings/Theming.

## 7. Success Metrics

- **Performance:** directory of 10k entries renders first frame < 300 ms (warm),
  scroll stays at 60 fps on mid-tier hardware.
- **Stability:** crash-free sessions ≥ 99.5% in beta.
- **Adoption proxy:** ≥ 60% of beta testers run a batch operation in week 1.
- **Quality:** 0 P0/P1 known defects at GA; ≥ 80% unit coverage on domain layer.

## 8. Constraints & Assumptions

- Android only, native Kotlin + Compose; no cross-platform framework.
- Must comply with scoped storage and Play policy on broad-storage access.
- Offline-first; the only network use in v1 is optional crash/anonymized
  diagnostics (opt-in), not file content.

## 9. Release Phasing

- **Phase 1 (this doc set):** plan, architecture, task breakdown — no code.
- **Phase 2:** foundation + browser (internal alpha).
- **Phase 3:** operations, media, search, archives (closed beta).
- **Phase 4:** storage insights, polish, hardening (open beta → GA).

See **Development Roadmap** and **Release Plan** for detail.

## 10. Open Questions

- Final brand name and icon (Filora is a working placeholder).
- Whether opt-in anonymized diagnostics ship in v1 or are deferred.
- Minimum supported API: proposed 26; to confirm against device analytics.

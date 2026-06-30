# Risk Assessment — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

Scored as Likelihood (L) × Impact (I), each 1–5; Severity = L×I.

---

| ID | Risk | L | I | Sev | Mitigation | Owner |
|----|------|---|---|-----|-----------|-------|
| R1 | **Scoped-storage / Play policy** rejects broad-storage access (`MANAGE_EXTERNAL_STORAGE`) | 4 | 5 | 20 | Default to SAF + MediaStore; gate broad access behind rationale; have a no-broad-access fallback that still ships | CTO/Eng |
| R2 | **Data loss** during move/delete (interrupted op, bad conflict handling) | 3 | 5 | 15 | Copy-verify-then-delete; transactional workers; confirmations; trash where supported; extensive tests | Eng |
| R3 | **Performance** degrades on huge directories (10k+), jank | 3 | 4 | 12 | Paging, lazy lists, off-main reads, thumbnail cache, perf budget in NFRs + CI perf check | Eng |
| R4 | **API fragmentation** (26 → latest) causes behavior gaps (SAF/MediaStore quirks) | 4 | 3 | 12 | Device/emulator matrix tests; abstraction in StorageRepository; per-API smoke tests | Eng |
| R5 | **Permission UX friction** lowers grant rate | 3 | 3 | 9 | Clear rationale; progressive permission; SAF fallback so app is useful even if denied | Eng/Design |
| R6 | **Thumbnail/memory pressure** → OOM on low-RAM devices | 2 | 4 | 8 | Bounded cache, downsampled decode, LeakCanary in debug, memory tests | Eng |
| R7 | **Scope creep** (cloud, root, transfer) delays GA | 3 | 3 | 9 | Non-goals fixed in PRD; changes require CTO sign-off and re-plan | CTO |
| R8 | **WorkManager constraints / OEM battery killers** interrupt long ops | 3 | 3 | 9 | Foreground service for active ops; resumable workers; user-visible progress | Eng |
| R9 | **Single-engineer bus factor** | 3 | 4 | 12 | Documented architecture (this doc set), small reviewed PRs, CI gates, knowledge in repo | CTO |
| R10| **Originality/IP** — must not copy existing apps' code/UI/branding | 2 | 5 | 10 | Original brand (Filora), original UI, clean-room implementation, no decompiled refs | CTO |
| R11| **Third-party library risk** (abandoned/licensing) | 2 | 3 | 6 | Prefer Jetpack/official libs; vet licenses; pin versions via catalog | Eng |
| R12| **Accessibility/localization** deferred and hard to retrofit | 3 | 3 | 9 | Build a11y + string externalization in from M0, not at the end | Eng |

## Top risks to watch
1. **R1 (Play policy / storage)** — highest severity; resolved by SAF-first design.
2. **R2 (Data loss)** — never acceptable; guarded by copy-verify-delete + tests.
3. **R3/R4/R9** — performance, fragmentation, bus factor — mitigated by NFR budgets,
   test matrix, and this documentation.

## Review cadence
Risks re-reviewed at each milestone exit; new risks logged here with score + mitigation.

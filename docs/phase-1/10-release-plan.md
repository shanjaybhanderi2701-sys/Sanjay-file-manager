# Release Plan — Filora

**Status:** Phase 1 — Draft for approval · **Last updated:** 2026-06-30

---

## 1. Release tracks (Google Play)

| Track            | Audience            | Gate to enter |
|------------------|---------------------|---------------|
| Internal alpha   | team only           | M2 complete (browser works) |
| Closed beta      | invited testers     | M5 complete (ops/media/search/archives) |
| Open beta        | public opt-in       | M6 complete + NFR pass |
| Production (GA)  | everyone            | M7 complete + GA checklist |

## 2. Versioning

- **SemVer** `MAJOR.MINOR.PATCH`; `versionCode` monotonically increasing.
- v0.x during alpha/beta; **1.0.0** at GA.
- Each release tagged in git (`vX.Y.Z`) with generated changelog.

## 3. Build & signing

- Release **AAB** with R8 minification + resource shrinking + baseline profiles.
- Play App Signing; upload key stored securely (never in repo).
- Separate `debug`/`release` configs; release is non-debuggable.

## 4. CI/CD pipeline

1. PR: build → ktlint/detekt → unit tests → assemble debug.
2. Merge to main: full test suite + instrumented smoke on emulator matrix.
3. Tagged release: assemble signed AAB → upload to chosen track (manual promote).

## 5. Pre-release quality gates (every track promotion)

- All NFR budgets met (startup, scroll fps, 10k-dir render).
- 0 open P0/P1 defects; crash-free ≥ 99.5% on prior track.
- Accessibility pass (TalkBack on core flows); strings externalized.
- Manual regression on the device matrix (API 26, 30, 33, latest; phone + tablet).

## 6. Store listing assets (prepared during M7)

- Original app icon + feature graphic (Filora branding).
- Screenshots (phone + tablet, light/dark).
- Short & full description; privacy policy URL; data-safety form
  (declares: no data collected / no data shared in v1).
- Content rating questionnaire.

## 7. Rollout strategy

- **Staged rollout** at GA: 10% → 25% → 50% → 100%, halting on crash-rate regression.
- Monitor Play vitals (ANRs, crashes) at each stage for 24–48 h before advancing.

## 8. Rollback / hotfix

- If a stage shows a crash-rate or ANR regression, halt rollout and ship a patch
  release; keep prior AAB available for emergency rollback.
- Hotfix branch off the release tag; fast-tracked through CI with a focused test.

## 9. Post-release

- Triage Play reviews & vitals weekly.
- Maintain a backlog for Phase 5+ candidates (cloud, LAN transfer, dedupe, etc.).
- Each release ships a changelog entry and updated docs.

## 10. GA exit checklist

- [ ] All milestones M0–M7 complete and verified.
- [ ] NFRs measured and passing.
- [ ] Store listing, privacy policy, data-safety form complete.
- [ ] Signed AAB uploaded; staged rollout configured.
- [ ] Monitoring/alerting on Play vitals in place.
- [ ] Original-IP review signed off (R10).

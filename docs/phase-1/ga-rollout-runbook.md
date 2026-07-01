# GA Staged Rollout Runbook — Filora

**Task:** T7.8 (APP-88) · **Status:** Setup complete; execution gated on QA sign-off (T7.6) + Play credentials
**Package:** `com.appblish.filora` · **Last updated:** 2026-07-01

Operationalizes the strategy in [`10-release-plan.md`](10-release-plan.md) §7–§10 into an executable procedure. The mechanism is wired in [`.github/workflows/release.yml`](../../.github/workflows/release.yml); this doc is the human-facing checklist for each promotion.

---

## 0. Prerequisites (one-time, ops/owner)

The pipeline builds a signed AAB on every `v*` tag today. To actually publish to Google Play, these repo secrets must be provisioned (owner/ops action — they hold real credentials, not something an agent can create):

| Secret | Purpose |
|--------|---------|
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Developer API service account (JSON), granted **Release manager** on the app. Enables the promote step. |
| `FILORA_RELEASE_STORE_BASE64` | base64 of the upload keystore (`base64 -i upload.jks`). |
| `FILORA_RELEASE_STORE_PASSWORD` / `FILORA_RELEASE_KEY_ALIAS` / `FILORA_RELEASE_KEY_PASSWORD` | Keystore credentials. |

Until these exist, `release.yml` still builds the signed AAB (debug-keystore fallback) and uploads it as a CI artifact, emitting a `::warning` that the Play promote was skipped. No shippable release is produced without the real upload key.

Play Console must also have App Signing enrolled and the first AAB uploaded manually once (to establish the app), after which the API can take over.

## 1. Release gate — do not promote until ALL are true

- [ ] All milestones M0–M7 `done` and verified.
- [ ] **T7.6 QA regression ([APP-86](/APP/issues/APP-86)) passed** on the NFR-4 device matrix — 0 open P0/P1, crash-free ≥ 99.5% on the prior track.
- [ ] NFRs measured and passing (startup, scroll fps, 10k-dir render, memory) — see `baseline-profile` CI job.
- [ ] Accessibility pass (TalkBack core flows); all strings externalized.
- [ ] Store listing, privacy policy, data-safety form complete ([APP-87](/APP/issues/APP-87)).
- [ ] Original-IP review (R10) signed off.

> The staged rollout below MUST NOT start until the QA gate (T7.6) is green. That gate is the single remaining GA dependency for this milestone.

## 2. Cut the release

1. Bump `versionCode` (monotonic) and `versionName` in `app/build.gradle.kts`; land on `main`.
2. Tag: `git tag v<X.Y.Z> && git push origin v<X.Y.Z>`.
3. The tag fires `release.yml` → builds the signed AAB → uploads to the **`internal`** track. Smoke-test the internal build on real devices.

## 3. Staged rollout (production track)

Each hop is a **manual** `workflow_dispatch` of `release.yml` with `track: production` and the fraction below. Watch Play vitals (crashes, ANRs) for **24–48 h** at each stage before advancing.

| Stage | `userFraction` | Halt criteria |
|-------|----------------|---------------|
| 1 | `0.10` | crash-free < 99.5% or ANR regression vs prior track |
| 2 | `0.25` | any new P0/P1; vitals regression |
| 3 | `0.50` | vitals regression |
| 4 | `1.0`  | full availability (`status: completed`) |

The workflow sets `status: inProgress` + `userFraction` for stages 1–3 and `status: completed` at `1.0` automatically.

## 4. Halt / rollback / hotfix

- **Halt:** in Play Console, pause the staged release (stops further exposure at the current fraction). No new dispatch needed.
- **Hotfix:** branch off the release tag, fix + focused test, fast-track through CI, cut a new `versionCode`, restart the rollout from stage 1.
- **Emergency rollback:** halt current rollout and resume-at-100% the prior known-good release (its AAB is retained on the Play track and as a 30-day CI artifact).

## 5. Post-release

- Triage Play reviews + vitals weekly.
- Ship a `CHANGELOG.md` entry and tag notes for every release.
- File Phase 5+ candidates (cloud, LAN transfer, dedupe) to backlog.

---

### What "setup complete" means for T7.8

Delivered by this task: the release/publish pipeline (`release.yml`), the staged-rollout mechanism (10→25→50→100% with vitals halts), the rollback/hotfix procedure, and this runbook. **Not** delivered (external, non-agent gates): the real Play service-account + upload-key secrets (ops/owner) and the actual promotion, which must wait on the T7.6 QA green light.

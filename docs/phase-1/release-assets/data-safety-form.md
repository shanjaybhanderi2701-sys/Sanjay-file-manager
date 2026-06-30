# Filora — Google Play Data Safety Form

**Status:** Ready for Play Console entry · **Last updated:** 2026-06-30
**Applies to:** Filora v1 (1.0.0), **standard** (Play-default) flavor.
**Security sign-off basis:** NFR-3 Security & Privacy (APP-17 security review).

> This document is the source of truth for the answers a release manager enters
> into **Play Console → App content → Data safety**. Every answer is justified
> against the app's actual behavior (manifest permissions + NFR-3) so the
> declaration is verifiable, not aspirational. It aligns with `privacy-policy.md`.

---

## 1. Top-level declarations

| Play question | Answer | Justification |
|---------------|--------|---------------|
| Does your app collect or share any of the required user data types? | **No** | No analytics/ads SDK bundled; no data leaves the device (NFR-3.1, NFR-3.4). |
| Is all of the user data collected by your app encrypted in transit? | **N/A** (no data collected/transmitted) | Core app makes no network requests. |
| Do you provide a way for users to request that their data be deleted? | **N/A** (no data collected) | Nothing is stored off-device; app-local data is cleared on uninstall / clear-data. |
| Privacy policy URL | **Required — provide hosted URL of `privacy-policy.md`** | Play requires a policy URL even when no data is collected. |

Because the top-level answer is **"No data collected or shared,"** Play presents
no per-data-type collection matrix. The table in §2 is recorded here only to show
the review was done deliberately, category by category.

## 2. Per-category review (all = Not collected, Not shared)

| Data category | Collected? | Shared? | Notes |
|---------------|-----------|---------|-------|
| Location (approx/precise) | No | No | No location permission requested. |
| Personal info (name, email, IDs, address) | No | No | No accounts, no sign-up. |
| Financial info | No | No | No purchases/payments. |
| Health & fitness | No | No | — |
| Messages (email, SMS, in-app) | No | No | — |
| Photos & videos | No | No | Read **on-device only** to display media hubs; never uploaded or collected (see §3). |
| Audio files | No | No | Read on-device only for the Audio hub; never uploaded. |
| Files & docs | No | No | Browsed/operated on **on-device only**; never uploaded (see §3). |
| Calendar | No | No | — |
| Contacts | No | No | — |
| App activity / interactions | No | No | No analytics SDK. |
| Web browsing history | No | No | — |
| App info & performance (crash logs, diagnostics) | No | No | No telemetry in v1. Opt-in diagnostics deferred (see §4). |
| Device or other IDs | No | No | No advertising ID or device fingerprint. |

## 3. Why on-device file access is **not** "collection"

Google Play's Data Safety definition of *collection* is transmitting data **off
the device**. Filora reads photos, videos, audio, and documents **only to render
and operate on them locally**:

- Reads are backed by `READ_MEDIA_IMAGES/VIDEO/AUDIO` (+ partial selected-photos
  on Android 14+), legacy `READ_EXTERNAL_STORAGE` (≤ API 32), and the Storage
  Access Framework. None of these transmit data off-device.
- The system **Share** sheet is a user-initiated handoff via a scoped,
  time-bounded `FileProvider` grant (NFR-3.3). It is the user transferring their
  own file through Android — not the app collecting or sharing data — and is out
  of Data Safety scope.

Therefore none of these accesses constitutes collection or sharing under the
Data Safety definitions.

## 4. Diagnostics: explicitly out of scope for this declaration

The PRD lists optional, opt-in anonymized crash diagnostics as an **open question**
deferred out of v1 (PRD §8/§10, NFR-8.2). **v1 ships with no diagnostics**, so the
"App info & performance" category is declared **Not collected**. If diagnostics
are ever added, the responsible release manager must:

1. Update `privacy-policy.md` §5 with exactly what is sent and to whom.
2. Flip "App info and performance → Diagnostics" to **Collected**, mark it
   **optional**, **not shared**, **encrypted in transit**, purpose = *App
   functionality / Analytics (crash)*.
3. Re-run the NFR-3 / APP-17 security check and re-submit the form **before** the
   feature reaches production.

## 5. `MANAGE_EXTERNAL_STORAGE` (all-files access) note

The broad-storage permission exists **only in the separate, gated `fullaccess`
flavor**, which is **not** the Play-default `standard` build (manifest A3;
NFR-3.2). The Data Safety form filed for the Play (standard) listing covers a
build with no all-files access. If the `fullaccess` flavor is ever distributed
through Play, it additionally requires the **Play all-files-access declaration**
with a permitted-use justification — tracked separately from this form.

## 6. Sign-off checklist (release manager)

- [ ] Privacy policy hosted at a stable public URL; URL entered in Play Console.
- [ ] Data Safety answers entered per §1–§2 ("No data collected / No data shared").
- [ ] Confirmed the uploaded AAB is the **standard** flavor (no `MANAGE_EXTERNAL_STORAGE`).
- [ ] Confirmed no analytics/ads SDK present in the release dependency graph (NFR-3.4).
- [ ] NFR-3 / APP-17 security sign-off re-confirmed against the shipped build.

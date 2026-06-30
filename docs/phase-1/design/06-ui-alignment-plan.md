# Filora — UI Alignment Plan to A-blend Hi-Fi Spec (APP-113)

**Owner:** Founding Android Engineer · **Status:** scoped + execution started (2026-07-01)
**Authoritative inputs:**
- Spec + redlines + tokens: `docs/phase-1/design/05-hifi-spec.md` (commit `c83aea7`), esp. §5 checklist.
- Hi-fi visual reference (13 frames): `design/filora-screens/hifi/index.html` (commit `c83aea7`).
- Existing tokens already A-blend-correct: `core/core-ui/theme/Color.kt`, `Type.kt`, `Shape.kt`.

> **Premise (spec §0/§5):** the build is already ~Direction A. This is a **targeted
> alignment**, not a rewrite. Items 1–2 / 5–7 are low-risk polish that lands incrementally;
> **items 3–4 are the real A-blend deltas** and get their own scoped subtasks.

---

## 0. Codebase grounding (measured this heartbeat)

| Signal | Finding | Implication |
|---|---|---|
| Literal `fontSize` / `FontWeight` in `feature/*/src/main` | **0 occurrences** | **Item 5 is essentially already done** — downgrade to a verify-only check. |
| Ad-hoc `.dp` across the 6 feature modules | ~65 (browser 11 · home 13 · media 8 · search 4 · settings 15 · storage 14) | Item 1 migration is mechanical, bounded, low-risk. |
| `WindowSizeClass` / `material3-adaptive` dependency | **absent** from `libs.versions.toml` | Item 3 must add the dep first (`material3-window-size-class` / `material3-adaptive*`). |
| Nav components | `FiloraBottomBar` exists; **no `NavigationRail`** | Item 3 includes the bottom-nav ↔ nav-rail swap. |
| State coverage (grep) | browser/home/media/storage reference Loading+Empty+Error; **search → Empty only**, **settings → Loading only** | Item 7 audit focuses on search + settings (others likely covered, verify). |

---

## 1. Status of the §5 checklist

| # | Item | Effort | Risk | Lands before v1? | Disposition |
|---|------|--------|------|------------------|-------------|
| 1 | `NEW` `Spacing.kt` + replace ad-hoc `.dp` | S (token) + M (migration) | Low | **Yes** | **Token landed this heartbeat (`eccdda0`).** Migration → subtask. |
| 2 | `NEW` `FiloraElevation` + motion util; migrate dialogs/transitions | S (tokens) + M (migration) | Low | **Yes** | **Tokens landed this heartbeat (`eccdda0`).** Migration → subtask. |
| 3 | **Adaptive dual-pane** (Browser + Media) + nav-rail swap, `WindowSizeClass`-gated | **L** | **Med-High** | **Stretch** (see §3) | **New subtask — loop in Android Architect.** |
| 4 | **Storage-story hero band** (static-first) | **M** | Med | **Yes** | **New subtask.** |
| 5 | Type-role audit (no literal `fontSize`/`FontWeight`) | XS | Low | Yes | **Already satisfied (0 found); fold a verify-check into item 7.** |
| 6 | Category color audit (media tiles tonal, not type-coded) | S | Low | Yes | Subtask (polish bundle). |
| 7 | State-coverage audit (skeleton/empty/error every screen) | M | Low | Yes | Subtask (polish bundle); priority = search + settings. |
| 8 | A11y pass on ≥600dp (dual-pane + RTL mirroring) | M | Low | Gated on #3 | Subtask, **blocked on item 3**; route to QA/a11y. |

Effort key: XS ≤0.5d · S ≈0.5–1d · M ≈1–2d · L ≈3–5d.

---

## 2. Execution order (by leverage, dependency-aware)

```
Phase 0 (DONE this heartbeat) ── tokens land, zero dependency
  └─ FiloraSpacing / FiloraElevation / FiloraMotion  → core-ui/theme  (eccdda0)

Phase 1 (parallel, low-risk polish — land incrementally before v1)
  ├─ A) Spacing/Elevation/Motion migration across feature screens   [item 1+2 tail]  M
  ├─ B) State-coverage + type-role verify audit (search, settings)  [item 5+7]       M
  └─ C) Category color audit (media tiles tonal)                    [item 6]          S

Phase 2 (the real A-blend deltas — own subtasks)
  ├─ D) Storage-story hero band (static-first)                      [item 4]          M
  └─ E) Adaptive dual-pane + nav-rail swap  ── ARCHITECT CONSULT    [item 3]          L
            └─ F) A11y pass on ≥600dp (dual-pane + RTL)             [item 8]  blocked-on-E  M
```

Phase 1 and the start of Phase 2-D can run concurrently. Item 3 (E) is sequenced last
because it is the largest new layout surface and benefits from the migrated tokens (A)
being in place so the dual-pane scaffold consumes `FiloraSpacing`/`FiloraElevation`
directly.

---

## 3. v1 cut line (recommendation)

**Land before v1:** Phase 0 (done), Phase 1 (A/B/C), Phase 2-D (storage-story).
These are calm-utility polish + the C borrowing; all low/medium risk.

**Item 3 (dual-pane + nav rail) recommendation: scope now, but treat as the v1 stretch /
fast-follow candidate.** Rationale:
- It is the only **High**-risk, **L**-effort surface and the only one that adds a brand-new
  layout mode (right-pane in-place updates, nav-rail swap, no phone regression).
- Phone single-column is fully shipped and is the dominant install base for a file manager.
- The spec itself frames dual-pane as *additive*, gated behind size class (§2), so deferring
  it does not regress phone and does not violate the approved direction.

→ **Decision to confirm with the UI/UX Designer + Android Architect:** does dual-pane
block v1, or ship as v1.1 fast-follow? Item 3's subtask carries this as an explicit
estimate + go/no-go, with the Architect owning the Compose architecture call.

---

## 4. Subtasks created from this plan

| Subtask | Scope | Owner | Blockers |
|---|---|---|---|
| Item 1+2 migration | Replace ~65 ad-hoc `.dp` with `FiloraSpacing`; route dialogs/transitions through `FiloraElevation`/`FiloraMotion` | Founding Android Eng | none (tokens landed) |
| Item 4 — storage-story hero band | `tertiaryContainer` hero, `displaySmall` free-space, one-shot count-up (≤600ms, reduce-motion aware), static after; data from existing breakdown (no 2nd scan) | Founding Android Eng | none |
| **Item 3 — adaptive dual-pane** | Add `material3-adaptive` dep; `WindowSizeClass` gating; dual-pane Browser+Media; nav-rail swap ≥600dp; no phone regression | Founding Android Eng, **Android Architect (architecture consult)** | tokens (done); recommend after migration |
| Item 6+7 — polish audit | Media tiles tonal not type-coded; skeleton/empty/error on every screen (search+settings first); verify 0 literal type styles | Founding Android Eng | none |
| Item 8 — a11y ≥600dp pass | Dual-pane + RTL mirroring, AA contrast on `onTertiaryContainer`, font-scale 200% reflow | QA / a11y | **blocked on item 3** |

---

## 5. Open questions (for UI/UX Designer — redline ambiguity)

1. **Dual-pane v1 vs v1.1** — does item 3 block v1 (see §3)? Drives whether the whole alignment
   blocks the store cut.
2. **Storage-story "freed this month" line** — needs a month-over-month delta the current
   breakdown does not persist. v1: show the neutral summary only (no fake stat per §3.5), and
   defer the "you freed X" line until a usage-history store exists? Confirm.
3. **Nav-rail at Medium vs Expanded** — spec §2 swaps to rail at ≥600dp (Medium). Confirm rail
   (not a navigation drawer) is wanted at the 600–839dp foldable-open width.

# Three Design Directions — Filora (for approval)

**Owner:** appblish · Chief of Staff (product) · **Status:** APP-103 deliverable 5 + approval gate (8)
**Last updated:** 2026-06-30
**Decision required from:** product owner (you), before high-fidelity UI is finalized.

This is the deliverable the issue is really about: *three genuinely different bets*
on how Filora looks and feels. They are not three skins of one layout — each leads
with different UX principles (`03-ux-principles.md`) and targets a different point
on the everyday-organizer ↔ power-user spectrum. **Pick one to take to high-fidelity
Figma + implementation. Do not assume Direction A is final.**

Lo-fi wireframes for the shared screen set already exist in
`design/filora-screens/wireframes/` (13 screens + flow) and are direction-neutral;
each direction below describes how those screens change in layout, density, color,
and motion.

---

## Direction A — "Calm Utility"  *(the balanced default; recommended)*

**One line:** Files by Google's polish, with real operations underneath.

- **Leads with principles:** #1 content-first, #2 common-path-one-tap, #6 trust.
- **Home:** category cards + storage summary + favorites/recents in a relaxed,
  whitespace-generous single column. Bottom nav (Home · Browse · Storage · Settings).
- **Density:** comfortable. Large tap targets, one primary action per screen.
- **Color:** neutral M3 surfaces, single dynamic accent from wallpaper, restrained.
- **Motion:** subtle, functional (shared-element on open, container transform).
- **Power features:** present but progressively disclosed (long-press → multi-select,
  overflow → archive/hidden).
- **Wins:** broadest appeal, lowest risk, fastest to ship beautifully, most "first-party" feel.
- **Risk:** power users may find it *too* calm; differentiation is polish, not novelty.

## Direction B — "Command Deck"  *(the power lane)*

**One line:** A pro tool that finally looks modern.

- **Leads with principles:** #2 power-path, #3 speed, #4 honest operations.
- **Home:** information-dense dashboard; compact rows; breadcrumb-forward navigation;
  persistent selection affordances. **Dual-pane on foldables/tablets.**
- **Density:** compact. More files per screen; type-coded icons; inline metadata.
- **Color:** darker default surface, file-type accent coding, high-contrast.
- **Motion:** fast, snappy, minimal — speed-signaling.
- **Power features:** front-and-center — multi-select toolbar always a tap away,
  keyboard/gesture shortcuts, quick filters.
- **Wins:** owns the underserved "powerful *and* beautiful" seam; strong for the
  enthusiast reviews that drive credibility.
- **Risk:** can intimidate casual users; needs careful onboarding; more layout work
  (adaptive dual-pane).

## Direction C — "Living Library"  *(the visual / discovery lane)*

**One line:** Your storage, made beautiful and motivating.

- **Leads with principles:** #1 content-first (visually), #5 designed states, #7 expressive.
- **Home:** large media thumbnails, rich category tiles with color extracted from
  content, a "storage story" dashboard that feels like a year-in-review.
- **Density:** spacious and visual; grid-forward; imagery is the hero.
- **Color:** warm, expressive, content-derived palettes; bold use of M3 Expressive.
- **Motion:** fluid, generous, delight-oriented (parallax, morphing cards).
- **Power features:** present but visually wrapped; operations feel lighter, friendlier.
- **Wins:** most screenshot-worthy, strongest "show a friend" pull, best for media-heavy users.
- **Risk:** weakest for document/power tasks; motion/thumbnail cost must be policed
  against principle #3 (speed); can read as "style over function" if overdone.

---

## Comparison at a glance

| | A · Calm Utility | B · Command Deck | C · Living Library |
|---|---|---|---|
| Primary user | Everyday organizer | Power user | Media-heavy / casual |
| Density | Comfortable | Compact | Spacious-visual |
| Risk | Low | Medium | Medium-high |
| Time to polished ship | Fastest | Medium | Medium |
| Differentiation | Polish | Capability | Beauty |
| Best review headline | "Looks first-party" | "Powerful and modern" | "Gorgeous" |

## Recommendation

**Direction A (Calm Utility) as the foundation, with the dual-pane adaptivity of B
adopted on large screens, and C's storage-story treatment used specifically on the
Storage Insights surface.** This leads with the safest, most broadly-loved base and
borrows each rival direction's single best idea where it does the most good —
without forcing the whole app into the power or visual extreme. It is the most
defensible reading of the vision's "calm and in control" feeling.

You may instead pick a single pure direction, or a different blend.

---

## ⚠️ Important reality to decide on: UI is already substantially built

Filora's UI has **already been implemented** across milestones M0–M7 (browser,
media hubs, search, storage, settings, operations — see the feature modules and the
phase-1 docs). The issue's intent — *approve a design direction before UI
implementation begins* — was, in practice, not gated ahead of that build. The
current implementation most closely resembles **Direction A (Calm Utility)** already.

So your approval decision also chooses the **path**:

- **Ratify A** → low rework: a polish/consistency pass aligns the existing UI to the
  approved spec. Fastest path to a top-tier result. *(Recommended.)*
- **Pick A-blend (A + B large-screen + C storage)** → moderate, targeted rework on
  adaptive layout and the storage screen only.
- **Pick B or C as a pure direction** → significant re-design/re-implementation of the
  existing screens; highest quality-ceiling change but largest cost. Consistent with
  the issue's "long-term quality over short-term speed" directive if that's your call.

**Next step on approval:** the chosen direction goes to a UI/UX Designer for
high-fidelity Figma mockups (deliverable 6), then a scoped UI alignment/rework plan
is created against the existing implementation. No high-fidelity UI work or rework
starts until you approve a direction here.

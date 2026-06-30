# UX Principles — Filora

**Owner:** appblish · Chief of Staff (product) · **Status:** APP-103 deliverable 3
**Last updated:** 2026-06-30

Seven principles. Every screen, component, and review is checked against them.
They are tie-breakers: when two designs are equally pretty, the one that serves
more principles wins.

---

### 1. Content first, chrome last
The user came for *their files*, not our UI. Maximize content density without
clutter; minimize persistent chrome; let toolbars and actions recede until needed.
*Test:* on any list screen, ≥ 80% of vertical space is the user's content.

### 2. The common path is one tap; the power path is one more
Open, browse, search, free space — reachable immediately. Bulk ops, archives,
hidden files — never hidden, but never in the casual user's way either. Progressive
disclosure, not two separate apps.

### 3. Speed is a design constraint, not an afterthought
Skeletons over spinners; render first screen before the full list resolves;
thumbnails load off the main thread and never block scroll; large folders page in.
If a design can't stay at 60 fps on mid-tier hardware, it is not the design.

### 4. Every operation is honest
No silent success, no silent failure. Conflicts (overwrite/skip/keep-both) are
always explicit. Destructive actions confirm and, where feasible, undo. Long jobs
show real progress and survive backgrounding. The user always knows what happened.

### 5. Designed empties, loadings, and errors
The first thing a new user sees is an empty state; the most-seen state on a slow
folder is loading; the most-remembered is an error. These are first-class screens
with helpful copy and a next action — never a blank box or a raw stack trace.

### 6. Trust is visible
Ad-free and on-device are not just facts in the privacy policy — the UI shows
permission *reasons* before asking, scopes access minimally, and never nags or
upsells. The absence of dark patterns is itself a designed feature.

### 7. One Filora, every screen size
Material 3 Expressive, dynamic color, consistent type/spacing/iconography across
phone, foldable, and tablet. Adaptive layout (e.g. dual-pane where space allows)
rather than a stretched phone UI. Full accessibility: 48dp targets, AA contrast,
TalkBack labels, RTL.

---

## Applying the principles

- **Design reviews** cite principle numbers ("fails #4 — conflict is silent").
- **The three design directions** (`04-design-directions.md`) each weight these
  differently; the approval decision is partly *which principles to lead with*.
- These extend, not replace, Material 3 and Android accessibility guidelines.

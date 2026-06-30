# Product Vision — Filora

**Owner:** appblish · Chief of Staff (product) · **Status:** APP-103 deliverable 2
**Last updated:** 2026-06-30

---

## 1. Vision statement

> **Filora is the file manager that feels like Android itself made it** — fast on
> the largest folder, honest about your storage, beautiful enough to enjoy opening,
> and powerful enough to never need a second app. Nothing leaves your device.

## 2. The one-line promise

**"Find it, move it, free it — in seconds, beautifully."**

## 3. Who it's for

- **The everyday organizer** — clears Downloads, frees space, finds that one PDF.
- **The power user** — bulk move/rename, archives, hidden files, large-tree search.
- **The privacy-conscious** — wants offline, ad-free, low-permission, and *legibly* so.

We design for the everyday organizer's **comfort** and the power user's **ceiling**
in the same surface — the central product tension this issue exists to resolve.

## 4. What "top-tier Android product team" means here (the bar)

Filora is finished only when it is, in order of priority:

1. **Fast** — 10k-entry folder renders first frame < 300 ms warm; 60 fps scroll on
   mid-tier hardware; no jank on thumbnail load. Speed is a *feature*, not a metric.
2. **Reliable** — operations never silently fail; conflicts are explicit; long jobs
   survive backgrounding (WorkManager) with clear progress; crash-free ≥ 99.5%.
3. **Beautiful** — Material 3 Expressive, dynamic color, intentional motion,
   restraint over decoration. It should be screenshot-worthy by default.
4. **Intuitive** — the common path needs no learning; the advanced path is
   discoverable, not hidden.
5. **Polished** — empty/loading/error states are designed, not afterthoughts;
   spacing, type, and icons are consistent to the pixel.
6. **Professional** — no ads, no dark patterns, no upsell interrupts; trust is the
   brand.

## 5. What Filora is *not* (v1)

No cloud accounts, no sync, no root, no in-app editing, no LAN transfer, no ads.
We open and share through the system, and we keep file contents on-device.

## 6. The feeling we are selling

Opening Filora should feel **calm and in control** — the opposite of the anxious,
ad-cluttered, "is this safe?" feeling most file managers give. Storage insight
should feel **motivating**, not scolding. A bulk operation should feel **certain**.

## 7. How we'll know we got it right

- A user shows a friend the storage screen *unprompted*.
- A reviewer writes "finally, a file manager that looks like it belongs on Android."
- A power user uninstalls their old paid manager and doesn't miss it.
- Zero one-star reviews mentioning ads, permissions-fear, or "it's slow."

See `01-competitive-analysis.md` for the market seam, `03-ux-principles.md` for how
this vision becomes rules, and `04-design-directions.md` for the three concrete bets
on how it looks and feels — one of which you approve before UI is finalized.

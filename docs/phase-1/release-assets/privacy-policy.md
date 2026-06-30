# Filora — Privacy Policy

**Effective date:** TBD (set on GA) · **Last updated:** 2026-06-30
**Publisher:** appblish
**Contact:** privacy@appblish.example (replace with the verified Play developer contact before publishing)

> This is the canonical privacy-policy text for the Filora Android app. It is
> hosted at the public **Privacy policy URL** declared in the Play Console store
> listing (see `store-listing.md`). It is intentionally written to match the app's
> actual behavior as defined by the Non-Functional Requirements (NFR-3 Security &
> Privacy) and the manifest permission set — see `data-safety-form.md` for the
> requirement-by-requirement mapping.

---

## 1. Summary

Filora is a fully on-device file manager. **We do not collect, transmit, or share
your personal data or the contents of your files.** Filora has no accounts, no
ads, and no third-party analytics or advertising SDKs in the version this policy
covers (v1 / 1.0.0). The app works fully offline.

## 2. What Filora accesses on your device

Filora reads and organizes files that already exist on your device and on
removable/USB/cloud-document storage you explicitly grant access to. This access
is used **only** to display, search, and operate on your files on the device. It
is never used to upload, copy off-device, profile, or monetize your data.

| Capability | Why it is used | Leaves the device? |
|------------|----------------|--------------------|
| Photo & media reads (`READ_MEDIA_IMAGES` / `_VIDEO` / `_AUDIO`, partial "selected photos" on Android 14+) | Show the Images/Video/Audio category hubs and media files | No |
| Legacy storage read (`READ_EXTERNAL_STORAGE`, Android 12 and below only) | Browse files on older Android versions | No |
| Storage Access Framework (folder/document picker) | Browse and operate on folders outside shared media, including SD/USB/cloud-document trees | No |
| All-files access (`MANAGE_EXTERNAL_STORAGE`) | **Optional, separate opt-in build only.** Not present in the standard Play build. Lets power users manage all storage when they explicitly choose it | No |
| Foreground service + notifications (`FOREGROUND_SERVICE*`, `POST_NOTIFICATIONS`) | Keep long copy/move/delete jobs running and show progress | No |

## 3. Data we collect

**None.** Filora (v1) does not collect any personal or device data. Specifically:

- No personal identifiers, accounts, or contact data.
- No file names, paths, or file contents are sent anywhere.
- No location, advertising ID, or device fingerprint.
- No usage analytics or telemetry SDK is bundled.

## 4. Data we share

**None.** Because no data is collected, nothing is shared with, sold to, or
transferred to any third party.

When you use the system **Share** sheet to send a file to another app, that
transfer is initiated by you and handled by Android and the destination app via a
scoped, time-bounded `FileProvider` grant. Filora itself does not receive or
forward that content. Whatever you share is then governed by the receiving app's
privacy policy.

## 5. Network use

Filora v1 is offline-first and makes no network requests for its core
functionality. No file content ever leaves the device (NFR-3.1).

If a future version adds **opt-in, anonymized crash diagnostics**, it will be:
off by default, clearly disclosed in-app, individually disableable, and limited
to anonymized stability data that never includes file names, paths, or contents
(NFR-3.4, NFR-8.2). This policy and the Play Data Safety form will be updated
**before** any such feature ships, and this section will describe exactly what is
sent and to whom.

## 6. Permissions you can revoke

All permissions are requested with rationale at the point of use and can be
revoked at any time in Android **Settings → Apps → Filora → Permissions**.
Revoking storage/media access limits what Filora can show but never causes data
to be transmitted off-device.

## 7. Children's privacy

Filora is a general-purpose utility, not directed at children, and collects no
data from any user, including children.

## 8. Data retention & deletion

Filora stores no user data on any server, so there is nothing for us to retain or
delete on your behalf. App-local settings (e.g. theme, favorites/recents) live
only on your device and are removed when you clear the app's data or uninstall.

## 9. Security

Files stay within Android's sandboxed, scoped-storage model. Inter-app sharing
uses `FileProvider` with scoped, time-bounded URI grants (NFR-3.3). The app ships
no secrets and bundles no SDK that exfiltrates file metadata (NFR-3.4).

## 10. Changes to this policy

If our data practices change, we will update this policy, change the "Last
updated" date, and — for material changes that introduce any data collection —
update the Play Data Safety declaration before the change reaches users.

## 11. Contact

Questions about this policy: **privacy@appblish.example** (replace with the
verified developer contact configured in Play Console before publishing).

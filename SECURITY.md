<!-- SPDX-FileCopyrightText: 2026 Stefan Richter -->
<!-- SPDX-License-Identifier: MIT -->
# Security Policy

Birthday Sync has no internet permission and makes no network connections. It reads your contacts
and reads and writes calendar events on the device, so we take bugs seriously that could expose
contact data to other apps, change or delete calendar entries that Birthday Sync did not create,
or let other apps trigger its components.

## Supported versions

Only the latest release receives security fixes. Updates are available on the
[Releases](https://github.com/brokoskokoli/birthday-sync/releases) page.

| Version | Supported |
|---|---|
| 0.1.x (latest) | ✅ |
| older | ❌ |

## Reporting a vulnerability

**Please do not open a public issue, discussion or pull request for security problems.**

Report privately via GitHub: open the repository's **Security** tab and click
**[Report a vulnerability](https://github.com/brokoskokoli/birthday-sync/security/advisories/new)**.
Only the maintainers can see the report.

Please include:

- the Birthday Sync version, your Android version and device model;
- a description of the issue and its impact;
- a minimal proof of concept or steps to reproduce (attach files to the advisory, never post them
  publicly – and no real contact data);
- any relevant `adb logcat` output.

What to expect:

- acknowledgement within **7 days**;
- an assessment and, for confirmed issues, a fix plan within **30 days**;
- coordinated disclosure: we publish a GitHub security advisory together with the fixed release and
  credit you unless you prefer to stay anonymous.

This is a volunteer project; there is no bug bounty.

## Verifying a download

Release APKs are signed with the project's release key. The SHA-256 fingerprint of the signing
certificate is listed in the [README](README.md#verify-the-download).

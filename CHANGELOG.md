<!-- SPDX-FileCopyrightText: 2026 Stefan Richter -->
<!-- SPDX-License-Identifier: MIT -->
# Changelog

All notable changes to Birthday Sync are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

<!-- The section "## [X.Y.Z] - <ISO date>" up to the next "## [" becomes the text of the GitHub
     release (release workflow). Before tagging: keep "## [Unreleased]" (empty), add
     "## [X.Y.Z] - <ISO date>" below it with a one-line intro, move the entries there, and update
     both links at the bottom ([Unreleased] compares from the new tag, [X.Y.Z] from the previous one).
     X.Y.Z must match versionName in app/build.gradle.kts. -->

## [Unreleased]

## [0.1.0] - 2026-10-28

First public release: Birthday Sync copies the birthdays from your Android contacts into a
calendar of your choice and keeps the events up to date – on the device, without internet access.

### Added

- **Contact selection** – choose whole contact accounts (Google, device, Exchange and others) or
  individual groups/labels whose birthdays are included (tab *Contacts*).
- **Target calendar** – write the birthdays as events into any writable calendar, for example your
  Google calendar, or let the app create its own local “Birthdays” calendar (tab *Calendar*).
- **Event options** – all-day or at a set time with a duration, an optional calendar reminder,
  and a title template with the placeholders `{name}` and `{age}` (default `🎂 {name} ({age})`).
- **Automatic sync** – daily or weekly in the background (WorkManager) for a range of 1–12
  months ahead; changed settings are applied after a few seconds, *Sync now* runs it at once.
- **Keeps the calendar in step** – changed names or birth dates update the events; removed or
  deselected contacts and a shorter range remove them. Events are recognised by a marker in their
  description, so no database is needed and the app picks up its events again after a reinstall.
- **Daily notification** – at a time you choose, a notification lists today’s birthdays, straight
  from your contacts and independent of the calendar (tab *Alerts*).
- **Guided setup** – on first start, one screen per missing permission explains why it is needed;
  every step can be skipped and completed later.
- **System checks** – the *System* tab checks permissions, battery optimization, background
  restrictions, app hibernation, exact alarms and Battery Saver, and gives hints for manufacturers
  known to stop background apps.
- **40 languages** – English, German and 38 more; many translations are machine-assisted and
  welcome review by native speakers.
- **Private by design** – no internet permission, no ads, no tracking, no analytics.

[Unreleased]: https://github.com/brokoskokoli/birthday-sync/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/brokoskokoli/birthday-sync/releases/tag/v0.1.0

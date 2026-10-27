<!-- SPDX-FileCopyrightText: 2026 Stefan Richter -->
<!-- SPDX-License-Identifier: MIT -->
# Contributing to Birthday Sync

Thanks for your interest! Bug reports, translations, documentation fixes and code are all welcome.
By participating you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

- **Found a bug?** Open a [bug report](https://github.com/brokoskokoli/birthday-sync/issues/new?template=bug_report.yml).
- **Have an idea?** Start a [feature request](https://github.com/brokoskokoli/birthday-sync/issues/new?template=feature_request.yml)
  or a thread in [Discussions](https://github.com/brokoskokoli/birthday-sync/discussions).
- **Translation wrong or awkward?** Use the [translation form](https://github.com/brokoskokoli/birthday-sync/issues/new?template=translation.yml)
  or send a pull request (see [Translations](#translations)).
- **Security issue?** Please don't open a public issue – see [SECURITY.md](SECURITY.md).

## Development setup

Requirements: **JDK 17 or later** (the JetBrains Runtime bundled with Android Studio works) and the
Android SDK (API 36). Android Studio is the easiest way to get both; open the project folder there.

```sh
# macOS with Android Studio's JDK (adjust the path on other systems)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest lintDebug   # unit tests and lint (same checks as CI)
```

Install the debug build on a device or emulator with `./gradlew installDebug` or
`adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug build uses its own debug
key, so it cannot be installed over a release APK – uninstall the release first.

Release builds (`./gradlew assembleRelease`) are signed only if a signing configuration is present,
either in `signing/keystore.properties` (keys `storeFile`, `storePassword`, `keyAlias`,
`keyPassword`; not in the repository) or in the environment variables `SIGNING_STORE_FILE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`. Without them the release
APK is unsigned.

## Project layout

| Path | Contents |
|---|---|
| `app/src/main/java/…/data/` | Contacts, calendar and settings access |
| `app/src/main/java/…/sync/` | Sync planning (`BirthdayPlanner`), sync run and scheduling (WorkManager) |
| `app/src/main/java/…/notify/` | Daily birthday notification |
| `app/src/main/java/…/system/` | Permission and system checks |
| `app/src/main/java/…/ui/` | Jetpack Compose screens and onboarding |
| `app/src/main/res/values*/strings.xml` | UI texts: English base, one folder per language |
| `app/src/test/` | Unit tests (planner, dates, translations) |
| `fastlane/metadata/android/` | Store listing texts per language |
| `website/` | Project homepage (GitHub Pages) |

## Translations

Birthday Sync is available in 40 languages. Apart from English and German, **many translations
were made with the help of AI and have not been reviewed by native speakers yet.** Reviews are very
welcome:

- App texts: `app/src/main/res/values-<lang>/strings.xml` (English base:
  `app/src/main/res/values/strings.xml`, with comments for translators).
- Store texts: `fastlane/metadata/android/<locale>/` (`title.txt`, `short_description.txt` ≤ 80
  characters, `full_description.txt`, `changelogs/`).
- Keep placeholders such as `%1$s`, `%2$d` and the tokens `{name}` and `{age}` unchanged; quoted
  Android setting names should match what Android shows in that language.
- `./gradlew testDebugUnitTest lintDebug` checks that no string is missing or has broken
  placeholders.

If you'd rather not send a pull request, open a [translation issue](https://github.com/brokoskokoli/birthday-sync/issues/new?template=translation.yml)
with the current and the suggested text.

## Coding conventions

- Kotlin, Jetpack Compose, no network code and no new permissions without discussion – “no
  internet access” is a core promise of the app.
- Every user-visible text goes into `strings.xml` and into all languages (lint fails otherwise).
- Licensing follows [REUSE](https://reuse.software): `REUSE.toml` covers all files with
  `MIT`; new files of a different origin need their own entry. Check with
  `pipx run --spec "reuse[charset-normalizer]==6.1.2" reuse lint`.
- Keep pull requests focused: one logical change per PR.

## Commit messages

- One logical change per commit. Summary line ≤ 72 characters, no trailing period.
- In the body, explain *why*, and reference issues (`#123`).
- Don't add e-mail addresses beyond your own author identity. Using your GitHub `noreply`
  address as the author e-mail is recommended.

## Licensing of contributions

Birthday Sync is licensed under the **MIT License**. Contributions are accepted under the same
licence (*inbound = outbound*): by opening a pull request you agree that your contribution is
licensed under MIT. There is **no CLA**. A `Signed-off-by` line
([Developer Certificate of Origin](https://developercertificate.org)) is welcome but not required.

Only contribute code you wrote yourself or that is under a compatible licence – say so in the PR
when you include third-party code, and add its licence to `LICENSES/` and `REUSE.toml`.

## Pull request checklist

- [ ] The change is described in an issue (for features: discussed first)
- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug` passes locally
- [ ] New behaviour has tests where practical
- [ ] New texts are in `strings.xml` for all languages
- [ ] User-visible changes are noted in [CHANGELOG.md](CHANGELOG.md) under *Unreleased*
- [ ] Docs updated where needed (README, website/)

The [pull request template](.github/PULL_REQUEST_TEMPLATE.md) contains the same list.

<!-- SPDX-FileCopyrightText: 2026 Stefan Richter -->
<!-- SPDX-License-Identifier: MIT -->
[English](README.md) · **Deutsch**

<div align="center">

<img src="packaging/brand/icon.svg" width="128" height="128" alt="App-Icon von Birthday Sync: eine weiße Geburtstagstorte mit Kerze auf rosa Grund">

# Birthday Sync

**Überträgt die Geburtstage aus deinen Android-Kontakten in den Kalender, den du wirklich nutzt – automatisch, auf deinem Gerät, ohne Internetzugriff.**

[![Android CI](https://github.com/brokoskokoli/birthday-sync/actions/workflows/android.yml/badge.svg)](https://github.com/brokoskokoli/birthday-sync/actions/workflows/android.yml)
[![Neueste Version](https://img.shields.io/github/v/release/brokoskokoli/birthday-sync?include_prereleases&sort=semver&label=Version)](https://github.com/brokoskokoli/birthday-sync/releases)
[![Lizenz: MIT](https://img.shields.io/badge/Lizenz-MIT-blue.svg)](LICENSE)
[![Plattform: Android 8.0+](https://img.shields.io/badge/Plattform-Android%208.0%2B-lightgrey.svg)](#installation)

[![APK herunterladen](https://img.shields.io/badge/APK_herunterladen-neueste_Version-b02a5b?style=for-the-badge&logo=android&logoColor=white)](https://github.com/brokoskokoli/birthday-sync/releases/latest)

</div>

> **Status:** 0.1.0 ist die erste öffentliche Version. Siehe [Changelog](CHANGELOG.md) (Englisch)
> und [Roadmap](#roadmap). Screenshots folgen.

## Warum Birthday Sync?

Google Kalender hat jahrelang die Geburtstage deiner Kontakte automatisch angezeigt. Seit 2024
klappt das bei vielen Nutzern in Deutschland und der EU nicht mehr zuverlässig – Google verweist
auf eine Vereinbarung mit einer deutschen Behörde. Die Geburtstage stehen weiter in deinen
Kontakten, sie tauchen nur nicht mehr im Kalender auf.

Birthday Sync schreibt für diese Geburtstage **echte Kalendertermine** in einen Kalender deiner
Wahl – zum Beispiel deinen Google-Kalender, damit sie auch im Web und auf deinen anderen Geräten
erscheinen – und hält sie aktuell. Du bestimmst, welche Kontakte dabei sind, wie die Termine
aussehen und ob du erinnert wirst. Zusätzlich kann die App dich jeden Tag per Benachrichtigung
auf die heutigen Geburtstage hinweisen.

[Birthday Adapter](https://f-droid.org/packages/org.birthdayadapter/) (F-Droid) ist eine gute
Wahl, wenn dir ein eigener lokaler Geburtstagskalender reicht. Birthday Sync kann in **jeden**
Kalender auf dem Gerät schreiben, auch in synchronisierte Konten, und bringt die tägliche
Benachrichtigung mit.

## Funktionen

- **Kontakte auswählen** – ganze Kontokonten (Google, Gerät, Exchange und andere) oder einzelne
  Gruppen/Labels.
- **Kalender auswählen** – jeder beschreibbare Kalender auf dem Gerät oder ein lokaler Kalender
  „Geburtstage“, den die App für dich anlegt.
- **Termine nach deinem Geschmack** – ganztägig oder zu einer Uhrzeit mit Dauer, optionale
  Kalender-Erinnerung, Titelvorlage mit `{name}` und `{age}` (Standard: `🎂 {name} ({age})`).
- **Automatischer Abgleich** – täglich oder wöchentlich im Hintergrund (WorkManager), 1–12 Monate
  im Voraus.
- **Kalender bleibt aktuell** – geänderte Namen oder Geburtsdaten aktualisieren die Termine;
  gelöschte oder abgewählte Kontakte verschwinden aus dem Kalender.
- **Tägliche Benachrichtigung** – zu einer Uhrzeit deiner Wahl, nur an Tagen mit Geburtstag,
  direkt aus deinen Kontakten.
- **Geführte Einrichtung** – fragt jede Berechtigung auf einem eigenen Bildschirm ab und erklärt,
  wofür sie gebraucht wird.
- **Systemprüfung** – Akku-Optimierung, Hintergrundbeschränkung, App-Pausierung, exakte Wecker,
  Energiesparmodus und Hinweise für Hersteller, die Hintergrund-Apps gern beenden.
- **40 Sprachen** – mit Sprachauswahl pro App ab Android 13.
- **Privat** – keine Internetberechtigung, keine Werbung, kein Tracking, keine Analyse, kein Konto.
- **Frei und quelloffen** – MIT-Lizenz.

## Wie der Abgleich funktioniert

Jeder Termin, den Birthday Sync anlegt, trägt am Ende der Beschreibung eine Markierung:
`#birthday-sync key=<Kontakt>_<Datum> v=<Version>`. Die App ändert ausschließlich Termine mit
dieser Markierung und braucht keine eigene Datenbank – sie findet ihre Termine auch nach einer
Neuinstallation wieder.

- Name, Titelvorlage, Uhrzeit oder Erinnerung geändert → Version weicht ab → Termin wird aktualisiert.
- Geburtsdatum geändert, Kontakt gelöscht oder abgewählt, Zeitraum verkürzt → Termin wird entfernt
  (und ggf. neu angelegt).
- Zielkalender gewechselt → die Termine wandern in den neuen Kalender.
- Vergangene Termine bleiben unverändert. Von Hand gelöschte zukünftige Termine legt der nächste
  Abgleich wieder an.
- *Kalender → Termine entfernen* löscht alle von Birthday Sync angelegten Termine aus allen Kalendern.

## Installation

Voraussetzung: **Android 8.0 (API 26) oder neuer**.

- **APK von GitHub:** `BirthdaySync-<version>.apk` aus der
  [neuesten Version](https://github.com/brokoskokoli/birthday-sync/releases/latest) herunterladen
  und auf dem Handy öffnen. Android fragt einmalig, ob Browser bzw. Dateimanager Apps installieren
  dürfen.
- **Obtainium:** in [Obtainium](https://github.com/ImranR98/Obtainium)
  `https://github.com/brokoskokoli/birthday-sync` hinzufügen – so kommen APK und Updates direkt
  von GitHub.
- **Google Play, IzzyOnDroid / F-Droid:** geplant.

Updates werden genauso installiert; die Einstellungen bleiben erhalten. Ein selbst gebautes
Debug-APK ist mit einem anderen Schlüssel signiert und lässt sich nicht über eine Release-Version
installieren.

### Download prüfen

Die Release-APKs sind mit dem Release-Schlüssel des Projekts signiert. Prüfen lässt sich das mit
`apksigner verify --print-certs BirthdaySync-<version>.apk` (Android SDK Build-Tools) oder in
AppVerifier. Der SHA-256-Fingerabdruck des Signaturzertifikats lautet:

```text
98:3B:31:F1:02:F0:75:7F:5F:27:88:A3:10:88:DF:A1:DF:60:97:18:8E:84:00:94:56:37:75:AF:83:DA:CB:40
```

## Erste Schritte

1. Birthday Sync öffnen. Die Einrichtung fragt jede Berechtigung einzeln ab und erklärt, wofür sie
   gebraucht wird; Schritte lassen sich überspringen und später im Tab *System* nachholen.
2. *Kontakte:* die Konten oder Gruppen wählen, deren Geburtstage übernommen werden sollen.
3. *Kalender:* Zielkalender wählen (oder den lokalen Kalender „Geburtstage“ anlegen), ganztägig
   oder mit Uhrzeit, Erinnerung, Titel und Zeitraum.
4. *Übersicht:* **Jetzt synchronisieren** tippen – danach gleicht die App automatisch ab.
5. *Hinweise:* die tägliche Benachrichtigung einschalten und eine Uhrzeit wählen.

Laufen Abgleich oder Benachrichtigung im Hintergrund nicht, hilft der Tab *System*: Manche
Hersteller beenden Hintergrund-Apps, solange man es nicht ausdrücklich erlaubt.

## Selbst bauen

Voraussetzungen: JDK 17 oder neuer (die mit Android Studio gelieferte JetBrains Runtime reicht)
und das Android SDK (API 36).

```sh
# macOS mit dem JDK aus Android Studio (auf anderen Systemen Pfad anpassen)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest lintDebug   # Unit-Tests und Lint
```

`./gradlew assembleRelease` signiert das Release-APK nur, wenn eine Signaturkonfiguration vorhanden
ist: entweder `signing/keystore.properties` (Schlüssel `storeFile`, `storePassword`, `keyAlias`,
`keyPassword`; nicht im Repository) oder die Umgebungsvariablen `SIGNING_STORE_FILE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` und `SIGNING_KEY_PASSWORD`. Ohne sie ist das
Release-APK unsigniert.

## Datenschutz

Birthday Sync hat **keine Internetberechtigung** und baut keinerlei Netzverbindungen auf. Die App
liest Namen, Geburtstage und die Zuordnung zu Konten/Gruppen aus deinen Kontakten und liest und
schreibt Kalendertermine – nur auf deinem Gerät, und sie ändert ausschließlich ihre eigenen
Geburtstagstermine. Einstellungen werden lokal gespeichert. Keine Werbung, keine Analyse, kein
Tracking. Die Links *Datenschutz* und *Quellcode* in der App öffnen deinen Browser. Details:
[Datenschutzerklärung](https://brokoskokoli.github.io/birthday-sync/de/privacy.html).

## Roadmap

- [x] 0.1.0 – erste öffentliche Version (APK auf GitHub)
- [ ] Screenshots
- [ ] Google Play
- [ ] IzzyOnDroid / F-Droid
- [ ] Geprüfte Übersetzungen

Ideen und Stimmen sind in den [Discussions](https://github.com/brokoskokoli/birthday-sync/discussions) willkommen.

## Übersetzungen

App und Store-Texte gibt es in 40 Sprachen. Außer Englisch und Deutsch sind viele Übersetzungen mit
Hilfe von KI entstanden. **Wenn du Muttersprachler bist, hilf bitte beim Prüfen** – die Texte
liegen in `app/src/main/res/values-<sprache>/strings.xml` und
`fastlane/metadata/android/<locale>/`. Einfach ein
[Übersetzungs-Issue](https://github.com/brokoskokoli/birthday-sync/issues/new?template=translation.yml)
oder einen Pull Request eröffnen; siehe [CONTRIBUTING.md](CONTRIBUTING.md#translations) (Englisch).

## Mitmachen

Fehlerberichte, Ideen, Übersetzungen und Pull Requests sind willkommen – bitte zuerst
[CONTRIBUTING.md](CONTRIBUTING.md) lesen (Englisch). Es gilt der
[Code of Conduct](CODE_OF_CONDUCT.md). Sicherheitslücken bitte vertraulich melden, siehe
[SECURITY.md](SECURITY.md).

## Lizenz

Birthday Sync ist freie Software unter der **MIT-Lizenz**, siehe [LICENSE](LICENSE). Das
Repository folgt der [REUSE](https://reuse.software)-Spezifikation ([REUSE.toml](REUSE.toml)); der
Gradle-Wrapper steht unter Apache-2.0, der Code of Conduct unter CC-BY-4.0.

## Danksagung

Gebaut mit [Kotlin](https://kotlinlang.org), [Jetpack Compose](https://developer.android.com/compose),
[Material 3](https://m3.material.io) und [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
(Apache-2.0).

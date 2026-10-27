# Birthday Sync

Android-App (ab Android 8), die Geburtstage aus ausgewählten Kontaktlisten in einen Kalender überträgt
und an Geburtstagen zu einer festen Uhrzeit benachrichtigt.

## Funktionen
- **Begrüßung beim ersten Start:** Willkommen, dann je ein Bildschirm pro fehlender Berechtigung mit Begründung
  (Kontakte, Kalender, Benachrichtigungen, ggf. exakte Wecker, Akku-Optimierung, App-Pausierung). Nach der
  Freigabe geht es automatisch weiter; jeder Schritt lässt sich überspringen und später im Tab „System“ nachholen.
- **Kontakte:** Konten (Google, Gerät, Exchange …) oder einzelne Gruppen/Labels auswählen
- **Kalender:** Zielkalender wählen oder eigenen Gerätekalender „Geburtstage“ anlegen; ganztägig oder mit
  Uhrzeit/Dauer; optionale Kalender-Erinnerung; Titelvorlage mit `{name}` und `{alter}`
- **Sync:** täglich oder wöchentlich (WorkManager), Zeitraum 1–12 Monate im Voraus, nach Einstellungsänderungen
  automatisch nach ca. 10 s
- **Benachrichtigung:** heutige Geburtstage zur eingestellten Uhrzeit (exakter Alarm, nach Neustart neu geplant)
- **System:** Prüfung von Berechtigungen, Akku-Optimierung, Hintergrundbeschränkung, App-Pausierung,
  exakten Alarmen und Energiesparmodus; „Einrichtung durchlaufen“ startet die Begrüßung für fehlende Punkte erneut

## Wie der Abgleich funktioniert
Jeder Termin trägt am Ende der Beschreibung eine Markierung `#birthday-sync key=<Kontakt>_<Datum> v=<Version>`.
- Name, Titelvorlage, Uhrzeit oder Erinnerung geändert → Version weicht ab → Termin wird überschrieben
- Geburtsdatum geändert, Kontakt gelöscht oder abgewählt, Zeitraum verkürzt → Termin wird entfernt (und ggf. neu angelegt)
- Zielkalender gewechselt → Termine wandern in den neuen Kalender
- Vergangene Termine bleiben unverändert. Von Hand gelöschte zukünftige Termine legt der nächste Sync wieder an.

## Bauen
```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest assembleRelease
```
Die Release-Signatur liest `signing/keystore.properties` (nicht im Repo). Ohne die Datei wird mit dem Debug-Key signiert.
**Den Keystore `signing/release.jks` sichern** – nur mit demselben Schlüssel lassen sich spätere Versionen als Update installieren.

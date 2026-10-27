package io.github.brokoskokoli.birthdaysync.sync

import android.content.Context
import io.github.brokoskokoli.birthdaysync.data.AppSettings
import io.github.brokoskokoli.birthdaysync.data.CalendarRepository
import io.github.brokoskokoli.birthdaysync.data.ContactRepository
import io.github.brokoskokoli.birthdaysync.data.EventSpec
import io.github.brokoskokoli.birthdaysync.system.Perms
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** [skipped]: noch nicht eingerichtet, kein Ergebnis für den Nutzer. */
data class SyncOutcome(val ok: Boolean, val message: String, val skipped: Boolean = false)

/**
 * Gleicht die Geburtstage der ausgewählten Kontaktlisten mit dem Zielkalender ab.
 *
 * Jeder Termin trägt in der Beschreibung eine Markierung mit Schlüssel (Kontakt + Datum) und Version
 * (Hash über Titel, Zeiten, Erinnerung). Daraus folgt:
 * - neuer Geburtstag → Termin anlegen
 * - Name/Einstellungen geändert → Version weicht ab → Termin überschreiben
 * - Geburtsdatum geändert, Kontakt gelöscht/abgewählt, Zeitraum verkürzt → Schlüssel nicht mehr gewünscht → Termin löschen
 * - Zielkalender gewechselt → Termin im alten Kalender löschen, im neuen anlegen
 * Vergangene Termine bleiben unangetastet.
 */
class BirthdaySync(private val context: Context) {

    fun run(settings: AppSettings): SyncOutcome {
        // Ohne Zielkalender gibt es nichts abzugleichen. Leere Quellen dagegen synchronisieren: dann werden Termine entfernt.
        if (settings.calendarId < 0) return SyncOutcome(true, "Kein Zielkalender gewählt", skipped = true)
        if (!Perms.contacts(context)) return SyncOutcome(false, "Keine Berechtigung für Kontakte")
        if (!Perms.calendar(context)) return SyncOutcome(false, "Keine Berechtigung für den Kalender")
        val calendars = CalendarRepository(context)
        if (calendars.findCalendar(settings.calendarId) == null) {
            return SyncOutcome(false, "Kein gültiger Zielkalender ausgewählt")
        }

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val birthdays = ContactRepository(context).loadBirthdays(settings.selectedSources)
        val desired = BirthdayPlanner.occurrences(birthdays, today, settings.syncRangeMonths)
            .associate { it.key to buildSpec(it, settings, zone) }

        var created = 0
        var updated = 0
        var deleted = 0
        val kept = mutableSetOf<String>()
        // Ganztägige Termine liegen auf UTC-Mitternacht, daher mit einem Tag Puffer suchen.
        val from = today.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        for (event in calendars.managedEvents(from)) {
            val date = BirthdayPlanner.dateOfKey(event.key) ?: continue
            if (date.isBefore(today)) continue
            val spec = desired[event.key]
            if (spec == null || event.calendarId != settings.calendarId || event.key in kept) {
                calendars.deleteEvent(event.id)
                deleted++
                continue
            }
            kept += event.key
            val version = version(spec)
            if (event.version != version) {
                calendars.updateEvent(event.id, spec, event.key, version)
                updated++
            }
        }
        for ((key, spec) in desired) {
            if (key in kept) continue
            calendars.insertEvent(settings.calendarId, spec, key, version(spec))
            created++
        }

        val summary = buildString {
            append("${birthdays.size} Geburtstage, ${desired.size} Termine im Zeitraum")
            if (created + updated + deleted == 0) append(" – alles aktuell")
            else append(" – $created neu, $updated aktualisiert, $deleted entfernt")
        }
        return SyncOutcome(true, summary)
    }

    /** Entfernt alle von der App angelegten Termine (auch vergangene). */
    fun removeAll(): Int {
        if (!Perms.calendar(context)) return 0
        val calendars = CalendarRepository(context)
        val events = calendars.managedEvents(0)
        events.forEach { calendars.deleteEvent(it.id) }
        return events.size
    }

    private fun buildSpec(o: Occurrence, s: AppSettings, zone: ZoneId): EventSpec {
        val title = BirthdayPlanner.renderTitle(s.titleTemplate, o.birthday.name, o.age)
        val description = buildString {
            append("Geburtstag von ${o.birthday.name}")
            o.birthday.date.year?.let { append(" (geboren $it)") }
            append("\n\nAutomatisch angelegt von Birthday Sync. Änderungen an diesem Termin werden beim nächsten Abgleich überschrieben.")
        }
        val reminder = if (s.eventReminderEnabled) s.eventReminderMinutes else null
        return if (s.allDay) {
            val start = o.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val end = o.date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            EventSpec(title, description, start, end, true, "UTC", reminder)
        } else {
            val start = o.date.atStartOfDay(zone).plusMinutes(s.eventStartMinuteOfDay.toLong())
            val end = start.plusMinutes(s.eventDurationMinutes.toLong())
            EventSpec(title, description, start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), false, zone.id, reminder)
        }
    }

    private fun version(spec: EventSpec): String = Integer.toHexString(spec.hashCode())
}

package io.github.brokoskokoli.birthdaysync.sync

import android.content.Context
import io.github.brokoskokoli.birthdaysync.R
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
        if (settings.calendarId < 0) return SyncOutcome(true, context.getString(R.string.sync_no_calendar), skipped = true)
        if (!Perms.contacts(context)) return SyncOutcome(false, context.getString(R.string.sync_no_contacts_permission))
        if (!Perms.calendar(context)) return SyncOutcome(false, context.getString(R.string.sync_no_calendar_permission))
        val calendars = CalendarRepository(context)
        if (calendars.findCalendar(settings.calendarId) == null) {
            return SyncOutcome(false, context.getString(R.string.sync_invalid_calendar))
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

        val r = context.resources
        val found = r.getQuantityString(R.plurals.sync_birthdays, birthdays.size, birthdays.size)
        val inRange = r.getQuantityString(R.plurals.sync_events_in_range, desired.size, desired.size)
        val summary = if (created + updated + deleted == 0) r.getString(R.string.sync_summary_up_to_date, found, inRange)
        else r.getString(R.string.sync_summary_changes, found, inRange, created, updated, deleted)
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
        val year = o.birthday.date.year
        val about = if (year != null) context.getString(R.string.event_description_born, o.birthday.name, year)
        else context.getString(R.string.event_description, o.birthday.name)
        // Absätze; die Markierung für den Abgleich hängt CalendarRepository unverändert an.
        val description = about + "\n\n" + context.getString(R.string.event_auto_created)
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

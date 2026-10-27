package io.github.brokoskokoli.birthdaysync.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import java.util.TimeZone

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
)

/** Ein Kalendertermin, wie er angelegt bzw. überschrieben werden soll. */
data class EventSpec(
    val title: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val timeZone: String,
    val reminderMinutes: Int?,
)

/** Ein von dieser App angelegter Termin, erkannt an der Markierung in der Beschreibung. */
data class ManagedEvent(val id: Long, val calendarId: Long, val key: String, val version: String)

class CalendarRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    /** Kalender, in die geschrieben werden darf. */
    fun writableCalendars(): List<CalendarInfo> {
        val list = mutableListOf<CalendarInfo>()
        resolver.query(
            Calendars.CONTENT_URI,
            arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE, Calendars.CALENDAR_COLOR),
            "${Calendars.CALENDAR_ACCESS_LEVEL}>=?", arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${Calendars.ACCOUNT_NAME}, ${Calendars.CALENDAR_DISPLAY_NAME}",
        )?.use { c ->
            while (c.moveToNext()) {
                list += CalendarInfo(
                    id = c.getLong(0),
                    name = c.getString(1) ?: "Kalender ${c.getLong(0)}",
                    accountName = c.getString(2).orEmpty(),
                    accountType = c.getString(3).orEmpty(),
                    color = c.getInt(4),
                )
            }
        }
        return list
    }

    fun findCalendar(id: Long): CalendarInfo? = writableCalendars().firstOrNull { it.id == id }

    /** Legt (falls noch nicht vorhanden) einen lokalen Kalender "Geburtstage" auf dem Gerät an. */
    fun ensureLocalCalendar(): Long {
        writableCalendars().firstOrNull {
            it.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL && it.accountName == LOCAL_ACCOUNT
        }?.let { return it.id }

        val uri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, "geburtstage")
            put(Calendars.CALENDAR_DISPLAY_NAME, "Geburtstage")
            put(Calendars.CALENDAR_COLOR, 0xFFE8618C.toInt())
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, LOCAL_ACCOUNT)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
            put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }
        val created = resolver.insert(uri, values) ?: error("Kalender konnte nicht angelegt werden")
        return ContentUris.parseId(created)
    }

    /** Alle von der App angelegten Termine ab [fromMillis] (über alle Kalender). */
    fun managedEvents(fromMillis: Long): List<ManagedEvent> {
        val list = mutableListOf<ManagedEvent>()
        resolver.query(
            Events.CONTENT_URI,
            arrayOf(Events._ID, Events.CALENDAR_ID, Events.DESCRIPTION),
            "${Events.DELETED}=0 AND ${Events.DTSTART}>=? AND ${Events.DESCRIPTION} LIKE ?",
            arrayOf(fromMillis.toString(), "%$MARKER%"),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val m = MARKER_REGEX.find(c.getString(2).orEmpty()) ?: continue
                list += ManagedEvent(c.getLong(0), c.getLong(1), m.groupValues[1], m.groupValues[2])
            }
        }
        return list
    }

    fun insertEvent(calendarId: Long, spec: EventSpec, key: String, version: String): Long {
        val values = eventValues(spec, key, version).apply { put(Events.CALENDAR_ID, calendarId) }
        val uri = resolver.insert(Events.CONTENT_URI, values) ?: error("Termin konnte nicht angelegt werden")
        val id = ContentUris.parseId(uri)
        writeReminder(id, spec.reminderMinutes)
        return id
    }

    fun updateEvent(id: Long, spec: EventSpec, key: String, version: String) {
        resolver.update(ContentUris.withAppendedId(Events.CONTENT_URI, id), eventValues(spec, key, version), null, null)
        resolver.delete(Reminders.CONTENT_URI, "${Reminders.EVENT_ID}=?", arrayOf(id.toString()))
        writeReminder(id, spec.reminderMinutes)
    }

    fun deleteEvent(id: Long) {
        resolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, id), null, null)
    }

    private fun eventValues(spec: EventSpec, key: String, version: String) = ContentValues().apply {
        put(Events.TITLE, spec.title)
        put(Events.DESCRIPTION, spec.description + "\n\n" + marker(key, version))
        put(Events.DTSTART, spec.startMillis)
        put(Events.DTEND, spec.endMillis)
        put(Events.ALL_DAY, if (spec.allDay) 1 else 0)
        put(Events.EVENT_TIMEZONE, spec.timeZone)
        put(Events.AVAILABILITY, Events.AVAILABILITY_FREE)
        put(Events.HAS_ALARM, if (spec.reminderMinutes != null) 1 else 0)
    }

    private fun writeReminder(eventId: Long, minutes: Int?) {
        if (minutes == null) return
        resolver.insert(Reminders.CONTENT_URI, ContentValues().apply {
            put(Reminders.EVENT_ID, eventId)
            put(Reminders.MINUTES, minutes)
            put(Reminders.METHOD, Reminders.METHOD_ALERT)
        })
    }

    companion object {
        const val LOCAL_ACCOUNT = "Birthday Sync"
        private const val MARKER = "#birthday-sync key="
        private val MARKER_REGEX = Regex("""#birthday-sync key=(\S+) v=(\S+)""")
        fun marker(key: String, version: String) = "$MARKER$key v=$version"
    }
}

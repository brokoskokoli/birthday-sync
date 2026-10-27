package io.github.brokoskokoli.birthdaysync.data

import android.content.Context
import androidx.core.content.edit
import io.github.brokoskokoli.birthdaysync.sync.BirthdayPlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    /** Ausgewählte Kontaktlisten, siehe [ContactRepository.accountSourceId] und [ContactRepository.groupSourceId]. */
    val selectedSources: Set<String> = emptySet(),
    val autoSyncEnabled: Boolean = true,
    val syncIntervalDays: Int = 1,
    val syncRangeMonths: Int = 12,
    val calendarId: Long = -1,
    val allDay: Boolean = true,
    val eventStartMinuteOfDay: Int = 9 * 60,
    val eventDurationMinutes: Int = 60,
    val eventReminderEnabled: Boolean = false,
    val eventReminderMinutes: Int = 900,
    val titleTemplate: String = BirthdayPlanner.DEFAULT_TITLE,
    val dailyNotificationEnabled: Boolean = true,
    val dailyNotificationMinuteOfDay: Int = 8 * 60,
) {
    /** Alles, was das Aussehen der Kalendertermine bestimmt; ändert es sich, wird zeitnah synchronisiert. */
    fun eventRelevant() = listOf(
        selectedSources, calendarId, allDay, eventStartMinuteOfDay, eventDurationMinutes,
        eventReminderEnabled, eventReminderMinutes, titleTemplate, syncRangeMonths,
    )
}

data class SyncStatus(
    val lastSyncAt: Long = 0,
    val lastSyncOk: Boolean = true,
    val lastSyncMessage: String = "",
)

/** Einstellungen in SharedPreferences; der Speicherzustand wird sofort im StateFlow gespiegelt (ein Prozess). */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())
    private val _status = MutableStateFlow(readStatus())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    val status: StateFlow<SyncStatus> = _status.asStateFlow()
    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(K_ONBOARDING, false))
    /** Ob der Begrüßungs- und Berechtigungsablauf beim ersten Start abgeschlossen wurde. */
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    fun setOnboardingDone(done: Boolean) {
        prefs.edit { putBoolean(K_ONBOARDING, done) }
        _onboardingDone.value = done
    }

    @Synchronized
    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_settings.value)
        prefs.edit {
            putStringSet(K_SOURCES, s.selectedSources)
            putBoolean(K_AUTO, s.autoSyncEnabled)
            putInt(K_INTERVAL, s.syncIntervalDays)
            putInt(K_RANGE, s.syncRangeMonths)
            putLong(K_CALENDAR, s.calendarId)
            putBoolean(K_ALL_DAY, s.allDay)
            putInt(K_START, s.eventStartMinuteOfDay)
            putInt(K_DURATION, s.eventDurationMinutes)
            putBoolean(K_REMINDER, s.eventReminderEnabled)
            putInt(K_REMINDER_MIN, s.eventReminderMinutes)
            putString(K_TITLE, s.titleTemplate)
            putBoolean(K_DAILY, s.dailyNotificationEnabled)
            putInt(K_DAILY_TIME, s.dailyNotificationMinuteOfDay)
        }
        _settings.value = s
    }

    fun saveStatus(status: SyncStatus) {
        prefs.edit {
            putLong(K_LAST_AT, status.lastSyncAt)
            putBoolean(K_LAST_OK, status.lastSyncOk)
            putString(K_LAST_MSG, status.lastSyncMessage)
        }
        _status.value = status
    }

    /** Datum (ISO), an dem die Tagesbenachrichtigung zuletzt gezeigt wurde – verhindert Doppelungen. */
    var lastDailyNotificationDate: String?
        get() = prefs.getString(K_LAST_DAILY, null)
        set(v) = prefs.edit { putString(K_LAST_DAILY, v) }

    /** Laufzeitberechtigungen, die schon einmal angefragt wurden (für "dauerhaft abgelehnt"-Erkennung). */
    fun wasAsked(permission: String) = prefs.getStringSet(K_ASKED, emptySet())!!.contains(permission)
    fun markAsked(permissions: Collection<String>) =
        prefs.edit { putStringSet(K_ASKED, prefs.getStringSet(K_ASKED, emptySet())!! + permissions) }

    private fun readSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            selectedSources = prefs.getStringSet(K_SOURCES, d.selectedSources)!!.toSet(),
            autoSyncEnabled = prefs.getBoolean(K_AUTO, d.autoSyncEnabled),
            syncIntervalDays = prefs.getInt(K_INTERVAL, d.syncIntervalDays),
            syncRangeMonths = prefs.getInt(K_RANGE, d.syncRangeMonths),
            calendarId = prefs.getLong(K_CALENDAR, d.calendarId),
            allDay = prefs.getBoolean(K_ALL_DAY, d.allDay),
            eventStartMinuteOfDay = prefs.getInt(K_START, d.eventStartMinuteOfDay),
            eventDurationMinutes = prefs.getInt(K_DURATION, d.eventDurationMinutes),
            eventReminderEnabled = prefs.getBoolean(K_REMINDER, d.eventReminderEnabled),
            eventReminderMinutes = prefs.getInt(K_REMINDER_MIN, d.eventReminderMinutes),
            titleTemplate = prefs.getString(K_TITLE, d.titleTemplate)!!,
            dailyNotificationEnabled = prefs.getBoolean(K_DAILY, d.dailyNotificationEnabled),
            dailyNotificationMinuteOfDay = prefs.getInt(K_DAILY_TIME, d.dailyNotificationMinuteOfDay),
        )
    }

    private fun readStatus() = SyncStatus(
        lastSyncAt = prefs.getLong(K_LAST_AT, 0),
        lastSyncOk = prefs.getBoolean(K_LAST_OK, true),
        lastSyncMessage = prefs.getString(K_LAST_MSG, "")!!,
    )

    companion object {
        private const val K_SOURCES = "sources"
        private const val K_AUTO = "auto_sync"
        private const val K_INTERVAL = "sync_interval_days"
        private const val K_RANGE = "sync_range_months"
        private const val K_CALENDAR = "calendar_id"
        private const val K_ALL_DAY = "all_day"
        private const val K_START = "event_start"
        private const val K_DURATION = "event_duration"
        private const val K_REMINDER = "event_reminder"
        private const val K_REMINDER_MIN = "event_reminder_minutes"
        private const val K_TITLE = "title_template"
        private const val K_DAILY = "daily_notification"
        private const val K_DAILY_TIME = "daily_notification_time"
        private const val K_LAST_AT = "last_sync_at"
        private const val K_LAST_OK = "last_sync_ok"
        private const val K_LAST_MSG = "last_sync_message"
        private const val K_LAST_DAILY = "last_daily_notification"
        private const val K_ASKED = "asked_permissions"
        private const val K_ONBOARDING = "onboarding_done"

        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}

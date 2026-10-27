package io.github.brokoskokoli.birthdaysync.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.brokoskokoli.birthdaysync.data.AppSettings
import io.github.brokoskokoli.birthdaysync.data.CalendarInfo
import io.github.brokoskokoli.birthdaysync.data.CalendarRepository
import io.github.brokoskokoli.birthdaysync.data.ContactRepository
import io.github.brokoskokoli.birthdaysync.data.ContactSource
import io.github.brokoskokoli.birthdaysync.data.SettingsStore
import io.github.brokoskokoli.birthdaysync.notify.DailyReminder
import io.github.brokoskokoli.birthdaysync.notify.Notifications
import io.github.brokoskokoli.birthdaysync.sync.BirthdayPlanner
import io.github.brokoskokoli.birthdaysync.sync.BirthdaySync
import io.github.brokoskokoli.birthdaysync.sync.Occurrence
import io.github.brokoskokoli.birthdaysync.sync.SyncScheduler
import io.github.brokoskokoli.birthdaysync.system.Perms
import io.github.brokoskokoli.birthdaysync.system.SystemCheck
import io.github.brokoskokoli.birthdaysync.system.SystemChecks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()
    private val store = SettingsStore.get(app)
    private val workManager = WorkManager.getInstance(app)

    val settings = store.settings
    val status = store.status
    val onboardingDone = store.onboardingDone

    private val _checks = MutableStateFlow<List<SystemCheck>>(emptyList())
    private val _sources = MutableStateFlow<List<ContactSource>>(emptyList())
    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    private val _upcoming = MutableStateFlow<List<Occurrence>>(emptyList())
    private val _hasContacts = MutableStateFlow(Perms.contacts(app))
    private val _hasCalendar = MutableStateFlow(Perms.calendar(app))
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val checks: StateFlow<List<SystemCheck>> = _checks.asStateFlow()
    val sources: StateFlow<List<ContactSource>> = _sources.asStateFlow()
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()
    val upcoming: StateFlow<List<Occurrence>> = _upcoming.asStateFlow()
    val hasContacts: StateFlow<Boolean> = _hasContacts.asStateFlow()
    val hasCalendar: StateFlow<Boolean> = _hasCalendar.asStateFlow()
    val messages = _messages.asSharedFlow()

    val syncRunning: StateFlow<Boolean> = workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.ONE_TIME)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Zeitpunkt des nächsten automatischen Syncs (0 = nicht geplant). */
    val nextPeriodicSync: StateFlow<Long> = workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.PERIODIC)
        .map { infos -> infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }?.nextScheduleTimeMillis ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** Liest Berechtigungen, Kontaktlisten, Kalender und anstehende Geburtstage neu ein. */
    suspend fun refreshNow() = withContext(Dispatchers.IO) {
        _checks.value = SystemChecks.evaluate(ctx)
        _hasContacts.value = Perms.contacts(ctx)
        _hasCalendar.value = Perms.calendar(ctx)
        _sources.value = if (_hasContacts.value) runCatching { ContactRepository(ctx).loadSources() }.getOrDefault(emptyList()) else emptyList()
        _calendars.value = if (_hasCalendar.value) runCatching { CalendarRepository(ctx).writableCalendars() }.getOrDefault(emptyList()) else emptyList()
        refreshUpcoming()
    }

    private fun refreshUpcoming() {
        _upcoming.value = if (_hasContacts.value) {
            val birthdays = runCatching { ContactRepository(ctx).loadBirthdays(settings.value.selectedSources) }.getOrDefault(emptyList())
            BirthdayPlanner.occurrences(birthdays, LocalDate.now(), 12)
        } else emptyList()
    }

    fun setOnboardingDone(done: Boolean) = store.setOnboardingDone(done)

    fun update(block: (AppSettings) -> AppSettings) = store.update(block)

    fun setSourceSelected(id: String, selected: Boolean) {
        store.update { it.copy(selectedSources = if (selected) it.selectedSources + id else it.selectedSources - id) }
        viewModelScope.launch(Dispatchers.IO) { refreshUpcoming() }
    }

    fun syncNow() = SyncScheduler.syncNow(ctx)

    fun createLocalCalendar() = viewModelScope.launch(Dispatchers.IO) {
        runCatching { CalendarRepository(ctx).ensureLocalCalendar() }
            .onSuccess { id ->
                store.update { it.copy(calendarId = id) }
                _calendars.value = CalendarRepository(ctx).writableCalendars()
                _messages.tryEmit("Kalender \"Geburtstage\" ist als Ziel ausgewählt")
            }
            .onFailure { _messages.tryEmit("Kalender konnte nicht angelegt werden: ${it.message}") }
    }

    fun removeAllEvents() = viewModelScope.launch(Dispatchers.IO) {
        val n = runCatching { BirthdaySync(ctx).removeAll() }.getOrElse {
            _messages.tryEmit("Fehler beim Entfernen: ${it.message}"); return@launch
        }
        _messages.tryEmit("$n Termine entfernt")
    }

    fun testNotification() = viewModelScope.launch(Dispatchers.IO) {
        val shown = Notifications.showToday(ctx, DailyReminder.todaysBirthdays(ctx), test = true)
        if (!shown) _messages.tryEmit("Benachrichtigungen sind nicht erlaubt – siehe Tab \"System\"")
    }
}

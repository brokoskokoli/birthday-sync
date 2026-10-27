package io.github.brokoskokoli.birthdaysync

import android.app.Application
import io.github.brokoskokoli.birthdaysync.data.SettingsStore
import io.github.brokoskokoli.birthdaysync.notify.DailyReminder
import io.github.brokoskokoli.birthdaysync.notify.Notifications
import io.github.brokoskokoli.birthdaysync.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class App : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        val settings = SettingsStore.get(this).settings

        // Planung folgt den Einstellungen: beim Start einmal anwenden, danach bei jeder Änderung.
        scope.launch {
            settings.map { it.autoSyncEnabled to it.syncIntervalDays }.distinctUntilChanged()
                .collect { SyncScheduler.schedulePeriodic(this@App, settings.value) }
        }
        scope.launch {
            settings.map { it.dailyNotificationEnabled to it.dailyNotificationMinuteOfDay }.distinctUntilChanged()
                .collect { DailyReminder.schedule(this@App) }
        }
        scope.launch {
            settings.map { it.eventRelevant() }.distinctUntilChanged().drop(1)
                .collect { SyncScheduler.syncSoon(this@App) }
        }
    }
}

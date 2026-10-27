package io.github.brokoskokoli.birthdaysync.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.brokoskokoli.birthdaysync.data.ContactRepository
import io.github.brokoskokoli.birthdaysync.data.SettingsStore
import io.github.brokoskokoli.birthdaysync.sync.BirthdayPlanner
import io.github.brokoskokoli.birthdaysync.sync.Occurrence
import io.github.brokoskokoli.birthdaysync.system.Perms
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.concurrent.thread

/** Plant die tägliche Benachrichtigung per AlarmManager (pünktlich auch im Doze-Modus). */
object DailyReminder {

    /**
     * Plant den nächsten Termin. Mit [catchUp] (nach dem Neustart) wird eine heute verpasste
     * Benachrichtigung in einer Minute nachgeholt.
     */
    fun schedule(context: Context, catchUp: Boolean = false) {
        val store = SettingsStore.get(context)
        val settings = store.settings.value
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        alarm.cancel(pending)
        if (!settings.dailyNotificationEnabled) return

        val now = ZonedDateTime.now()
        val todayAt = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(settings.dailyNotificationMinuteOfDay.toLong())
        val missedToday = todayAt.isBefore(now) && store.lastDailyNotificationDate != now.toLocalDate().toString()
        val trigger = when {
            todayAt.isAfter(now) -> todayAt
            catchUp && missedToday -> now.plusMinutes(1)
            else -> todayAt.plusDays(1)
        }.toInstant().toEpochMilli()

        val exact = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
        if (exact) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    fun todaysBirthdays(context: Context): List<Occurrence> {
        if (!Perms.contacts(context)) return emptyList()
        val sources = SettingsStore.get(context).settings.value.selectedSources
        return BirthdayPlanner.on(ContactRepository(context).loadBirthdays(sources), LocalDate.now())
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, DailyReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        thread {
            try {
                val store = SettingsStore.get(context)
                val today = LocalDate.now().toString()
                if (store.settings.value.dailyNotificationEnabled && store.lastDailyNotificationDate != today) {
                    Notifications.showToday(context, DailyReminder.todaysBirthdays(context))
                    store.lastDailyNotificationDate = today
                }
                DailyReminder.schedule(context)
            } finally {
                result.finish()
            }
        }
    }
}

/** Neustart, App-Update, Zeit-/Zeitzonenwechsel, Änderung der Wecker-Berechtigung → Alarm neu planen. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyReminder.schedule(context, catchUp = intent.action == Intent.ACTION_BOOT_COMPLETED)
    }
}

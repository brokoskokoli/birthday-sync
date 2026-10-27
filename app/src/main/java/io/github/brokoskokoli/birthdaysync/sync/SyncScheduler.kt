package io.github.brokoskokoli.birthdaysync.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.brokoskokoli.birthdaysync.data.AppSettings
import java.util.concurrent.TimeUnit

object SyncScheduler {
    const val PERIODIC = "birthday-sync-periodic"
    const val ONE_TIME = "birthday-sync-now"

    fun schedulePeriodic(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (!settings.autoSyncEnabled) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncWorker>(settings.syncIntervalDays.toLong(), TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun syncNow(context: Context) = enqueueOnce(context, 0)

    /** Nach Änderungen an den Einstellungen; mehrere Änderungen kurz hintereinander ergeben einen Sync. */
    fun syncSoon(context: Context) = enqueueOnce(context, 10)

    private fun enqueueOnce(context: Context, delaySeconds: Long) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.REPLACE, request)
    }
}

package io.github.brokoskokoli.birthdaysync.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.brokoskokoli.birthdaysync.data.SettingsStore
import io.github.brokoskokoli.birthdaysync.data.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val store = SettingsStore.get(applicationContext)
        val outcome = try {
            BirthdaySync(applicationContext).run(store.settings.value)
        } catch (e: Exception) {
            SyncOutcome(false, "Fehler: ${e.message ?: e.javaClass.simpleName}")
        }
        if (!outcome.skipped) store.saveStatus(SyncStatus(System.currentTimeMillis(), outcome.ok, outcome.message))
        // Kein Retry: Fehler sind fast immer Konfiguration (Berechtigung, Kalender) und der nächste Lauf kommt ohnehin.
        Result.success()
    }
}

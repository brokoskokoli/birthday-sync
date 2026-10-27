package io.github.brokoskokoli.birthdaysync.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.brokoskokoli.birthdaysync.data.SettingsStore
import io.github.brokoskokoli.birthdaysync.system.CheckId
import io.github.brokoskokoli.birthdaysync.system.Perms
import io.github.brokoskokoli.birthdaysync.system.SystemChecks

/**
 * Liefert eine Funktion, die zu einer Prüfung die passende Aktion auslöst: Laufzeitberechtigung anfragen
 * oder die zugehörige Systemeinstellung öffnen. Das Ergebnis wird beim nächsten ON_RESUME neu eingelesen.
 */
@Composable
fun rememberCheckAction(): (CheckId) -> Unit {
    val context = LocalContext.current
    val activity = context as Activity
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Auswertung erfolgt beim anschließenden ON_RESUME
    }

    fun openSettings(id: CheckId) {
        for (intent in SystemChecks.intentsFor(context, id)) {
            // Im eigenen Task starten, damit "Zurück" in die App führt.
            if (runCatching { activity.startActivity(intent) }.isSuccess) return
        }
    }

    fun requestRuntime(id: CheckId, permissions: List<String>) {
        val store = SettingsStore.get(context)
        val missing = permissions.filterNot { Perms.has(context, listOf(it)) }
        if (missing.isEmpty()) { openSettings(id); return }
        // Schon einmal gefragt und keine Begründung mehr möglich → dauerhaft abgelehnt → App-Einstellungen
        val permanentlyDenied = missing.all { store.wasAsked(it) && !activity.shouldShowRequestPermissionRationale(it) }
        if (permanentlyDenied) openSettings(id)
        else {
            store.markAsked(missing)
            launcher.launch(missing.toTypedArray())
        }
    }

    return { id ->
        when (id) {
            CheckId.CONTACTS -> requestRuntime(id, Perms.CONTACTS)
            CheckId.CALENDAR -> requestRuntime(id, Perms.CALENDAR)
            CheckId.NOTIFICATIONS -> requestRuntime(id, Perms.NOTIFICATIONS)
            else -> openSettings(id)
        }
    }
}

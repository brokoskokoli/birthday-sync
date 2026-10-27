package io.github.brokoskokoli.birthdaysync.system

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.concurrent.futures.await
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants

enum class CheckId {
    CONTACTS, CALENDAR, NOTIFICATIONS, EXACT_ALARM, BATTERY_OPTIMIZATION,
    BACKGROUND_RESTRICTION, APP_HIBERNATION, POWER_SAVE, VENDOR_AUTOSTART,
}

enum class Severity { REQUIRED, RECOMMENDED, INFO }

data class SystemCheck(
    val id: CheckId,
    val title: String,
    val description: String,
    val ok: Boolean,
    val severity: Severity,
    val actionLabel: String,
)

/** Prüft alles, was die App für regelmäßige Syncs und pünktliche Benachrichtigungen braucht. */
object SystemChecks {

    suspend fun evaluate(context: Context): List<SystemCheck> {
        val pkg = context.packageName
        val power = context.getSystemService(PowerManager::class.java)
        val list = mutableListOf<SystemCheck>()

        list += SystemCheck(
            CheckId.CONTACTS, "Kontakte lesen",
            "Nötig, um Geburtstage aus den Kontaktlisten zu lesen.",
            Perms.contacts(context), Severity.REQUIRED, "Erlauben",
        )
        list += SystemCheck(
            CheckId.CALENDAR, "Kalender lesen und schreiben",
            "Nötig, um Geburtstagstermine im Zielkalender anzulegen und aktuell zu halten.",
            Perms.calendar(context), Severity.REQUIRED, "Erlauben",
        )
        list += SystemCheck(
            CheckId.NOTIFICATIONS, "Benachrichtigungen",
            "Nötig für die tägliche Geburtstags-Benachrichtigung und Kalendererinnerungen.",
            Perms.notifications(context), Severity.REQUIRED, "Erlauben",
        )
        if (Build.VERSION.SDK_INT >= 31) {
            list += SystemCheck(
                CheckId.EXACT_ALARM, "Exakte Wecker und Erinnerungen",
                "Damit die tägliche Benachrichtigung genau zur eingestellten Uhrzeit kommt. Ohne Freigabe kann sie sich etwas verspäten.",
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
                Severity.RECOMMENDED, "Öffnen",
            )
        }
        list += SystemCheck(
            CheckId.BATTERY_OPTIMIZATION, "Akku-Optimierung deaktivieren",
            "Verhindert, dass Android den Hintergrund-Sync verschiebt oder unterdrückt.",
            power.isIgnoringBatteryOptimizations(pkg), Severity.RECOMMENDED, "Öffnen",
        )
        if (Build.VERSION.SDK_INT >= 28) {
            val restricted = context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
            list += SystemCheck(
                CheckId.BACKGROUND_RESTRICTION, "Hintergrundaktivität erlaubt",
                "Die App darf im Hintergrund nicht eingeschränkt sein (App-Info → Akku → \"Nicht eingeschränkt\").",
                !restricted, Severity.REQUIRED, "App-Info öffnen",
            )
        }
        hibernationCheck(context)?.let { list += it }
        list += SystemCheck(
            CheckId.POWER_SAVE, "Energiesparmodus",
            if (power.isPowerSaveMode) "Der Energiesparmodus ist aktiv und kann Syncs verzögern."
            else "Der Energiesparmodus ist aus.",
            !power.isPowerSaveMode, Severity.INFO, "Einstellungen",
        )
        if (vendorAutostartIntents().isNotEmpty()) {
            list += SystemCheck(
                CheckId.VENDOR_AUTOSTART, "Hersteller-Akkuverwaltung (${Build.MANUFACTURER})",
                "Dieser Hersteller beendet Hintergrund-Apps oft zusätzlich. Bitte Autostart bzw. " +
                    "\"Im Hintergrund aktiv lassen\" für Birthday Sync erlauben. Der Status lässt sich nicht automatisch prüfen.",
                true, Severity.INFO, "Öffnen",
            )
        }
        return list
    }

    private suspend fun hibernationCheck(context: Context): SystemCheck? {
        val status = runCatching { PackageManagerCompat.getUnusedAppRestrictionsStatus(context).await() }
            .getOrNull() ?: return null
        if (status == UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE || status == UnusedAppRestrictionsConstants.ERROR) return null
        return SystemCheck(
            CheckId.APP_HIBERNATION, "App-Pausierung bei Nichtnutzung aus",
            "Sonst entzieht Android nach einigen Monaten ohne Öffnen die Berechtigungen und stoppt den Sync.",
            status == UnusedAppRestrictionsConstants.DISABLED, Severity.RECOMMENDED, "Öffnen",
        )
    }

    /** Einstellungsseiten für eine Prüfung, in absteigender Präferenz (die erste startbare gewinnt). */
    fun intentsFor(context: Context, id: CheckId): List<Intent> {
        val pkg = context.packageName
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        return when (id) {
            CheckId.CONTACTS, CheckId.CALENDAR, CheckId.BACKGROUND_RESTRICTION -> listOf(details)
            CheckId.NOTIFICATIONS -> listOf(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
                details,
            )
            CheckId.EXACT_ALARM ->
                if (Build.VERSION.SDK_INT >= 31) listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg")), details)
                else listOf(details)
            CheckId.BATTERY_OPTIMIZATION -> listOf(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                details,
            )
            CheckId.APP_HIBERNATION -> listOf(IntentCompat.createManageUnusedAppRestrictionsIntent(context, pkg), details)
            CheckId.POWER_SAVE -> listOf(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            CheckId.VENDOR_AUTOSTART -> vendorAutostartIntents() + details
        }
    }

    private fun vendorAutostartIntents(): List<Intent> {
        val components = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> listOf(
                "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            )
            "huawei", "honor" -> listOf(
                "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
            )
            "oppo", "realme" -> listOf(
                "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
                "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            )
            "vivo", "iqoo" -> listOf(
                "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            )
            "oneplus" -> listOf(
                "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
            )
            "samsung" -> listOf(
                "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
            )
            else -> emptyList()
        }
        return components.map { (p, c) -> Intent().setComponent(ComponentName(p, c)) }
    }
}

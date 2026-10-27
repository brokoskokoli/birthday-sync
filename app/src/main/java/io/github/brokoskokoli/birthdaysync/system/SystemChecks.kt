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
import io.github.brokoskokoli.birthdaysync.R

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
        val s = { id: Int -> context.getString(id) }

        list += SystemCheck(
            CheckId.CONTACTS, s(R.string.check_contacts_title), s(R.string.check_contacts_desc),
            Perms.contacts(context), Severity.REQUIRED, s(R.string.action_allow),
        )
        list += SystemCheck(
            CheckId.CALENDAR, s(R.string.check_calendar_title), s(R.string.check_calendar_desc),
            Perms.calendar(context), Severity.REQUIRED, s(R.string.action_allow),
        )
        list += SystemCheck(
            CheckId.NOTIFICATIONS, s(R.string.check_notifications_title), s(R.string.check_notifications_desc),
            Perms.notifications(context), Severity.REQUIRED, s(R.string.action_allow),
        )
        if (Build.VERSION.SDK_INT >= 31) {
            list += SystemCheck(
                CheckId.EXACT_ALARM, s(R.string.check_exact_alarm_title), s(R.string.check_exact_alarm_desc),
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
                Severity.RECOMMENDED, s(R.string.action_open),
            )
        }
        list += SystemCheck(
            CheckId.BATTERY_OPTIMIZATION, s(R.string.check_battery_title), s(R.string.check_battery_desc),
            power.isIgnoringBatteryOptimizations(pkg), Severity.RECOMMENDED, s(R.string.action_open),
        )
        if (Build.VERSION.SDK_INT >= 28) {
            val restricted = context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
            list += SystemCheck(
                CheckId.BACKGROUND_RESTRICTION, s(R.string.check_background_title), s(R.string.check_background_desc),
                !restricted, Severity.REQUIRED, s(R.string.action_open_app_info),
            )
        }
        hibernationCheck(context)?.let { list += it }
        list += SystemCheck(
            CheckId.POWER_SAVE, s(R.string.check_power_save_title),
            s(if (power.isPowerSaveMode) R.string.check_power_save_on else R.string.check_power_save_off),
            !power.isPowerSaveMode, Severity.INFO, s(R.string.action_settings),
        )
        if (vendorAutostartIntents().isNotEmpty()) {
            list += SystemCheck(
                CheckId.VENDOR_AUTOSTART, context.getString(R.string.check_vendor_title, Build.MANUFACTURER),
                s(R.string.check_vendor_desc), true, Severity.INFO, s(R.string.action_open),
            )
        }
        return list
    }

    private suspend fun hibernationCheck(context: Context): SystemCheck? {
        val status = runCatching { PackageManagerCompat.getUnusedAppRestrictionsStatus(context).await() }
            .getOrNull() ?: return null
        if (status == UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE || status == UnusedAppRestrictionsConstants.ERROR) return null
        return SystemCheck(
            CheckId.APP_HIBERNATION, context.getString(R.string.check_hibernation_title),
            context.getString(R.string.check_hibernation_desc),
            status == UnusedAppRestrictionsConstants.DISABLED, Severity.RECOMMENDED, context.getString(R.string.action_open),
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

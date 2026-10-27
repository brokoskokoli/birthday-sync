package io.github.brokoskokoli.birthdaysync.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Perms {
    val CONTACTS = listOf(Manifest.permission.READ_CONTACTS)
    val CALENDAR = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    val NOTIFICATIONS =
        if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()

    fun has(context: Context, permissions: List<String>) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun contacts(context: Context) = has(context, CONTACTS)
    fun calendar(context: Context) = has(context, CALENDAR)
    fun notifications(context: Context) =
        has(context, NOTIFICATIONS) && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

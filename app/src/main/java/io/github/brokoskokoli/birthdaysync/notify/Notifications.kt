package io.github.brokoskokoli.birthdaysync.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.brokoskokoli.birthdaysync.R
import io.github.brokoskokoli.birthdaysync.sync.Occurrence
import io.github.brokoskokoli.birthdaysync.system.Perms
import io.github.brokoskokoli.birthdaysync.ui.MainActivity

object Notifications {
    private const val CHANNEL_TODAY = "birthdays_today"
    private const val ID_TODAY = 1

    fun createChannels(context: Context) {
        val channel = NotificationChannel(CHANNEL_TODAY, "Geburtstage heute", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Tägliche Benachrichtigung über die heutigen Geburtstage" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun describe(o: Occurrence): String =
        if (o.age != null) "${o.birthday.name} wird heute ${o.age}" else "${o.birthday.name} hat heute Geburtstag"

    /** Zeigt die heutigen Geburtstage; [test] erzeugt bei leerer Liste einen Hinweis statt nichts. */
    @SuppressLint("MissingPermission")
    fun showToday(context: Context, today: List<Occurrence>, test: Boolean = false): Boolean {
        if (!Perms.notifications(context)) return false
        if (today.isEmpty() && !test) return false

        val single = today.singleOrNull()
        val title = when {
            today.isEmpty() -> "Heute hat niemand Geburtstag"
            single != null -> "🎂 ${single.birthday.name} hat Geburtstag"
            else -> "🎂 ${today.size} Geburtstage heute"
        }
        val text = when {
            today.isEmpty() -> "Test-Benachrichtigung von Birthday Sync"
            single != null -> describe(single)
            else -> today.joinToString(", ") { it.birthday.name }
        }
        val target = if (single != null) {
            Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(single.birthday.contactId, single.birthday.lookupKey))
        } else {
            Intent(context, MainActivity::class.java)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context, 0, target, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle().also { s -> today.forEach { s.addLine(describe(it)) } }

        val notification = NotificationCompat.Builder(context, CHANNEL_TODAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(if (today.size > 1) style else NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()
        NotificationManagerCompat.from(context).notify(ID_TODAY, notification)
        return true
    }
}

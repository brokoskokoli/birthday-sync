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
import io.github.brokoskokoli.birthdaysync.ui.formatList

object Notifications {
    private const val CHANNEL_TODAY = "birthdays_today"
    private const val ID_TODAY = 1

    fun createChannels(context: Context) {
        // Bei jedem Start neu angelegt: Name und Beschreibung folgen so der aktuellen Sprache.
        val channel = NotificationChannel(CHANNEL_TODAY, context.getString(R.string.channel_today_name), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_today_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun describe(context: Context, o: Occurrence): String =
        if (o.age != null) context.resources.getQuantityString(R.plurals.notification_turns_today, o.age, o.birthday.name, o.age)
        else context.getString(R.string.notification_birthday_today, o.birthday.name)

    /** Zeigt die heutigen Geburtstage; [test] erzeugt bei leerer Liste einen Hinweis statt nichts. */
    @SuppressLint("MissingPermission")
    fun showToday(context: Context, today: List<Occurrence>, test: Boolean = false): Boolean {
        if (!Perms.notifications(context)) return false
        if (today.isEmpty() && !test) return false

        val single = today.singleOrNull()
        val title = when {
            today.isEmpty() -> context.getString(R.string.notification_nobody_today)
            single != null -> context.getString(R.string.notification_single_title, single.birthday.name)
            else -> context.resources.getQuantityString(R.plurals.notification_multi_title, today.size, today.size)
        }
        val text = when {
            today.isEmpty() -> context.getString(R.string.notification_test_text)
            single != null -> describe(context, single)
            else -> formatList(context, today.map { it.birthday.name })
        }
        val target = if (single != null) {
            Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(single.birthday.contactId, single.birthday.lookupKey))
        } else {
            Intent(context, MainActivity::class.java)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context, 0, target, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle().also { s -> today.forEach { s.addLine(describe(context, it)) } }

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

package io.github.brokoskokoli.birthdaysync.ui

import android.content.Context
import android.icu.text.DateFormat
import android.icu.text.ListFormatter
import android.icu.util.TimeZone
import io.github.brokoskokoli.birthdaysync.R
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

// Formatierung nach Sprache der App und 12/24-Stunden-Einstellung des Systems (ICU-Skelette statt fester Muster).

private fun Context.locale(): Locale = resources.configuration.locales[0]

private fun Context.timeSkeleton() = if (android.text.format.DateFormat.is24HourFormat(this)) "Hm" else "hm"

private fun Context.format(skeleton: String, millis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    DateFormat.getInstanceForSkeleton(skeleton, locale()).apply { timeZone = zone }.format(Date(millis))

private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

/** Uhrzeit, z. B. "08:00" bzw. "8:00 AM". */
fun formatTime(context: Context, minuteOfDay: Int): String = context.format(context.timeSkeleton(), minuteOfDay * 60_000L, UTC)

/** Wochentag, Datum und Uhrzeit, z. B. "Di., 27.10.2026, 08:00". */
fun formatInstant(context: Context, millis: Long): String = context.format("EEEyMMdd" + context.timeSkeleton(), millis)

/** Wochentag und Datum ohne Jahr, z. B. "Di., 27.10.". */
fun formatDay(context: Context, date: LocalDate): String =
    context.format("EEEddMM", date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), UTC)

/** Aufzählung nach den Regeln der Sprache, z. B. "A, B und C". */
fun formatList(context: Context, items: List<String>): String = ListFormatter.getInstance(context.locale()).format(items)

fun formatDuration(context: Context, minutes: Int): String {
    val r = context.resources
    return when {
        minutes < 60 -> r.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
        minutes % 60 == 0 -> r.getQuantityString(R.plurals.duration_hours, minutes / 60, minutes / 60)
        else -> r.getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }
}

/** Erinnerung für Termine mit Uhrzeit: [minutes] vor Beginn. */
fun formatTimedReminder(context: Context, minutes: Int): String {
    val r = context.resources
    return when {
        minutes == 0 -> r.getString(R.string.reminder_at_start)
        minutes % WEEK == 0 -> r.getQuantityString(R.plurals.reminder_weeks_before, minutes / WEEK, minutes / WEEK)
        minutes % DAY == 0 -> r.getQuantityString(R.plurals.reminder_days_before, minutes / DAY, minutes / DAY)
        minutes % 60 == 0 -> r.getQuantityString(R.plurals.reminder_hours_before, minutes / 60, minutes / 60)
        else -> r.getQuantityString(R.plurals.reminder_minutes_before, minutes, minutes)
    }
}

/**
 * Erinnerung für ganztägige Termine: [minutes] vor Mitternacht des Termintags,
 * ausgedrückt als Tag und Uhrzeit (z. B. 900 → "Am Vortag um 09:00").
 */
fun formatAllDayReminder(context: Context, minutes: Int): String {
    val r = context.resources
    val days = (minutes + DAY - 1) / DAY
    val time = formatTime(context, days * DAY - minutes)
    return when {
        days == 0 -> r.getString(R.string.reminder_on_day_at, time)
        days == 1 -> r.getString(R.string.reminder_day_before_at, time)
        days % 7 == 0 -> r.getQuantityString(R.plurals.reminder_weeks_before_at, days / 7, days / 7, time)
        else -> r.getQuantityString(R.plurals.reminder_days_before_at, days, days, time)
    }
}

private const val DAY = 24 * 60
private const val WEEK = 7 * DAY

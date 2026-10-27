package io.github.brokoskokoli.birthdaysync.sync

import io.github.brokoskokoli.birthdaysync.data.Birthday
import java.security.MessageDigest
import java.time.LocalDate

/** Ein konkreter Geburtstagstermin an einem Datum. */
data class Occurrence(
    val birthday: Birthday,
    val date: LocalDate,
    val age: Int?,
) {
    /**
     * Identität des Kalendertermins: Kontakt + Datum. Ändert sich das Geburtsdatum eines Kontakts,
     * entsteht ein neuer Schlüssel; der alte Termin wird dann beim Sync gelöscht.
     */
    val key: String get() = BirthdayPlanner.key(birthday.lookupKey, date)
}

object BirthdayPlanner {

    /** Alle Geburtstage im Zeitraum [today, today + months). */
    fun occurrences(birthdays: List<Birthday>, today: LocalDate, months: Int): List<Occurrence> {
        val end = today.plusMonths(months.toLong())
        val result = mutableListOf<Occurrence>()
        for (b in birthdays) {
            for (y in today.year..end.year) {
                val d = b.date.inYear(y)
                if (!d.isBefore(today) && d.isBefore(end)) result += Occurrence(b, d, b.date.ageOn(d))
            }
        }
        return result.sortedWith(compareBy({ it.date }, { it.birthday.name }))
    }

    fun on(birthdays: List<Birthday>, date: LocalDate): List<Occurrence> =
        birthdays.filter { it.date.inYear(date.year) == date }
            .map { Occurrence(it, date, it.date.ageOn(date)) }
            .sortedBy { it.birthday.name }

    fun key(lookupKey: String, date: LocalDate): String = "${sha1(lookupKey).take(16)}_$date"

    fun dateOfKey(key: String): LocalDate? =
        runCatching { LocalDate.parse(key.substringAfterLast('_')) }.getOrNull()

    /**
     * Setzt den Titel aus der Vorlage zusammen. Platzhalter: {name}, {age} ({alter} als Alias für ältere Vorlagen).
     * Ist das Alter unbekannt, entfällt eine Klammer, die den Alters-Platzhalter enthält, z. B. " ({age})".
     */
    fun renderTitle(template: String, name: String, age: Int?): String {
        var t = template.ifBlank { DEFAULT_TITLE }.replace(AGE_ALIAS, AGE)
        if (age == null) {
            t = t.replace(Regex("""\s*\([^()]*\{age\}[^()]*\)"""), "").replace(AGE, "")
        }
        return t.replace("{name}", name).replace(AGE, age?.toString().orEmpty()).trim()
    }

    private const val AGE = "{age}"
    private const val AGE_ALIAS = "{alter}"
    const val DEFAULT_TITLE = "🎂 {name} ({age})"

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}

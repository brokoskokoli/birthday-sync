package io.github.brokoskokoli.birthdaysync.data

import java.time.LocalDate
import java.time.Year

/** Geburtsdatum ohne Zeitzone; das Jahr ist optional (viele Kontakte speichern nur Tag und Monat). */
data class BirthDate(val month: Int, val day: Int, val year: Int?) {

    /** Datum des Geburtstags im angegebenen Jahr; der 29.02. fällt in Nicht-Schaltjahren auf den 28.02. */
    fun inYear(y: Int): LocalDate =
        if (month == 2 && day == 29 && !Year.isLeap(y.toLong())) LocalDate.of(y, 2, 28)
        else LocalDate.of(y, month, day)

    fun ageOn(date: LocalDate): Int? = year?.let { date.year - it }?.takeIf { it >= 0 }

    companion object {
        private val ISO = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""")
        private val NO_YEAR = Regex("""^--(\d{1,2})-?(\d{1,2})$""")
        private val COMPACT = Regex("""^(\d{4})(\d{2})(\d{2})$""")
        private val DOTTED = Regex("""^(\d{1,2})\.(\d{1,2})\.?(\d{4})?$""")

        /** Erkennt die üblichen Formate aus Kontakt-Apps: 1990-05-12, --05-12, 19900512, 12.05.1990, 12.05. */
        fun parse(raw: String?, maxYear: Int = LocalDate.now().year): BirthDate? {
            val s = raw?.trim().orEmpty()
            if (s.isEmpty()) return null
            ISO.find(s)?.let { m -> return of(m.groupValues[2], m.groupValues[3], m.groupValues[1], maxYear) }
            NO_YEAR.find(s)?.let { m -> return of(m.groupValues[1], m.groupValues[2], null, maxYear) }
            COMPACT.find(s)?.let { m -> return of(m.groupValues[2], m.groupValues[3], m.groupValues[1], maxYear) }
            DOTTED.find(s)?.let { m -> return of(m.groupValues[2], m.groupValues[1], m.groupValues[3].ifEmpty { null }, maxYear) }
            return null
        }

        private fun of(month: String, day: String, year: String?, maxYear: Int): BirthDate? {
            val mo = month.toInt()
            val d = day.toInt()
            if (mo !in 1..12) return null
            // 2000 ist ein Schaltjahr, damit ist der 29.02. gültig
            if (d !in 1..LocalDate.of(2000, mo, 1).lengthOfMonth()) return null
            // Platzhalterjahre (z. B. 1604 bei iOS, 0000) als "Jahr unbekannt" behandeln
            val y = year?.toInt()?.takeIf { it in 1800..maxYear }
            if (y != null && mo == 2 && d == 29 && !Year.isLeap(y.toLong())) return null
            return BirthDate(mo, d, y)
        }
    }
}

/** Ein Kontakt mit Geburtstag. [lookupKey] ist der stabile Schlüssel des Android-Kontakts. */
data class Birthday(
    val contactId: Long,
    val lookupKey: String,
    val name: String,
    val date: BirthDate,
)

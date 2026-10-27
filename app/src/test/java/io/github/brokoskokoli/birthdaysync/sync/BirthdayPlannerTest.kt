package io.github.brokoskokoli.birthdaysync.sync

import io.github.brokoskokoli.birthdaysync.data.BirthDate
import io.github.brokoskokoli.birthdaysync.data.Birthday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class BirthdayPlannerTest {

    private val today = LocalDate.of(2026, 10, 27)
    private fun b(name: String, date: BirthDate) = Birthday(1, "lk-$name", name, date)

    @Test fun includesTodayAndExcludesEnd() {
        val list = listOf(b("Heute", BirthDate(10, 27, 1990)), b("Gestern", BirthDate(10, 26, null)))
        val occ = BirthdayPlanner.occurrences(list, today, 12)
        // "Heute" nur einmal (27.10.2027 liegt am exklusiven Ende), "Gestern" erst wieder 2027
        assertEquals(listOf(LocalDate.of(2026, 10, 27), LocalDate.of(2027, 10, 26)), occ.map { it.date })
        assertEquals(36, occ.first().age)
    }

    @Test fun shortRange() {
        val list = listOf(b("A", BirthDate(11, 5, null)), b("B", BirthDate(1, 5, null)))
        assertEquals(listOf("A"), BirthdayPlanner.occurrences(list, today, 2).map { it.birthday.name })
    }

    @Test fun keyChangesWhenDateChanges() {
        val k1 = BirthdayPlanner.key("lk", LocalDate.of(2027, 3, 1))
        val k2 = BirthdayPlanner.key("lk", LocalDate.of(2027, 3, 2))
        assertNotEquals(k1, k2)
        assertEquals(LocalDate.of(2027, 3, 1), BirthdayPlanner.dateOfKey(k1))
    }

    @Test fun titleWithAndWithoutAge() {
        assertEquals("🎂 Anna (40)", BirthdayPlanner.renderTitle("🎂 {name} ({age})", "Anna", 40))
        assertEquals("🎂 Anna", BirthdayPlanner.renderTitle("🎂 {name} ({age})", "Anna", null))
        assertEquals("Anna turns 40", BirthdayPlanner.renderTitle("{name} turns {age}", "Anna", 40))
        assertEquals("Birthday Anna", BirthdayPlanner.renderTitle("Birthday {name} (turns {age})", "Anna", null))
    }

    @Test fun defaultTitle() {
        assertEquals("🎂 Anna (40)", BirthdayPlanner.renderTitle("", "Anna", 40))
        assertEquals("🎂 Anna", BirthdayPlanner.renderTitle(BirthdayPlanner.DEFAULT_TITLE, "Anna", null))
    }

    /** Gespeicherte Vorlagen aus älteren Versionen nutzen noch den deutschen Platzhalter {alter}. */
    @Test fun legacyAgeAlias() {
        assertEquals("🎂 Anna (40)", BirthdayPlanner.renderTitle("🎂 {name} ({alter})", "Anna", 40))
        assertEquals("🎂 Anna", BirthdayPlanner.renderTitle("🎂 {name} ({alter})", "Anna", null))
        assertEquals("Anna wird 40", BirthdayPlanner.renderTitle("{name} wird {alter}", "Anna", 40))
        assertEquals("Geburtstag Anna", BirthdayPlanner.renderTitle("Geburtstag {name} (wird {alter})", "Anna", null))
        assertEquals("Anna 40/40", BirthdayPlanner.renderTitle("{name} {alter}/{age}", "Anna", 40))
    }
}

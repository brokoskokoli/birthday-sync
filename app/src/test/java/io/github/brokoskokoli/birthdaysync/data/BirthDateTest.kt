package io.github.brokoskokoli.birthdaysync.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BirthDateTest {

    @Test fun parsesIso() = assertEquals(BirthDate(5, 12, 1990), BirthDate.parse("1990-05-12", 2026))
    @Test fun parsesIsoWithTime() = assertEquals(BirthDate(5, 12, 1990), BirthDate.parse("1990-05-12T00:00:00.000Z", 2026))
    @Test fun parsesWithoutYear() = assertEquals(BirthDate(5, 12, null), BirthDate.parse("--05-12", 2026))
    @Test fun parsesCompact() = assertEquals(BirthDate(5, 12, 1990), BirthDate.parse("19900512", 2026))
    @Test fun parsesGermanFormat() = assertEquals(BirthDate(5, 12, 1990), BirthDate.parse("12.05.1990", 2026))
    @Test fun parsesGermanFormatWithoutYear() = assertEquals(BirthDate(5, 12, null), BirthDate.parse("12.05.", 2026))
    @Test fun placeholderYearIsUnknown() = assertEquals(BirthDate(5, 12, null), BirthDate.parse("1604-05-12", 2026))
    @Test fun futureYearIsUnknown() = assertEquals(BirthDate(5, 12, null), BirthDate.parse("2030-05-12", 2026))
    @Test fun rejectsInvalid() {
        assertNull(BirthDate.parse("1990-13-01", 2026))
        assertNull(BirthDate.parse("1990-02-30", 2026))
        assertNull(BirthDate.parse("1991-02-29", 2026))
        assertNull(BirthDate.parse("irgendwas", 2026))
        assertNull(BirthDate.parse(null, 2026))
    }

    @Test fun leapDayFallsOnFeb28() {
        val b = BirthDate(2, 29, 2000)
        assertEquals(LocalDate.of(2027, 2, 28), b.inYear(2027))
        assertEquals(LocalDate.of(2028, 2, 29), b.inYear(2028))
    }
}

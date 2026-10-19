package com.arashrasoulzadeh.einkdash

import com.arashrasoulzadeh.einkdash.util.Jalali
import org.junit.Assert.assertEquals
import org.junit.Test

class JalaliTest {
    @Test fun knownDates() {
        assertEquals(Jalali.Date(1405, 7, 27), Jalali.fromGregorian(2026, 10, 19))
        assertEquals(Jalali.Date(1403, 1, 1), Jalali.fromGregorian(2024, 3, 20)) // Nowruz 1403
        assertEquals(Jalali.Date(1402, 12, 29), Jalali.fromGregorian(2024, 3, 19))
        assertEquals(Jalali.Date(1399, 12, 30), Jalali.fromGregorian(2021, 3, 20)) // leap Esfand
    }
}

class JalaliReverseTest {
    @Test fun roundTripsAndLengths() {
        assertEquals(Jalali.Date(2026, 10, 19), Jalali.toGregorian(1405, 7, 27))
        assertEquals(Jalali.Date(2024, 3, 20), Jalali.toGregorian(1403, 1, 1))
        assertEquals(Jalali.Date(2021, 3, 20), Jalali.toGregorian(1399, 12, 30))
        for (y in 1395..1410) for (m in 1..12) {
            val g = Jalali.toGregorian(y, m, 1)
            assertEquals(Jalali.Date(y, m, 1), Jalali.fromGregorian(g.year, g.month, g.day))
        }
        assertEquals(30, Jalali.monthLength(1399, 12)) // leap
        assertEquals(29, Jalali.monthLength(1400, 12))
        assertEquals(31, Jalali.monthLength(1405, 1))
    }
}

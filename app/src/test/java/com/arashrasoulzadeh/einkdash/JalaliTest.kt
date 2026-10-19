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

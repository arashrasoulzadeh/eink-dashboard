package com.arashrasoulzadeh.einkdash.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLayoutTest {

    private fun page() = Page(4, 6).apply { tryUpdate(Placement(1, "clock", 0, 0, 4, 2)) }

    @Test fun rejectsOverlapAndOutOfBounds() {
        val p = page()
        assertFalse(p.fits(Placement(2, "notes", 1, 1, 1, 1)))
        assertFalse(p.fits(Placement(2, "notes", 3, 5, 2, 1)))
        assertTrue(p.fits(Placement(2, "notes", 0, 2, 4, 4)))
    }

    @Test fun itemMayMoveOverItsOwnCells() {
        val p = page()
        assertTrue(p.tryUpdate(Placement(1, "clock", 0, 1, 4, 2)))
        assertEquals(1, p.items.size)
    }

    @Test fun firstFreeFindsTopLeftSpotOrNull() {
        val p = page()
        assertEquals(Placement(2, "w", 0, 2, 2, 2), p.firstFree(2, "w", 2, 2))
        p.tryUpdate(Placement(3, "big", 0, 2, 4, 4))
        assertNull(p.firstFree(4, "w", 1, 1))
    }

    @Test fun jsonRoundTripAndGridMismatch() {
        val layout = DashboardLayout(4, 6, mutableListOf(page(), Page(4, 6)))
        val back = DashboardLayout.fromJson(layout.toJson(), 4, 6)!!
        assertEquals(2, back.pages.size)
        assertEquals(layout.pages[0].items, back.pages[0].items)
        assertNull(DashboardLayout.fromJson(layout.toJson(), 6, 4))
        assertNull(DashboardLayout.fromJson("garbage", 4, 6))
        assertEquals(2L, back.nextId())
    }
}

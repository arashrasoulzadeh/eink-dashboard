package com.arashrasoulzadeh.einkdash

import com.arashrasoulzadeh.einkdash.face.Night
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWindowTest {
    @Test fun wrapsPastMidnight() {
        assertTrue(Night.inWindow(22, 22, 7))
        assertTrue(Night.inWindow(23, 22, 7))
        assertTrue(Night.inWindow(3, 22, 7))
        assertFalse(Night.inWindow(7, 22, 7))
        assertFalse(Night.inWindow(12, 22, 7))
    }

    @Test fun sameDayWindow() {
        assertTrue(Night.inWindow(14, 13, 15))
        assertFalse(Night.inWindow(15, 13, 15))
    }
}

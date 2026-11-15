package com.getfuckocus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceMissFormatTest {
    @Test
    fun distanceUnits() {
        assertEquals("320m", formatDistance(320.4f))
        assertEquals("1.5km", formatDistance(1480f))
        assertEquals("123km", formatDistance(123_400f))
    }

    @Test
    fun eightDirections() {
        assertEquals("북", directionOf(0f))
        assertEquals("북", directionOf(350f))
        assertEquals("북동", directionOf(40f))
        assertEquals("남서", directionOf(-130f))
        assertEquals("서", directionOf(270f))
    }
}

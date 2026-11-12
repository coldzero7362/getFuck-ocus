package com.getfuckocus.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickedPlaceTest {
    @Test
    fun `장소는 문자열로 주고받아도 그대로다`() {
        val place = PickedPlace(37.5665, 126.978, 150f)
        assertEquals(place, PickedPlace.decode(place.encode()))
    }

    @Test
    fun `잘못된 문자열은 받지 않는다`() {
        assertNull(PickedPlace.decode(null))
        assertNull(PickedPlace.decode("37.5,126.9"))
        assertNull(PickedPlace.decode("abc,126.9,150"))
        assertNull(PickedPlace.decode("91,126.9,150"))
        assertNull(PickedPlace.decode("37.5,126.9,0"))
    }
}

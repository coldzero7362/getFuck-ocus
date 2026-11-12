package com.getfuckocus.place

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressFormatTest {
    @Test
    fun `장소 이름이 있으면 이름을 크게, 주소는 큰 단위부터 보조 줄로`() {
        val result = AddressFormat.from(
            name = "서울특별시청",
            displayName = "서울특별시청, 110, 세종대로, 태평로1가, 중구, 서울특별시, 04524, 대한민국",
        )
        assertEquals(PlaceAddress("서울특별시청", "서울특별시 중구 태평로1가 세종대로 110"), result)
    }

    @Test
    fun `이름이 없으면 주소만 큰 글자로`() {
        val result = AddressFormat.from(name = "", displayName = "12, 판교역로, 백현동, 분당구, 성남시, 경기도, 13529, 대한민국")
        assertEquals(PlaceAddress("경기도 성남시 분당구 백현동 판교역로 12", null), result)
    }

    @Test
    fun `아무것도 없으면 null`() {
        assertNull(AddressFormat.from(name = null, displayName = null))
        assertNull(AddressFormat.from(name = " ", displayName = "대한민국"))
    }
}

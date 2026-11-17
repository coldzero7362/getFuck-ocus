package com.getfuckocus.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagDebouncerTest {
    private val a = "04:AA:BB"
    private val b = "04:CC:DD"

    @Test
    fun `한 번 대는 동안 여러 번 읽혀도 한 번만 받는다`() {
        val d = TagDebouncer(windowMs = 1_500)
        // 대고 있는 2초 동안 0.3초마다 다시 읽힘
        val accepted = (0L..2_000L step 300).count { d.accept(a, it) }
        assertEquals(1, accepted)
    }

    @Test
    fun `떼었다가 다시 대면 다시 받는다`() {
        val d = TagDebouncer(windowMs = 1_500)
        assertTrue(d.accept(a, 0))
        assertFalse(d.accept(a, 500))
        // 마지막으로 보인 500ms에서 1.5초 넘게 지나 다시 댐
        assertTrue(d.accept(a, 2_100))
    }

    @Test
    fun `다른 태그는 바로 받는다`() {
        val d = TagDebouncer(windowMs = 1_500)
        assertTrue(d.accept(a, 0))
        assertTrue(d.accept(b, 100))
        // 다른 태그를 거쳤으면 처음 태그도 새 터치로 본다
        assertTrue(d.accept(a, 200))
    }

    @Test
    fun `등록할 때처럼 떼었다 다시 대는 두 번은 모두 받는다`() {
        val d = TagDebouncer()
        assertTrue(d.accept(a, 10_000))
        assertTrue(d.accept(a, 10_000 + TagDebouncer.DEFAULT_WINDOW_MS + 200))
    }
}

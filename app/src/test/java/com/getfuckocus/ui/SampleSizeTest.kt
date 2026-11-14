package com.getfuckocus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {
    @Test
    fun `작은 이미지는 줄이지 않는다`() {
        assertEquals(1, sampleSizeFor(1000, 800, 1440))
        assertEquals(1, sampleSizeFor(2879, 1000, 1440))
    }

    @Test
    fun `줄인 뒤에도 긴 변이 기준 이상 남는 가장 큰 배율을 고른다`() {
        assertEquals(2, sampleSizeFor(2880, 1000, 1440))
        assertEquals(2, sampleSizeFor(4000, 3000, 1440))
        assertEquals(4, sampleSizeFor(1000, 6000, 1440))
    }

    @Test
    fun `크기를 모르면 1`() {
        assertEquals(1, sampleSizeFor(0, 0, 1080))
        assertEquals(1, sampleSizeFor(-1, -1, 1080))
    }
}

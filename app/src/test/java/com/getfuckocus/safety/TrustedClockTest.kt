package com.getfuckocus.safety

import org.junit.Assert.assertEquals
import org.junit.Test

class TrustedClockTest {
    private val hour = 60 * 60 * 1000L
    private val anchor = Stamp(wallMillis = 100 * hour, elapsedMillis = 10 * hour, bootCount = 3)

    @Test
    fun `처음에는 기기 시계를 그대로 쓴다`() {
        assertEquals(5L, trustedWallMillis(Stamp(5, 0, 1), anchor = null))
    }

    @Test
    fun `같은 부팅에서 시계를 뒤로 돌려도 실제로 흐른 시간만큼은 지난 것으로 본다`() {
        // 부팅 후 1시간이 흘렀는데 기기 시계는 2시간 뒤로 돌아갔다
        val now = Stamp(wallMillis = 99 * hour, elapsedMillis = 11 * hour, bootCount = 3)
        assertEquals(101 * hour, trustedWallMillis(now, anchor))
    }

    @Test
    fun `시계를 앞으로 돌리는 것은 그대로 받아들인다`() {
        val now = Stamp(wallMillis = 105 * hour, elapsedMillis = 11 * hour, bootCount = 3)
        assertEquals(105 * hour, trustedWallMillis(now, anchor))
    }

    @Test
    fun `재부팅 뒤에는 마지막으로 확인한 시각보다 이르게 나오지 않는다`() {
        val now = Stamp(wallMillis = 98 * hour, elapsedMillis = 1 * hour, bootCount = 4)
        assertEquals(100 * hour, trustedWallMillis(now, anchor))
    }

    @Test
    fun `12시간보다 크게 뒤로 가면 시계를 바로잡은 것으로 보고 따른다`() {
        val now = Stamp(wallMillis = 80 * hour, elapsedMillis = 11 * hour, bootCount = 3)
        assertEquals(80 * hour, trustedWallMillis(now, anchor))
    }
}

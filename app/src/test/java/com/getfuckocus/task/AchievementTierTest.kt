package com.getfuckocus.task

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementTierTest {
    private fun reached(streak: Int, owned: List<Int> = emptyList()) =
        AchievementTier.newlyReached(streak, owned).map { it.milestone }

    @Test
    fun `기준에 못 미치면 아무것도 받지 않는다`() {
        assertEquals(emptyList<Int>(), reached(2))
    }

    @Test
    fun `기준에 닿으면 그 단계를 받는다`() {
        assertEquals(listOf(3), reached(3))
        assertEquals(listOf(7), reached(7, owned = listOf(3)))
    }

    @Test
    fun `이미 받은 단계는 다시 주지 않는다`() {
        assertEquals(emptyList<Int>(), reached(5, owned = listOf(3)))
    }

    @Test
    fun `기능이 생기기 전에 쌓은 연속 기록은 넘어선 단계를 모두 준다`() {
        assertEquals(listOf(3, 7, 14), reached(20))
    }
}

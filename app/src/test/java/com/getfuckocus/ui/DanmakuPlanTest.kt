package com.getfuckocus.ui

import com.getfuckocus.data.NagContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DanmakuPlanTest {
    @Test
    fun `문구·사진마다 1~5개씩 복제되어 흘러간다`() {
        repeat(50) { seed ->
            val wave = DanmakuPlan.wave(listOf("물 마셔", "지금 해"), listOf("/a.jpg"), Random(seed))
            val counts = wave.groupingBy {
                when (it) {
                    is Flyer.Text -> it.text
                    is Flyer.Picture -> it.path
                }
            }.eachCount()
            assertEquals(setOf("물 마셔", "지금 해", "/a.jpg"), counts.keys)
            counts.values.forEach { assertTrue(it in 1..DanmakuPlan.MAX_COPIES) }
            // 복제본도 서로 다른 것으로 다룬다
            assertEquals(wave.size, wave.map { it.id }.toSet().size)
        }
        // 여러 번 돌리면 실제로 두 개 이상 복제되는 경우가 있다
        val maxCopies = (0 until 50).maxOf { seed ->
            DanmakuPlan.wave(listOf("x"), emptyList(), Random(seed)).size
        }
        assertTrue(maxCopies > 1)
    }

    @Test
    fun `문구가 없으면 기본 문구가 흘러간다`() {
        val wave = DanmakuPlan.wave(emptyList(), emptyList(), Random(2))
        assertTrue(wave.isNotEmpty())
        assertTrue(wave.all { it is Flyer.Text && it.text == DEFAULT_NAG_MESSAGE })
    }

    @Test
    fun `크기와 속도는 상한을 넘지 않고 무작위로`() {
        repeat(50) { seed ->
            DanmakuPlan.wave(listOf("a", "b", "c"), listOf("/p"), Random(seed)).forEach { f ->
                assertTrue(f.lane in DanmakuPlan.LANE)
                assertTrue(f.startDelayMs in 0..DanmakuPlan.MAX_START_DELAY_MS)
                assertTrue(f.speedDpPerSec in DanmakuPlan.MIN_SPEED_DP_PER_SEC..DanmakuPlan.MAX_SPEED_DP_PER_SEC)
                when (f) {
                    is Flyer.Text -> assertTrue(f.sizeSp in DanmakuPlan.MIN_TEXT_SIZE_SP..DanmakuPlan.MAX_TEXT_SIZE_SP)
                    is Flyer.Picture -> assertTrue(f.heightDp in DanmakuPlan.MIN_PICTURE_HEIGHT_DP..DanmakuPlan.MAX_PICTURE_HEIGHT_DP)
                }
            }
        }
        val wave = DanmakuPlan.wave(List(8) { "t$it" }, emptyList(), Random(7))
        assertTrue(wave.map { (it as Flyer.Text).sizeSp }.toSet().size > 1)
        assertTrue(wave.map { it.speedDpPerSec }.toSet().size > 1)
    }

    @Test
    fun `문구·사진은 줄바꿈으로 여러 개 담고 빈 줄은 버린다`() {
        assertEquals("a\nb", NagContent.join(listOf(" a ", "", "b")))
        assertEquals(listOf("a", "b"), NagContent.split("a\n\n b \n"))
        assertEquals(emptyList<String>(), NagContent.split(null))
        assertEquals(listOf("x y"), NagContent.split(NagContent.join(listOf("x\ny"))))
    }
}

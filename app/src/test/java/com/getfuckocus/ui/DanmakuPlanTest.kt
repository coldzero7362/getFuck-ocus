package com.getfuckocus.ui

import com.getfuckocus.data.NagContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DanmakuPlanTest {
    @Test
    fun `설정한 문구와 사진이 한 번씩 모두 흘러간다`() {
        val wave = DanmakuPlan.wave(listOf("물 마셔", "지금 해"), listOf("/a.jpg", "/b.jpg"), Random(1))
        assertEquals(setOf("물 마셔", "지금 해"), wave.filterIsInstance<Flyer.Text>().map { it.text }.toSet())
        assertEquals(setOf("/a.jpg", "/b.jpg"), wave.filterIsInstance<Flyer.Picture>().map { it.path }.toSet())
        assertEquals(4, wave.map { it.id }.toSet().size)
    }

    @Test
    fun `문구가 없으면 기본 문구가 흘러간다`() {
        val wave = DanmakuPlan.wave(emptyList(), emptyList(), Random(2))
        assertEquals(listOf(DEFAULT_NAG_MESSAGE), wave.filterIsInstance<Flyer.Text>().map { it.text })
    }

    @Test
    fun `크기·높이·시각은 정해 둔 범위 안에서 무작위로`() {
        repeat(50) { seed ->
            DanmakuPlan.wave(listOf("a", "b", "c"), listOf("/p"), Random(seed)).forEach { f ->
                assertTrue(f.lane in DanmakuPlan.LANE)
                assertTrue(f.startDelayMs in 0..DanmakuPlan.MAX_START_DELAY_MS)
                assertTrue(f.durationMs in DanmakuPlan.DURATION_MS)
                when (f) {
                    is Flyer.Text -> assertTrue(f.sizeSp in DanmakuPlan.TEXT_SIZE_SP)
                    is Flyer.Picture -> assertTrue(f.heightDp in DanmakuPlan.PICTURE_HEIGHT_DP)
                }
            }
        }
        // 크기가 실제로 제각각인지
        val sizes = DanmakuPlan.wave(List(8) { "t$it" }, emptyList(), Random(7))
            .filterIsInstance<Flyer.Text>().map { it.sizeSp }.toSet()
        assertTrue(sizes.size > 1)
    }

    @Test
    fun `문구·사진은 줄바꿈으로 여러 개 담고 빈 줄은 버린다`() {
        assertEquals("a\nb", NagContent.join(listOf(" a ", "", "b")))
        assertEquals(listOf("a", "b"), NagContent.split("a\n\n b \n"))
        assertEquals(emptyList<String>(), NagContent.split(null))
        // 한 문구 안의 줄바꿈은 빈칸으로 바뀐다
        assertEquals(listOf("x y"), NagContent.split(NagContent.join(listOf("x\ny"))))
    }
}

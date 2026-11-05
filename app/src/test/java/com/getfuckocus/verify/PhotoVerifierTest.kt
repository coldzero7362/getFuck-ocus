package com.getfuckocus.verify

import com.getfuckocus.at
import com.getfuckocus.safety.Stamp
import com.getfuckocus.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhotoVerifierTest {
    private val verifier = PhotoVerifier()
    private val shutter = at(7, 30).toInstant().toEpochMilli()

    private fun evidence(
        labels: List<DetectedLabel> = emptyList(),
        exifMillis: Long? = shutter,
    ) = Evidence(
        photo = File("unused.jpg"),
        capturedAt = Stamp(shutter, 0, 1),
        location = null,
        labels = labels,
        exif = ExifSummary(exifMillis, null, null),
    )

    @Test
    fun `조건이 없으면 시간창 안에서 통과`() {
        assertEquals(Verdict.Pass, verifier.verify(task(), evidence(), inWindow = true))
    }

    @Test
    fun `시간창 밖이면 실패`() {
        val verdict = verifier.verify(task(), evidence(), inWindow = false)
        assertTrue(verdict is Verdict.Fail)
    }

    @Test
    fun `라벨은 쉼표로 여러 개 중 하나만 기준 이상이면 통과`() {
        val t = task(requiredLabel = "TV, Dog")
        val verdict = verifier.verify(t, evidence(listOf(DetectedLabel("dog", 0.53f))), inWindow = true)
        assertEquals(Verdict.Pass, verdict)
    }

    @Test
    fun `이름은 맞지만 신뢰도가 기준 미만이면 그 사실을 알려준다`() {
        val t = task(requiredLabel = "Dog")
        val verdict = verifier.verify(t, evidence(listOf(DetectedLabel("Dog", 0.42f))), inWindow = true)
        assertTrue(verdict is Verdict.Fail)
        assertTrue((verdict as Verdict.Fail).reasons.single().contains("42%"))
    }

    @Test
    fun `EXIF 촬영 시각이 셔터 시각과 크게 다르면 실패`() {
        val tenMinutesEarlier = shutter - 10 * 60 * 1000
        val verdict = verifier.verify(task(), evidence(exifMillis = tenMinutesEarlier), inWindow = true)
        assertTrue(verdict is Verdict.Fail)
    }
}

package com.getfuckocus.verify

import com.getfuckocus.data.MissAction
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.saveProblem
import com.getfuckocus.data.screenshotKeywordList
import com.getfuckocus.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScreenshotRulesTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private fun at(h: Int, m: Int, day: Int = 17): ZonedDateTime = ZonedDateTime.of(2026, 11, day, h, m, 0, 0, zone)

    @Test
    fun `단어는 띄어쓰기와 대소문자를 무시하고 찾는다`() {
        val text = "출 근 처리가\n완료되었습니다 08:52 OK"
        assertEquals(emptyList<String>(), ScreenshotRules.missingKeywords(listOf("출근", "완료", "ok"), text))
        assertEquals(listOf("퇴근"), ScreenshotRules.missingKeywords(listOf("출근", "퇴근"), text))
    }

    @Test
    fun `글자 속 시각을 24시간제로 읽는다`() {
        val times = ScreenshotRules.timesIn("9:41  출근 08:52\n오후 6:03 퇴근 · 7:15 PM · 오전 12:30")
        assertEquals(
            listOf(LocalTime.of(9, 41), LocalTime.of(8, 52), LocalTime.of(18, 3), LocalTime.of(19, 15), LocalTime.of(0, 30)),
            times,
        )
    }

    @Test
    fun `날짜나 긴 숫자 속 콜론은 시각으로 보지 않는다`() {
        assertEquals(emptyList<LocalTime>(), ScreenshotRules.timesIn("ID 04:04:CC 2026.11.17 123:456"))
    }

    @Test
    fun `캡처 속 시각 중 하나라도 인증 시간 안이면 된다`() {
        val start = at(8, 30)
        val end = at(9, 30)
        assertTrue(ScreenshotRules.anyTimeInWindow(listOf(LocalTime.of(7, 0), LocalTime.of(8, 52)), start, end))
        assertFalse(ScreenshotRules.anyTimeInWindow(listOf(LocalTime.of(7, 0), LocalTime.of(10, 0)), start, end))
        assertFalse(ScreenshotRules.anyTimeInWindow(emptyList(), start, end))
    }

    // Shiftee 홈 화면을 글자 인식한 모양 (줄 글자와 세로 위치)
    private val shiftee = listOf(
        OcrLine("10:0921•", 20, 60),
        OcrLine("출근/퇴근 누락 기록", 400, 450),
        OcrLine("출근하기", 790, 840),
        OcrLine("요청", 790, 840),
        OcrLine("출근", 925, 965),
        OcrLine("08:22", 925, 965),
        OcrLine("퇴근", 990, 1030),
        OcrLine("18:17", 990, 1030),
        OcrLine("출퇴근기록", 1850, 1880),
    )

    @Test
    fun `단어와 같은 줄에 찍힌 시각만 쓴다`() {
        assertEquals(listOf(LocalTime.of(8, 22)), ScreenshotRules.keywordRowTimes(listOf("출근"), shiftee))
        assertEquals(listOf(LocalTime.of(18, 17)), ScreenshotRules.keywordRowTimes(listOf("퇴근"), shiftee))
    }

    @Test
    fun `버튼 글자처럼 옆에 시각이 없으면 시각을 찾지 못한다`() {
        assertEquals(emptyList<LocalTime>(), ScreenshotRules.keywordRowTimes(listOf("출근하기"), shiftee))
    }

    @Test
    fun `아직 출근 전이라 시각이 비어 있으면 시각을 찾지 못한다`() {
        val before = listOf(OcrLine("출근하기", 790, 840), OcrLine("출근", 925, 965), OcrLine("-", 925, 965))
        assertEquals(emptyList<LocalTime>(), ScreenshotRules.keywordRowTimes(listOf("출근"), before))
    }

    @Test
    fun `단어와 시각이 한 줄 글자에 같이 있어도 찾는다`() {
        val lines = listOf(OcrLine("출근 처리가 완료되었습니다", 480, 540), OcrLine("출근 시각 13:04", 640, 700))
        assertEquals(listOf(LocalTime.of(13, 4)), ScreenshotRules.keywordRowTimes(listOf("출근", "완료"), lines))
    }

    @Test
    fun `자정을 넘는 회차는 다음 날 시각도 맞춰 본다`() {
        assertTrue(ScreenshotRules.anyTimeInWindow(listOf(LocalTime.of(0, 20)), at(23, 30), at(0, 30, day = 18)))
    }

    @Test
    fun `파일 이름에서 캡처 시각을 읽는다`() {
        val expected = LocalDateTime.of(2026, 11, 17, 8, 52, 0)
        assertEquals(expected, ScreenshotRules.timeFromFileName("Screenshot_20261117-085200.png"))
        assertEquals(expected, ScreenshotRules.timeFromFileName("Screenshot_20261117_085200_Flex.jpg"))
        assertEquals(expected, ScreenshotRules.timeFromFileName("스크린샷 2026-11-17 085200.png"))
        assertNull(ScreenshotRules.timeFromFileName("IMG_1234.jpg"))
        assertNull(ScreenshotRules.timeFromFileName(null))
    }

    @Test
    fun `스크린샷 인증은 단어가 하나 이상 있어야 저장된다`() {
        assertNotNull(saveProblem(VerifyMethod.SCREENSHOT, hasGeoFence = false, nfcTagCount = 0, MissAction.NAG, screenshotKeywordCount = 0))
        assertNull(saveProblem(VerifyMethod.SCREENSHOT, hasGeoFence = false, nfcTagCount = 0, MissAction.NAG, screenshotKeywordCount = 1))
    }

    @Test
    fun `과제의 단어 목록은 빈칸과 중복을 버린다`() {
        val t = task().copy(verifyMethod = VerifyMethod.SCREENSHOT, screenshotKeywords = "출근, 완료, ,출근")
        assertEquals(listOf("출근", "완료"), t.screenshotKeywordList())
    }
}

package com.getfuckocus.verify

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 스크린샷 인증 판정에 쓰는 순수 함수들 (기기 기능 없이 시험할 수 있게 따로 둔다).
 * - 캡처에 정해 둔 단어가 모두 보이는지
 * - 정한 단어와 같은 줄에 찍힌 시각(예: "출근 ··· 08:22")이 인증 시간 안인지
 * - 파일 이름에서 캡처한 시각 읽기
 */
object ScreenshotRules {

    /** 글자 인식은 띄어쓰기를 자주 틀리므로 빈칸을 모두 빼고 소문자로 맞춰 비교한다 */
    fun normalize(text: String): String = text.filterNot { it.isWhitespace() }.lowercase()

    /** [keywords] 중 [text]에서 찾지 못한 단어들 */
    fun missingKeywords(keywords: List<String>, text: String): List<String> {
        val haystack = normalize(text)
        return keywords.filter { normalize(it) !in haystack }
    }

    // "08:52", "8 : 52", "오후 6:03", "6:03 PM". 날짜나 긴 숫자, 콜론으로 이어진 ID(04:04:CC) 속 콜론은 걸러낸다
    private val TIME = Regex("""(오전|오후|AM|PM|am|pm)?\s*(?<![0-9:])([01]?[0-9]|2[0-3])\s*[:：]\s*([0-5][0-9])(?![0-9]|:[0-9A-Fa-f])\s*(AM|PM|am|pm)?""")

    /** 글자 속에 보이는 시각들 (오전·오후 표시가 있으면 24시간제로 바꾼다) */
    fun timesIn(text: String): List<LocalTime> = TIME.findAll(text).mapNotNull { m ->
        val marker = (m.groupValues[1].ifEmpty { m.groupValues[4] }).lowercase()
        var hour = m.groupValues[2].toInt()
        val minute = m.groupValues[3].toInt()
        when (marker) {
            "오후", "pm" -> if (hour in 1..11) hour += 12
            "오전", "am" -> if (hour == 12) hour = 0
        }
        if (hour > 23) null else LocalTime.of(hour, minute)
    }.distinct().toList()

    /**
     * [times] 중 회차 시간([start]부터 [end]까지) 안에 드는 것이 있는지.
     * 시각만 보이므로 회차 시작 날짜와 그다음 날 둘 다 맞춰 본다 (자정을 넘는 과제).
     */
    fun anyTimeInWindow(times: List<LocalTime>, start: ZonedDateTime, end: ZonedDateTime): Boolean {
        val day = start.toLocalDate()
        return times.any { t ->
            listOf(day, day.plusDays(1)).any { d ->
                val at = d.atTime(t).atZone(start.zone)
                !at.isBefore(start) && !at.isAfter(end)
            }
        }
    }

    /**
     * [keywords] 중 하나가 든 줄과 **같은 높이**에 있는 시각들.
     * 출퇴근 앱은 "출근 ········ 08:22"처럼 이름과 시각을 한 줄 양 끝에 두는데, 글자 인식은 둘을 따로 된 줄로 돌려준다.
     * 그래서 한쪽 줄의 세로 가운데가 다른 쪽 줄의 위아래 안에 들면 같은 줄로 본다.
     * "출근하기" 버튼처럼 옆에 시각이 없는 줄은 아무것도 보태지 않는다.
     */
    fun keywordRowTimes(keywords: List<String>, lines: List<OcrLine>): List<LocalTime> {
        val keys = keywords.map(::normalize).filter { it.isNotEmpty() }
        val anchors = lines.filter { line -> keys.any { it in normalize(line.text) } }
        return anchors.flatMap { anchor -> lines.filter { sameRow(anchor, it) }.flatMap { timesIn(it.text) } }.distinct()
    }

    private fun sameRow(a: OcrLine, b: OcrLine): Boolean {
        val ca = (a.top + a.bottom) / 2
        val cb = (b.top + b.bottom) / 2
        return cb in a.top..a.bottom || ca in b.top..b.bottom
    }

    // Screenshot_20261117-085200.png, Screenshot_20261117_085200_Kakao.jpg, 스크린샷 2026-11-17 085200.png 등
    private val NAME_TIME = Regex("""(20\d{2})[-_.]?(\d{2})[-_.]?(\d{2})[-_ ]+(\d{2})[-_.:]?(\d{2})[-_.:]?(\d{2})""")

    /** 파일 이름에 들어 있는 캡처 시각 (없거나 말이 안 되면 null) */
    fun timeFromFileName(name: String?): LocalDateTime? {
        val m = NAME_TIME.find(name ?: return null) ?: return null
        val (y, mo, d, h, mi, s) = m.destructured
        return runCatching {
            LocalDateTime.of(LocalDate.of(y.toInt(), mo.toInt(), d.toInt()), LocalTime.of(h.toInt(), mi.toInt(), s.toInt()))
        }.getOrNull()
    }

    fun hhmm(time: ZonedDateTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

/** 글자 인식 결과 한 줄과 화면 속 세로 위치(픽셀) */
data class OcrLine(val text: String, val top: Int, val bottom: Int)

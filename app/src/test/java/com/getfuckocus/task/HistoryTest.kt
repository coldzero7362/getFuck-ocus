package com.getfuckocus.task

import com.getfuckocus.DAY
import com.getfuckocus.at
import com.getfuckocus.data.PassRecord
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 기본 과제: 매일 07:00~08:00. DAY(2026-11-05)는 목요일. */
class HistoryTest {
    private fun pass(date: LocalDate, hour: Int, minute: Int = 0, taskId: Long = 1) =
        PassRecord(taskId, TaskSchedule.dayKey(date), at(hour, minute, date = date).toInstant().toEpochMilli())

    @Test
    fun `마감 전 통과, 늦은 통과, 놓침, 진행 중을 구분한다`() {
        val passes = PassIndex(listOf(pass(DAY.minusDays(1), 7, 30), pass(DAY.minusDays(2), 9)))
        val t = task()
        assertEquals(OccurrenceResult.PASSED, History.occurrenceResult(t, DAY.minusDays(1), at(12), passes))
        assertEquals(OccurrenceResult.LATE, History.occurrenceResult(t, DAY.minusDays(2), at(12), passes))
        assertEquals(OccurrenceResult.MISSED, History.occurrenceResult(t, DAY.minusDays(3), at(12), passes))
        assertEquals(OccurrenceResult.PENDING, History.occurrenceResult(t, DAY, at(7, 30), passes))
    }

    @Test
    fun `과제를 만들기 전 날짜와 반복 요일이 아닌 날은 결과가 없다`() {
        val created = task(createdAt = at(0, date = DAY.minusDays(1)).toInstant().toEpochMilli())
        assertEquals(OccurrenceResult.NONE, History.occurrenceResult(created, DAY.minusDays(2), at(12), PassIndex(emptyList())))
        val weekdays = task(repeatDays = RepeatDays.WEEKDAYS)
        assertEquals(OccurrenceResult.NONE, History.occurrenceResult(weekdays, DAY.minusDays(4), at(12), PassIndex(emptyList())))
    }

    @Test
    fun `연속 달성은 오늘 진행 중인 회차를 건너뛰고 놓친 날에서 멈춘다`() {
        val passes = PassIndex(
            listOf(pass(DAY.minusDays(1), 7, 10), pass(DAY.minusDays(2), 7, 20), pass(DAY.minusDays(3), 7, 5))
        )
        // 오늘 07:30, 오늘 회차는 진행 중 / 어제·그제·그끄제 통과 / 4일 전 놓침
        assertEquals(3, History.streak(task(), at(7, 30), passes))
    }

    @Test
    fun `늦게 통과한 날도 연속 달성을 끊는다`() {
        val passes = PassIndex(listOf(pass(DAY.minusDays(1), 7, 10), pass(DAY.minusDays(2), 9)))
        assertEquals(1, History.streak(task(), at(7, 30), passes))
    }

    @Test
    fun `쉬는 날은 연속 달성을 끊지 않는다`() {
        // 평일만 반복. 이번 주 월(DAY-3)~수(DAY-1) 통과, 지난 금(DAY-6) 통과, 주말은 회차 없음
        val weekdays = task(repeatDays = RepeatDays.WEEKDAYS)
        val passes = PassIndex(
            listOf(
                pass(DAY.minusDays(1), 7, 10), pass(DAY.minusDays(2), 7, 10), pass(DAY.minusDays(3), 7, 10),
                pass(DAY.minusDays(6), 7, 10),
            )
        )
        assertEquals(4, History.streak(weekdays, at(7, 30), passes))
    }

    @Test
    fun `하루 결과 합치기`() {
        val a = task(id = 1)
        val b = task(id = 2, startHour = 9)
        val yesterday = DAY.minusDays(1)
        val both = PassIndex(listOf(pass(yesterday, 7, 10, taskId = 1), pass(yesterday, 9, 10, taskId = 2)))
        val one = PassIndex(listOf(pass(yesterday, 7, 10, taskId = 1)))
        assertEquals(DayResult.PASSED, History.dayResult(listOf(a, b), yesterday, at(12), both))
        assertEquals(DayResult.PARTIAL, History.dayResult(listOf(a, b), yesterday, at(12), one))
        assertEquals(DayResult.MISSED, History.dayResult(listOf(a, b), yesterday, at(12), PassIndex(emptyList())))
        assertEquals(DayResult.FUTURE, History.dayResult(listOf(a, b), DAY.plusDays(1), at(12), both))
    }
}

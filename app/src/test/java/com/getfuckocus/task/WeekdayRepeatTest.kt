package com.getfuckocus.task

import com.getfuckocus.DAY
import com.getfuckocus.ZONE
import com.getfuckocus.at
import com.getfuckocus.data.PassedOccurrence
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.task
import com.getfuckocus.ui.TaskStatus
import com.getfuckocus.ui.taskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/** DAY(2026-11-05)는 목요일이다. */
class WeekdayRepeatTest {
    private val weekdays = task(repeatDays = RepeatDays.WEEKDAYS)
    private val saturday = DAY.plusDays(2)
    private val monday = DAY.plusDays(4)

    @Test
    fun `기준 날짜는 목요일`() {
        assertEquals(DayOfWeek.THURSDAY, DAY.dayOfWeek)
    }

    @Test
    fun `반복 요일이 아니면 회차가 없다`() {
        assertTrue(TaskSchedule.hasOccurrence(weekdays, DAY, ZONE))
        assertFalse(TaskSchedule.hasOccurrence(weekdays, saturday, ZONE))
        assertNull(TaskSchedule.occurrenceContaining(weekdays, at(7, 30, date = saturday)))
    }

    @Test
    fun `반복 요일이 아니어도 그날 바로 시작하면 회차가 생긴다`() {
        val started = weekdays.copy(startedAt = at(10, date = saturday).toInstant().toEpochMilli())
        assertTrue(TaskSchedule.hasOccurrence(started, saturday, ZONE))
        assertEquals(saturday, TaskSchedule.occurrenceContaining(started, at(10, 30, date = saturday)))
    }

    @Test
    fun `자정을 넘는 회차는 시작한 날의 요일을 따른다`() {
        // 금요일 23:30 시작, 토요일 00:10은 아직 금요일 회차 안
        val friday = DAY.plusDays(1)
        val night = task(startHour = 23, startMinute = 30, durationMinutes = 60, repeatDays = RepeatDays.WEEKDAYS)
        assertEquals(friday, TaskSchedule.occurrenceContaining(night, at(0, 10, date = saturday)))
    }

    @Test
    fun `금요일 마감 뒤 다음 마감은 월요일`() {
        val friday = DAY.plusDays(1)
        assertEquals(at(8, date = monday), TaskSchedule.nextDeadline(listOf(weekdays), at(9, date = friday)))
        assertEquals(at(7, date = monday), TaskSchedule.nextOccurrenceStart(weekdays, at(9, date = friday)))
    }

    @Test
    fun `반복 요일이 하나도 없으면 다음 회차도 없다`() {
        assertNull(TaskSchedule.nextOccurrenceStart(task(repeatDays = 0), at(6)))
    }

    @Test
    fun `쉬는 날에는 놓침이 아니라 쉬는 날로 보이고 다음 회차를 알려 준다`() {
        val status = taskStatus(weekdays, at(12, date = saturday), emptySet())
        assertEquals(TaskStatus.RestDay(at(7, date = monday)), status)
    }

    @Test
    fun `반복 요일에는 기존처럼 놓침과 완료를 판정한다`() {
        assertEquals(TaskStatus.Missed, taskStatus(weekdays, at(12), emptySet()))
        val passed = setOf(PassedOccurrence(1, TaskSchedule.dayKey(DAY)))
        assertEquals(TaskStatus.Done, taskStatus(weekdays, at(12), passed))
    }

    @Test
    fun `요일 설명`() {
        assertEquals("매일", TaskSchedule.describeDays(RepeatDays.EVERY_DAY))
        assertEquals("평일", TaskSchedule.describeDays(RepeatDays.WEEKDAYS))
        assertEquals("주말", TaskSchedule.describeDays(RepeatDays.WEEKEND))
        val monWedFri = RepeatDays.bit(DayOfWeek.MONDAY) or RepeatDays.bit(DayOfWeek.WEDNESDAY) or
            RepeatDays.bit(DayOfWeek.FRIDAY)
        assertEquals("월·수·금", TaskSchedule.describeDays(monWedFri))
    }
}

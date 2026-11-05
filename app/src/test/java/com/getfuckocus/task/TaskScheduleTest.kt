package com.getfuckocus.task

import com.getfuckocus.DAY
import com.getfuckocus.ZONE
import com.getfuckocus.at
import com.getfuckocus.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class TaskScheduleTest {

    @Test
    fun `회차는 정해진 시작 시각부터 소요 시간 뒤에 마감된다`() {
        val t = task(startHour = 7, durationMinutes = 90)
        assertEquals(at(7), TaskSchedule.windowStart(t, DAY, ZONE))
        assertEquals(at(8, 30), TaskSchedule.deadline(t, DAY, ZONE))
    }

    @Test
    fun `바로 시작은 그날 회차에만 적용된다`() {
        val t = task(startHour = 7, startedAt = at(5, 12).toInstant().toEpochMilli())
        assertEquals(at(5, 12), TaskSchedule.windowStart(t, DAY, ZONE))
        assertEquals(at(7, date = DAY.plusDays(1)), TaskSchedule.windowStart(t, DAY.plusDays(1), ZONE))
    }

    @Test
    fun `자정을 넘는 회차는 다음 날 새벽에도 어제 회차로 잡힌다`() {
        val t = task(startHour = 23, startMinute = 30, durationMinutes = 60)
        val afterMidnight = at(0, 10, date = DAY.plusDays(1))
        assertEquals(DAY, TaskSchedule.occurrenceContaining(t, afterMidnight))
        assertNull(TaskSchedule.occurrenceContaining(t, at(0, 40, date = DAY.plusDays(1))))
    }

    @Test
    fun `시간창 경계 - 시작은 포함하고 마감은 포함하지 않는다`() {
        val t = task(startHour = 7, durationMinutes = 60)
        assertEquals(DAY, TaskSchedule.occurrenceContaining(t, at(7)))
        assertNull(TaskSchedule.occurrenceContaining(t, at(8)))
        assertNull(TaskSchedule.occurrenceContaining(t, at(6, 59)))
    }

    @Test
    fun `다음 마감은 꺼진 과제를 빼고 가장 가까운 것`() {
        val tasks = listOf(
            task(id = 1, startHour = 9),
            task(id = 2, startHour = 8, enabled = false),
            task(id = 3, startHour = 6),
        )
        // 07:30 기준: 과제3은 07:00에 이미 마감, 과제1이 10:00 마감
        assertEquals(at(10), TaskSchedule.nextDeadline(tasks, at(7, 30)))
        // 밤에는 내일 회차
        assertEquals(at(7, date = DAY.plusDays(1)), TaskSchedule.nextDeadline(tasks, at(22)))
        assertNull(TaskSchedule.nextDeadline(listOf(task(enabled = false)), at(7)))
    }

    @Test
    fun `기본 시작 시각은 10분 단위로 내린다`() {
        assertEquals(14 * 60 + 30, TaskSchedule.defaultStartMinute(LocalTime.of(14, 37)))
        assertEquals(9 * 60, TaskSchedule.defaultStartMinute(LocalTime.of(9, 9)))
    }

    @Test
    fun `자정을 넘는 마감은 다음 날로 표시한다`() {
        assertEquals("23:30부터 1시간 (다음 날 00:30 마감)", TaskSchedule.describeWindow(23 * 60 + 30, 60))
        assertEquals("07:00부터 1시간 30분 (08:30 마감)", TaskSchedule.describeWindow(7 * 60, 90))
    }
}

package com.getfuckocus.task

import com.getfuckocus.DAY
import com.getfuckocus.at
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기본 과제: 매일 07:00부터 60분, 시작 알림 켬, 마감 15분 전 알림 */
class RemindersTest {

    @Test
    fun `시작 알림과 마감 전 알림이 시각 순으로 나온다`() {
        val list = Reminders.between(listOf(task()), at(6), at(9))
        assertEquals(listOf(ReminderKind.START, ReminderKind.BEFORE_DEADLINE), list.map { it.kind })
        assertEquals(listOf(at(7), at(7, 45)), list.map { it.at })
        assertTrue(list.all { it.dayKey == TaskSchedule.dayKey(DAY) })
    }

    @Test
    fun `알림을 끈 종류는 빠진다`() {
        val quiet = task().copy(remindAtStart = false, remindBeforeMinutes = 0)
        assertTrue(Reminders.between(listOf(quiet), at(6), at(9)).isEmpty())
        val startOnly = task().copy(remindBeforeMinutes = 0)
        assertEquals(listOf(ReminderKind.START), Reminders.between(listOf(startOnly), at(6), at(9)).map { it.kind })
    }

    @Test
    fun `시간창이 알림 간격보다 짧으면 마감 전 알림은 넣지 않는다`() {
        val short = task(durationMinutes = 10).copy(remindBeforeMinutes = 15)
        assertEquals(listOf(ReminderKind.START), Reminders.between(listOf(short), at(6), at(9)).map { it.kind })
    }

    @Test
    fun `다음 알림은 지금 이후 가장 이른 것`() {
        assertEquals(at(7, 45), Reminders.nextTime(listOf(task()), at(7, 10)))
        // 오늘 것을 다 지났으면 내일 시작 알림
        assertEquals(at(7, date = DAY.plusDays(1)), Reminders.nextTime(listOf(task()), at(8)))
    }

    @Test
    fun `반복 요일이 아닌 날은 건너뛴다`() {
        // 목요일 저녁 기준, 월요일만 반복하는 과제의 다음 알림은 다음 주 월요일
        val mondayOnly = task(repeatDays = RepeatDays.bit(java.time.DayOfWeek.MONDAY))
        assertEquals(at(7, date = DAY.plusDays(4)), Reminders.nextTime(listOf(mondayOnly), at(20)))
    }

    @Test
    fun `꺼진 과제는 알리지 않는다`() {
        assertNull(Reminders.nextTime(listOf(task(enabled = false)), at(6)))
    }
}

package com.getfuckocus.ui

import com.getfuckocus.DAY
import com.getfuckocus.at
import com.getfuckocus.data.PassedOccurrence
import com.getfuckocus.task
import com.getfuckocus.task.TaskSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStatusTest {
    private val passedToday = setOf(PassedOccurrence(1, TaskSchedule.dayKey(DAY)))

    @Test
    fun `시작 전`() {
        assertEquals(TaskStatus.Upcoming(at(7)), taskStatus(task(), at(6), emptySet()))
    }

    @Test
    fun `진행 중이면 지난 비율을 계산한다`() {
        val status = taskStatus(task(durationMinutes = 60), at(7, 15), emptySet())
        assertTrue(status is TaskStatus.InProgress)
        assertEquals(0.25f, (status as TaskStatus.InProgress).progress, 0.001f)
    }

    @Test
    fun `통과했으면 진행 중이어도 완료`() {
        assertEquals(TaskStatus.Done, taskStatus(task(), at(7, 30), passedToday))
        assertEquals(TaskStatus.Done, taskStatus(task(), at(12), passedToday))
    }

    @Test
    fun `마감이 지났는데 통과 기록이 없으면 놓침`() {
        assertEquals(TaskStatus.Missed, taskStatus(task(), at(12), emptySet()))
    }

    @Test
    fun `마감 뒤에 만든 과제는 내일 회차를 기다린다`() {
        val created = task(createdAt = at(9).toInstant().toEpochMilli())
        assertEquals(TaskStatus.Upcoming(at(7, date = DAY.plusDays(1))), taskStatus(created, at(10), emptySet()))
    }

    @Test
    fun `꺼진 과제`() {
        assertEquals(TaskStatus.Disabled, taskStatus(task(enabled = false), at(7, 30), emptySet()))
    }
}

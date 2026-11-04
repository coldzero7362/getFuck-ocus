package com.getfuckocus.ui

import com.getfuckocus.data.PassedOccurrence
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.task.TaskSchedule
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** 홈 화면 카드에 보여줄 과제의 현재 상태. */
sealed interface TaskStatus {
    data object Disabled : TaskStatus
    data class Upcoming(val startsAt: ZonedDateTime) : TaskStatus
    data class InProgress(val deadline: ZonedDateTime, val progress: Float) : TaskStatus
    data object Done : TaskStatus
    data object Missed : TaskStatus
}

fun taskStatus(task: TaskEntity, now: ZonedDateTime, passed: Set<PassedOccurrence>): TaskStatus {
    if (!task.enabled) return TaskStatus.Disabled
    fun passedOn(date: LocalDate) = PassedOccurrence(task.id, TaskSchedule.dayKey(date)) in passed

    TaskSchedule.occurrenceContaining(task, now)?.let { date ->
        if (passedOn(date)) return TaskStatus.Done
        val start = TaskSchedule.windowStart(task, date, now.zone)
        val deadline = TaskSchedule.deadline(task, date, now.zone)
        val total = Duration.between(start, deadline).toMillis().toFloat()
        val elapsed = Duration.between(start, now).toMillis().toFloat()
        return TaskStatus.InProgress(deadline, (elapsed / total).coerceIn(0f, 1f))
    }

    val today = now.toLocalDate()
    val todayStart = TaskSchedule.windowStart(task, today, now.zone)
    if (now.isBefore(todayStart)) return TaskStatus.Upcoming(todayStart)

    // 오늘 회차는 끝났다
    if (passedOn(today)) return TaskStatus.Done
    val deadline = TaskSchedule.deadline(task, today, now.zone)
    if (task.createdAt >= deadline.toInstant().toEpochMilli()) {
        // 오늘 마감이 지난 뒤에 만든 과제는 내일부터
        return TaskStatus.Upcoming(TaskSchedule.windowStart(task, today.plusDays(1), now.zone))
    }
    return TaskStatus.Missed
}

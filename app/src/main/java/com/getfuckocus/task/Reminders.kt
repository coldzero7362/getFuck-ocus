package com.getfuckocus.task

import com.getfuckocus.data.TaskEntity
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class ReminderKind { START, BEFORE_DEADLINE }

/** 어느 과제의 어느 회차([dayKey])에 대해 [at]에 울릴 알림 하나. */
data class Reminder(
    val taskId: Long,
    val kind: ReminderKind,
    val at: ZonedDateTime,
    val dayKey: String,
)

object Reminders {
    /** 반복 요일이 아무리 드물어도 일주일 안에는 회차가 있으므로 그만큼만 내다본다 */
    private const val LOOKAHEAD_DAYS = 8L

    /** ([from], [until]] 사이에 울릴 알림을 시각 순으로. 꺼진 과제와 알림을 끈 과제는 뺀다. */
    fun between(tasks: List<TaskEntity>, from: ZonedDateTime, until: ZonedDateTime): List<Reminder> {
        val firstDate = from.toLocalDate().minusDays(1)
        val days = ChronoUnit.DAYS.between(firstDate, until.toLocalDate())
        return tasks.filter { it.enabled }
            .flatMap { task ->
                (0..days).map { firstDate.plusDays(it) }
                    .filter { TaskSchedule.hasOccurrence(task, it, from.zone) }
                    .flatMap { date -> remindersOf(task, date, from) }
            }
            .filter { it.at.isAfter(from) && !it.at.isAfter(until) }
            .sortedBy { it.at.toInstant() }
    }

    /** [now] 뒤에 오는 가장 이른 알림 시각. 알람은 이 시각 하나만 건다. */
    fun nextTime(tasks: List<TaskEntity>, now: ZonedDateTime): ZonedDateTime? =
        between(tasks, now, now.plusDays(LOOKAHEAD_DAYS)).firstOrNull()?.at

    private fun remindersOf(task: TaskEntity, date: LocalDate, now: ZonedDateTime): List<Reminder> {
        val start = TaskSchedule.windowStart(task, date, now.zone)
        val deadline = TaskSchedule.deadline(task, date, now.zone)
        val dayKey = TaskSchedule.dayKey(date)
        return buildList {
            if (task.remindAtStart) add(Reminder(task.id, ReminderKind.START, start, dayKey))
            if (task.remindBeforeMinutes > 0) {
                val before = deadline.minusMinutes(task.remindBeforeMinutes.toLong())
                // 시간창이 알림 간격보다 짧으면 시작 알림과 겹치므로 넣지 않는다
                if (before.isAfter(start)) add(Reminder(task.id, ReminderKind.BEFORE_DEADLINE, before, dayKey))
            }
        }
    }
}

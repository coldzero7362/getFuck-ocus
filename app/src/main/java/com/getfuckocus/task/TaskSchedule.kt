package com.getfuckocus.task

import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.TaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 과제는 고른 요일마다 [TaskEntity.windowStartMinute]에 시작해 [TaskEntity.durationMinutes] 뒤 마감되는 "회차"를 가진다.
 * 회차는 시작 날짜로 구분하며(dayKey), 소요 시간이 하루보다 짧으므로 자정을 넘어도 어제·오늘 회차만 보면 된다.
 * 반복 요일이 아니어도 그날 "바로 시작"했다면 회차가 생긴다.
 */
object TaskSchedule {
    const val DEFAULT_DURATION_MINUTES = 60
    const val MAX_DURATION_MINUTES = 24 * 60 - 1

    /** 반복 요일이 아무리 드문드문해도 일주일 안에는 다음 회차가 있다 */
    private const val LOOKAHEAD_DAYS = 8L

    fun dayKey(date: LocalDate): String = date.toString()

    /** [date]에 이 과제의 회차가 있는지. 반복 요일이거나, 그날 바로 시작했으면 있다. */
    fun hasOccurrence(task: TaskEntity, date: LocalDate, zone: ZoneId): Boolean =
        RepeatDays.contains(task.repeatDays, date.dayOfWeek) || startedOn(task, zone) == date

    private fun startedOn(task: TaskEntity, zone: ZoneId): LocalDate? =
        task.startedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun windowStart(task: TaskEntity, date: LocalDate, zone: ZoneId): ZonedDateTime {
        // 그날 "바로 시작"했다면 그 시각이 회차의 시작이다
        task.startedAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
            ?.takeIf { it.toLocalDate() == date }
            ?.let { return it }
        return date.atStartOfDay(zone).plusMinutes(task.windowStartMinute.toLong())
    }

    fun deadline(task: TaskEntity, date: LocalDate, zone: ZoneId): ZonedDateTime =
        windowStart(task, date, zone).plusMinutes(task.durationMinutes.toLong())

    /** [at]이 어느 회차의 시간창 안에 있으면 그 회차의 시작 날짜. 아니면 null. */
    fun occurrenceContaining(task: TaskEntity, at: ZonedDateTime): LocalDate? =
        candidateDates(at).firstOrNull { date ->
            hasOccurrence(task, date, at.zone) &&
                !at.isBefore(windowStart(task, date, at.zone)) && at.isBefore(deadline(task, date, at.zone))
        }

    /** 오늘 회차와, 자정을 넘어 아직 영향이 남아 있을 수 있는 어제 회차. 최근 것부터. */
    fun candidateDates(at: ZonedDateTime): List<LocalDate> =
        listOf(at.toLocalDate(), at.toLocalDate().minusDays(1))

    /** [from] 이후에 시작하는 가장 가까운 회차의 시작 시각. 반복 요일이 하나도 없으면 null. */
    fun nextOccurrenceStart(task: TaskEntity, from: ZonedDateTime): ZonedDateTime? =
        (0..LOOKAHEAD_DAYS).asSequence()
            .map { from.toLocalDate().plusDays(it) }
            .filter { hasOccurrence(task, it, from.zone) }
            .map { windowStart(task, it, from.zone) }
            .firstOrNull { it.isAfter(from) }

    /** [now] 이후 가장 가까운 마감. 꺼진 과제는 뺀다. 마감 알람을 걸 때 쓴다. */
    fun nextDeadline(tasks: List<TaskEntity>, now: ZonedDateTime): ZonedDateTime? {
        val today = now.toLocalDate()
        return tasks.filter { it.enabled }
            .flatMap { task ->
                (-1L..LOOKAHEAD_DAYS).map { today.plusDays(it) }
                    .filter { hasOccurrence(task, it, now.zone) }
                    .map { deadline(task, it, now.zone) }
            }
            .filter { it.isAfter(now) }
            .minByOrNull { it.toInstant() }
    }

    /** 새 과제의 기본 시작 시각: 지금 시각을 10분 단위로 내린 값 (예: 14:37 → 14:30). */
    fun defaultStartMinute(now: LocalTime = LocalTime.now()): Int =
        now.hour * 60 + now.minute / 10 * 10

    fun formatMinute(minute: Int): String {
        val m = minute % (24 * 60)
        return "%02d:%02d".format(m / 60, m % 60)
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "${rest}분"
            rest == 0 -> "${hours}시간"
            else -> "${hours}시간 ${rest}분"
        }
    }

    /** "12:30부터 1시간 (13:30 마감)" */
    fun describeWindow(task: TaskEntity): String =
        describeWindow(task.windowStartMinute, task.durationMinutes)

    fun describeWindow(startMinute: Int, durationMinutes: Int): String {
        val end = startMinute + durationMinutes
        val nextDay = if (end >= 24 * 60) "다음 날 " else ""
        return "${formatMinute(startMinute)}부터 ${formatDuration(durationMinutes)} " +
            "($nextDay${formatMinute(end)} 마감)"
    }

    /** "매일", "평일", "주말", "월·수·금" */
    fun describeDays(mask: Int): String = when (mask and RepeatDays.EVERY_DAY) {
        RepeatDays.EVERY_DAY -> "매일"
        RepeatDays.WEEKDAYS -> "평일"
        RepeatDays.WEEKEND -> "주말"
        0 -> "반복 없음"
        else -> DayOfWeek.entries.filter { RepeatDays.contains(mask, it) }.joinToString("·") { dayName(it) }
    }

    fun dayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}

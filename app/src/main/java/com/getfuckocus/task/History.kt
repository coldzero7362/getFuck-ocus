package com.getfuckocus.task

import com.getfuckocus.data.PassRecord
import com.getfuckocus.data.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** 한 과제의 한 회차 결과. */
enum class OccurrenceResult {
    /** 그날 회차가 없다 (반복 요일이 아니거나, 과제를 만들기 전) */
    NONE,
    /** 마감 전에 통과 */
    PASSED,
    /** 마감을 놓친 뒤 늦게 인증해서 잠금을 풀었다. 연속 달성은 끊긴다. */
    LATE,
    MISSED,
    /** 아직 마감 전 */
    PENDING,
}

/** 달력 칸 하나의 결과: 그날 회차들을 합친 것. */
enum class DayResult { NONE, PASSED, PARTIAL, MISSED, PENDING, FUTURE }

/** 회차 결과를 [PassRecord] 목록에서 빠르게 찾기 위한 색인. */
class PassIndex(records: List<PassRecord>) {
    private val firstPassedAt = records.associate { (it.taskId to it.dayKey) to it.firstPassedAt }
    fun firstPassedAt(taskId: Long, dayKey: String): Long? = firstPassedAt[taskId to dayKey]
}

object History {
    /** 연속 달성을 셀 때 거슬러 올라가는 최대 일수 */
    private const val MAX_STREAK_DAYS = 400

    fun occurrenceResult(task: TaskEntity, date: LocalDate, now: ZonedDateTime, passes: PassIndex): OccurrenceResult {
        if (!TaskSchedule.hasOccurrence(task, date, now.zone)) return OccurrenceResult.NONE
        val deadline = TaskSchedule.deadline(task, date, now.zone).toInstant().toEpochMilli()
        if (task.createdAt >= deadline) return OccurrenceResult.NONE
        val passedAt = passes.firstPassedAt(task.id, TaskSchedule.dayKey(date))
        return when {
            passedAt != null && passedAt < deadline -> OccurrenceResult.PASSED
            passedAt != null -> OccurrenceResult.LATE
            now.toInstant().toEpochMilli() < deadline -> OccurrenceResult.PENDING
            else -> OccurrenceResult.MISSED
        }
    }

    fun dayResult(tasks: List<TaskEntity>, date: LocalDate, now: ZonedDateTime, passes: PassIndex): DayResult {
        if (date.isAfter(now.toLocalDate())) return DayResult.FUTURE
        val results = tasks.filter { it.enabled }
            .map { occurrenceResult(it, date, now, passes) }
            .filter { it != OccurrenceResult.NONE }
        if (results.isEmpty()) return DayResult.NONE
        val good = results.count { it == OccurrenceResult.PASSED }
        val bad = results.count { it == OccurrenceResult.MISSED || it == OccurrenceResult.LATE }
        return when {
            bad > 0 && good > 0 -> DayResult.PARTIAL
            bad > 0 -> DayResult.MISSED
            good == results.size -> DayResult.PASSED
            else -> DayResult.PENDING
        }
    }

    /**
     * 가장 최근 회차부터 거슬러 올라가며 마감 전에 통과한 회차 수를 센다.
     * 아직 마감 전인 오늘 회차는 끊긴 것으로 보지 않고 건너뛴다. 놓치거나 늦게 통과하면 거기서 멈춘다.
     */
    fun streak(task: TaskEntity, now: ZonedDateTime, passes: PassIndex): Int {
        val createdDate = Instant.ofEpochMilli(task.createdAt).atZone(now.zone).toLocalDate()
        var date = now.toLocalDate()
        var count = 0
        repeat(MAX_STREAK_DAYS) {
            if (date.isBefore(createdDate)) return count
            when (occurrenceResult(task, date, now, passes)) {
                OccurrenceResult.PASSED -> count++
                OccurrenceResult.MISSED, OccurrenceResult.LATE -> return count
                OccurrenceResult.PENDING, OccurrenceResult.NONE -> Unit
            }
            date = date.minusDays(1)
        }
        return count
    }
}

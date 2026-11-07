package com.getfuckocus.task

import com.getfuckocus.data.TaskDao
import com.getfuckocus.lock.LockController
import com.getfuckocus.safety.DeviceClock
import com.getfuckocus.safety.SafetyPolicy
import java.time.ZonedDateTime

class DeadlineEvaluator(
    private val dao: TaskDao,
    private val lock: LockController,
    private val clock: DeviceClock,
) {
    /**
     * 마감이 지났는데 통과 기록이 없는 회차가 있으면 잠근다. 새로 잠갔으면 true.
     * 기본값은 시계를 뒤로 돌려도 줄어들지 않는 시각이라, 시계 조작으로 마감을 미룰 수 없다.
     */
    suspend fun evaluate(now: ZonedDateTime = clock.trustedNow()): Boolean {
        if (lock.current() != null) return false
        val nowMillis = now.toInstant().toEpochMilli()

        for (task in dao.enabledTasks()) {
            for (date in TaskSchedule.candidateDates(now)) {
                // 반복 요일이 아니면 그날은 회차가 없다
                if (!TaskSchedule.hasOccurrence(task, date, now.zone)) continue
                val deadlineMillis = TaskSchedule.deadline(task, date, now.zone).toInstant().toEpochMilli()
                val dayKey = TaskSchedule.dayKey(date)
                val missed = nowMillis >= deadlineMillis &&
                    // 최대 잠금 시간보다 오래전에 놓친 회차는 잠가도 바로 풀리므로 건너뛴다 (기기를 꺼 둔 경우 등)
                    nowMillis < deadlineMillis + SafetyPolicy.MAX_LOCK_DURATION_MS &&
                    // 마감이 지난 뒤에 만든 과제로 바로 잠기지 않도록
                    task.createdAt < deadlineMillis &&
                    !lock.isHandled(task.id, dayKey) &&
                    !dao.passedOn(task.id, dayKey)
                if (missed) {
                    lock.lock(task.id, task.title, dayKey)
                    return true
                }
            }
        }
        return false
    }
}

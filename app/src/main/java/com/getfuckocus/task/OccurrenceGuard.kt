package com.getfuckocus.task

import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.lock.LockController
import java.time.ZonedDateTime

/**
 * 과제 조건을 바꿔 약속에서 빠져나가는 것을 막는다.
 * 진행 중이면서 아직 통과하지 않은 회차가 있거나, 이 과제 때문에 잠겨 있는 동안에는
 * 시간·조건·사용 여부를 바꾸거나 과제를 지울 수 없다. 이름은 언제든 바꿀 수 있다.
 *
 * @return 편집이 막히는 이유. 막히지 않으면 null.
 */
suspend fun editFreezeReason(
    task: TaskEntity,
    dao: TaskDao,
    lock: LockController,
    now: ZonedDateTime,
): String? {
    if (lock.current()?.taskId == task.id) return "이 과제 때문에 잠겨 있는 동안에는 조건을 바꿀 수 없어요"
    if (!task.enabled) return null
    val date = TaskSchedule.occurrenceContaining(task, now) ?: return null
    if (dao.passedOn(task.id, TaskSchedule.dayKey(date))) return null
    val deadline = TaskSchedule.deadline(task, date, now.zone)
    return "진행 중인 회차가 ${TaskSchedule.formatMinute(deadline.hour * 60 + deadline.minute)}에 끝나야 " +
        "시간과 조건을 바꿀 수 있어요. 이름만 바꿀 수 있어요"
}

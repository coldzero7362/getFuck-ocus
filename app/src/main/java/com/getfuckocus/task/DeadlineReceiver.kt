package com.getfuckocus.task

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getfuckocus.container
import com.getfuckocus.lock.BlockerServiceStatus
import com.getfuckocus.lock.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 마감 알람과 과제 알림 알람 때 깨어난다. [DeadlineScheduler]가 건 앱 내부 알람만 받으므로 내보내지 않는다
 * (exported=false). 다른 앱이 지난 시각을 넣어 알림을 쏟아내게 하거나 판정을 흔들지 못하게 하기 위해서다.
 * 재부팅·앱 업데이트는 [BootReceiver]가 받는다.
 */
class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderAt = when (intent.action) {
            ACTION_DEADLINE -> null
            ACTION_REMINDER -> intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())
            else -> return
        }
        runAsync(this) { DeadlineWork.run(context, reminderAt) }
    }

    companion object {
        const val ACTION_DEADLINE = "com.getfuckocus.action.DEADLINE"
        const val ACTION_REMINDER = "com.getfuckocus.action.REMINDER"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
    }
}

/**
 * 재부팅·앱 업데이트 뒤에 마감을 다시 판정하고 알람을 다시 건다.
 * 시스템 브로드캐스트를 받으려고 내보내지만, 시스템만 보낼 수 있는 동작만 처리하고 다른 것은 무시한다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runAsync(this) { DeadlineWork.run(context, reminderScheduledAt = null) }
    }
}

private fun runAsync(receiver: BroadcastReceiver, work: suspend () -> Unit) {
    val pending = receiver.goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            work()
        } finally {
            pending.finish()
        }
    }
}

/**
 * - 마감을 판정하고, 잠금이 새로 걸리면 알린다. 잠금 서비스가 꺼져 있으면 실제로 막지는 못하므로 그 사실을 알린다.
 * - 알림 시각이면 그 시각에 해당하는 과제 알림을 띄운다. 이미 통과한 회차는 알리지 않는다.
 * - 끝나면 다음 알람을 다시 건다.
 */
private object DeadlineWork {
    /** 알람이 몇 초 일찍 울려도 그 시각의 알림을 놓치지 않게 */
    private const val DUE_SLACK_MS = 60_000L

    suspend fun run(context: Context, reminderScheduledAt: Long?) {
        val container = context.container
        reminderScheduledAt?.let { showDueReminders(context, it) }
        if (container.deadlineEvaluator.evaluate()) {
            container.lockController.current()?.let { lock ->
                NotificationHelper.showLocked(
                    context,
                    taskId = lock.taskId,
                    taskTitle = lock.taskTitle,
                    mode = lock.mode,
                    serviceEnabled = BlockerServiceStatus.isEnabled(context),
                )
            }
        }
        container.deadlineScheduler.schedule(container.taskDao.enabledTasks())
    }

    /** 알람이 걸린 시각부터 지금까지 울려야 할 알림을 모두 띄운다 (같은 시각에 여러 개일 수 있다). */
    private suspend fun showDueReminders(context: Context, scheduledAt: Long) {
        val dao = context.container.taskDao
        val tasks = dao.enabledTasks()
        val zone = ZoneId.systemDefault()
        val from = Instant.ofEpochMilli(scheduledAt - DUE_SLACK_MS).atZone(zone)
        val now = ZonedDateTime.now(zone)
        for (reminder in Reminders.between(tasks, from, now)) {
            if (dao.passedOn(reminder.taskId, reminder.dayKey)) continue
            val task = tasks.firstOrNull { it.id == reminder.taskId } ?: continue
            val date = LocalDate.parse(reminder.dayKey)
            NotificationHelper.showReminder(context, task, reminder.kind, TaskSchedule.deadline(task, date, zone))
        }
    }
}

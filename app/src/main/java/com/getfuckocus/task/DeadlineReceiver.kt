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
 * 마감 알람, 과제 알림 알람, 재부팅, 앱 업데이트 때 깨어난다.
 * - 마감을 판정하고, 잠금이 새로 걸리면 알린다. 잠금 서비스가 꺼져 있으면 실제로 막지는 못하므로 그 사실을 알린다.
 * - 알림 시각이면 그 시각에 해당하는 과제 알림을 띄운다. 이미 통과한 회차는 알리지 않는다.
 * - 끝나면 다음 알람을 다시 건다.
 */
class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        val container = context.container
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (intent.action == ACTION_REMINDER) {
                    showDueReminders(context, intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis()))
                }
                if (container.deadlineEvaluator.evaluate()) {
                    container.lockController.current()?.let { lock ->
                        NotificationHelper.showLocked(
                            context,
                            taskTitle = lock.taskTitle,
                            mode = lock.mode,
                            serviceEnabled = BlockerServiceStatus.isEnabled(context),
                        )
                    }
                }
                container.deadlineScheduler.schedule(container.taskDao.enabledTasks())
            } finally {
                pending.finish()
            }
        }
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

    companion object {
        const val ACTION_DEADLINE = "com.getfuckocus.action.DEADLINE"
        const val ACTION_REMINDER = "com.getfuckocus.action.REMINDER"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"

        /** 알람이 몇 초 일찍 울려도 그 시각의 알림을 놓치지 않게 */
        private const val DUE_SLACK_MS = 60_000L

        private val HANDLED_ACTIONS = setOf(
            ACTION_DEADLINE,
            ACTION_REMINDER,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

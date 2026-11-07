package com.getfuckocus.task

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.getfuckocus.data.TaskEntity
import java.time.ZonedDateTime

/**
 * 알람 두 개를 건다.
 * - 다음 마감 직후: [DeadlineReceiver]가 깨어나 마감을 판정한다. 접근성 서비스가 꺼져 있거나
 *   시스템이 서비스를 내렸어도 마감 판정과 알림은 일어나게 하기 위해서다.
 * - 다음 과제 알림 시각: 시작 알림, 마감 전 알림.
 *
 * 정확한 알람 권한(SCHEDULE_EXACT_ALARM)은 요청하지 않는다. 절전 중에는 몇 분 늦을 수 있지만,
 * 서비스가 켜져 있으면 서비스가 1분마다 따로 판정하므로 마감 알람은 안전망 역할만 한다.
 */
class DeadlineScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(tasks: List<TaskEntity>, now: ZonedDateTime = ZonedDateTime.now()) {
        set(
            pendingIntent(REQUEST_DEADLINE, DeadlineReceiver.ACTION_DEADLINE, null),
            TaskSchedule.nextDeadline(tasks, now)?.toInstant()?.toEpochMilli()?.plus(AFTER_DEADLINE_MS),
        )
        val reminderAt = Reminders.nextTime(tasks, now)?.toInstant()?.toEpochMilli()
        set(pendingIntent(REQUEST_REMINDER, DeadlineReceiver.ACTION_REMINDER, reminderAt), reminderAt)
    }

    private fun set(pending: PendingIntent, atMillis: Long?) {
        if (atMillis == null) {
            alarms.cancel(pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    private fun pendingIntent(requestCode: Int, action: String, scheduledAt: Long?): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, DeadlineReceiver::class.java)
                .setAction(action)
                .apply { scheduledAt?.let { putExtra(DeadlineReceiver.EXTRA_SCHEDULED_AT, it) } },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val REQUEST_DEADLINE = 0
        const val REQUEST_REMINDER = 1

        /** 마감 시각과 겹치지 않게 조금 뒤에 깨운다 */
        const val AFTER_DEADLINE_MS = 5_000L
    }
}

package com.getfuckocus.task

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.getfuckocus.data.TaskEntity
import java.time.ZonedDateTime

/**
 * 다음 마감 직후에 [DeadlineReceiver]가 깨어나도록 알람을 건다.
 * 접근성 서비스가 꺼져 있거나 시스템이 서비스를 내렸어도 마감 판정과 알림은 일어나게 하기 위해서다.
 *
 * 정확한 알람 권한(SCHEDULE_EXACT_ALARM)은 요청하지 않는다. 절전 중에는 몇 분 늦을 수 있지만,
 * 서비스가 켜져 있으면 서비스가 1분마다 따로 판정하므로 이 알람은 안전망 역할만 한다.
 */
class DeadlineScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(tasks: List<TaskEntity>, now: ZonedDateTime = ZonedDateTime.now()) {
        val pending = pendingIntent()
        val next = TaskSchedule.nextDeadline(tasks, now)
        if (next == null) {
            alarms.cancel(pending)
            return
        }
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli() + AFTER_DEADLINE_MS,
            pending,
        )
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DeadlineReceiver::class.java).setAction(DeadlineReceiver.ACTION_DEADLINE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        /** 마감 시각과 겹치지 않게 조금 뒤에 깨운다 */
        const val AFTER_DEADLINE_MS = 5_000L
    }
}

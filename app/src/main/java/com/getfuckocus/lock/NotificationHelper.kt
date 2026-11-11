package com.getfuckocus.lock

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.getfuckocus.MainActivity
import com.getfuckocus.R
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.task.CheckInReceiver
import com.getfuckocus.verify.CheckInResult
import com.getfuckocus.verify.Verdict
import com.getfuckocus.hasPermission
import com.getfuckocus.task.ReminderKind
import com.getfuckocus.task.TaskSchedule
import java.time.ZonedDateTime

/**
 * - 잠금 서비스가 켜져 있는 동안 상시 알림을 띄워 숨어서 동작하지 않게 한다.
 * - 마감을 놓쳐 잠기면 알린다. 서비스가 꺼져 있어 실제로 막지 못할 때도 알린다.
 */
object NotificationHelper {
    private const val CHANNEL_ACTIVE = "service_active"
    private const val CHANNEL_ALERTS = "lock_alerts"
    private const val ID_SERVICE_ACTIVE = 1
    private const val ID_LOCKED = 2
    private const val CHANNEL_REMINDERS = "task_reminders"
    private const val ID_REMINDER_BASE = 1000
    private const val CHECK_IN_RESULT_TIMEOUT_MS = 4_000L

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVE, context.getString(R.string.channel_active), NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /**
     * 과제 시작·마감 전 알림.
     * - 사진 인증 과제: 누르면 그 과제의 촬영 화면이 열린다.
     * - 탭 체크 과제: "완료" 버튼으로 앱을 열지 않고 바로 체크한다. 누르면 홈이 열린다.
     *   장소 조건이 있으면 앱이 꺼진 채로는 위치를 잴 수 없어서(백그라운드 위치 권한을 받지 않음) 버튼을 달지 않는다.
     */
    fun showReminder(context: Context, task: TaskEntity, kind: ReminderKind, deadline: ZonedDateTime) {
        val deadlineText = TaskSchedule.formatMinute(deadline.hour * 60 + deadline.minute)
        val tap = task.verifyMethod == VerifyMethod.TAP
        val (title, text) = when (kind) {
            ReminderKind.START ->
                "'${task.title}' 시작했어요" to
                    if (tap) "${deadlineText}까지 완료를 눌러 체크하세요." else "${deadlineText}까지 사진으로 인증하세요."
            ReminderKind.BEFORE_DEADLINE ->
                "'${task.title}' 마감 ${task.remindBeforeMinutes}분 전" to
                    if (tap) "${deadlineText}까지 체크해 주세요." else "${deadlineText}까지 인증하지 않으면 잠겨요."
        }
        val id = reminderId(task.id, kind)
        val open = if (tap) Intent(context, MainActivity::class.java) else MainActivity.captureIntent(context, task.id)
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        if (tap && !task.hasGeoFence()) {
            val checkIn = Intent(context, CheckInReceiver::class.java)
                .setAction(CheckInReceiver.ACTION_CHECK_IN)
                .putExtra(CheckInReceiver.EXTRA_TASK_ID, task.id)
                .putExtra(CheckInReceiver.EXTRA_NOTIFICATION_ID, id)
            builder.addAction(
                android.R.drawable.checkbox_on_background,
                "완료",
                PendingIntent.getBroadcast(context, id, checkIn, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
        // 과제·종류마다 따로 쌓이고, 같은 과제의 같은 알림은 새것으로 바뀐다
        notify(context, id, builder)
    }

    /** 알림의 "완료" 버튼 결과. 통과하면 몇 초 뒤 저절로 사라진다. */
    fun showCheckInResult(context: Context, notificationId: Int, taskTitle: String, result: CheckInResult) {
        val text = when (val verdict = result.verdict) {
            Verdict.Pass -> if (result.late) "늦었지만 기록했어요." else "완료했어요. 잘했어요!"
            is Verdict.Fail -> verdict.reasons.joinToString(" ")
        }
        notify(
            context, notificationId,
            NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.checkbox_on_background)
                .setContentTitle("'$taskTitle'")
                .setContentText(text)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .apply { if (result.verdict == Verdict.Pass) setTimeoutAfter(CHECK_IN_RESULT_TIMEOUT_MS) },
        )
    }

    private fun reminderId(taskId: Long, kind: ReminderKind): Int = ID_REMINDER_BASE + (taskId * 2 + kind.ordinal).toInt()

    fun showServiceActive(context: Context) {
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        notify(
            context, ID_SERVICE_ACTIVE,
            NotificationCompat.Builder(context, CHANNEL_ACTIVE)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(context.getString(R.string.notification_active_title))
                .setContentText(context.getString(R.string.notification_active_text))
                .setOngoing(true)
                .setContentIntent(openApp),
        )
    }

    fun cancelServiceActive(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_SERVICE_ACTIVE)
    }

    fun showLocked(context: Context, taskTitle: String, serviceEnabled: Boolean) {
        val (text, target) = if (serviceEnabled) {
            "'$taskTitle' 마감을 놓쳐서 잠겼어요. 지금 인증하면 풀려요." to LockActivity.intent(context)
        } else {
            "'$taskTitle' 마감을 놓쳤지만 잠금 서비스가 꺼져 있어 잠기지 않았어요. 눌러서 켜 주세요." to
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        notify(
            context, ID_LOCKED,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(if (serviceEnabled) "잠금 중" else "잠금 서비스가 꺼져 있어요")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, ID_LOCKED, target, PendingIntent.FLAG_IMMUTABLE)),
        )
    }

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) return
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}

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
import com.getfuckocus.hasPermission

/**
 * - 잠금 서비스가 켜져 있는 동안 상시 알림을 띄워 숨어서 동작하지 않게 한다.
 * - 마감을 놓쳐 잠기면 알린다. 서비스가 꺼져 있어 실제로 막지 못할 때도 알린다.
 */
object NotificationHelper {
    private const val CHANNEL_ACTIVE = "service_active"
    private const val CHANNEL_ALERTS = "lock_alerts"
    private const val ID_SERVICE_ACTIVE = 1
    private const val ID_LOCKED = 2

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVE, context.getString(R.string.channel_active), NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH)
        )
    }

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

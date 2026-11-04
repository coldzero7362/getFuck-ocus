package com.getfuckocus.lock

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.getfuckocus.MainActivity
import com.getfuckocus.R
import com.getfuckocus.hasPermission

/** 잠금 서비스가 켜져 있는 동안 상시 알림을 띄워 숨어서 동작하지 않게 한다. */
object NotificationHelper {
    private const val CHANNEL_ID = "service_active"
    private const val ID_SERVICE_ACTIVE = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_active),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showServiceActive(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) return

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(context.getString(R.string.notification_active_title))
            .setContentText(context.getString(R.string.notification_active_text))
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(ID_SERVICE_ACTIVE, notification)
    }

    fun cancelServiceActive(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_SERVICE_ACTIVE)
    }
}

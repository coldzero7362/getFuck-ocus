package com.getfuckocus.lock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object BlockerServiceStatus {
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AppBlockerService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

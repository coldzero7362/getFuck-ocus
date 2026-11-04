package com.getfuckocus.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit

data class InstalledApp(val packageName: String, val label: String)

/** 잠금 중에도 열 수 있는 앱 목록. */
class Whitelist(private val context: Context) {
    private val prefs = context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)

    @Volatile
    private var systemCache: Set<String>? = null

    fun isAllowed(packageName: String): Boolean =
        packageName in systemPackages() || packageName in userPackages()

    /** 항상 허용되며 사용자가 끌 수 없는 앱: 런처, 전화, 문자, 알람, 설정, 키보드, 시스템 UI, 이 앱. */
    fun systemPackages(): Set<String> =
        systemCache ?: computeSystemPackages().also { systemCache = it }

    /** 사용자가 추가로 허용한 앱. 처음에는 지도와 2단계 인증 앱이 기본으로 들어 있다. */
    fun userPackages(): Set<String> =
        prefs.getStringSet(KEY_USER, null) ?: DEFAULT_USER_PACKAGES

    fun setUserAllowed(packageName: String, allowed: Boolean) {
        val updated = if (allowed) userPackages() + packageName else userPackages() - packageName
        prefs.edit { putStringSet(KEY_USER, updated) }
    }

    /** 설정 화면에 보여줄, 실행 가능한 앱 중 시스템 허용 목록에 없는 것들. */
    fun launchableApps(): List<InstalledApp> {
        val pm = context.packageManager
        val system = systemPackages()
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .filterNot { it.packageName in system }
            .sortedBy { it.label }
    }

    private fun computeSystemPackages(): Set<String> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(Settings.ACTION_SETTINGS),
        )
        val resolved = intents.flatMap { pm.queryIntentActivities(it, 0) }.map { it.activityInfo.packageName }
        val keyboards = context.getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            .orEmpty()
        return buildSet {
            add(context.packageName)
            addAll(ALWAYS_ALLOWED)
            addAll(resolved)
            addAll(keyboards)
        }
    }

    private companion object {
        const val KEY_USER = "user_packages"

        val ALWAYS_ALLOWED = setOf(
            "android",
            "com.android.systemui",
            // 전화 / 긴급전화
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.emergency",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            // 권한 대화상자와 앱 삭제는 막지 않는다
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )

        val DEFAULT_USER_PACKAGES = setOf(
            "com.google.android.apps.maps",
            "com.nhn.android.nmap",
            "net.daum.android.map",
            "com.google.android.apps.authenticator2",
            "com.azure.authenticator",
        )
    }
}

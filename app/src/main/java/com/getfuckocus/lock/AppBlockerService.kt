package com.getfuckocus.lock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.getfuckocus.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 잠금 중에 허용 목록 밖의 앱이 앞으로 나오면 잠금 화면을 띄운다.
 * 창 전환 이벤트의 패키지명만 보고, 화면 내용은 읽지 않는다
 * (accessibility_service_config.xml의 canRetrieveWindowContent=false).
 */
class AppBlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastForeground: String? = null

    override fun onServiceConnected() {
        NotificationHelper.showServiceActive(this)
        scope.launch {
            while (isActive) {
                if (container.deadlineEvaluator.evaluate()) lastForeground?.let(::enforce)
                delay(EVALUATE_INTERVAL_MS)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        lastForeground = packageName
        enforce(packageName)
    }

    private fun enforce(packageName: String) {
        if (container.lockController.current() == null) return
        if (container.whitelist.isAllowed(packageName)) return
        startActivity(LockActivity.intent(this))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        NotificationHelper.cancelServiceActive(this)
        super.onDestroy()
    }

    private companion object {
        const val EVALUATE_INTERVAL_MS = 60_000L
    }
}

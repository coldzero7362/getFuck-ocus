package com.getfuckocus.lock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.getfuckocus.container
import com.getfuckocus.data.MissAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 잠금 중에 허용 목록 밖의 앱이 앞으로 나오면 잠금 화면을 띄운다.
 * 억제 화면 모드에서는 같은 화면을 [NagPolicy.INTERVAL_MS]마다 한 번씩만 띄운다.
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
                container.deadlineEvaluator.evaluate()
                // 잠금은 마감 알람 쪽에서 걸릴 수도 있으므로, 새로 잠갔는지와 상관없이 지금 앞에 있는 앱을 다시 확인한다
                lastForeground?.let(::enforce)
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
        val lock = container.lockController.current() ?: return
        if (container.whitelist.isAllowed(packageName)) return
        // 잠금은 매번 막고, 억제 화면은 몇 분마다 한 번씩만 덮는다 (그사이에는 쓰던 앱을 계속 쓸 수 있다)
        if (lock.mode == MissAction.NAG && !container.lockController.claimNagSlot()) return
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

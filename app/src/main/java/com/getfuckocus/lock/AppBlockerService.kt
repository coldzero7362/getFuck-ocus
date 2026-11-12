package com.getfuckocus.lock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.getfuckocus.container
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.nagImagePaths
import com.getfuckocus.data.nagMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * - 잠금: 허용 목록 밖의 앱이 앞으로 나오면 잠금 화면을 띄운다.
 * - 억제 화면: 화면을 막지 않고, 쓰던 앱 위로 설정한 문구·사진이 1분마다 흘러가게 한다 ([NagOverlay]).
 *   인증하거나 비상 해제·자동 해제될 때까지 계속 반복한다. 전화·설정 화면 위에서는 숨긴다.
 * 창 전환 이벤트의 패키지명만 보고, 화면 내용은 읽지 않는다
 * (accessibility_service_config.xml의 canRetrieveWindowContent=false).
 */
class AppBlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastForeground: String? = null
    private lateinit var overlay: NagOverlay

    override fun onServiceConnected() {
        NotificationHelper.showServiceActive(this)
        overlay = NagOverlay(this)
        // 인증·비상 해제로 풀리면 바로 흘러가는 층을 걷는다
        scope.launch { container.lockController.state.collect { syncOverlay() } }
        scope.launch {
            while (isActive) {
                container.deadlineEvaluator.evaluate()
                // 잠금은 마감 알람 쪽에서 걸릴 수도 있으므로, 새로 잠갔는지와 상관없이 지금 앞에 있는 앱을 다시 확인한다
                lastForeground?.let(::enforce)
                // 자동 해제를 확인하고, 그사이 바꾼 문구·사진을 반영한다
                syncOverlay()
                delay(EVALUATE_INTERVAL_MS)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        // 흘러가는 층(우리 오버레이 창)도 이 앱 이름으로 창 이벤트를 낸다. 그걸 "앱을 열었다"로 읽으면
        // 스스로 숨어 버리므로, 이 앱에서 온 이벤트는 실제 화면(Activity)일 때만 받는다
        if (packageName == this.packageName && event.className?.toString()?.endsWith("Activity") != true) return
        lastForeground = packageName
        enforce(packageName)
        if (::overlay.isInitialized) overlay.setHidden(hideOverlayOver(packageName))
    }

    /** 전화·설정 화면, 그리고 이 앱 안(인증 화면에는 자기 흘러가는 층이 따로 있다)에서는 띄우지 않는다 */
    private fun hideOverlayOver(packageName: String?): Boolean =
        packageName == this.packageName || packageName in container.whitelist.safetyPackages()

    private fun enforce(packageName: String) {
        val lock = container.lockController.current() ?: return
        // 억제 화면은 앱을 막지 않는다
        if (lock.mode != MissAction.LOCK) return
        if (container.whitelist.isAllowed(packageName)) return
        startActivity(LockActivity.intent(this))
    }

    /** 억제 중이면 흘러가는 층을 띄우고(내용은 과제에서 읽는다), 아니면 걷는다 */
    private fun syncOverlay() {
        if (!::overlay.isInitialized) return
        val lock = container.lockController.current()
        if (lock == null || lock.mode != MissAction.NAG) {
            overlay.dismiss()
            return
        }
        scope.launch {
            val task = container.taskDao.get(lock.taskId)
            // 그사이 풀렸으면 띄우지 않는다
            if (container.lockController.current()?.taskId != lock.taskId) return@launch
            overlay.show(task?.nagMessages().orEmpty(), task?.nagImagePaths().orEmpty())
            overlay.setHidden(hideOverlayOver(lastForeground))
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        if (::overlay.isInitialized) overlay.dismiss()
        NotificationHelper.cancelServiceActive(this)
        super.onDestroy()
    }

    private companion object {
        const val EVALUATE_INTERVAL_MS = 60_000L
    }
}

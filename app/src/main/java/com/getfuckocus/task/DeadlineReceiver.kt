package com.getfuckocus.task

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getfuckocus.container
import com.getfuckocus.lock.BlockerServiceStatus
import com.getfuckocus.lock.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 마감 알람, 재부팅, 앱 업데이트 때 깨어나 마감을 판정하고 다음 알람을 다시 건다.
 * 잠금이 새로 걸리면 알림으로 알린다. 잠금 서비스가 꺼져 있으면 실제로 막지는 못하므로 그 사실을 알린다.
 */
class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        val container = context.container
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (container.deadlineEvaluator.evaluate()) {
                    container.lockController.current()?.let { lock ->
                        NotificationHelper.showLocked(
                            context,
                            taskTitle = lock.taskTitle,
                            serviceEnabled = BlockerServiceStatus.isEnabled(context),
                        )
                    }
                }
                container.deadlineScheduler.schedule(container.taskDao.enabledTasks())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DEADLINE = "com.getfuckocus.action.DEADLINE"
        private val HANDLED_ACTIONS = setOf(
            ACTION_DEADLINE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

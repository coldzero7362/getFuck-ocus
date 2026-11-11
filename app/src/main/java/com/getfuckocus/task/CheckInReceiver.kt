package com.getfuckocus.task

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getfuckocus.container
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.lock.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 탭 체크 과제 알림의 "완료" 버튼. 앱을 열지 않고 바로 체크하고 결과를 같은 알림 자리에 띄운다.
 * 다른 앱이 과제를 대신 완료 처리하지 못하게 내보내지 않는 수신기(exported=false)로 둔다.
 * 알림 버튼의 PendingIntent는 이 앱 자격으로 실행되므로 그대로 닿는다.
 */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK_IN) return
        val pending = goAsync()
        val container = context.container
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val task = container.taskDao.get(intent.getLongExtra(EXTRA_TASK_ID, -1)) ?: return@launch
                if (task.verifyMethod != VerifyMethod.TAP) return@launch
                val result = container.verificationFlow.checkIn(task)
                NotificationHelper.showCheckInResult(
                    context,
                    notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0),
                    taskTitle = task.title,
                    result = result,
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_IN = "com.getfuckocus.action.CHECK_IN"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}

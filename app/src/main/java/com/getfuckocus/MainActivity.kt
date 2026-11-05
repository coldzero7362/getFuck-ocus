package com.getfuckocus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.getfuckocus.ui.AppNavHost
import com.getfuckocus.ui.theme.GetFocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 상단 바가 짙은 카본이라 상태 표시줄 아이콘은 항상 밝게 둔다
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val captureTaskId = intent.getLongExtra(EXTRA_CAPTURE_TASK_ID, 0L).takeIf { it > 0 }
        setContent {
            GetFocusTheme {
                AppNavHost(startCaptureTaskId = captureTaskId)
            }
        }
    }

    companion object {
        private const val EXTRA_CAPTURE_TASK_ID = "capture_task_id"

        /** 잠금 화면의 "지금 인증하기"에서 바로 촬영 화면을 연다. */
        fun captureIntent(context: Context, taskId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_CAPTURE_TASK_ID, taskId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

package com.getfuckocus.lock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import com.getfuckocus.MainActivity
import com.getfuckocus.ui.LockScreen
import com.getfuckocus.ui.theme.GetFocusTheme

class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 뒤로 가기는 막힌 앱이 아니라 홈 화면으로 보낸다
        onBackPressedDispatcher.addCallback(this) { goHome() }
        setContent {
            GetFocusTheme {
                LockScreen(
                    onVerifyNow = { taskId ->
                        startActivity(MainActivity.captureIntent(this, taskId))
                        finish()
                    },
                    onUnlocked = { finish() },
                    onEmergencyCall = { number ->
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    },
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, LockActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
    }
}

package com.getfuckocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfuckocus.container
import com.getfuckocus.safety.SafetyPolicy
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusProgressBar
import com.getfuckocus.ui.components.FocusRing
import com.getfuckocus.ui.theme.LockColors
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    onVerifyNow: (taskId: Long) -> Unit,
    onUnlocked: () -> Unit,
    onEmergencyCall: (number: String) -> Unit,
) {
    val lock = LocalContext.current.container.lockController
    val state by lock.state.collectAsState()
    var autoRemaining by remember { mutableLongStateOf(SafetyPolicy.MAX_LOCK_DURATION_MS) }
    var emergencyRemaining by remember { mutableStateOf<Long?>(null) }

    val s = state
    LaunchedEffect(s) {
        if (s == null) {
            onUnlocked()
            return@LaunchedEffect
        }
        while (true) {
            // current()가 최대 잠금 시간 경과 시 자동 해제를 처리한다
            val live = lock.current() ?: break
            autoRemaining = lock.autoReleaseRemaining(live)
            emergencyRemaining = lock.emergencyRemaining(live)
            delay(1_000)
        }
    }
    if (s == null) return

    CompositionLocalProvider(LocalContentColor provides LockColors.content) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LockColors.background)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(LockColors.badge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = LockColors.onBadge, modifier = Modifier.size(36.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("잠금 중", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "'${s.taskTitle}' 과제를 마감까지 인증하지 못했어요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LockColors.contentMuted,
                    textAlign = TextAlign.Center,
                )
            }

            AutoReleaseRing(autoRemaining)

            FocusButton(
                "지금 인증하고 해제",
                onClick = { onVerifyNow(s.taskId) },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                containerColor = LockColors.accent,
                contentColor = LockColors.onAccent,
            )

            EmergencyUnlock(
                remaining = emergencyRemaining,
                onRequest = { lock.requestEmergencyUnlock() },
                onComplete = { lock.completeEmergencyUnlock() },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("112", "119").forEach { number ->
                    FocusButton(
                        "긴급전화 $number",
                        onClick = { onEmergencyCall(number) },
                        style = FocusButtonStyle.Ghost,
                        icon = Icons.Default.Phone,
                        height = 44.dp,
                        contentColor = LockColors.content,
                    )
                }
            }
            Text(
                "전화, 문자, 알람, 설정과 허용한 앱은 잠금 중에도 쓸 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = LockColors.contentMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AutoReleaseRing(remaining: Long) {
    Box(contentAlignment = Alignment.Center) {
        FocusRing(
            progress = remaining.toFloat() / SafetyPolicy.MAX_LOCK_DURATION_MS,
            modifier = Modifier.size(184.dp),
            color = LockColors.accent,
            trackColor = LockColors.track,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("자동 해제까지", style = MaterialTheme.typography.labelMedium, color = LockColors.contentMuted)
            Text(formatRemaining(remaining), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmergencyUnlock(remaining: Long?, onRequest: () -> Unit, onComplete: () -> Unit) {
    when {
        remaining == null -> FocusButton(
            "비상 해제 요청 · ${SafetyPolicy.EMERGENCY_DELAY_MS / 60_000}분 대기",
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth(),
            style = FocusButtonStyle.Outline,
            contentColor = LockColors.content,
        )

        remaining > 0 -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "비상 해제까지 ${formatRemaining(remaining)}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            FocusProgressBar(
                progress = 1f - remaining.toFloat() / SafetyPolicy.EMERGENCY_DELAY_MS,
                color = LockColors.content,
                trackColor = LockColors.track,
            )
        }

        else -> FocusButton(
            "비상 해제",
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            containerColor = LockColors.content,
            contentColor = LockColors.background,
        )
    }
}

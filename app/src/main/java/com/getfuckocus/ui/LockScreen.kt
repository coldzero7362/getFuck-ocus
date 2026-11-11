package com.getfuckocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfuckocus.container
import com.getfuckocus.safety.SafetyPolicy
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusProgressBar
import com.getfuckocus.ui.components.FocusRing
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.LockColors
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.theme.circleBevel
import com.getfuckocus.ui.theme.plate
import com.getfuckocus.ui.theme.halftone
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
                .halftone()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(LockColors.badge).circleBevel(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = LockColors.onBadge, modifier = Modifier.size(36.dp))
            }
            Text("잠금 중", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

            // 잠금 사유: 어떤 과제 때문에 잠겼는지
            Column(
                Modifier
                    .fillMaxWidth()
                    .plate(LockColors.raised, ChromeShapes.panel, bevel = false)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("잠금 사유", style = MaterialTheme.typography.labelMedium, color = LockColors.contentMuted)
                Text("'${s.taskTitle}'", style = MaterialTheme.typography.titleMedium, color = LockColors.content)
                Text("마감까지 인증하지 못했어요", style = MaterialTheme.typography.bodyMedium, color = LockColors.contentMuted)
            }

            AutoReleaseRing(autoRemaining)

            FocusButton(
                "지금 인증하고 해제",
                onClick = { onVerifyNow(s.taskId) },
                modifier = Modifier.fillMaxWidth(),
                height = 60.dp,
                icon = PixelIcons.Camera,
                containerColor = LockColors.accent,
                contentColor = LockColors.onAccent,
            )

            EmergencyUnlock(
                remaining = emergencyRemaining,
                onRequest = { lock.requestEmergencyUnlock() },
                onComplete = { lock.completeEmergencyUnlock() },
            )

            // 긴급전화는 잠금 해제와 별개의 칸으로 둔다
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, LockColors.contentMuted.copy(alpha = 0.4f), ChromeShapes.panel)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = LockColors.ring, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("긴급전화", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Text("잠금 중에도 항상 걸 수 있어요", style = MaterialTheme.typography.bodySmall, color = LockColors.contentMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("112", "119").forEach { number ->
                        FocusButton(
                            number,
                            onClick = { onEmergencyCall(number) },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Phone,
                            height = 48.dp,
                            containerColor = LockColors.raised,
                            contentColor = LockColors.content,
                        )
                    }
                }
                Text(
                    "전화, 문자, 알람, 설정과 허용한 앱도 잠금 중에 쓸 수 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = LockColors.contentMuted,
                )
            }
        }
    }
}

@Composable
private fun AutoReleaseRing(remaining: Long) {
    Box(contentAlignment = Alignment.Center) {
        FocusRing(
            progress = remaining.toFloat() / SafetyPolicy.MAX_LOCK_DURATION_MS,
            modifier = Modifier.size(196.dp),
            color = LockColors.ring,
            trackColor = LockColors.track,
            strokeWidth = 12.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(PixelIcons.Hourglass, contentDescription = null, tint = LockColors.contentMuted, modifier = Modifier.size(18.dp))
            Text("자동 해제까지", style = MaterialTheme.typography.labelMedium, color = LockColors.contentMuted)
            Text(formatRemaining(remaining), style = DigitStyle.copy(fontSize = 30.sp), color = LockColors.content)
        }
    }
}

@Composable
private fun EmergencyUnlock(remaining: Long?, onRequest: () -> Unit, onComplete: () -> Unit) {
    when {
        remaining == null -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 보조 행동: 주 버튼보다 낮은 테두리 버튼
            FocusButton(
                "비상 해제 요청",
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LockColors.contentMuted, ChromeShapes.control),
                height = 52.dp,
                icon = Icons.Default.Lock,
                containerColor = LockColors.raised,
                contentColor = LockColors.content,
            )
            Text(
                "요청하면 ${SafetyPolicy.EMERGENCY_DELAY_MS / 60_000}분 기다린 뒤 풀 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = LockColors.contentMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        remaining > 0 -> Column(
            Modifier
                .fillMaxWidth()
                .plate(LockColors.raised, ChromeShapes.panel, bevel = false)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PixelIcons.Hourglass, contentDescription = null, tint = LockColors.ring, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("비상 해제 대기 중", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(formatRemaining(remaining), style = DigitStyle.copy(fontSize = 22.sp), color = LockColors.ring)
            }
            FocusProgressBar(
                progress = 1f - remaining.toFloat() / SafetyPolicy.EMERGENCY_DELAY_MS,
                color = LockColors.ring,
                trackColor = LockColors.track,
            )
            Text(
                "다 차면 \"비상 해제\" 버튼이 나와요. 그동안 인증하면 바로 풀려요",
                style = MaterialTheme.typography.bodySmall,
                color = LockColors.contentMuted,
            )
        }

        else -> FocusButton(
            "비상 해제",
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            icon = Icons.Default.Lock,
            containerColor = LockColors.content,
            contentColor = LockColors.background,
        )
    }
}

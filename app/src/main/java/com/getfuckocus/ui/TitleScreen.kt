package com.getfuckocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfuckocus.container
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.LockColors
import com.getfuckocus.ui.theme.Mulmaru
import com.getfuckocus.ui.theme.halftone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

/** 자물쇠가 잠긴 뒤 남은 과제 수를 읽을 시간 */
private const val HOLD_MS = 700L
private const val FADE_OUT_MS = 280

/** 오늘 남은 과제 수. 과제가 하나도 없으면 null */
private suspend fun remainingToday(context: android.content.Context): Int? {
    val dao = context.container.taskDao
    val now = ZonedDateTime.now()
    val tasks = dao.observeAll().first()
    if (tasks.isEmpty()) return null
    val keys = TaskSchedule.candidateDates(now).map(TaskSchedule::dayKey)
    val passed = dao.observePassed(keys).first().toSet()
    return tasks.count {
        when (val s = taskStatus(it, now, passed)) {
            is TaskStatus.InProgress -> true
            is TaskStatus.Upcoming -> s.startsAt.toLocalDate() == now.toLocalDate()
            else -> false
        }
    }
}

/**
 * 앱을 처음 열 때 잠깐 보여 주는 게임 타이틀 화면.
 * 자물쇠가 "딸깍" 잠기고, 오늘 남은 과제 수를 알려 준 뒤 홈으로 넘어간다.
 * 홈 데이터를 다 읽은 다음에 걷히므로 홈의 빈 화면이 번쩍이지 않는다. 화면을 누르면 바로 넘어간다.
 */
@Composable
fun TitleScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onFinished)
    var remaining by remember { mutableStateOf<Int?>(null) }
    var animDone by remember { mutableStateOf(false) }
    var skipped by remember { mutableStateOf(false) }

    val shackleDrop = remember { Animatable(0f) }   // 0 = 열림, 1 = 잠김
    val bodyShake = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    var dataReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        remaining = runCatching { remainingToday(context) }.getOrNull()
        dataReady = true
    }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(220))
        delay(260)
        shackleDrop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        bodyShake.animateTo(0f, keyframes {
            durationMillis = 180
            -3f at 30
            3f at 75
            -2f at 120
            0f at 180
        })
        animDone = true
    }
    val loaded = dataReady && animDone
    // 홈 데이터가 준비되면: 눌렀으면 바로, 아니면 잠긴 자물쇠와 문구를 잠깐 보여 주고 걷힌다
    LaunchedEffect(dataReady, animDone, skipped) {
        if (!dataReady || !(animDone || skipped)) return@LaunchedEffect
        if (!skipped) delay(HOLD_MS)
        screenAlpha.animateTo(0f, tween(FADE_OUT_MS))
        finish()
    }

    val blink = rememberInfiniteTransition(label = "press-start")
    val startAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes { durationMillis = 900; 1f at 0; 1f at 520; 0f at 540; 0f at 880 },
            RepeatMode.Restart,
        ),
        label = "press-start-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(LockColors.background)
            .halftone()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { skipped = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.alpha(contentAlpha.value).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PixelLock(
                shut = shackleDrop.value,
                modifier = Modifier
                    .size(width = 128.dp, height = 152.dp)
                    .offset(x = bodyShake.value.dp),
            )
            Spacer(Modifier.height(36.dp))
            ShadowTitle("getFuck-ocus")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "집중 안 하면 잠근다",
                style = TextStyle(fontFamily = Mulmaru, fontSize = 16.sp, color = LockColors.contentMuted),
            )
            Spacer(Modifier.height(44.dp))
            RemainingLine(loaded = loaded, remaining = remaining)
            Spacer(Modifier.height(28.dp))
            Text(
                text = "▶ PRESS START",
                modifier = Modifier.alpha(if (loaded) startAlpha else 0f),
                style = DigitStyle.copy(fontSize = 18.sp, color = Chrome.amber),
            )
        }
    }
}

/** 오렌지 글자 아래에 짙은 그림자를 두 칸 겹쳐 찍은 픽셀 타이틀 */
@Composable
private fun ShadowTitle(text: String) {
    val style = TextStyle(fontFamily = Mulmaru, fontSize = 38.sp, textAlign = TextAlign.Center)
    Box {
        Text(text, style = style.copy(color = Color(0xFF0B0F1E)), modifier = Modifier.offset(3.dp, 4.dp))
        Text(text, style = style.copy(color = Chrome.chromeIndigo), modifier = Modifier.offset(1.5.dp, 2.dp))
        Text(text, style = style.copy(color = Chrome.signal))
    }
}

@Composable
private fun RemainingLine(loaded: Boolean, remaining: Int?) {
    val base = TextStyle(fontFamily = Mulmaru, fontSize = 18.sp, color = LockColors.content)
    // 자리를 미리 잡아 두어 글자가 생길 때 위아래가 들썩이지 않게 한다
    Box(Modifier.alpha(if (loaded) 1f else 0f), contentAlignment = Alignment.Center) {
        when {
            remaining == null -> Text("첫 과제를 만들어 볼까요", style = base)
            remaining == 0 -> Text("오늘 남은 과제 없음", style = base.copy(color = Chrome.ice))
            else -> androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text("오늘 남은 과제 ", style = base)
                Text("$remaining", style = DigitStyle.copy(fontSize = 26.sp, color = Chrome.signal))
                Text("개", style = base)
            }
        }
    }
}

/**
 * 도트로 찍은 자물쇠. 앱 아이콘(자물쇠 + F↗)을 16×19칸 픽셀 그림으로 옮겼다.
 * [shut]이 0이면 고리가 위로 들려 있고 1이면 몸통에 꽂힌다.
 */
@Composable
private fun PixelLock(shut: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cols = 16
        val rows = 19
        val cell = minOf(size.width / cols, size.height / rows)
        val ox = (size.width - cell * cols) / 2
        val oy = (size.height - cell * rows) / 2
        // 고리가 들린 높이: 열림 3칸 → 잠김 0칸
        val lift = (1f - shut) * 3f * cell

        fun DrawScope.px(c: Int, r: Int, color: Color, w: Int = 1, h: Int = 1, dy: Float = 0f) =
            drawRect(color, Offset(ox + c * cell, oy + r * cell - dy), Size(cell * w, cell * h))

        // 고리 (몸통 뒤로 들어가므로 먼저 그린다)
        val ring = Chrome.sky
        val ringShade = Chrome.mutedIndigo
        px(5, 0, ring, w = 6, dy = lift)
        px(4, 1, ring, w = 1, h = 1, dy = lift); px(11, 1, ring, dy = lift)
        px(3, 2, ring, h = 7, dy = lift); px(4, 2, ringShade, h = 7, dy = lift)
        px(12, 2, ring, h = 7, dy = lift); px(11, 2, ringShade, h = 7, dy = lift)
        px(5, 1, ringShade, w = 6, dy = lift)

        // 몸통: 앰버 판 + 위쪽 밝은 선 + 아래 그림자
        px(1, 8, Chrome.signal, w = 14, h = 10)
        px(1, 8, Chrome.amber, w = 14)
        px(1, 17, Color(0xFFB4650F), w = 14)
        px(0, 9, Chrome.signal, h = 8); px(15, 9, Color(0xFFB4650F), h = 8)

        // 몸통 안의 F↗ 문양 (카본)
        val ink = Chrome.carbon
        px(3, 10, ink, w = 4)          // F 윗줄
        px(3, 11, ink, h = 5)          // F 기둥
        px(4, 12, ink, w = 2)          // F 가운데
        px(8, 15, ink); px(9, 14, ink); px(10, 13, ink); px(11, 12, ink)   // ↗ 대각선 (한 칸씩 계단)
        px(9, 10, ink, w = 4); px(12, 11, ink, h = 3)                      // ↗ 화살촉: 오른쪽 위 꺾쇠
        px(11, 11, ink)                                                    // 대각선과 꺾쇠 이음

        // 바닥 그림자
        drawRect(Color(0x55000000), Offset(ox + cell * 1, oy + cell * 18), Size(cell * 14, cell))
    }
}

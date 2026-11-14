package com.getfuckocus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfuckocus.data.AchievementDao
import com.getfuckocus.data.TaskDao
import com.getfuckocus.task.AchievementDecor
import com.getfuckocus.task.AchievementTier
import com.getfuckocus.ui.theme.AchievementArt
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.TaskIcon
import com.getfuckocus.ui.theme.TwoToneIcon
import kotlinx.coroutines.delay
import kotlin.math.hypot

/** 채움과 외곽선을 겹쳐 그린다. [outline]이 null이면 채움만 (지금 PixelIcons처럼 한 색으로) 그린다. */
@Composable
fun TwoToneImage(
    art: TwoToneIcon,
    fill: Color,
    outline: Color?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(modifier.aspectRatio(art.aspectRatio)) {
        if (outline != null) {
            Icon(art.outline, contentDescription = null, tint = outline, modifier = Modifier.fillMaxSize())
        }
        Icon(art.fill, contentDescription = contentDescription, tint = fill, modifier = Modifier.fillMaxSize())
    }
}

/** 과제 아이콘 (밝은 카드 위: 짙은 채움, 외곽선 없음) */
@Composable
fun TaskIconMark(icon: TaskIcon, modifier: Modifier = Modifier, tint: Color = Chrome.carbon) {
    TwoToneImage(icon.art, fill = tint, outline = null, modifier = modifier, contentDescription = icon.label)
}

/**
 * 업적 배지: 아이콘에 단계별 장식(별, 오라, 왕관)을 붙인다. 아이콘이 없는 과제는 트로피를 쓴다.
 * 짙은 바탕 위에 올리는 걸 전제로 흰 채움 + 검은 외곽선으로 그린다.
 */
@Composable
fun AchievementBadge(icon: TaskIcon?, decor: AchievementDecor, size: Dp, modifier: Modifier = Modifier) {
    val art = icon?.art ?: AchievementArt.Trophy
    val hasAura = decor == AchievementDecor.AURA || decor == AchievementDecor.AURA_STAR || decor == AchievementDecor.CROWN
    val auraColor = if (decor == AchievementDecor.CROWN) Chrome.signal else Chrome.amber
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (hasAura) PixelAura(auraColor, Modifier.fillMaxSize())
        TwoToneImage(art, fill = Chrome.white, outline = Color.Black, modifier = Modifier.size(size * 0.6f))
        when (decor) {
            AchievementDecor.STAR, AchievementDecor.AURA_STAR -> Star(size * 0.34f, Alignment.TopEnd)
            AchievementDecor.TWO_STARS -> {
                Star(size * 0.34f, Alignment.TopEnd)
                Star(size * 0.26f, Alignment.BottomStart)
            }
            AchievementDecor.CROWN -> TwoToneImage(
                AchievementArt.Crown,
                fill = Chrome.amber,
                outline = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = -size * 0.08f).size(size * 0.36f),
            )
            AchievementDecor.NONE, AchievementDecor.AURA -> Unit
        }
    }
}

@Composable
private fun BoxScope.Star(size: Dp, alignment: Alignment) {
    TwoToneImage(
        AchievementArt.Star,
        fill = Chrome.amber,
        outline = Color.Black,
        modifier = Modifier.align(alignment).size(size),
    )
}

/** 픽셀 칸으로 찍은 원형 오라. 천천히 숨 쉬듯 밝아졌다 어두워진다. */
@Composable
private fun PixelAura(color: Color, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "aura").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "auraAlpha",
    )
    Canvas(modifier.graphicsLayer { alpha = pulse }) {
        val cells = 20
        val cell = this.size.minDimension / cells
        val center = cells / 2f
        for (y in 0 until cells) for (x in 0 until cells) {
            val d = hypot(x + 0.5f - center, y + 0.5f - center)
            // 안쪽 고리는 꽉 채우고, 바깥 고리는 체크무늬로 흩뿌려 빛이 번지는 느낌을 낸다
            val solid = d in 7.2f..8.4f
            val sparse = d in 8.4f..9.8f && (x + y) % 2 == 0
            if (solid || sparse) {
                drawRect(color, topLeft = Offset(x * cell, y * cell), size = Size(cell, cell))
            }
        }
    }
}

/**
 * 화면 위에서 내려오는 업적 알림. 아직 보여 주지 않은 업적을 하나씩 띄우고 본 것으로 표시한다.
 * 앱 맨 위 층에 한 번만 둔다. 누르면 바로 닫힌다.
 */
@Composable
fun AchievementToastHost(achievements: AchievementDao, tasks: TaskDao, modifier: Modifier = Modifier) {
    val unseen by remember { achievements.observeUnseen() }.collectAsState(initial = emptyList())
    val current = unseen.firstOrNull()
    var shown by remember { mutableStateOf<ToastContent?>(null) }
    var visible by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        val item = current ?: return@LaunchedEffect
        val task = tasks.get(item.taskId)
        val tier = AchievementTier.of(item.milestone)
        // 지워진 과제나 없어진 단계는 조용히 넘긴다
        if (task != null && tier != null) {
            shown = ToastContent(TaskIcon.fromKey(task.iconKey), tier, task.title)
            dismissed = false
            visible = true
            val until = System.currentTimeMillis() + TOAST_MILLIS
            while (!dismissed && System.currentTimeMillis() < until) delay(100)
            visible = false
            delay(400) // 올라가는 애니메이션
        }
        achievements.markSeen(item.taskId, item.milestone)
    }

    Box(modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it * 2 } + fadeIn(),
            exit = slideOutVertically { -it * 2 } + fadeOut(),
        ) {
            shown?.let { AchievementToast(it, onClick = { dismissed = true }) }
        }
    }
}

private const val TOAST_MILLIS = 4_000L

private data class ToastContent(val icon: TaskIcon?, val tier: AchievementTier, val taskTitle: String)

@Composable
private fun AchievementToast(content: ToastContent, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .background(Chrome.carbon, ChromeShapes.small)
            .border(2.dp, Chrome.hairline, ChromeShapes.small)
            .clickable(onClick = onClick)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AchievementBadge(content.icon, content.tier.decor, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("업적 달성! ${content.tier.title}", style = MaterialTheme.typography.titleSmall, color = Chrome.amber)
            Text(
                "'${content.taskTitle}' ${content.tier.milestone}번 연속 성공",
                style = MaterialTheme.typography.bodySmall,
                color = Chrome.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

package com.getfuckocus.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.Skin
import com.getfuckocus.ui.theme.SkinState
import kotlin.math.max

/**
 * 지도 그림 위에 까는 스킨별 무늬. 지도 바로 위, 핀·버튼 아래에 둔다. 터치는 받지 않는다.
 * - 뉴트로: 게임 월드맵 같은 모눈 격자
 * - 카세트 퓨처리즘: CRT 화면 (주사선, 가장자리 어둠, 형광 빛, 위에서 아래로 흐르는 갱신 띠)
 */
@Composable
fun MapSkinOverlay(modifier: Modifier = Modifier) {
    when (SkinState.skin) {
        Skin.NEWTRO -> MapGrid(modifier)
        Skin.CASSETTE -> MapCrt(modifier)
    }
}

/** 작은 칸 24dp, 네 칸마다 진한 선. 화면 가운데에서 시작해 핀이 늘 교차점 위에 온다 */
@Composable
private fun MapGrid(modifier: Modifier) {
    Canvas(modifier) {
        val step = 24.dp.toPx()
        val thin = 1.dp.toPx()
        val thick = 1.6.dp.toPx()
        val minor = Chrome.chromeIndigo.copy(alpha = 0.16f)
        val major = Chrome.chromeIndigo.copy(alpha = 0.34f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val cols = (cx / step).toInt() + 1
        val rows = (cy / step).toInt() + 1
        for (i in -cols..cols) {
            val x = cx + i * step
            val isMajor = i % 4 == 0
            drawLine(if (isMajor) major else minor, Offset(x, 0f), Offset(x, size.height), if (isMajor) thick else thin)
        }
        for (j in -rows..rows) {
            val y = cy + j * step
            val isMajor = j % 4 == 0
            drawLine(if (isMajor) major else minor, Offset(0f, y), Offset(size.width, y), if (isMajor) thick else thin)
        }
    }
}

@Composable
private fun MapCrt(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "crt")
    // 갱신 띠가 위에서 아래로 천천히 지나간다
    val sweep by transition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(CRT_SWEEP_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "crtSweep",
    )
    // 형광체가 아주 미세하게 깜빡인다
    val flicker by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(tween(CRT_FLICKER_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "crtFlicker",
    )
    Canvas(modifier) {
        // 형광 녹색 빛을 살짝 덮는다
        drawRect(Chrome.phosphor.copy(alpha = flicker))

        // 주사선: 3dp마다 1dp 어두운 줄
        val step = 3.dp.toPx()
        val line = max(1f, step / 3f)
        var y = 0f
        while (y < size.height) {
            drawRect(Color.Black.copy(alpha = 0.22f), topLeft = Offset(0f, y), size = Size(size.width, line))
            y += step
        }

        // 갱신 띠
        val band = 90.dp.toPx()
        val top = sweep * size.height - band / 2f
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.7f to Chrome.phosphor.copy(alpha = 0.10f),
                1f to Color.White.copy(alpha = 0.14f),
                startY = top,
                endY = top + band,
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, band),
        )

        // 볼록한 브라운관처럼 가장자리를 어둡게
        val radius = max(size.width, size.height) * 0.75f
        drawRect(
            Brush.radialGradient(
                0.55f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.62f),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius,
            )
        )
    }
}

private const val CRT_SWEEP_MS = 5200
private const val CRT_FLICKER_MS = 140

package com.getfuckocus.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 크롬 디자인의 모양. 큰 판은 모서리를 45도로 잘라내고, 조작 부품은 거의 각지게 둔다. */
object ChromeShapes {
    val panel: Shape = CutCornerShape(10.dp)
    val sheet: Shape = CutCornerShape(topStart = 14.dp, topEnd = 14.dp)
    val small: Shape = RoundedCornerShape(4.dp)
    val control: Shape = RoundedCornerShape(2.dp)
}

/**
 * 사출 성형한 플라스틱 판처럼 보이게 한다: 위쪽에 밝은 테두리, 아래쪽에 남색 그림자 선.
 * 흐릿한 그림자 대신 이 단단한 테두리로 깊이를 표현한다.
 */
fun Modifier.bevel(
    highlight: Color = Chrome.highlight,
    shadow: Color = Chrome.chromeIndigo,
    highlightWidth: Dp = 1.5.dp,
    shadowWidth: Dp = 2.dp,
): Modifier = drawWithContent {
    drawContent()
    val top = highlightWidth.toPx()
    val bottom = shadowWidth.toPx()
    drawRect(highlight, topLeft = Offset.Zero, size = size.copy(height = top))
    drawRect(shadow, topLeft = Offset(0f, size.height - bottom), size = size.copy(height = bottom))
}

/**
 * 원형 부품용 테두리. 사각형 선을 원 안에 그리면 위아래가 잘려 평평한 띠처럼 보이므로,
 * 원 둘레를 따라 위쪽은 밝은 호, 아래쪽은 어두운 호로 그린다.
 */
fun Modifier.circleBevel(
    highlight: Color = Chrome.highlight,
    shadow: Color = Color(0x55000000),
    width: Dp = 2.5.dp,
): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    val arcSize = size.copy(width = size.width - stroke, height = size.height - stroke)
    val topLeft = Offset(stroke / 2, stroke / 2)
    drawArc(highlight, 200f, 140f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
    drawArc(shadow, 20f, 140f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
}

/** 판에 파인 홈처럼 보이게 한다: 위쪽과 왼쪽에 어두운 선. 입력칸에 쓴다. */
fun Modifier.insetBevel(shadow: Color = Color(0x663D4F97), width: Dp = 2.dp): Modifier = drawWithContent {
    drawContent()
    val w = width.toPx()
    drawRect(shadow, topLeft = Offset.Zero, size = size.copy(height = w))
    drawRect(shadow, topLeft = Offset.Zero, size = size.copy(width = w / 2))
}

/**
 * 둘레 판([surface])에 살짝 파여 들어간 칸. 흰 칸을 따로 얹는 대신 판과 같은 계열 색을 한 단 어둡게 칠하고,
 * 안쪽 위·왼쪽에 그늘, 아래·오른쪽에 밝은 선을 그어 눌러 판 홈처럼 보이게 한다.
 * 누를 수 없는 정보 칸(점수판, 기록)에 쓴다. 누를 수 있는 것은 튀어나온 판([bevel])으로 둔다.
 */
fun Modifier.recessed(surface: Color = Chrome.platinum, shape: Shape = ChromeShapes.small): Modifier =
    clip(shape)
        .background(lerp(surface, Chrome.chromeIndigo, 0.07f))
        .drawWithContent {
            drawContent()
            val top = 2.5.dp.toPx()
            val side = 1.5.dp.toPx()
            val glint = 1.5.dp.toPx()
            drawRect(Color(0x573D4F97), topLeft = Offset.Zero, size = size.copy(height = top))
            drawRect(Color(0x383D4F97), topLeft = Offset(0f, top), size = size.copy(width = side, height = size.height - top))
            drawRect(Color(0x99FFFFFF), topLeft = Offset(0f, size.height - glint), size = size.copy(height = glint))
            drawRect(Color(0x66FFFFFF), topLeft = Offset(size.width - side, top), size = size.copy(width = side, height = size.height - top - glint))
        }

/** 모양대로 자르고, 색을 칠하고, 테두리를 단 판 하나. */
fun Modifier.plate(color: Color, shape: Shape = ChromeShapes.panel, bevel: Boolean = true): Modifier =
    clip(shape).background(color).then(if (bevel) Modifier.bevel() else Modifier)

/** 스피커 그릴 같은 점무늬. 카본 판(상단 바, 잠금 화면)에 깐다. */
fun Modifier.halftone(dot: Color = Color(0x14FFFFFF), step: Dp = 5.dp, radius: Dp = 0.9.dp): Modifier =
    drawBehind {
        val s = step.toPx()
        val r = radius.toPx()
        var y = s / 2
        var row = 0
        while (y < size.height) {
            // 줄마다 반 칸씩 엇갈려 찍는다
            var x = if (row % 2 == 0) s / 2 else s
            while (x < size.width) {
                drawCircle(dot, r, Offset(x, y))
                x += s
            }
            y += s
            row++
        }
    }

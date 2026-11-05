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

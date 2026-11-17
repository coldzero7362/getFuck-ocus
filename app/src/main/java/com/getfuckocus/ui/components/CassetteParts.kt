package com.getfuckocus.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.SkinState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.phosphorGlow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.geometry.CornerRadius
import com.getfuckocus.ui.theme.scanlines

/*
 * 카세트 퓨처리즘 스킨 전용 부품.
 * 70~80년대 단말기·카세트 데크처럼: 옆면이 보이는 두툼한 사각 키캡, 판을 고정한 나사, 몸체에 두른 색 띠.
 */

/** 키캡 모서리. 둥근 버튼 대신 거의 각진 사각형으로 */
val KeyShape: Shape = RoundedCornerShape(4.dp)

/** 키 옆면 높이. 뉴트로 버튼(1dp 눌림)보다 훨씬 두툼하게 튀어나와 보이게 한다 */
val KEY_DEPTH = 6.dp

/**
 * 옆면(스커트)이 아래로 보이는 사각 키캡. 누르면 옆면 높이만큼 쑥 내려간다.
 * 크기는 [modifier]로 정하고(옆면 높이 포함), 고정 크기가 없으면 윗면 내용에 맞춘다.
 * @param faceModifier 윗면 크기 (예: 최소 높이)
 * @param contentPadding 윗면 안쪽 여백
 */
@Composable
fun Keycap(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Chrome.signal,
    enabled: Boolean = true,
    depth: Dp = KEY_DEPTH,
    shape: Shape = KeyShape,
    onClickLabel: String? = null,
    faceModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel = depth - 1.dp
    val sink by animateDpAsState(if (pressed && enabled) travel else 0.dp, tween(70), label = "keySink")
    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clickable(
                interaction, indication = null, enabled = enabled,
                onClickLabel = onClickLabel, role = Role.Button, onClick = onClick,
            ),
        propagateMinConstraints = true,
    ) {
        // 옆면: 키 색을 어둡게 한 몸통. 윗면 아래로 depth만큼 드러난다
        Box(
            Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(shape)
                .background(lerp(color, Color.Black, 0.42f))
                .drawBehind { drawSkirtShading(depth.toPx()) }
                .border(1.5.dp, Chrome.carbon, shape)
        )
        Box(
            Modifier
                .padding(top = sink, bottom = depth - sink)
                .then(faceModifier)
                .clip(shape)
                .background(color)
                // 무광 도장 윗면: 광택 없이, 아래 끝에만 아주 옅은 그늘
                .background(
                    Brush.verticalGradient(
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.06f),
                    )
                )
                .drawWithContent {
                    drawContent()
                    if (!pressed) {
                        // 윗면 가장자리의 반사광 한 줄
                        val inset = 4.dp.toPx()
                        drawRect(
                            Color.White.copy(alpha = 0.08f),
                            topLeft = Offset(inset, 2.dp.toPx()),
                            size = Size(size.width - inset * 2, 1.5.dp.toPx()),
                        )
                    }
                }
                .border(1.5.dp, Chrome.carbon, shape)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * 옆면 명암. 앞면은 그대로 두고, 양 끝이 옆으로 돌아 들어가는 모서리처럼 어두워진다.
 * 윗면 바로 아래는 처마 그늘, 맨 아래 가장자리엔 바닥에서 받은 옅은 반사광.
 */
private fun DrawScope.drawSkirtShading(depthPx: Float) {
    // 평소엔 맨 아래 depthPx만큼만 드러난다
    val shown = size.height - depthPx
    val corner = minOf(14.dp.toPx(), size.width / 4f)
    // 왼쪽 모서리: 바깥쪽이 가장 어둡다
    drawRect(
        Brush.horizontalGradient(
            0f to Color.Black.copy(alpha = 0.45f),
            0.35f to Color.Black.copy(alpha = 0.22f),
            1f to Color.Transparent,
            startX = 0f, endX = corner,
        ),
        size = Size(corner, size.height),
    )
    // 오른쪽 모서리
    drawRect(
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.65f to Color.Black.copy(alpha = 0.22f),
            1f to Color.Black.copy(alpha = 0.45f),
            startX = size.width - corner, endX = size.width,
        ),
        topLeft = Offset(size.width - corner, 0f),
        size = Size(corner, size.height),
    )
    // 윗면 처마 밑 그늘
    drawRect(
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.30f),
            1f to Color.Transparent,
            startY = shown, endY = shown + depthPx * 0.7f,
        ),
        topLeft = Offset(0f, shown),
        size = Size(size.width, depthPx),
    )
    // 바닥 반사광 한 줄
    val line = 1.dp.toPx()
    drawRect(
        Color.White.copy(alpha = 0.10f),
        topLeft = Offset(corner * 0.6f, size.height - line * 3f),
        size = Size(size.width - corner * 1.2f, line),
    )
}

/**
 * 눌린 채 걸려 있는 키(완료 표시처럼 누를 수 없는 상태 칸). 옆면은 1dp만 보이고 윗면이 판에 거의 붙어 있다.
 */
@Composable
fun LatchedKey(modifier: Modifier = Modifier, color: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(KeyShape)
            .background(lerp(color, Color.Black, 0.42f))
            .padding(top = 2.dp)
            .clip(KeyShape)
            .background(color)
            .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.08f), 0.25f to Color.Transparent))
            .border(1.5.dp, Chrome.carbon, KeyShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** 몸체에 두른 70년대식 색 띠: 앰버 · 오렌지 · 레드 · 브라운 */
@Composable
fun RetroStripes(modifier: Modifier = Modifier, stripe: Dp = 3.dp) {
    Column(modifier.fillMaxWidth()) {
        STRIPE_COLORS.forEach { c -> Spacer(Modifier.fillMaxWidth().height(stripe).background(c)) }
    }
}

private val STRIPE_COLORS = listOf(
    Color(0xFFF2B632),
    Color(0xFFE0662A),
    Color(0xFFC8442A),
    Color(0xFF6B3A1E),
)

/** 금속판을 결 따라 깎은 듯한 가는 가로줄. 카세트 스킨의 상단 바 판에 쓴다 */
fun Modifier.brushedRibs(step: Dp = 4.dp): Modifier = drawWithContent {
    drawContent()
    val s = step.toPx()
    val line = 1.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawRect(Color.White.copy(alpha = 0.045f), topLeft = Offset(0f, y), size = Size(size.width, line))
        y += s
    }
}

/**
 * 스피커 그릴: 철판에 뚫은 네모 구멍 격자. 구멍마다 아래쪽에 옅은 밝은 턱을 그려 판이 뚫린 것처럼 보이게 한다.
 * 크기는 [modifier]로 정하고, 그 안에 들어가는 만큼 가운데 정렬로 채운다.
 */
@Composable
fun SpeakerGrille(modifier: Modifier = Modifier, step: Dp = 7.dp, hole: Dp = 2.5.dp) {
    val ink = Chrome.carbon
    Canvas(modifier) {
        val s = step.toPx()
        val h = hole.toPx()
        val cols = ((size.width - h) / s).toInt() + 1
        val rows = ((size.height - h) / s).toInt() + 1
        val ox = (size.width - ((cols - 1) * s + h)) / 2f
        val oy = (size.height - ((rows - 1) * s + h)) / 2f
        val lip = 1.dp.toPx()
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = ox + c * s
            val y = oy + r * s
            drawRect(Color.White.copy(alpha = 0.22f), topLeft = Offset(x, y + lip), size = Size(h, h))
            drawRect(ink.copy(alpha = 0.62f), topLeft = Offset(x, y), size = Size(h, h))
        }
    }
}

/** 눈금줄: 계기판 테두리에 새긴 짧은 세로 눈금을 같은 간격으로 늘어놓는다 */
@Composable
fun TickRuler(modifier: Modifier = Modifier, step: Dp = 6.dp, tick: Dp = 2.dp) {
    val ink = Chrome.carbon
    Canvas(modifier) {
        val s = step.toPx()
        val w = tick.toPx()
        var x = 0f
        while (x + w <= size.width) {
            drawRect(ink.copy(alpha = 0.55f), topLeft = Offset(x, 0f), size = Size(w, size.height))
            x += s
        }
    }
}

/**
 * 화면 맨 아래 명판: 스피커 그릴 + 눈금 + 모델 번호. 카세트 스킨에서만 보이고 읽기 프로그램은 건너뛴다.
 */
@Composable
fun CassetteNameplate(modifier: Modifier = Modifier) {
    if (!SkinState.isCassette) return
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { }
            .padding(top = 8.dp)
            .clip(KeyShape)
            .background(lerp(Chrome.canvas, Color.Black, 0.10f))
            .border(1.dp, Chrome.carbon.copy(alpha = 0.35f), KeyShape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpeakerGrille(Modifier.width(92.dp).height(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TickRuler(Modifier.fillMaxWidth().height(10.dp))
            Text(
                "GF-80  FOCUS UNIT",
                style = DigitStyle.copy(fontSize = 13.sp),
                color = Chrome.carbon.copy(alpha = 0.75f),
                maxLines = 1,
            )
            Text(
                "SELF-CONTROL TERMINAL · MK-II",
                style = DigitStyle.copy(fontSize = 10.sp),
                color = Chrome.carbon.copy(alpha = 0.55f),
                maxLines = 1,
            )
        }
    }
}

/** 경고(시간 초과·실패)를 띄우는 붉은 형광 */
val CrtAlert = Color(0xFFFF6A4D)

/** 짙은 갈색 테에 끼운 검은 CRT 화면 바탕. 안쪽 여백은 호출하는 쪽에서 준다 */
fun Modifier.crtScreen(corner: Dp = 8.dp): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(Chrome.carbon)
    .padding(3.dp)
    .clip(RoundedCornerShape(corner - 2.dp))
    .background(Chrome.screen)
    .scanlines()

/**
 * 카세트 데크의 테이프 카운터: 검은 드럼 칸에 흰 숫자가 하나씩. 드럼이 둥글게 돌아가는 것처럼 위아래를 어둡게 한다.
 */
@Composable
fun TapeCounter(value: Int, modifier: Modifier = Modifier, digits: Int = 3) {
    TapeDrums(value.coerceIn(0, "9".repeat(digits).toInt()).toString().padStart(digits, '0'), modifier)
}

/** [TapeCounter]와 같은 드럼에 글자를 그대로 한 칸씩 (요일 글자처럼 숫자가 아닌 것도) */
@Composable
fun TapeDrums(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Chrome.carbon)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        text.forEach { d ->
            Box(
                Modifier
                    .width(if (d.isDigit()) 20.dp else 28.dp) // 글자(요일)는 숫자보다 넓게
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF151310))
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.7f),
                                0.3f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.7f),
                            )
                        )
                        // 드럼 가운데를 지나는 가는 맞춤선
                        drawRect(
                            Color.White.copy(alpha = 0.08f),
                            topLeft = Offset(0f, size.height / 2f),
                            size = Size(size.width, 1.dp.toPx()),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("$d", style = DigitStyle.copy(fontSize = 18.sp), color = Color(0xFFF1ECE0))
            }
        }
    }
}

/**
 * 판에 파묻힌 키 묶음: 몸체를 음각으로 판 홈(위·왼쪽 테는 그늘, 아래·오른쪽 테는 빛) 안에 키가 맞닿아 있다.
 * 계기판 조작부처럼 쓴다. 키는 [BankKey]로 채우고, [caption]이 있으면 아래에 작은 각인을 단다.
 */
@Composable
fun KeyBank(caption: String?, modifier: Modifier = Modifier, keys: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(WELL_BG)
                .drawWithContent {
                    drawContent()
                    val e = 2.dp.toPx()
                    // 음각 테: 위·왼쪽은 그늘, 아래·오른쪽은 빛
                    drawRect(WELL_SHADE, size = Size(size.width, e))
                    drawRect(WELL_SHADE.copy(alpha = 0.9f), size = Size(e, size.height))
                    drawRect(WELL_LIGHT, topLeft = Offset(0f, size.height - e), size = Size(size.width, e))
                    drawRect(WELL_LIGHT.copy(alpha = 0.9f), topLeft = Offset(size.width - e, 0f), size = Size(e, size.height))
                    // 홈 안쪽 위로 드리운 그림자
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.14f),
                            1f to Color.Transparent,
                            startY = e,
                            endY = e + 5.dp.toPx(),
                        ),
                        topLeft = Offset(e, e),
                        size = Size(size.width - e * 2, 5.dp.toPx()),
                    )
                }
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) { keys() }
        if (caption != null) {
            Text(
                caption,
                style = DigitStyle.copy(fontSize = 9.sp),
                color = Chrome.chromeIndigo,
                modifier = Modifier.padding(top = 2.dp).clearAndSetSemantics { },
            )
        }
    }
}

/**
 * [KeyBank] 안의 납작한 베이지 키 한 개. 광택 없는 단색 윗면에 아래로 2dp 턱이 보이고, 누르면 턱만큼 내려가며 윗면이 어두워진다.
 * 쓸 수 없을 때는 눌린 채 멈춘 모양에 기호만 흐리게 둔다.
 */
@Composable
fun BankKey(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 쓸 수 없는 키는 눌린 채 걸려 있는 모양으로 멈춰 둔다
    val down = !enabled || pressed
    val sink by animateDpAsState(if (down) BANK_LIP else 0.dp, tween(50), label = "bankSink")
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .size(width = 38.dp, height = 29.dp + BANK_LIP)
            .clickable(
                interaction, indication = null, enabled = enabled,
                onClickLabel = onClickLabel, role = Role.Button, onClick = onClick,
            ),
        propagateMinConstraints = true,
    ) {
        // 아래 턱 (눌리면 윗면에 가려 사라진다)
        Box(
            Modifier
                .matchParentSize()
                .padding(top = BANK_LIP)
                .clip(shape)
                .background(BANK_LIP_COLOR)
        )
        Box(
            Modifier
                .padding(top = sink, bottom = BANK_LIP - sink)
                .clip(shape)
                // 무광 단색 윗면. 눌리면 한 단계 어둡게
                .background(if (down) Color(0xFF938B7D) else Color(0xFFAAA292))
                .drawWithContent {
                    drawContent()
                    val line = 1.dp.toPx()
                    if (down) {
                        // 눌린 윗면: 안쪽 위로 그늘
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.25f),
                                1f to Color.Transparent,
                                endY = 3.dp.toPx(),
                            ),
                            size = Size(size.width, 3.dp.toPx()),
                        )
                    } else {
                        // 위 테두리에만 밝은 선 한 줄
                        drawRect(Color(0xFFD9D2C5), size = Size(size.width, line))
                                            }
                }
                .border(1.dp, Color(0xFF5C564B), shape),
            contentAlignment = Alignment.Center,
        ) {
            // 걸린 키의 기호는 흐리게
            Box(Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f }, content = content)
        }
    }
}

private val BANK_LIP = 2.dp
private val BANK_LIP_COLOR = Color(0xFF595347)
private val WELL_BG = Color(0xFF9E9687)
private val WELL_SHADE = Color(0xFF6E675B)
private val WELL_LIGHT = Color(0xFFE3DDD1)

/** 묶음 키 위의 글자·기호 색 */
val BankInk = Color(0xFF2B2823)

/** 판 키·모따기 스위치의 단단한 검은 테와 바닥 */
private val KEY_EDGE = Color(0xFF1C1B18)
private val PLATE_LIP = 3.dp
private val PLATE_SHAPE = RoundedCornerShape(5.dp)
private val PLATE_KEY_SHAPE = RoundedCornerShape(3.dp)

/**
 * 플랫 판에 박은 키: 카드와 같은 계열의 단색 판(네 귀퉁이 리벳, 아래 2dp 검은 바닥) 안에
 * 단색 키가 올라가 있다. 그라데이션 없이 위 1dp 밝은 선과 아래 3dp 검은 턱만으로 깊이를 낸다.
 * 누르면 턱만큼 내려가 한 단계 어두워지고 위에 2dp 그늘 선이 생긴다.
 * [latched]면 눌린 채 걸린 표시 칸으로, 누를 수 없다.
 */
@Composable
fun PlateKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Chrome.signal,
    enabled: Boolean = true,
    latched: Boolean = false,
    onClickLabel: String? = null,
    faceModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val down = latched || (pressed && enabled)
    val sink by animateDpAsState(if (down) PLATE_LIP else 0.dp, tween(50), label = "plateSink")
    Box(
        modifier
            .graphicsLayer { alpha = if (enabled || latched) 1f else 0.45f }
            // 판 아래 검은 바닥
            .drawBehind {
                val off = 2.dp.toPx()
                drawRoundRect(
                    KEY_EDGE,
                    topLeft = Offset(0f, off),
                    size = Size(size.width, size.height - off),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            }
            .padding(bottom = 2.dp)
            .clip(PLATE_SHAPE)
            .background(Chrome.lavender)
            .drawWithContent {
                drawContent()
                // 네 귀퉁이 리벳
                val r = 1.4.dp.toPx()
                val i = 4.dp.toPx()
                listOf(
                    Offset(i, i), Offset(size.width - i, i),
                    Offset(i, size.height - i), Offset(size.width - i, size.height - i),
                ).forEach { drawCircle(KEY_EDGE, radius = r, center = it) }
            }
            .border(1.5.dp, KEY_EDGE, PLATE_SHAPE)
            .then(
                if (latched) Modifier else Modifier.clickable(
                    interaction, indication = null, enabled = enabled,
                    onClickLabel = onClickLabel, role = Role.Button, onClick = onClick,
                )
            )
            .padding(6.dp),
        propagateMinConstraints = true,
    ) {
        // 키 아래 검은 턱
        Box(
            Modifier
                .matchParentSize()
                .padding(top = PLATE_LIP)
                .clip(PLATE_KEY_SHAPE)
                .background(KEY_EDGE)
        )
        Box(
            Modifier
                .padding(top = sink, bottom = PLATE_LIP - sink)
                .then(faceModifier)
                .clip(PLATE_KEY_SHAPE)
                .background(if (down) lerp(color, Color.Black, 0.10f) else color)
                .drawWithContent {
                    drawContent()
                    if (down) {
                        drawRect(lerp(color, Color.Black, 0.35f), topLeft = Offset(0f, 2.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
                    } else {
                        // 테두리 바로 안쪽에 밝은 선
                        drawRect(lerp(color, Color.White, 0.22f), topLeft = Offset(0f, 2.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
                    }
                }
                .border(2.dp, KEY_EDGE, PLATE_KEY_SHAPE)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private val CHAMFER = CutCornerShape(6.dp)
private val SWITCH_LIP = 3.dp

/**
 * 모따기 택티컬 스위치: 얇은 단색 틀 안에 귀를 깎은 팔각 키, 오른쪽 위에 작은 상태 LED.
 * 평소 LED는 꺼진 짙은 빨강, 누르는 동안 형광 녹으로 켜진다. 그라데이션 없음.
 */
@Composable
fun ChamferSwitch(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Chrome.signal,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) SWITCH_LIP else 0.dp, tween(50), label = "switchSink")
    val frame = RoundedCornerShape(3.dp)
    Box(
        modifier
            .drawBehind {
                val off = 2.dp.toPx()
                drawRoundRect(
                    KEY_EDGE,
                    topLeft = Offset(0f, off),
                    size = Size(size.width, size.height - off),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            .padding(bottom = 2.dp)
            .clip(frame)
            .background(Chrome.platinum)
            .border(1.5.dp, KEY_EDGE, frame)
            .clickable(interaction, indication = null, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(4.dp),
        propagateMinConstraints = true,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = SWITCH_LIP)
                .clip(CHAMFER)
                .background(KEY_EDGE)
        )
        Box(
            Modifier
                .padding(top = sink, bottom = SWITCH_LIP - sink)
                .clip(CHAMFER)
                .background(if (pressed) lerp(color, Color.Black, 0.13f) else color)
                .drawWithContent {
                    drawContent()
                    if (pressed) {
                        drawRect(lerp(color, Color.Black, 0.35f), topLeft = Offset(0f, 2.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
                    } else {
                        // 테두리 바로 안쪽에 밝은 선
                        drawRect(lerp(color, Color.White, 0.22f), topLeft = Offset(0f, 2.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
                    }
                }
                .border(2.dp, KEY_EDGE, CHAMFER),
            contentAlignment = Alignment.Center,
        ) {
            content()
            // 상태 LED
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .size(6.dp)
                    .background(if (pressed) Color(0xFF52FF7F) else Color(0xFF5C1808))
                    .border(1.dp, KEY_EDGE)
            )
        }
    }
}

/**
 * 카세트 스킨의 팝업 메뉴: 짙은 갈색 테를 두른 검은 CRT 화면에 "> 항목" 줄이 형광 글자로 뜬다.
 * 항목은 [CrtMenuItem]으로 채운다.
 */
@Composable
fun CrtMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Chrome.screen,
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(3.dp, Chrome.carbon),
        shadowElevation = 6.dp,
        modifier = Modifier.scanlines(),
    ) {
        Text(
            "MENU",
            style = DigitStyle.copy(fontSize = 9.sp),
            color = Chrome.phosphorDim.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp).clearAndSetSemantics { },
        )
        content()
    }
}

/** [CrtMenu]의 한 줄: 커서 ">" + 형광 글자. 위험한 항목은 [ink]를 [CrtAlert]로 */
@Composable
fun CrtMenuItem(label: String, icon: ImageVector, ink: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            // 아이콘과 글자를 붙여 한 줄 명령처럼
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "> $label",
                    style = DigitStyle.copy(fontSize = 13.sp, shadow = phosphorGlow(ink)),
                    color = ink,
                )
            }
        },
        onClick = onClick,
        // 기본 메뉴 줄(48dp)보다 촘촘한 단말기 목록
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    )
}

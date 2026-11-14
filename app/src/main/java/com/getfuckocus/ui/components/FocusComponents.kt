package com.getfuckocus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.bevel
import com.getfuckocus.ui.theme.circleBevel
import com.getfuckocus.ui.theme.halftone
import com.getfuckocus.ui.theme.insetBevel
import com.getfuckocus.ui.theme.plate
import kotlin.math.abs

/*
 * 앱 전용 UI 부품. "콘솔 크롬" 디자인 언어(design/DESIGN.md)를 따른다.
 * - 조작 부품은 거의 각진 모서리(2dp), 큰 판은 45도로 잘라낸 모서리.
 * - 깊이는 흐린 그림자가 아니라 위쪽 밝은 선 + 아래쪽 남색 선(bevel)으로 표현한다.
 * - 따뜻한 색(시그널 오렌지, 앰버)은 누를 수 있는 것에만 쓴다.
 */

enum class FocusButtonStyle { Primary, Tonal, Outline, Danger, Ghost }

/** 돌출된 버튼. 누르면 아래로 1dp 들어가고 위쪽 밝은 선이 사라진다. */
@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FocusButtonStyle = FocusButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val (defaultContainer, defaultContent) = when (style) {
        // 시그널 오렌지에 흰 글자는 대비가 부족해서 카본 글자를 쓴다
        FocusButtonStyle.Primary -> Chrome.signal to Chrome.carbon
        FocusButtonStyle.Tonal -> Chrome.amber to Chrome.carbon
        FocusButtonStyle.Outline -> Chrome.white to Chrome.chromeIndigo
        FocusButtonStyle.Danger -> Chrome.red to Chrome.white
        FocusButtonStyle.Ghost -> Color.Transparent to Chrome.chromeIndigo
    }
    val container = containerColor ?: defaultContainer
    val content = contentColor ?: defaultContent
    val raised = style != FocusButtonStyle.Ghost
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed && enabled && raised) 1.dp else 0.dp, label = "buttonSink")

    Row(
        modifier = modifier
            .offset(y = sink)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .heightIn(min = height)
            .clip(ChromeShapes.control)
            .background(if (pressed && !raised) content.copy(alpha = 0.12f) else container)
            .then(
                when {
                    style == FocusButtonStyle.Outline -> Modifier.border(1.dp, Chrome.hairline, ChromeShapes.control)
                    raised -> Modifier.bevel(
                        highlight = if (pressed) Color.Transparent else Chrome.highlight,
                        shadow = Color(0x55000000),
                    )
                    else -> Modifier
                }
            )
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

/** 네모난 아이콘 버튼. 누르면 뒤에 옅은 판이 깔린다. */
@Composable
fun FocusIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(ChromeShapes.small)
            .background(if (pressed) tint.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** 시그널 오렌지 원판 안의 재생 삼각형. 과제를 바로 시작할 때 쓴다. */
@Composable
fun FocusPlayButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    /** 재생 말고 다른 행동(촬영, 체크)에도 같은 크기·위치의 버튼을 쓰도록 아이콘을 바꿀 수 있다 */
    icon: ImageVector = Icons.Default.PlayArrow,
    containerColor: Color = Chrome.signal,
    iconScale: Float = 0.55f,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "playScale")
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(CircleShape)
            .background(Chrome.chromeIndigo)
            .padding(3.dp)
            .clip(CircleShape)
            .background(containerColor)
            .circleBevel(highlight = if (pressed) Color.Transparent else Chrome.highlight)
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Chrome.carbon,
            modifier = Modifier.size(size * iconScale),
        )
    }
}

/**
 * 화면 위에 떠 있는 둥근 주 버튼. 카본 테두리 + 바로 아래로 비켜 찍은 단단한 그림자로
 * 페리윙클 바탕과 아래 카드들에서 확실히 떨어져 보이게 한다. 누르면 그림자 자리로 곧게 눌려 내려간다.
 */
@Composable
fun FocusFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shadowOffset = 6.dp
    val sink by animateDpAsState(if (pressed) shadowOffset else 0.dp, label = "fabSink")
    Box(modifier.size(width = size, height = size + shadowOffset)) {
        Box(
            Modifier
                .offset(y = shadowOffset)
                .size(size)
                .clip(CircleShape)
                .background(Chrome.carbon.copy(alpha = 0.75f))
        )
        Box(
            Modifier
                .offset(y = sink)
                .size(size)
                .clip(CircleShape)
                .background(Chrome.carbon)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Chrome.signal)
                .circleBevel(highlight = if (pressed) Color.Transparent else Chrome.highlight)
                .clickable(interaction, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = Chrome.carbon, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** 선택형 칩. 선택되면 앰버 판으로 튀어나온다. */
@Composable
fun FocusChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    /** 요일처럼 한 글자짜리를 한 줄에 여러 개 나눠 담을 때: 좌우 여백 없이 가운데 정렬 */
    compact: Boolean = false,
) {
    val container by animateColorAsState(if (selected) Chrome.amber else Chrome.white, label = "chipContainer")
    val content = Chrome.carbon
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .height(46.dp)
            .clip(ChromeShapes.control)
            .background(container)
            .then(
                if (selected) Modifier.bevel(shadow = Color(0x55000000))
                else Modifier.border(1.dp, Chrome.hairline, ChromeShapes.control)
            )
            .clickable(
                remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (compact) 0.dp else 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
    }
}

/** 켜고 끄는 슬라이드 스위치. 켜지면 홈이 앰버로 바뀌고 네모난 손잡이가 미끄러진다. */
@Composable
fun FocusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val track by animateColorAsState(if (checked) Chrome.amber else Chrome.mutedIndigo, label = "switchTrack")
    // 홈 안쪽(50×26)에서 손잡이(22×22)를 위·아래·옆 모두 2dp씩 띄운다
    val thumbOffset by animateDpAsState(if (checked) 26.dp else 2.dp, label = "switchThumb")
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .size(width = 54.dp, height = 30.dp)
            .clip(ChromeShapes.small)
            .background(Chrome.chromeIndigo)
            .padding(2.dp)
            .clip(ChromeShapes.control)
            .background(track)
            // 홈 안쪽 그늘: 위·왼쪽은 어둡게, 아래는 얇게 밝혀 판에 파인 홈처럼
            .drawBehind {
                val top = 3.dp.toPx()
                val side = 1.5.dp.toPx()
                drawRect(Color(0x59000000), size = size.copy(height = top))
                drawRect(Color(0x26000000), topLeft = Offset(0f, top), size = size.copy(height = 1.5.dp.toPx()))
                drawRect(Color(0x33000000), topLeft = Offset(0f, top), size = size.copy(width = side, height = size.height - top))
                drawRect(Color(0x4DFFFFFF), topLeft = Offset(0f, size.height - 1.dp.toPx()), size = size.copy(height = 1.dp.toPx()))
            }
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        // 손잡이: 붉은 기 도는 회색 플라스틱에, 옛 게임기 슬라이드 스위치처럼 미끄럼 방지 홈 세 줄
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(ChromeShapes.control)
                .background(SWITCH_THUMB)
                .border(1.dp, Chrome.hairline, ChromeShapes.control)
                .drawBehind {
                    // 튀어나온 손잡이: 위 밝은 선, 아래 그늘
                    drawRect(Color(0x99FFFFFF), topLeft = Offset(1.dp.toPx(), 1.dp.toPx()), size = size.copy(width = size.width - 2.dp.toPx(), height = 1.5.dp.toPx()))
                    drawRect(Color(0x33000000), topLeft = Offset(0f, size.height - 3.dp.toPx()), size = size.copy(height = 2.dp.toPx()))
                    // 세로 홈 세 줄: 어두운 선 오른쪽에 밝은 선을 붙여 파인 것처럼
                    val gap = 4.dp.toPx()
                    val line = 1.dp.toPx()
                    val top = 6.dp.toPx()
                    val length = size.height - top * 2
                    for (i in -1..1) {
                        val x = size.width / 2 + i * gap - line / 2
                        drawRect(Color(0x73302828), topLeft = Offset(x, top), size = Size(line, length))
                        drawRect(Color(0x80FFFFFF), topLeft = Offset(x + line, top), size = Size(line, length))
                    }
                }
        )
    }
}

/** 스위치 손잡이 색: 붉은 기가 살짝 도는 회색 */
private val SWITCH_THUMB = Color(0xFFC9BCBC)

/**
 * 판에 파인 홈 같은 입력칸.
 * - 라벨은 칸 왼쪽 위에 붙은 카본 탭이고, 입력 중이면 앰버 탭으로 바뀐다.
 * - 칸은 위·왼쪽에 어두운 선이 있어 판 안으로 파여 보인다. 입력 중이면 테두리가 앰버로 두꺼워진다.
 * - [trailing]은 칸 오른쪽 끝의 하늘색 버튼 칸에 들어간다.
 */
@Composable
fun FocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val frame by animateColorAsState(if (focused) Chrome.amber else Chrome.carbon, label = "fieldFrame")
    val borderWidth by animateDpAsState(if (focused) 2.dp else 1.dp, label = "fieldBorder")
    val fieldShape = RoundedCornerShape(topStart = 0.dp, topEnd = 2.dp, bottomEnd = 2.dp, bottomStart = 2.dp)

    Column(modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f }) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (focused) Chrome.carbon else Chrome.sky,
            modifier = Modifier
                .clip(CutCornerShape(topEnd = 8.dp))
                .background(frame)
                .padding(start = 10.dp, end = 14.dp, top = 4.dp, bottom = 3.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 50.dp)
                .clip(fieldShape)
                .background(Chrome.white)
                .insetBevel()
                .border(borderWidth, if (focused) Chrome.amber else Chrome.carbon, fieldShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 13.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Chrome.carbon),
                cursorBrush = SolidColor(Chrome.chromeIndigo),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interaction,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Chrome.hairline)
                        }
                        inner()
                    }
                },
            )
            if (trailing != null) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(52.dp)
                        .background(Chrome.sky)
                        .border(width = 1.dp, color = Chrome.carbon.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides Chrome.carbon) { trailing() }
                }
            }
        }
    }
}

/** 판 하나. 기본은 모서리를 잘라낸 플래티넘 판이다. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    shape: Shape = ChromeShapes.panel,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 1.dp else 0.dp, label = "cardSink")
    Column(
        modifier = modifier
            .offset(y = sink)
            .plate(color, shape)
            .then(
                if (onClick != null) Modifier.clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/**
 * 제목 띠가 붙은 판. 라이트 블루 띠에 아이콘과 굵은 제목, 아래는 웜 그레이 내용 판.
 * 화면 안의 구역(과제, 시간, 장소…)을 나눌 때 쓴다.
 * - [index]를 주면 제목 앞에 "01" 같은 번호 탭이 붙는다 (게임 메뉴의 항목 번호).
 * - [expanded]를 주면 띠를 눌러 접고 펼 수 있다. null이면 항상 펼쳐 둔다.
 */
@Composable
fun FocusPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = Icons.Default.Menu,
    index: String? = null,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val toggle = onToggle.takeIf { expanded != null }
    val collapsible = toggle != null
    Column(modifier.plate(Chrome.platinum)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Chrome.sky)
                .bevel(shadow = Chrome.chromeIndigo, shadowWidth = 1.dp)
                .then(
                    if (toggle != null) Modifier.clickable(
                        remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = if (expanded == true) "$title 접기" else "$title 펼치기",
                        onClick = toggle,
                    ) else Modifier
                )
                .heightIn(min = 48.dp)
                .padding(start = if (index != null) 0.dp else 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index != null) {
                Text(
                    index,
                    style = DigitStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                    color = Chrome.navGold,
                    modifier = Modifier
                        .clip(CutCornerShape(bottomEnd = 8.dp))
                        .background(Chrome.carbon)
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                )
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
                }
            }
            trailing?.invoke()
            if (collapsible) {
                Icon(
                    if (expanded == true) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Chrome.carbon,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (expanded != false) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides Chrome.carbon) { content() }
            }
        }
    }
}

/**
 * 상단 바. 점무늬를 깐 카본 판에 금색 제목, 그 아래 스카이색 보조 띠에 부제목.
 * 상태 표시줄 영역까지 카본으로 칠한다.
 */
@Composable
fun FocusTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** 보조 띠 오른쪽 끝에 고정폭 숫자로 보여줄 값 (예: 현재 시각) */
    subtitleTrailing: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Chrome.carbon)
                .halftone()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides Chrome.sky) {
                if (onBack != null) {
                    FocusIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", onClick = onBack)
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Chrome.navGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                actions()
            }
        }
        if (subtitle != null || subtitleTrailing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Chrome.sky)
                    .bevel(shadow = Chrome.chromeIndigo, shadowWidth = 1.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    subtitle.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Chrome.carbon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (subtitleTrailing != null) {
                    Text(
                        subtitleTrailing,
                        style = DigitStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                        color = Chrome.carbon,
                    )
                }
            }
        }
    }
}

/** 제목 띠, 내용, 버튼 두 개로 된 대화상자. */
@Composable
fun FocusDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = "취소",
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().plate(Chrome.platinum, ChromeShapes.panel)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Chrome.navGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Chrome.carbon)
                    .halftone()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides Chrome.carbon) { content() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    FocusButton(dismissText, onDismiss, style = FocusButtonStyle.Ghost, height = 44.dp)
                    FocusButton(
                        confirmText, onConfirm,
                        style = if (danger) FocusButtonStyle.Danger else FocusButtonStyle.Primary,
                        height = 44.dp,
                    )
                }
            }
        }
    }
}

/** 시와 분을 위아래로 굴려 고르는 대화상자. */
@Composable
fun TimeWheelDialog(
    title: String,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (minute: Int) -> Unit,
    hourSuffix: String = "",
    minuteSuffix: String = "",
) {
    var hour by remember { mutableIntStateOf(initialMinute / 60) }
    var minute by remember { mutableIntStateOf(initialMinute % 60) }
    FocusDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = "확인",
        onConfirm = { onConfirm(hour * 60 + minute) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            WheelPicker(count = 24, initial = hour, onSelected = { hour = it }, modifier = Modifier.width(96.dp)) {
                "%02d%s".format(it, hourSuffix)
            }
            Text(
                ":",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            WheelPicker(count = 60, initial = minute, onSelected = { minute = it }, modifier = Modifier.width(96.dp)) {
                "%02d%s".format(it, minuteSuffix)
            }
        }
    }
}

/** 0 ~ count-1 중 하나를 굴려서 고르는 휠. 가운데 앰버 칸이 선택값이다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    count: Int,
    initial: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (Int) -> String,
) {
    val itemHeight = 48.dp
    val visibleItems = 5
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: initial
        }
    }
    LaunchedEffect(centered) { onSelected(centered) }

    Box(
        modifier
            .height(itemHeight * visibleItems)
            .clip(ChromeShapes.small)
            .background(Chrome.white)
            .border(1.dp, Chrome.hairline, ChromeShapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .plate(Chrome.amber, ChromeShapes.control)
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count) { index ->
                val isCenter = index == centered
                Box(Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label(index),
                        style = if (isCenter) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCenter) FontWeight.Black else FontWeight.Normal,
                        color = if (isCenter) Chrome.carbon else Chrome.hairline,
                    )
                }
            }
        }
    }
}

/** 남색 홈에 끼운 가로 진행 막대. */
@Composable
fun FocusProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Chrome.signal,
    trackColor: Color = Chrome.mutedIndigo,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(ChromeShapes.control)
            .background(trackColor)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(color)
                .bevel(shadowWidth = 1.dp, shadow = Color(0x44000000), highlightWidth = 1.dp)
        )
    }
}

/** 원형 진행 표시. 12시 방향에서 시계 방향으로 찬다. */
@Composable
fun FocusRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Chrome.signal,
    trackColor: Color = Chrome.mutedIndigo,
    strokeWidth: Dp = 10.dp,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2, stroke / 2)
        drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        drawArc(
            color, -90f, 360f * progress.coerceIn(0f, 1f), useCenter = false,
            topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
        )
    }
}

/** 돌아가는 로딩 표시. */
@Composable
fun FocusSpinner(
    modifier: Modifier = Modifier.size(36.dp),
    color: Color = Chrome.signal,
    strokeWidth: Dp = 4.dp,
) {
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinnerRotation",
    )
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        drawArc(
            color, rotation, 100f, useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Butt),
        )
    }
}

/**
 * 게임 상자 표지 글씨처럼 굵은 외곽선과 단단한 그림자를 두른 큰 글자.
 * 외곽선 글자, 채운 글자를 겹쳐 그린다.
 */
@Composable
fun OutlinedDisplayText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fill: Color = Chrome.white,
    outline: Color = Chrome.carbon,
    outlineWidth: Dp = 6.dp,
    shadowOffset: Dp = 4.dp,
) {
    val stroke = Stroke(width = with(LocalDensity.current) { outlineWidth.toPx() })
    val outlined = style.copy(color = outline, drawStyle = stroke)
    Box(modifier) {
        // 오른쪽 아래로 비켜 찍은 단단한 그림자, 그 위에 외곽선, 그 위에 채운 글자
        Text(text, style = outlined, modifier = Modifier.offset(x = shadowOffset, y = shadowOffset))
        Text(text, style = outlined)
        Text(text, style = style.copy(color = fill))
    }
}

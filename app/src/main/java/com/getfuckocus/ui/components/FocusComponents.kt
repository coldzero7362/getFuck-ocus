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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.getfuckocus.ui.theme.bevel
import com.getfuckocus.ui.theme.circleBevel
import com.getfuckocus.ui.theme.halftone
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
            .size(44.dp)
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
            .background(Chrome.signal)
            .circleBevel(highlight = if (pressed) Color.Transparent else Chrome.highlight)
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = contentDescription,
            tint = Chrome.carbon,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/**
 * 화면 위에 떠 있는 둥근 주 버튼. 카본 테두리 + 오른쪽 아래로 비켜 찍은 단단한 그림자로
 * 페리윙클 바탕과 아래 카드들에서 확실히 떨어져 보이게 한다. 누르면 그림자 자리로 눌려 들어간다.
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
    val shadowOffset = 5.dp
    val sink by animateDpAsState(if (pressed) shadowOffset else 0.dp, label = "fabSink")
    Box(modifier.size(size + shadowOffset)) {
        Box(
            Modifier
                .offset(x = shadowOffset, y = shadowOffset)
                .size(size)
                .clip(CircleShape)
                .background(Chrome.carbon.copy(alpha = 0.75f))
        )
        Box(
            Modifier
                .offset(x = sink, y = sink)
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
) {
    val container by animateColorAsState(if (selected) Chrome.amber else Chrome.white, label = "chipContainer")
    val content = Chrome.carbon
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .height(40.dp)
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
            .padding(horizontal = 14.dp),
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
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        // 아래쪽에만 그림자 선을 넣으면 손잡이가 위로 쏠려 보여서, 테두리를 사방 똑같이 둔다
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(ChromeShapes.control)
                .background(Chrome.white)
                .border(1.dp, Chrome.hairline, ChromeShapes.control)
        )
    }
}

/** 위에 라벨이 붙는 흰 입력칸. 포커스되면 테두리가 남색으로 두꺼워진다. */
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
    val borderWidth by animateDpAsState(if (focused) 2.dp else 1.dp, label = "fieldBorder")

    Column(
        modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Chrome.chromeIndigo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .clip(ChromeShapes.control)
                .background(Chrome.white)
                .border(borderWidth, if (focused) Chrome.chromeIndigo else Chrome.hairline, ChromeShapes.control)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(vertical = 13.dp),
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
            trailing?.invoke()
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
 * 제목 띠가 붙은 판. 페리윙클 띠에 ≡ 표시와 굵은 제목, 아래는 플래티넘 내용 판.
 * 화면 안의 구역(과제, 시간, 장소…)을 나눌 때 쓴다.
 */
@Composable
fun FocusPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.plate(Chrome.platinum)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Chrome.periwinkle)
                .bevel(shadow = Chrome.chromeIndigo, shadowWidth = 1.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
                }
            }
            trailing?.invoke()
        }
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides Chrome.carbon) { content() }
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
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = Chrome.carbon,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Chrome.sky)
                    .bevel(shadow = Chrome.chromeIndigo, shadowWidth = 1.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
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

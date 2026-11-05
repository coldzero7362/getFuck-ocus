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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs

/*
 * 앱 전용 UI 부품. Material 기본 컴포넌트 대신 이것들을 쓴다.
 * 색은 MaterialTheme.colorScheme(앱 고유 팔레트)에서만 가져온다.
 */

enum class FocusButtonStyle { Primary, Tonal, Outline, Danger, Ghost }

/** 누르면 살짝 줄어드는 둥근 버튼. */
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
    val colors = MaterialTheme.colorScheme
    val (defaultContainer, defaultContent) = when (style) {
        FocusButtonStyle.Primary -> colors.primary to colors.onPrimary
        FocusButtonStyle.Tonal -> colors.secondaryContainer to colors.onSecondaryContainer
        FocusButtonStyle.Outline -> Color.Transparent to colors.primary
        FocusButtonStyle.Danger -> colors.error to colors.onError
        FocusButtonStyle.Ghost -> Color.Transparent to colors.primary
    }
    val container = containerColor ?: defaultContainer
    val content = contentColor ?: defaultContent
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, label = "buttonScale")

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .heightIn(min = height)
            .clip(shape)
            .background(if (pressed && style == FocusButtonStyle.Ghost) content.copy(alpha = 0.08f) else container)
            .then(
                if (style == FocusButtonStyle.Outline) Modifier.border(1.5.dp, content.copy(alpha = 0.5f), shape)
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** 원형 아이콘 버튼. */
@Composable
fun FocusIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (pressed) tint.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** 남색 원 안에 재생 삼각형이 있는 버튼. 과제를 바로 시작할 때 쓴다. */
@Composable
fun FocusPlayButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "playScale")
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = contentDescription,
            tint = colors.onPrimary,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** 선택형 칩. 선택되면 주황으로 채워진다. [trailingIcon]이 있으면 오른쪽에 붙는다. */
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
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.surfaceContainerLowest,
        label = "chipContainer",
    )
    val content = if (selected) colors.onSecondaryContainer else colors.onSurface
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .height(40.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, if (selected) Color.Transparent else colors.outlineVariant, shape)
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
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
    }
}

/** 켜고 끄는 토글. 손잡이가 미끄러지듯 움직인다. */
@Composable
fun FocusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val track by animateColorAsState(if (checked) colors.primary else colors.outlineVariant, label = "switchTrack")
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 2.dp, label = "switchThumb")
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(50))
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
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(28.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** 위에 라벨이 붙는 입력칸. 포커스되면 테두리가 남색으로 바뀐다. */
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
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) colors.primary else Color.Transparent, label = "fieldBorder")
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(shape)
                .background(colors.surfaceContainerHighest)
                .border(2.dp, border, shape)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interaction,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                },
            )
            trailing?.invoke()
        }
    }
}

/** 둥근 면. Card 대신 쓴다. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cardScale")
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) { content() }
    }
}

/** 상단 바. 상태 표시줄 높이만큼 스스로 내려간다. */
@Composable
fun FocusTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(68.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            FocusIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", onClick = onBack)
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

/** 제목, 내용, 버튼 두 개로 된 대화상자. */
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
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

/** 시와 분을 위아래로 굴려 고르는 대화상자. 시계 모양 TimePicker 대신 쓴다. */
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            WheelPicker(count = 60, initial = minute, onSelected = { minute = it }, modifier = Modifier.width(96.dp)) {
                "%02d%s".format(it, minuteSuffix)
            }
        }
    }
}

/** 0 ~ count-1 중 하나를 굴려서 고르는 휠. 가운데 칸이 선택값이다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    count: Int,
    initial: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (Int) -> String,
) {
    val colors = MaterialTheme.colorScheme
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

    Box(modifier.height(itemHeight * visibleItems), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.primaryContainer)
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
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) colors.onPrimaryContainer else colors.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

/** 둥근 끝의 가로 진행 막대. */
@Composable
fun FocusProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** 원형 진행 표시. 12시 방향에서 시계 방향으로 찬다. */
@Composable
fun FocusRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 10.dp,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2, stroke / 2)
        drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        drawArc(
            color, -90f, 360f * progress.coerceIn(0f, 1f), useCenter = false,
            topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/** 돌아가는 로딩 표시. */
@Composable
fun FocusSpinner(
    modifier: Modifier = Modifier.size(36.dp),
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 3.dp,
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
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

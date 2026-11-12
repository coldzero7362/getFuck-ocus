package com.getfuckocus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.bevel
import com.getfuckocus.ui.theme.insetBevel
import com.getfuckocus.ui.theme.plate
import com.getfuckocus.ui.theme.recessed

/*
 * 상태를 알리는 부품. 상태는 색만으로 구분하지 않고 항상 아이콘과 글자를 함께 쓴다.
 */

/** 알림의 성격. 색과 기본 아이콘이 함께 정해진다. */
enum class Tone(val container: Color, val content: Color, val accent: Color, val icon: ImageVector) {
    Info(Chrome.sky, Chrome.carbon, Chrome.chromeIndigo, Icons.Default.Info),
    Success(Chrome.ice, Chrome.teal, Chrome.success, Icons.Default.CheckCircle),
    Warning(Chrome.warningSoft, Chrome.carbon, Chrome.warning, Icons.Default.Warning),
    Danger(Chrome.redSoft, Chrome.redInk, Chrome.red, Icons.Default.Close),
}

/**
 * 한 줄짜리 상태 안내. 큰 경고 배너 대신, 왼쪽 색 띠 + 아이콘 + 짧은 글로 자리를 적게 차지한다.
 * [actionLabel]이 있으면 줄 전체가 버튼이 되고 오른쪽에 화살표가 붙는다.
 */
@Composable
fun FocusNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Info,
    title: String? = null,
    icon: ImageVector = tone.icon,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(ChromeShapes.small)
            .background(tone.container)
            .then(
                if (onAction != null) Modifier.clickable(
                    remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = onAction,
                ) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(6.dp).heightIn(min = 48.dp).background(tone.accent))
        Spacer(Modifier.width(10.dp))
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = tone.content)
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = tone.content)
        }
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = tone.content,
                modifier = Modifier.padding(start = 8.dp),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tone.content,
                modifier = Modifier.padding(end = 6.dp).size(22.dp),
            )
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

/** 켜짐/꺼짐 같은 상태 배지. 색과 함께 아이콘·글자를 같이 보여 준다. */
@Composable
fun FocusStatusBadge(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector = tone.icon) {
    Row(
        modifier = modifier
            .clip(ChromeShapes.control)
            .background(tone.container)
            .border(1.dp, tone.accent, ChromeShapes.control)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone.content, maxLines = 1)
    }
}

/**
 * 판 위에 붙은 카본 탭 글자. 카드의 성격(다음 과제, 진행 중, 잠금 중…)을 게임 메뉴 탭처럼 표시한다.
 */
@Composable
fun FocusTab(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = Chrome.carbon,
    content: Color = Chrome.navGold,
) {
    Row(
        modifier = modifier
            .clip(CutCornerShape(topEnd = 10.dp))
            .background(container)
            .padding(start = 10.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

/**
 * 판에 파인 흰 칸 안의 수치 하나. 위에 작은 라벨, 아래에 고정폭 큰 숫자.
 * 게임의 점수판처럼 핵심 숫자(남은 시간, 완료율)를 보여 줄 때 쓴다.
 */
@Composable
fun FocusStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = Chrome.carbon,
    caption: String? = null,
    /** 가장 중요한 시간 하나에만 true: 32sp 큰 숫자 */
    large: Boolean = false,
    /** 이 칸이 놓인 판의 색. 그 색 계열로 살짝 파여 들어간 칸이 된다 */
    surface: Color = Chrome.platinum,
) {
    Column(
        modifier = modifier
            .recessed(surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Chrome.chromeIndigo, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = Chrome.chromeIndigo, maxLines = 1)
        }
        Text(
            value,
            style = DigitStyle.copy(
                fontSize = when {
                    !large -> MaterialTheme.typography.titleMedium.fontSize
                    // 큰 숫자가 칸을 넘지 않게 글자 수에 따라 한 단계씩 줄인다
                    value.length <= 4 -> 32.sp
                    value.length <= 6 -> 26.sp
                    else -> 22.sp
                },
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo, maxLines = 1)
        }
    }
}

/** 목록 위 구역 이름. 굵은 글자 + 개수 배지, 아래쪽에 남색 선. */
@Composable
fun FocusSectionLabel(text: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(Chrome.navGold))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                "$count",
                style = DigitStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                color = Chrome.sky,
                modifier = Modifier
                    .clip(ChromeShapes.control)
                    .background(Chrome.carbon)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

/** 비어 있거나, 쓸 수 없거나, 권한이 없을 때 목록 자리에 놓는 안내 판. */
@Composable
fun FocusEmptyState(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .plate(Chrome.platinum)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(ChromeShapes.small)
                .background(tone.container)
                .bevel(shadow = tone.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(32.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Chrome.carbon, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Chrome.chromeIndigo, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.size(4.dp))
            FocusButton(actionLabel, onClick = onAction)
        }
    }
}

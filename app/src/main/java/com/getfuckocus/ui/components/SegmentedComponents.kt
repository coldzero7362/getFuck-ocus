package com.getfuckocus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes

/** 붙은 버튼 블록의 칸 하나 */
data class SegmentItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val icon: ImageVector? = null,
)

/**
 * 칸을 틈 없이 한 줄로 붙인 버튼 블록. 옛 카세트 플레이어의 재생·정지 키처럼
 * 고르지 않은 칸은 튀어나와 있고, 고른 칸은 안으로 눌려 들어가 있다.
 * 칸 사이는 짙은 홈으로 나눈다. 여러 줄이 필요하면 블록을 줄마다 하나씩 쓴다.
 */
@Composable
fun FocusSegmented(items: List<SegmentItem>, modifier: Modifier = Modifier, height: Dp = 46.dp) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            // 모서리는 둥글리지 않고 각지게 둔다
            // 칸 사이와 둘레로 보이는 짙은 홈 (키가 박힌 본체)
            .background(Chrome.hairline)
            .padding(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items.forEach { item -> CassetteKey(item, Modifier.weight(1f).fillMaxHeight()) }
    }
}

@Composable
private fun CassetteKey(item: SegmentItem, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val touching by interaction.collectIsPressedAsState()
    // 고른 칸은 눌린 채로 있고, 다른 칸도 누르는 동안에는 눌린 모양을 보인다
    val pressed = item.selected || (touching && item.enabled)
    Box(
        modifier
            .graphicsLayer { alpha = if (item.enabled) 1f else 0.4f }
            .background(if (item.selected) PRESSED_AMBER else Chrome.white)
            .cassetteShading(pressed)
            .clickable(interaction, indication = null, enabled = item.enabled, role = Role.RadioButton, onClick = item.onClick)
            .semantics { selected = item.selected },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            // 눌린 키는 글자도 살짝 내려앉는다
            Modifier.offset(y = if (pressed) 1.5.dp else 0.dp).padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item.icon?.let {
                Icon(it, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                item.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (item.selected) FontWeight.Black else FontWeight.Medium,
                color = Chrome.carbon,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 고른 칸: 기본 앰버보다 한 단 짙게 해서 눌려 그늘진 느낌을 준다 */
private val PRESSED_AMBER = lerp(Chrome.amber, Chrome.navGold, 0.2f)

/**
 * 카세트 키 음영. 흐린 그라데이션 대신 단단한 띠로 각지게 그린다 (디자인 가이드: 깊이는 단단한 베벨로).
 * - 튀어나온 키: 위쪽 밝은 선, 아래쪽 그림자 띠
 * - 눌린 키: 위쪽 두 단 그늘 + 왼쪽 그늘(안으로 꺼진 홈), 아래쪽에 가는 반사광
 */
private fun Modifier.cassetteShading(pressed: Boolean): Modifier = drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    if (pressed) {
        val top1 = 3.dp.toPx()
        val top2 = 1.5.dp.toPx()
        drawRect(Color(0x40000000), size = Size(w, top1))
        drawRect(Color(0x1A000000), topLeft = Offset(0f, top1), size = Size(w, top2))
        drawRect(Color(0x2E000000), topLeft = Offset(0f, top1), size = Size(2.dp.toPx(), h - top1))
        drawRect(Color(0x1A000000), topLeft = Offset(w - 1.5.dp.toPx(), top1), size = Size(1.5.dp.toPx(), h - top1))
        val glint = 1.dp.toPx()
        drawRect(Color(0x40FFFFFF), topLeft = Offset(0f, h - glint), size = Size(w, glint))
    } else {
        drawRect(Color(0xB3FFFFFF), size = Size(w, 1.5.dp.toPx()))
        val shadow = 3.dp.toPx()
        drawRect(Color(0x24000000), topLeft = Offset(0f, h - shadow), size = Size(w, shadow))
    }
}

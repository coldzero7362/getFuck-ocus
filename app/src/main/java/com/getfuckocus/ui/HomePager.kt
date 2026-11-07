package com.getfuckocus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.bevel
import com.getfuckocus.ui.theme.plate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGE_HOME = 0
private const val PAGE_HISTORY = 1
private val PAGE_NAMES = listOf("과제", "기록")

/**
 * 홈과 기록을 옆으로 넘기는 두 쪽짜리 화면. 기록이 홈의 오른쪽에 있어서,
 * 홈에서 왼쪽으로 밀면 기록이 오른쪽에서 들어오고, 기록에서 오른쪽으로 밀면 홈으로 돌아온다.
 * 상단 바의 달력 버튼과 기록 화면의 뒤로 버튼도 같은 페이지 이동을 한다.
 * 아래 가운데의 쪽 표시가 지금 어느 쪽인지 보여 주고, 눌러서 넘길 수도 있다.
 */
@Composable
fun HomePager(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onCapture: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = PAGE_HOME) { PAGE_NAMES.size }
    val scope = rememberCoroutineScope()
    fun goTo(page: Int) = scope.launch { pager.animateScrollToPage(page) }

    // 기록에서 뒤로 가기를 누르면 앱이 닫히지 않고 홈으로 돌아온다
    BackHandler(enabled = pager.currentPage == PAGE_HISTORY) { goTo(PAGE_HOME) }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, beyondViewportPageCount = 1) { page ->
            when (page) {
                PAGE_HISTORY -> HistoryScreen(onBack = { goTo(PAGE_HOME) })
                else -> TaskListScreen(
                    onAdd = onAdd,
                    onEdit = onEdit,
                    onCapture = onCapture,
                    onSettings = onSettings,
                    onHistory = { goTo(PAGE_HISTORY) },
                )
            }
        }
        PageIndicator(
            pager = pager,
            onSelect = { goTo(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        )
    }
}

private val SEGMENT_WIDTH = 64.dp
private val SEGMENT_HEIGHT = 34.dp
private val INSET = 4.dp
private const val PLATE_ALPHA = 0.8f
private const val AMBER_ALPHA = 0.9f
/** 숨어 있을 때 아래로 내려가 있는 거리. 표시 높이 + 아래 여백 + 내비게이션 바보다 크게 */
private val HIDDEN_DROP = 140.dp
/** 다 넘어간 뒤 사라지기 전에 머무는 시간 */
private const val LINGER_MS = 700L

/**
 * 카본 판 위에 쪽 이름을 나란히 두고, 앰버 조각이 손가락을 따라 미끄러지며 지금 쪽을 덮는다.
 * 넘기는 동안에만 아래에서 튀어나와 어느 쪽으로 가고 있는지 보여 준다.
 */
@Composable
private fun PageIndicator(pager: PagerState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // 0(과제) ~ 1(기록) 사이의 연속 위치
    val position = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, (PAGE_NAMES.size - 1).toFloat())
    // 평소에는 화면 아래 밖에 숨어 있다가, 넘기기 시작하면 튀어 올라오고
    // 다 넘어간 뒤 잠깐 머물렀다가 다시 아래로 들어간다
    val moving = pager.isScrollInProgress
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(moving) {
        if (moving) {
            shown = true
        } else if (shown) {
            delay(LINGER_MS)
            shown = false
        }
    }
    val drop by animateDpAsState(
        if (shown) 0.dp else HIDDEN_DROP,
        animationSpec = if (shown) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(durationMillis = 220)
        },
        label = "indicatorDrop",
    )
    // 바탕 판만 비치게 하고 글자는 그대로 둔다. 뒤 화면 내용이 글자와 섞여 읽기 어려워지지 않게
    Box(
        modifier
            .offset(y = drop)
            .plate(Chrome.carbon.copy(alpha = PLATE_ALPHA), ChromeShapes.small)
            .padding(INSET),
    ) {
        Box(
            Modifier
                .offset(x = SEGMENT_WIDTH * position)
                .size(width = SEGMENT_WIDTH, height = SEGMENT_HEIGHT)
                .clip(ChromeShapes.control)
                .background(Chrome.amber.copy(alpha = AMBER_ALPHA))
                .bevel(shadow = Chrome.navGold, shadowWidth = 2.dp)
        )
        Row {
            PAGE_NAMES.forEachIndexed { index, name ->
                // 앰버 조각에 절반 넘게 덮이면 글자를 어둡게 바꿔 읽히게 한다
                val covered = kotlin.math.abs(position - index) < 0.5f
                Box(
                    Modifier
                        .width(SEGMENT_WIDTH)
                        .height(SEGMENT_HEIGHT)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab) {
                            onSelect(index)
                        }
                        .semantics { selected = covered },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (covered) Chrome.carbon else Chrome.sky,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

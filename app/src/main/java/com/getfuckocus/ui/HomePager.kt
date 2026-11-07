package com.getfuckocus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private const val PAGE_HISTORY = 0
private const val PAGE_HOME = 1

/**
 * 홈과 기록을 옆으로 넘기는 두 쪽짜리 화면. 기록이 홈의 왼쪽에 있어서,
 * 홈에서 오른쪽으로 밀면 기록이 나오고 기록에서 왼쪽으로 밀면 홈으로 돌아온다.
 * 상단 바의 달력 버튼과 기록 화면의 뒤로 버튼도 같은 페이지 이동을 한다.
 */
@Composable
fun HomePager(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onCapture: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = PAGE_HOME) { 2 }
    val scope = rememberCoroutineScope()
    fun goTo(page: Int) = scope.launch { pager.animateScrollToPage(page) }

    // 기록에서 뒤로 가기를 누르면 앱이 닫히지 않고 홈으로 돌아온다
    BackHandler(enabled = pager.currentPage == PAGE_HISTORY) { goTo(PAGE_HOME) }

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
}

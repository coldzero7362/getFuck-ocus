package com.getfuckocus.ui

import com.getfuckocus.ui.components.CassetteNameplate
import com.getfuckocus.ui.components.CrtAlert
import com.getfuckocus.ui.components.BankInk
import com.getfuckocus.ui.components.BankKey
import com.getfuckocus.ui.components.KeyBank
import com.getfuckocus.ui.components.TapeCounter
import com.getfuckocus.ui.components.TapeDrums
import com.getfuckocus.ui.components.crtScreen
import com.getfuckocus.ui.theme.SkinState
import com.getfuckocus.ui.theme.phosphorGlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getfuckocus.container
import com.getfuckocus.data.AttemptEntity
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.task.DayResult
import com.getfuckocus.task.History
import com.getfuckocus.task.OccurrenceResult
import com.getfuckocus.task.PassIndex
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.components.FocusStatBox
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.recessed
import com.getfuckocus.ui.theme.DigitStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/** 달력으로 날짜별 결과를 보고, 날짜를 누르면 그날 인증 기록을 본다. 과제별 연속 달성도 보여준다. */
@Composable
fun HistoryScreen() {
    val dao = LocalContext.current.container.taskDao
    val now = rememberNow(60_000)
    val tasks by remember { dao.observeAll() }.collectAsState(initial = emptyList())
    val records by remember { dao.observePassRecords() }.collectAsState(initial = emptyList())
    val passes = remember(records) { PassIndex(records) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(now)) }
    var selected by rememberSaveable { mutableStateOf(now.toLocalDate()) }
    val attempts by remember(selected) { dao.observeAttemptsOn(TaskSchedule.dayKey(selected)) }
        .collectAsState(initial = emptyList())
    val activeTasks = tasks.filter { it.enabled }

    Scaffold(
        topBar = { FocusTopBar("기록", subtitle = "날짜를 누르면 아래에 그날 기록이 나와요") },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CalendarPanel(
                    month = month,
                    onMonthChange = { month = it },
                    selected = selected,
                    onSelect = { selected = it },
                    tasks = activeTasks,
                    now = now,
                    passes = passes,
                )
            }
            item { DayDetailPanel(selected, activeTasks, attempts, now, passes) }
            // 부가 통계는 달력과 그날 기록 아래에 둔다
            item { StreakPanel(activeTasks, now, passes) }
            item { CassetteNameplate() }
        }
    }
}

@Composable
private fun StreakPanel(tasks: List<TaskEntity>, now: ZonedDateTime, passes: PassIndex) {
    FocusPanel(title = "연속 달성", subtitle = "마감 전에 연달아 통과한 횟수예요", icon = Icons.Default.Star) {
        if (tasks.isEmpty()) {
            Text("아직 과제가 없어요", style = MaterialTheme.typography.bodyMedium)
        }
        // 과제마다 판에 파인 줄 하나씩. 앞에 순번을 달아 목록처럼 읽히게 한다
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tasks.forEachIndexed { index, task ->
                val streak = History.streak(task, now, passes)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .recessed(Chrome.platinum)
                        .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "%02d".format(index + 1),
                        style = DigitStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                        color = Chrome.chromeIndigo,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (SkinState.isCassette) {
                        // 카세트 데크의 테이프 카운터처럼 숫자 드럼으로
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "${streak}일 연속" },
                        ) {
                            TapeCounter(streak)
                            Spacer(Modifier.width(6.dp))
                            Text("일 연속", style = MaterialTheme.typography.labelMedium, color = Chrome.chromeIndigo)
                        }
                    } else Pill(
                        "${streak}일 연속",
                        if (streak > 0) Chrome.amber else Chrome.white,
                        Chrome.carbon,
                        icon = Icons.Default.Star,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarPanel(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    tasks: List<TaskEntity>,
    now: ZonedDateTime,
    passes: PassIndex,
) {
    val thisMonth = YearMonth.from(now)
    // 이번 달 스테이지 결과: 지난 회차만 센다 (마감 전인 회차는 빼고)
    val results = remember(month, tasks, passes, now.toLocalDate()) {
        (1..month.lengthOfMonth()).map(month::atDay)
            .filter { !it.isAfter(now.toLocalDate()) }
            .flatMap { date -> tasks.map { History.occurrenceResult(it, date, now, passes) } }
    }
    val passedCount = results.count { it == OccurrenceResult.PASSED }
    val lateCount = results.count { it == OccurrenceResult.LATE }
    val missedCount = results.count { it == OccurrenceResult.MISSED }
    val finished = passedCount + lateCount + missedCount
    FocusPanel(
        title = "${month.year}년 ${month.monthValue}월",
        icon = Icons.Default.DateRange,
        // 카세트 스킨: 연도·달을 테이프 카운터 드럼으로
        cassetteTitle = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TapeCounter(month.year, digits = 4)
                Text(" 년 ", style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
                TapeCounter(month.monthValue, digits = 2)
                Text(" 월", style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
            }
        },
        trailing = {
            if (SkinState.isCassette) {
                // 카세트 스킨: 붙은 납작한 키 두 개
                KeyBank(caption = null) {
                    BankKey(onClick = { onMonthChange(month.minusMonths(1)) }, onClickLabel = "이전 달") {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달", tint = BankInk, modifier = Modifier.size(18.dp))
                    }
                    BankKey(
                        onClick = { onMonthChange(month.plusMonths(1)) },
                        onClickLabel = "다음 달",
                        enabled = month.isBefore(thisMonth),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달", tint = BankInk, modifier = Modifier.size(18.dp))
                    }
                }
                return@FocusPanel
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MonthButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 달", enabled = true) {
                    onMonthChange(month.minusMonths(1))
                }
                MonthButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 달", enabled = month.isBefore(thisMonth)) {
                    onMonthChange(month.plusMonths(1))
                }
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        ) {
            FocusStatBox(
                label = "완료율",
                value = if (finished == 0) "-" else "${passedCount * 100 / finished}%",
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FocusStatBox(
                label = "통과",
                value = "${passedCount + lateCount}회",
                caption = if (lateCount > 0) "늦게 $lateCount" else null,
                icon = Icons.Default.CheckCircle,
                valueColor = Chrome.teal,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FocusStatBox(
                label = "시간 초과",
                value = "${missedCount}회",
                icon = Icons.Default.Close,
                valueColor = Chrome.redInk,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        if (SkinState.isCassette) {
            CrtCalendar(month, selected, onSelect, tasks, now, passes)
        } else {
            ClassicCalendar(month, selected, onSelect, tasks, now, passes)
        }
    }
}

/** 뉴트로 스킨의 달력: 요일 줄, 결과 색으로 칠한 날짜 칸, 범례 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassicCalendar(
    month: YearMonth,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    tasks: List<TaskEntity>,
    now: ZonedDateTime,
    passes: PassIndex,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    TaskSchedule.dayName(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = Chrome.chromeIndigo,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // 월요일부터 시작하는 격자. 앞쪽 빈칸 수 = 1일의 요일 - 1
        val leading = month.atDay(1).dayOfWeek.value - 1
        val cells = leading + month.lengthOfMonth()
        for (week in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val dayOfMonth = week * 7 + col - leading + 1
                    if (dayOfMonth in 1..month.lengthOfMonth()) {
                        val date = month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            result = History.dayResult(tasks, date, now, passes),
                            selected = date == selected,
                            today = date == now.toLocalDate(),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(DayResult.PASSED, DayResult.PARTIAL, DayResult.MISSED).forEach { result ->
                val style = dayStyle(result)
                Pill(style.label, style.container, style.content, icon = style.icon)
            }
            Pill("오늘", Chrome.carbon, Chrome.white)
            Pill("선택한 날", Chrome.white, Chrome.carbon, modifier = Modifier.border(2.dp, Chrome.signal, ChromeShapes.control))
        }
    }
}

/**
 * 카세트 스킨의 달력: 검은 CRT 화면에 형광 글자로 찍은 날짜 격자.
 * 결과는 글자색과 기호로 함께 구분하고, 오늘은 반전(형광 바탕에 검은 글자), 고른 날은 앰버 테로 표시한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CrtCalendar(
    month: YearMonth,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    tasks: List<TaskEntity>,
    now: ZonedDateTime,
    passes: PassIndex,
) {
    Column(
        Modifier.fillMaxWidth().crtScreen().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    TaskSchedule.dayName(day),
                    style = MaterialTheme.typography.labelMedium.copy(shadow = phosphorGlow(Chrome.phosphorDim)),
                    color = Chrome.phosphorDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val leading = month.atDay(1).dayOfWeek.value - 1
        val cells = leading + month.lengthOfMonth()
        for (week in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val dayOfMonth = week * 7 + col - leading + 1
                    if (dayOfMonth in 1..month.lengthOfMonth()) {
                        val date = month.atDay(dayOfMonth)
                        CrtDayCell(
                            date = date,
                            result = History.dayResult(tasks, date, now, passes),
                            selected = date == selected,
                            today = date == now.toLocalDate(),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp)
                .background(Chrome.phosphorDim.copy(alpha = 0.3f))
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(DayResult.PASSED, DayResult.PARTIAL, DayResult.MISSED).forEach { result ->
                val style = dayStyle(result)
                CrtLegend(style.label, crtInk(result), style.icon)
            }
            CrtLegend("오늘", Chrome.phosphor, null, inverse = true)
            CrtLegend("선택한 날", Chrome.phosphorAmber, null, framed = true)
        }
    }
}

/** CRT 화면에서 결과별 글자색 */
private fun crtInk(result: DayResult): Color = when (result) {
    DayResult.PASSED -> Chrome.phosphor
    DayResult.PARTIAL -> Chrome.phosphorAmber
    DayResult.MISSED -> CrtAlert
    DayResult.PENDING -> Chrome.phosphorDim
    DayResult.NONE -> Chrome.phosphorDim.copy(alpha = 0.55f)
    DayResult.FUTURE -> Chrome.phosphorDim.copy(alpha = 0.22f)
}

@Composable
private fun CrtDayCell(
    date: LocalDate,
    result: DayResult,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val style = dayStyle(result)
    val ink = crtInk(result)
    val lit = result == DayResult.PASSED || result == DayResult.PARTIAL || result == DayResult.MISSED
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .clip(shape)
            .background(if (lit) ink.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
            .border(if (selected) 2.dp else 1.dp, if (selected) Chrome.phosphorAmber else ink.copy(alpha = 0.28f), shape)
            .clickable(onClickLabel = "${date.monthValue}월 ${date.dayOfMonth}일 기록 보기", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${date.dayOfMonth}",
                style = DigitStyle.copy(fontSize = 15.sp, shadow = if (today) null else phosphorGlow(ink)),
                color = if (today) Chrome.screen else ink,
                textAlign = TextAlign.Center,
                modifier = if (today) Modifier.background(Chrome.phosphor).padding(horizontal = 4.dp) else Modifier,
            )
            val icon = style.icon
            if (icon != null) {
                Icon(icon, contentDescription = style.label, tint = ink, modifier = Modifier.size(12.dp))
            } else {
                Spacer(Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun CrtLegend(label: String, ink: Color, icon: ImageVector?, inverse: Boolean = false, framed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            icon != null -> Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(12.dp))
            inverse -> Box(Modifier.size(12.dp).background(ink))
            framed -> Box(Modifier.size(12.dp).border(2.dp, ink))
        }
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(shadow = phosphorGlow(ink)), color = ink)
    }
}

private data class DayStyle(val label: String, val container: Color, val content: Color, val icon: ImageVector?)

/** 결과는 색과 아이콘을 함께 쓴다 (색만으로 구분하지 않는다) */
private fun dayStyle(result: DayResult): DayStyle = when (result) {
    DayResult.PASSED -> DayStyle("통과", Chrome.ice, Chrome.teal, Icons.Default.CheckCircle)
    DayResult.PARTIAL -> DayStyle("일부", Chrome.warningSoft, Chrome.carbon, Icons.Default.Warning)
    DayResult.MISSED -> DayStyle("시간 초과", Chrome.redSoft, Chrome.redInk, Icons.Default.Close)
    DayResult.PENDING -> DayStyle("마감 전", Chrome.sky, Chrome.carbon, PixelIcons.Hourglass)
    DayResult.NONE -> DayStyle("없음", Chrome.white, Chrome.hairline, null)
    DayResult.FUTURE -> DayStyle("", Chrome.white.copy(alpha = 0.45f), Chrome.hairline, null)
}

@Composable
private fun DayCell(
    date: LocalDate,
    result: DayResult,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val style = dayStyle(result)
    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .clip(ChromeShapes.control)
            .background(style.container)
            .then(if (selected) Modifier.border(3.dp, Chrome.signal, ChromeShapes.control) else Modifier)
            .clickable(onClickLabel = "${date.monthValue}월 ${date.dayOfMonth}일 기록 보기", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    today -> Chrome.white
                    result == DayResult.FUTURE -> Chrome.hairline
                    else -> Chrome.carbon
                },
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = if (today) Modifier
                    .clip(ChromeShapes.control)
                    .background(Chrome.carbon)
                    .padding(horizontal = 5.dp) else Modifier,
            )
            val icon = style.icon
            if (icon != null) {
                Icon(icon, contentDescription = style.label, tint = style.content, modifier = Modifier.size(14.dp))
            } else {
                Spacer(Modifier.size(14.dp))
            }
        }
    }
}

/** 달 넘김 버튼. 띠 위에서도 잘 보이게 흰 판에 테두리를 둔다. */
@Composable
private fun MonthButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .clip(ChromeShapes.small)
            .background(Chrome.white)
            .border(1.dp, Chrome.hairline, ChromeShapes.small)
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Chrome.carbon, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun DayDetailPanel(
    date: LocalDate,
    tasks: List<TaskEntity>,
    attempts: List<AttemptEntity>,
    now: ZonedDateTime,
    passes: PassIndex,
) {
    val occurrences = tasks.mapNotNull { task ->
        History.occurrenceResult(task, date, now, passes).takeIf { it != OccurrenceResult.NONE }?.let { task to it }
    }
    val passedN = occurrences.count { it.second == OccurrenceResult.PASSED || it.second == OccurrenceResult.LATE }
    val missedN = occurrences.count { it.second == OccurrenceResult.MISSED }
    FocusPanel(
        title = "${date.monthValue}월 ${date.dayOfMonth}일 ${TaskSchedule.dayName(date.dayOfWeek)}요일",
        subtitle = if (occurrences.isEmpty()) null else "과제 ${occurrences.size}개 · 통과 $passedN · 시간 초과 $missedN",
        icon = PixelIcons.Flag,
        // 카세트 스킨: 날짜를 드럼 카운터로 (월 2칸 · 일 2칸 · 요일 1칸)
        cassetteTitle = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TapeCounter(date.monthValue, digits = 2)
                Text(" 월 ", style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
                TapeCounter(date.dayOfMonth, digits = 2)
                Text(" 일 ", style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
                TapeDrums(TaskSchedule.dayName(date.dayOfWeek))
                Text(" 요일", style = MaterialTheme.typography.titleSmall, color = Chrome.carbon)
            }
        },
    ) {
        if (occurrences.isEmpty()) {
            Text("이날은 과제가 없었어요", style = MaterialTheme.typography.bodyMedium, color = Chrome.chromeIndigo)
        }
        if (SkinState.isCassette) {
            occurrences.forEach { (task, result) -> CrtOccurrenceLog(task, result, attempts.filter { it.taskId == task.id }) }
            return@FocusPanel
        }
        occurrences.forEach { (task, result) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    // 흰 칸을 얹지 않고 패널 판에 살짝 파인 칸으로 둔다
                    .recessed(Chrome.platinum)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ResultPill(result)
                }
                val taskAttempts = attempts.filter { it.taskId == task.id }
                if (taskAttempts.isEmpty()) {
                    Text("인증 시도가 없어요", style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
                }
                taskAttempts.forEach { AttemptRow(it) }
            }
        }
    }
}

/**
 * 카세트 스킨의 그날 기록: 과제 하나를 단말기 출력 한 덩어리로. 첫 줄은 "> 과제 이름 [결과]",
 * 아래로 인증 시도가 시각순 로그 줄로 찍히고 실패 이유는 들여쓴 줄로 붙는다.
 */
@Composable
private fun CrtOccurrenceLog(task: TaskEntity, result: OccurrenceResult, attempts: List<AttemptEntity>) {
    val (tag, tagInk) = when (result) {
        OccurrenceResult.PASSED -> "통과" to Chrome.phosphor
        OccurrenceResult.LATE -> "늦게 통과" to Chrome.phosphorAmber
        OccurrenceResult.MISSED -> "시간 초과" to CrtAlert
        OccurrenceResult.PENDING -> "마감 전" to Chrome.phosphorDim
        OccurrenceResult.NONE -> "" to Chrome.phosphorDim
    }
    val head = Chrome.phosphor
    Column(
        Modifier.fillMaxWidth().crtScreen(6.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "> ${task.title}",
                style = DigitStyle.copy(fontSize = 16.sp, shadow = phosphorGlow(head)),
                color = head,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text("[$tag]", style = DigitStyle.copy(fontSize = 14.sp, shadow = phosphorGlow(tagInk)), color = tagInk)
        }
        if (attempts.isEmpty()) {
            Text("  인증 시도 없음", style = DigitStyle.copy(fontSize = 13.sp), color = Chrome.phosphorDim.copy(alpha = 0.75f))
        }
        attempts.forEach { CrtAttemptLine(it) }
    }
}

@Composable
private fun CrtAttemptLine(attempt: AttemptEntity) {
    val time = Instant.ofEpochMilli(attempt.capturedAt).atZone(ZoneId.systemDefault())
    val photo = attempt.method == VerifyMethod.PHOTO
    val ink = if (attempt.passed) Chrome.phosphor else CrtAlert
    Row(verticalAlignment = Alignment.Top) {
        if (photo) {
            AttemptThumbnail(attempt.photoPath, size = 40.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${TaskSchedule.formatMinute(time.hour * 60 + time.minute)}  ${attempt.method.title}  " +
                    if (attempt.passed) "통과" else "실패",
                style = DigitStyle.copy(fontSize = 14.sp, shadow = phosphorGlow(ink)),
                color = ink,
            )
            attempt.reasons.lineSequence().filter { it.isNotBlank() }.take(3).forEach {
                Text(
                    "  └ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Chrome.phosphorDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ResultPill(result: OccurrenceResult) {
    when (result) {
        OccurrenceResult.PASSED -> Pill("통과", Chrome.ice, Chrome.teal, icon = Icons.Default.CheckCircle)
        OccurrenceResult.LATE -> Pill("늦게 통과", Chrome.amber, Chrome.carbon, icon = Icons.Default.Warning)
        OccurrenceResult.MISSED -> Pill("시간 초과", Chrome.redSoft, Chrome.redInk, icon = Icons.Default.Close)
        OccurrenceResult.PENDING -> Pill("마감 전", Chrome.sky, Chrome.carbon, icon = PixelIcons.Hourglass)
        OccurrenceResult.NONE -> Unit
    }
}

@Composable
private fun AttemptRow(attempt: AttemptEntity) {
    val time = Instant.ofEpochMilli(attempt.capturedAt).atZone(ZoneId.systemDefault())
    val photo = attempt.method == VerifyMethod.PHOTO
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (photo) AttemptThumbnail(attempt.photoPath) else MethodMark(attempt.method, attempt.passed)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${TaskSchedule.formatMinute(time.hour * 60 + time.minute)} · " +
                    (if (photo) "" else "${attempt.method.title} ") + (if (attempt.passed) "통과" else "실패"),
                style = MaterialTheme.typography.labelLarge,
                color = if (attempt.passed) Chrome.teal else Chrome.redInk,
            )
            // 실패 이유는 줄마다 하나씩 (최대 3개)
            attempt.reasons.lineSequence().filter { it.isNotBlank() }.take(3).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 카메라 없는 인증 기록: 사진 대신 같은 크기의 칸에 방식 아이콘 (탭 체크는 체크 표시) */
@Composable
private fun MethodMark(method: VerifyMethod, passed: Boolean) {
    Box(
        Modifier
            .size(52.dp)
            .clip(ChromeShapes.small)
            .background(if (passed) Chrome.ice else Chrome.redSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            when {
                !passed -> Icons.Default.Close
                method == VerifyMethod.TAP -> Icons.Default.CheckCircle
                else -> method.icon
            },
            contentDescription = "${method.title} ${if (passed) "통과" else "실패"}",
            tint = if (passed) Chrome.teal else Chrome.redInk,
            modifier = Modifier.size(28.dp),
        )
    }
}

/** 보존 기간이 지나 사진이 지워졌으면 빈 칸으로 둔다. */
@Composable
private fun AttemptThumbnail(path: String, size: Dp = 52.dp) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = if (path.isEmpty()) null else withContext(Dispatchers.IO) {
            runCatching {
                File(path).takeIf { it.exists() }?.let { file ->
                    BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = 8 })?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    Box(
        Modifier
            .size(size)
            .clip(ChromeShapes.small)
            .background(Chrome.carbon),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, contentDescription = "인증 사진", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text("사진\n없음", style = MaterialTheme.typography.labelSmall, color = Chrome.sky, textAlign = TextAlign.Center)
        }
    }
}

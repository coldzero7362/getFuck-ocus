package com.getfuckocus.ui

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
import androidx.compose.material3.Text
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
fun HistoryScreen(onBack: () -> Unit) {
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
        topBar = { FocusTopBar("기록", subtitle = "날짜를 누르면 아래에 그날 기록이 나와요", onBack = onBack) },
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
        }
    }
}

@Composable
private fun StreakPanel(tasks: List<TaskEntity>, now: ZonedDateTime, passes: PassIndex) {
    FocusPanel(title = "연속 달성", subtitle = "마감 전에 통과한 회차를 이어서 센 수예요", icon = Icons.Default.Star) {
        if (tasks.isEmpty()) {
            Text("아직 과제가 없어요", style = MaterialTheme.typography.bodyMedium)
        }
        tasks.forEach { task ->
            val streak = History.streak(task, now, passes)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Pill(
                    "${streak}일 연속",
                    if (streak > 0) Chrome.amber else Chrome.white,
                    Chrome.carbon,
                    icon = Icons.Default.Star,
                )
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
        trailing = {
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
                label = "놓침",
                value = "${missedCount}회",
                icon = Icons.Default.Close,
                valueColor = Chrome.redInk,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
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

private data class DayStyle(val label: String, val container: Color, val content: Color, val icon: ImageVector?)

/** 결과는 색과 아이콘을 함께 쓴다 (색만으로 구분하지 않는다) */
private fun dayStyle(result: DayResult): DayStyle = when (result) {
    DayResult.PASSED -> DayStyle("통과", Chrome.ice, Chrome.teal, Icons.Default.CheckCircle)
    DayResult.PARTIAL -> DayStyle("일부", Chrome.warningSoft, Chrome.carbon, Icons.Default.Warning)
    DayResult.MISSED -> DayStyle("놓침", Chrome.redSoft, Chrome.redInk, Icons.Default.Close)
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
        subtitle = if (occurrences.isEmpty()) null else "과제 ${occurrences.size}개 · 통과 $passedN · 놓침 $missedN",
        icon = PixelIcons.Flag,
    ) {
        if (occurrences.isEmpty()) {
            Text("이날은 과제가 없었어요", style = MaterialTheme.typography.bodyMedium, color = Chrome.chromeIndigo)
        }
        occurrences.forEach { (task, result) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(ChromeShapes.small)
                    .background(Chrome.white)
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

@Composable
private fun ResultPill(result: OccurrenceResult) {
    when (result) {
        OccurrenceResult.PASSED -> Pill("통과", Chrome.ice, Chrome.teal, icon = Icons.Default.CheckCircle)
        OccurrenceResult.LATE -> Pill("늦게 통과", Chrome.amber, Chrome.carbon, icon = Icons.Default.Warning)
        OccurrenceResult.MISSED -> Pill("놓침", Chrome.redSoft, Chrome.redInk, icon = Icons.Default.Close)
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
private fun AttemptThumbnail(path: String) {
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
            .size(52.dp)
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

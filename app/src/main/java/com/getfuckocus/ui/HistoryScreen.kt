package com.getfuckocus.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import com.getfuckocus.task.DayResult
import com.getfuckocus.task.History
import com.getfuckocus.task.OccurrenceResult
import com.getfuckocus.task.PassIndex
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusPanel
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
        topBar = { FocusTopBar("기록", subtitle = "날짜를 누르면 그날 인증을 볼 수 있어요", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StreakPanel(activeTasks, now, passes) }
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
        }
    }
}

@Composable
private fun StreakPanel(tasks: List<TaskEntity>, now: ZonedDateTime, passes: PassIndex) {
    FocusPanel(title = "연속 달성", subtitle = "마감 전에 통과한 회차를 이어서 센 수예요") {
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
    FocusPanel(
        title = "${month.year}년 ${month.monthValue}월",
        trailing = {
            Row {
                FocusIconButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    onClick = { onMonthChange(month.minusMonths(1)) },
                )
                FocusIconButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    onClick = { onMonthChange(month.plusMonths(1)) },
                    enabled = month.isBefore(thisMonth),
                )
            }
        },
    ) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DayResult.PASSED, DayResult.PARTIAL, DayResult.MISSED).forEach { result ->
                val style = dayStyle(result)
                Pill("${style.mark} ${style.label}", style.container, style.content)
            }
        }
    }
}

private data class DayStyle(val mark: String, val label: String, val container: Color, val content: Color)

private fun dayStyle(result: DayResult): DayStyle = when (result) {
    DayResult.PASSED -> DayStyle("✓", "통과", Chrome.ice, Chrome.teal)
    DayResult.PARTIAL -> DayStyle("△", "일부", Chrome.amber, Chrome.carbon)
    DayResult.MISSED -> DayStyle("✗", "놓침", Color(0xFFFFD6D2), Color(0xFF7A0009))
    DayResult.PENDING -> DayStyle("…", "마감 전", Chrome.sky, Chrome.carbon)
    DayResult.NONE -> DayStyle("", "없음", Chrome.white, Chrome.hairline)
    DayResult.FUTURE -> DayStyle("", "", Chrome.white.copy(alpha = 0.5f), Chrome.hairline)
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
            .aspectRatio(1f)
            .clip(ChromeShapes.control)
            .background(style.container)
            .then(
                when {
                    selected -> Modifier.border(2.dp, Chrome.carbon, ChromeShapes.control)
                    today -> Modifier.border(2.dp, Chrome.chromeIndigo, ChromeShapes.control)
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = if (result == DayResult.FUTURE) Chrome.hairline else Chrome.carbon,
                fontWeight = if (today) FontWeight.Black else FontWeight.Bold,
            )
            if (style.mark.isNotEmpty()) {
                Text(style.mark, style = MaterialTheme.typography.labelSmall, color = style.content)
            }
        }
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
    FocusPanel(title = "${date.monthValue}월 ${date.dayOfMonth}일 ${TaskSchedule.dayName(date.dayOfWeek)}요일") {
        if (occurrences.isEmpty()) {
            Text("이날은 과제가 없었어요", style = MaterialTheme.typography.bodyMedium)
        }
        occurrences.forEach { (task, result) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResultPill(result)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
        OccurrenceResult.MISSED -> Pill("놓침", Color(0xFFFFD6D2), Color(0xFF7A0009), icon = Icons.Default.Close)
        OccurrenceResult.PENDING -> Pill("마감 전", Chrome.sky, Chrome.carbon)
        OccurrenceResult.NONE -> Unit
    }
}

@Composable
private fun AttemptRow(attempt: AttemptEntity) {
    val time = Instant.ofEpochMilli(attempt.capturedAt).atZone(ZoneId.systemDefault())
    Row(verticalAlignment = Alignment.CenterVertically) {
        AttemptThumbnail(attempt.photoPath)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${TaskSchedule.formatMinute(time.hour * 60 + time.minute)} · ${if (attempt.passed) "통과" else "실패"}",
                style = MaterialTheme.typography.labelLarge,
                color = if (attempt.passed) Chrome.teal else Color(0xFF7A0009),
            )
            attempt.reasons.lineSequence().firstOrNull { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
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

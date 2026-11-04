package com.getfuckocus.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.getfuckocus.container
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.data.requiredLabels
import com.getfuckocus.lock.BlockerServiceStatus
import com.getfuckocus.lock.LockActivity
import com.getfuckocus.lock.LockState
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusDialog
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusPlayButton
import com.getfuckocus.ui.components.FocusProgressBar
import com.getfuckocus.ui.components.FocusTopBar
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

@Composable
fun TaskListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onCapture: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.container
    val dao = container.taskDao
    val now = rememberNow()

    val tasks by remember { dao.observeAll() }.collectAsState(initial = emptyList())
    val today = now.toLocalDate()
    val recentKeys = remember(today) { TaskSchedule.candidateDates(now).map(TaskSchedule::dayKey) }
    val passed by remember(recentKeys) { dao.observePassed(recentKeys) }.collectAsState(initial = emptyList())
    val lock by container.lockController.state.collectAsState()
    val serviceEnabled = rememberBlockerServiceEnabled()

    val passedSet = remember(passed) { passed.toSet() }
    val entries = tasks.map { it to taskStatus(it, now, passedSet) }
    val scope = rememberCoroutineScope()
    var confirmStart by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = "오늘의 과제",
                subtitle = now.format(DATE_FORMAT),
                actions = { FocusIconButton(Icons.Default.Settings, contentDescription = "설정", onClick = onSettings) },
            )
        },
        floatingActionButton = {
            FocusButton(
                "과제 추가",
                onClick = onAdd,
                icon = Icons.Default.Add,
                height = 56.dp,
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(
                    now = now,
                    lock = lock,
                    entries = entries,
                    autoReleaseRemaining = lock?.let { container.lockController.autoReleaseRemaining(it) },
                    onOpenLock = { context.startActivity(LockActivity.intent(context)) },
                )
            }
            if (!serviceEnabled) {
                item { ServiceOffBanner(onEnable = { BlockerServiceStatus.openSettings(context) }) }
            }
            if (tasks.isEmpty()) {
                item { EmptyState(onAdd) }
            }
            items(entries, key = { it.first.id }) { (task, status) ->
                TaskCard(
                    task = task,
                    status = status,
                    now = now,
                    lockedByThis = lock?.taskId == task.id,
                    onCapture = { onCapture(task.id) },
                    onEdit = { onEdit(task.id) },
                    onStartNow = { confirmStart = task },
                )
            }
        }
    }

    confirmStart?.let { task ->
        val deadline = now.plusMinutes(task.durationMinutes.toLong())
        FocusDialog(
            title = "지금 바로 시작할까요?",
            onDismiss = { confirmStart = null },
            confirmText = "시작",
            onConfirm = {
                confirmStart = null
                scope.launch { dao.markStarted(task.id, System.currentTimeMillis()) }
            },
        ) {
            Text(
                "'${task.title}'을(를) 지금 시작하면 ${TaskSchedule.formatMinute(deadline.hour * 60 + deadline.minute)}까지 " +
                    "인증해야 해요. 못 하면 잠겨요. 오늘 정해진 시작 시각은 이번 한 번만 바뀌어요.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    now: ZonedDateTime,
    lock: LockState?,
    entries: List<Pair<TaskEntity, TaskStatus>>,
    autoReleaseRemaining: Long?,
    onOpenLock: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val inProgress = entries.mapNotNull { (t, s) -> (s as? TaskStatus.InProgress)?.let { t to it } }
        .minByOrNull { it.second.deadline }
    val upcoming = entries.mapNotNull { (t, s) -> (s as? TaskStatus.Upcoming)?.let { t to it } }
        .minByOrNull { it.second.startsAt }

    val message = when {
        lock != null -> "'${lock.taskTitle}' 마감을 놓쳐서 잠겨 있어요"
        inProgress != null ->
            "'${inProgress.first.title}' 진행 중 · 마감까지 " +
                formatMinutesLeft(Duration.between(now, inProgress.second.deadline).toMillis())
        upcoming != null ->
            "다음 과제 '${upcoming.first.title}' · " +
                "${formatMinutesLeft(Duration.between(now, upcoming.second.startsAt).toMillis())} 뒤 시작"
        entries.isEmpty() -> "과제를 추가해서 시작해 보세요"
        entries.all { it.second == TaskStatus.Done || it.second == TaskStatus.Disabled } -> "오늘 과제를 모두 끝냈어요"
        else -> "오늘 남은 과제가 없어요"
    }

    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        color = if (lock != null) colors.errorContainer else colors.primaryContainer,
        contentColor = if (lock != null) colors.onErrorContainer else colors.onPrimaryContainer,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            TaskSchedule.formatMinute(now.hour * 60 + now.minute),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (lock != null) {
            autoReleaseRemaining?.let {
                Text("자동 해제까지 ${formatRemaining(it)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.size(4.dp))
            FocusButton("잠금 화면 열기", onClick = onOpenLock, style = FocusButtonStyle.Danger, icon = Icons.Default.Lock)
        }
    }
}

@Composable
private fun ServiceOffBanner(onEnable: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "잠금 서비스가 꺼져 있어 마감을 놓쳐도 잠기지 않아요",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            FocusButton(
                "켜기", onClick = onEnable,
                style = FocusButtonStyle.Ghost,
                height = 40.dp,
                contentColor = colors.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("아직 과제가 없어요", style = MaterialTheme.typography.titleMedium)
        Text(
            "시작 시각과 인증 조건을 정해 첫 과제를 만들어 보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        FocusButton("첫 과제 만들기", onClick = onAdd)
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    status: TaskStatus,
    now: ZonedDateTime,
    lockedByThis: Boolean,
    onCapture: () -> Unit,
    onEdit: () -> Unit,
    onStartNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(status)
            Spacer(Modifier.weight(1f))
            FocusIconButton(Icons.Default.Edit, contentDescription = "편집", onClick = onEdit, tint = colors.onSurfaceVariant)
        }
        Text(task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            TaskSchedule.describeWindow(task),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        ConditionPills(task)

        when (status) {
            is TaskStatus.InProgress -> {
                FocusProgressBar(status.progress)
                Text(
                    "마감까지 ${formatMinutesLeft(Duration.between(now, status.deadline).toMillis())}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondary,
                )
                FocusButton("촬영해서 인증", onClick = onCapture, modifier = Modifier.fillMaxWidth())
            }
            is TaskStatus.Upcoming -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${formatMinutesLeft(Duration.between(now, status.startsAt).toMillis())} 뒤 시작",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        "지금 바로 시작할 수도 있어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                FocusPlayButton(onClick = onStartNow, contentDescription = "${task.title} 바로 시작")
            }
            TaskStatus.Missed -> if (lockedByThis) {
                Text("지금 인증하면 잠금이 풀려요", style = MaterialTheme.typography.labelLarge, color = colors.error)
                FocusButton(
                    "지금 인증하기", onClick = onCapture,
                    modifier = Modifier.fillMaxWidth(),
                    style = FocusButtonStyle.Danger,
                )
            }
            TaskStatus.Done, TaskStatus.Disabled -> Unit
        }
    }
}

private data class BadgeStyle(val text: String, val icon: ImageVector, val container: Color, val content: Color)

@Composable
private fun StatusBadge(status: TaskStatus) {
    val colors = MaterialTheme.colorScheme
    val style = when (status) {
        is TaskStatus.Upcoming -> BadgeStyle("시작 전", Icons.Default.DateRange, colors.surfaceVariant, colors.onSurfaceVariant)
        is TaskStatus.InProgress -> BadgeStyle("진행 중", Icons.Default.PlayArrow, colors.secondaryContainer, colors.onSecondaryContainer)
        TaskStatus.Done -> BadgeStyle("완료", Icons.Default.CheckCircle, colors.tertiaryContainer, colors.onTertiaryContainer)
        TaskStatus.Missed -> BadgeStyle("놓침", Icons.Default.Warning, colors.errorContainer, colors.onErrorContainer)
        TaskStatus.Disabled -> BadgeStyle("꺼짐", Icons.Default.Close, colors.surfaceVariant, colors.onSurfaceVariant)
    }
    Pill(style.text, style.container, style.content, icon = style.icon)
}

@Composable
private fun ConditionPills(task: TaskEntity) {
    val labels = task.requiredLabels()
    if (!task.hasGeoFence() && labels.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (task.hasGeoFence()) {
            Pill(
                "${task.geoRadiusMeters!!.toInt()}m 이내",
                colors.surfaceContainerHigh,
                colors.onSurface,
                icon = Icons.Default.LocationOn,
            )
        }
        if (labels.isNotEmpty()) {
            Pill(labels.joinToString(" · "), colors.surfaceContainerHigh, colors.onSurface, icon = Icons.Default.Search)
        }
    }
}

/** 설정에서 돌아올 때마다 다시 확인한다. */
@Composable
fun rememberBlockerServiceEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(BlockerServiceStatus.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        enabled = BlockerServiceStatus.isEnabled(context)
        onPauseOrDispose { }
    }
    return enabled
}

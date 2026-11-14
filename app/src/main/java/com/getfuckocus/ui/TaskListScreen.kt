package com.getfuckocus.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
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
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.getfuckocus.container
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.fenceRadiusMeters
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.requiredLabels
import com.getfuckocus.hasPermission
import com.getfuckocus.lock.BlockerServiceStatus
import com.getfuckocus.lock.LockActivity
import com.getfuckocus.lock.LockState
import com.getfuckocus.task.History
import com.getfuckocus.task.PassIndex
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusDialog
import com.getfuckocus.ui.components.FocusEmptyState
import com.getfuckocus.ui.components.FocusFab
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.FocusPlayButton
import com.getfuckocus.ui.components.FocusProgressBar
import com.getfuckocus.ui.components.FocusSectionLabel
import com.getfuckocus.ui.components.FocusStatBox
import com.getfuckocus.ui.components.FocusTab
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.Dimens
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.theme.TaskIcon
import com.getfuckocus.ui.components.TaskIconMark
import com.getfuckocus.ui.components.TaskAchievementStrip
import com.getfuckocus.ui.theme.bevel
import com.getfuckocus.ui.theme.circleBevel
import com.getfuckocus.verify.Verdict
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
    onCheckScreen: (Long) -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
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
    // 연속 달성은 기록이 바뀌거나 날짜가 바뀔 때만 다시 센다 (매초 다시 셀 필요가 없다)
    val records by remember { dao.observePassRecords() }.collectAsState(initial = emptyList())
    val streaks = remember(records, tasks, today) {
        val passes = PassIndex(records)
        tasks.associate { it.id to History.streak(it, now, passes) }
    }
    val achievementRows by remember { container.achievementDao.observeAll() }.collectAsState(initial = emptyList())
    val achievements = remember(achievementRows) {
        achievementRows.groupBy({ it.taskId }, { it.milestone }).mapValues { it.value.toSet() }
    }
    val scope = rememberCoroutineScope()
    var confirmStart by remember { mutableStateOf<TaskEntity?>(null) }

    // 탭·위치 체크는 홈에서 바로 한다. 통과하면 카드가 곧바로 "완료"로 바뀌므로 따로 알리지 않고, 실패할 때만 이유를 보여 준다
    var checkFailure by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var waitingForLocation by remember { mutableStateOf<TaskEntity?>(null) }
    fun runCheckIn(task: TaskEntity) = scope.launch {
        val result = container.verificationFlow.checkIn(task)
        (result.verdict as? Verdict.Fail)?.let { checkFailure = task.title to it.reasons }
    }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        waitingForLocation?.let { task ->
            if (granted) runCheckIn(task) else checkFailure = task.title to listOf("위치 권한이 없어 장소를 확인할 수 없어요")
        }
        waitingForLocation = null
    }
    /** 인증 방식에 맞게: 사진은 촬영 화면, NFC는 태그 화면, 탭·위치는 그 자리에서 체크 */
    fun verify(task: TaskEntity) {
        when (task.verifyMethod) {
            VerifyMethod.PHOTO -> onCapture(task.id)
            VerifyMethod.NFC -> onCheckScreen(task.id)
            VerifyMethod.LOCATION ->
                if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    waitingForLocation = task
                    locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    runCheckIn(task)
                }
            VerifyMethod.TAP -> runCheckIn(task)
        }
    }

    // 맨 위 큰 카드에 올릴 과제: 잠금 > 진행 중(마감 빠른 순) > 다음 시작
    val inProgress = entries.filter { it.second is TaskStatus.InProgress }
        .minByOrNull { (it.second as TaskStatus.InProgress).deadline }
    val upcoming = entries.filter { it.second is TaskStatus.Upcoming }
        .minByOrNull { (it.second as TaskStatus.Upcoming).startsAt }
    val currentLock = lock
    val focus = if (currentLock != null) null else inProgress ?: upcoming
    val heroTaskId = currentLock?.taskId ?: focus?.first?.id
    val others = entries.filter { it.first.id != heroTaskId }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = "오늘의 과제",
                subtitle = now.format(DATE_FORMAT),
                subtitleTrailing = TaskSchedule.formatMinute(now.hour * 60 + now.minute),
                actions = {
                    FocusIconButton(Icons.Default.DateRange, contentDescription = "기록", onClick = onHistory)
                    FocusIconButton(Icons.Default.Settings, contentDescription = "설정", onClick = onSettings)
                },
            )
        },
        floatingActionButton = {
            FocusFab(Icons.Default.Add, contentDescription = "과제 추가", onClick = onAdd)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.screenPadding,
                top = Dimens.screenPadding,
                end = Dimens.screenPadding,
                bottom = Dimens.fabClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            // 큰 경고 배너 대신 한 줄 상태 안내
            if (!serviceEnabled) {
                item {
                    FocusNotice(
                        title = "잠금 서비스 꺼짐",
                        text = "마감을 놓쳐도 앱이 잠기지 않아요",
                        tone = Tone.Warning,
                        icon = PixelIcons.Shield,
                        actionLabel = "켜기",
                        onAction = { BlockerServiceStatus.openSettings(context) },
                    )
                }
            }
            when {
                currentLock != null -> item {
                    val lockTask = tasks.firstOrNull { it.id == currentLock.taskId }
                    LockHeroCard(
                        lock = currentLock,
                        method = lockTask?.verifyMethod ?: VerifyMethod.PHOTO,
                        autoReleaseRemaining = container.lockController.autoReleaseRemaining(currentLock),
                        onVerifyNow = { lockTask?.let(::verify) ?: onCapture(currentLock.taskId) },
                        onOpenLock = { context.startActivity(LockActivity.intent(context)) },
                    )
                }
                focus != null -> item {
                    val (task, status) = focus
                    HeroCard(
                        task = task,
                        status = status,
                        now = now,
                        streak = streaks[task.id] ?: 0,
                        achievements = achievements[task.id].orEmpty(),
                        onEdit = { onEdit(task.id) },
                        onStartNow = { confirmStart = task },
                        onVerify = { verify(task) },
                    )
                }
                tasks.isNotEmpty() -> item { DaySummaryCard(entries) }
            }
            if (tasks.isEmpty()) {
                item {
                    FocusEmptyState(
                        title = "아직 과제가 없어요",
                        body = "시작 시각과 인증 방법을 정해 첫 과제를 만들어 보세요",
                        icon = PixelIcons.Flag,
                        actionLabel = "첫 과제 만들기",
                        onAction = onAdd,
                    )
                }
            }
            if (others.isNotEmpty()) {
                item {
                    FocusSectionLabel(
                        if (heroTaskId != null) "다른 과제" else "오늘의 과제",
                        count = others.size,
                    )
                }
            }
            items(others, key = { it.first.id }) { (task, status) ->
                TaskCard(
                    task = task,
                    status = status,
                    now = now,
                    lockedByThis = currentLock?.taskId == task.id,
                    streak = streaks[task.id] ?: 0,
                    achievements = achievements[task.id].orEmpty(),
                    onVerify = { verify(task) },
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
                    when (task.missAction) {
                        MissAction.LOCK -> "인증해야 해요. 못 하면 잠겨요. "
                        MissAction.NAG -> "인증해야 해요. 못 하면 억제 화면이 떠요. "
                        MissAction.NOTIFY -> "체크하면 돼요. "
                    } +
                    "오늘 정해진 시작 시각은 이번 한 번만 바뀌어요.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    checkFailure?.let { (title, reasons) ->
        FocusDialog(
            title = "'$title' 체크하지 못했어요",
            onDismiss = { checkFailure = null },
            confirmText = "확인",
            onConfirm = { checkFailure = null },
        ) {
            reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/**
 * 맨 위 큰 카드: 지금 가장 먼저 할 과제 하나. 실행 가능한 시간과 남은 시간을 나눠 보여 주고,
 * 주 버튼 하나로 바로 다음 행동(시작, 촬영, 체크)을 하게 한다.
 */
@Composable
private fun HeroCard(
    task: TaskEntity,
    status: TaskStatus,
    now: ZonedDateTime,
    streak: Int,
    achievements: Set<Int>,
    onEdit: () -> Unit,
    onStartNow: () -> Unit,
    onVerify: () -> Unit,
) {
    val method = task.verifyMethod
    val inProgress = status as? TaskStatus.InProgress
    val start = TaskSchedule.formatMinute(task.windowStartMinute)
    val end = TaskSchedule.formatMinute((task.windowStartMinute + task.durationMinutes) % (24 * 60))

    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        color = Chrome.lavender,
        onClick = onEdit,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusTab(
                if (inProgress != null) "진행 중" else "다음 과제",
                icon = if (inProgress != null) Icons.Default.PlayArrow else PixelIcons.Flag,
            )
            Spacer(Modifier.weight(1f))
        }
        Column(
            Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    TaskSchedule.describeDays(task.repeatDays) + " · " + TaskSchedule.formatDuration(task.durationMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Chrome.chromeIndigo,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                val (label, value) = when {
                    inProgress != null ->
                        "마감까지" to formatMinutesLeft(Duration.between(now, inProgress.deadline).toMillis())
                    status is TaskStatus.Upcoming && status.startsAt.toLocalDate() == now.toLocalDate() ->
                        "시작까지" to formatMinutesLeft(Duration.between(now, status.startsAt).toMillis())
                    status is TaskStatus.Upcoming ->
                        "다음 시작" to formatStartsIn(now, status.startsAt).removeSuffix(" 시작")
                    else -> "상태" to "-"
                }
                FocusStatBox(
                    label = label,
                    value = value,
                    icon = PixelIcons.Hourglass,
                    large = true,
                    valueColor = if (inProgress != null) Chrome.redInk else Chrome.carbon,
                    modifier = Modifier.weight(1.15f).fillMaxHeight(),
                    surface = Chrome.lavender,
                )
                FocusStatBox(
                    label = "실행 가능",
                    value = start,
                    caption = "~ $end 마감",
                    icon = PixelIcons.Clock,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    surface = Chrome.lavender,
                )
            }
            inProgress?.let { FocusProgressBar(it.progress) }
            ConditionPills(task)
            TaskAchievementStrip(TaskIcon.fromKey(task.iconKey), achievements, streak)
            when {
                inProgress != null -> FocusButton(
                    method.actionLabel, onClick = onVerify,
                    modifier = Modifier.fillMaxWidth(), icon = method.actionIcon, height = 56.dp,
                )
                else -> FocusButton(
                    "지금 바로 시작", onClick = onStartNow,
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Default.PlayArrow, height = 56.dp,
                )
            }
        }
    }
}

/** 잠기거나 억제 화면이 걸린 동안의 맨 위 카드. 일반 홈과 같은 판 구조에 빨간 탭으로 상태만 바꿔 보여 준다. */
@Composable
private fun LockHeroCard(
    lock: LockState,
    method: VerifyMethod,
    autoReleaseRemaining: Long,
    onVerifyNow: () -> Unit,
    onOpenLock: () -> Unit,
) {
    val nag = lock.mode == MissAction.NAG
    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        color = Chrome.platinum,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Chrome.red))
        FocusTab(
            if (nag) "억제 중" else "잠금 중",
            icon = lock.mode.icon,
            container = Chrome.red,
            content = Chrome.white,
        )
        Column(
            Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(lock.taskTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    if (nag) "인증할 때까지 ${DANMAKU_INTERVAL_MS / 60_000}분마다 문구가 지나가요"
                    else "인증할 때까지 다른 앱이 잠겨 있어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Chrome.redInk,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                FocusStatBox(
                    label = "자동 해제까지",
                    value = formatMinutesLeft(autoReleaseRemaining),
                    icon = PixelIcons.Hourglass,
                    large = true,
                    valueColor = Chrome.redInk,
                    caption = "늦게라도 인증하면 바로 풀려요",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            // 앱 안에서는 잠금 화면을 거치지 않고 바로 인증한다
            FocusButton(
                "지금 인증하기", onClick = onVerifyNow,
                modifier = Modifier.fillMaxWidth(), icon = method.actionIcon, height = 56.dp,
            )
            // 비상 해제와 긴급전화는 잠금 화면에만 있으므로 그리로 가는 길은 남겨 둔다
            FocusButton(
                "비상 해제 · 긴급전화", onClick = onOpenLock,
                modifier = Modifier.fillMaxWidth(), style = FocusButtonStyle.Outline,
                icon = Icons.Default.Lock, height = 48.dp,
            )
        }
    }
}

/** 지금 할 과제가 없을 때 맨 위 카드: 오늘 상황을 한 줄로 */
@Composable
private fun DaySummaryCard(entries: List<Pair<TaskEntity, TaskStatus>>) {
    val (message, icon) = when {
        entries.all { it.second is TaskStatus.RestDay || it.second == TaskStatus.Disabled } ->
            "오늘은 쉬는 날이에요" to Icons.Default.Home
        entries.all { it.second == TaskStatus.Done || it.second == TaskStatus.Disabled || it.second is TaskStatus.RestDay } ->
            "오늘 과제를 모두 끝냈어요" to PixelIcons.Sparkle
        else -> "오늘 남은 과제가 없어요" to PixelIcons.CheckCircle
    }
    val done = entries.count { it.second == TaskStatus.Done }
    val total = entries.count { it.second != TaskStatus.Disabled && it.second !is TaskStatus.RestDay }
    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        color = Chrome.lavender,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        FocusTab("오늘", icon = PixelIcons.Flag)
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (total > 0) {
                Text(
                    "$done/$total",
                    style = DigitStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    color = Chrome.carbon,
                )
            }
        }
    }
}

/**
 * 목록의 간결한 과제 카드. 이름·시간·상태를 먼저 보여 주고, 행동 버튼은 모든 카드에서
 * 오른쪽 같은 자리·같은 크기에 둔다. 카드를 누르면 편집 화면으로 간다.
 */
@Composable
private fun TaskCard(
    task: TaskEntity,
    status: TaskStatus,
    now: ZonedDateTime,
    lockedByThis: Boolean,
    streak: Int,
    achievements: Set<Int>,
    onVerify: () -> Unit,
    onEdit: () -> Unit,
    onStartNow: () -> Unit,
) {
    // 카메라 없는 인증은 마감 뒤에도 늦은 체크로 기록할 수 있다 (사진은 잠겨 있을 때만 늦게 받는다)
    val lateCheckable = task.verifyMethod != VerifyMethod.PHOTO
    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusBadge(status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaskIcon.fromKey(task.iconKey)?.let {
                        TaskIconMark(it, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PixelIcons.Clock, contentDescription = null, tint = Chrome.chromeIndigo, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${TaskSchedule.describeDays(task.repeatDays)} · ${TaskSchedule.describeWindow(task)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Chrome.chromeIndigo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            CardAction(task, status, lateCheckable, lockedByThis, onStartNow, onVerify)
        }
        ConditionPills(task)
        // 꺼 둔 과제는 쌓인 기록이 있을 때만 보여 준다
        if (task.enabled || streak > 0 || achievements.isNotEmpty()) {
            TaskAchievementStrip(TaskIcon.fromKey(task.iconKey), achievements, streak)
        }
        when (status) {
            is TaskStatus.InProgress -> {
                FocusProgressBar(status.progress)
                StatusLine("마감까지 ${formatMinutesLeft(Duration.between(now, status.deadline).toMillis())}", PixelIcons.Hourglass)
            }
            is TaskStatus.Upcoming -> StatusLine(formatStartsIn(now, status.startsAt), PixelIcons.Hourglass)
            is TaskStatus.RestDay -> StatusLine(
                "오늘은 쉬는 날 · " + (status.nextStart?.let { formatStartsIn(now, it) } ?: "반복 요일이 없어요"),
                Icons.Default.Home,
            )
            // 늦게라도 했으면 기록을 남길 수 있다 (기록 화면에는 "늦게 통과")
            TaskStatus.Missed -> when {
                lockedByThis -> StatusLine(
                    if (task.missAction == MissAction.NAG) "지금 인증하면 억제 화면이 멈춰요" else "지금 인증하면 잠금이 풀려요",
                    task.missAction.icon,
                    Chrome.redInk,
                )
                lateCheckable -> StatusLine("마감이 지났어요 · 했다면 늦게라도 체크해 두세요", Icons.Default.Warning)
                else -> StatusLine("마감을 놓쳤어요", Icons.Default.Warning, Chrome.redInk)
            }
            // 완료는 배지와 오른쪽 체크 원으로 충분해서 상태 줄을 따로 두지 않는다
            TaskStatus.Done -> Unit
            TaskStatus.Disabled -> StatusLine("꺼 둔 과제예요 · 눌러서 켤 수 있어요", Icons.Default.Close)
        }
    }
}

/** 카드 오른쪽의 행동 버튼. 모든 상태에서 같은 52dp 원형 자리를 쓴다. */
@Composable
private fun CardAction(
    task: TaskEntity,
    status: TaskStatus,
    lateCheckable: Boolean,
    lockedByThis: Boolean,
    onStartNow: () -> Unit,
    onVerify: () -> Unit,
) {
    val title = task.title
    val method = task.verifyMethod
    // 픽셀 아이콘은 머티리얼 아이콘보다 꽉 차 보여서 조금 줄인다
    val iconScale = if (method == VerifyMethod.PHOTO || method == VerifyMethod.NFC) 0.45f else null
    when {
        status is TaskStatus.Upcoming || status is TaskStatus.RestDay ->
            FocusPlayButton(onClick = onStartNow, contentDescription = "$title 바로 시작")
        status is TaskStatus.InProgress -> VerifyPlayButton(
            onClick = onVerify, contentDescription = "$title ${method.actionLabel}",
            icon = method.actionIcon, iconScale = iconScale,
        )
        status == TaskStatus.Missed && lockedByThis -> VerifyPlayButton(
            onClick = onVerify, contentDescription = "$title 지금 인증하기",
            icon = method.actionIcon, iconScale = iconScale,
        )
        status == TaskStatus.Missed && lateCheckable -> VerifyPlayButton(
            onClick = onVerify, contentDescription = "$title 늦게라도 체크",
            icon = method.actionIcon, iconScale = iconScale, containerColor = Chrome.amber,
        )
        // 완료 원: 위는 밝게, 아래는 그늘지게 둘러 살짝 튀어나온 단추처럼
        status == TaskStatus.Done -> Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Chrome.ice)
                .circleBevel(highlight = Color(0xCCFFFFFF), shadow = Color(0x591F7A58), width = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PixelIcons.Check, contentDescription = "완료", tint = Chrome.teal, modifier = Modifier.size(28.dp))
        }
        else -> Spacer(Modifier.size(52.dp))
    }
}

@Composable
private fun VerifyPlayButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: ImageVector,
    iconScale: Float?,
    containerColor: Color = Chrome.signal,
) = FocusPlayButton(
    onClick = onClick,
    contentDescription = contentDescription,
    icon = icon,
    containerColor = containerColor,
    iconScale = iconScale ?: 0.55f,
)

@Composable
private fun StatusLine(text: String, icon: ImageVector, color: Color = Chrome.chromeIndigo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

private data class BadgeStyle(val text: String, val icon: ImageVector, val container: Color, val content: Color)

@Composable
private fun StatusBadge(status: TaskStatus) {
    val style = when (status) {
        is TaskStatus.Upcoming -> BadgeStyle("시작 전", PixelIcons.Hourglass, Chrome.sky, Chrome.carbon)
        is TaskStatus.RestDay -> BadgeStyle("쉬는 날", Icons.Default.Home, Chrome.sky, Chrome.carbon)
        is TaskStatus.InProgress -> BadgeStyle("진행 중", Icons.Default.PlayArrow, Chrome.amber, Chrome.carbon)
        TaskStatus.Done -> BadgeStyle("완료", PixelIcons.CheckCircle, Chrome.ice, Chrome.teal)
        TaskStatus.Missed -> BadgeStyle("놓침", Icons.Default.Warning, Chrome.redSoft, Chrome.redInk)
        TaskStatus.Disabled -> BadgeStyle("꺼짐", Icons.Default.Close, Chrome.white, Chrome.hairline)
    }
    // 상태 배지도 위 밝은 선 + 아래 그늘로 살짝 튀어나온 명찰처럼
    Pill(
        style.text, style.container, style.content, icon = style.icon,
        modifier = Modifier.bevel(highlight = Color(0xB3FFFFFF), shadow = Color(0x40000000), highlightWidth = 1.dp, shadowWidth = 2.dp),
    )
}

/** 인증 방식·장소·사진 조건을 아이콘과 짧은 라벨로. 좁은 화면에서는 다음 줄로 넘긴다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionPills(task: TaskEntity) {
    // 사진 조건은 사진 인증 과제에만 의미가 있다
    val labels = if (task.verifyMethod == VerifyMethod.PHOTO) task.requiredLabels() else emptyList()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Pill(task.verifyMethod.title, Chrome.white, Chrome.carbon, icon = task.verifyMethod.icon)
        task.missAction.pillText?.let { Pill(it, Chrome.white, Chrome.redInk, icon = task.missAction.icon) }
        // 탭 체크와 NFC는 장소를 보지 않는다
        val radius = task.fenceRadiusMeters()
        if (radius != null && (task.verifyMethod == VerifyMethod.PHOTO || task.verifyMethod == VerifyMethod.LOCATION)) {
            Pill("${radius.toInt()}m 이내", Chrome.white, Chrome.carbon, icon = Icons.Default.LocationOn)
        }
        if (labels.isNotEmpty()) {
            val text = if (labels.size <= 2) labels.joinToString(" · ") else "${labels.first()} 외 ${labels.size - 1}개"
            Pill(text, Chrome.white, Chrome.carbon, icon = Icons.Default.Search)
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

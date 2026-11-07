package com.getfuckocus.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.getfuckocus.container
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.TaskDao
import java.time.DayOfWeek
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.requiredLabels
import com.getfuckocus.hasPermission
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.task.editFreezeReason
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusChip
import com.getfuckocus.ui.components.FocusDialog
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.components.FocusSwitch
import com.getfuckocus.ui.components.FocusTextField
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.TimeWheelDialog
import kotlinx.coroutines.launch

private enum class TimeField { START, DURATION }

private val DURATION_PRESETS = listOf(10, 30, 60, 90, 120, 180)
private val RADIUS_PRESETS = listOf(50f, 100f, 150f, 300f, 500f)
private const val DEFAULT_RADIUS_M = 150f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(taskId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()

    // 화면 회전 등으로 Activity가 다시 만들어져도 입력 중인 값이 남도록 rememberSaveable로 둔다
    var original by remember { mutableStateOf<TaskEntity?>(null) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var startMinute by rememberSaveable { mutableIntStateOf(TaskSchedule.defaultStartMinute()) }
    var durationMinutes by rememberSaveable { mutableIntStateOf(TaskSchedule.DEFAULT_DURATION_MINUTES) }
    var repeatDays by rememberSaveable { mutableIntStateOf(RepeatDays.EVERY_DAY) }
    var picking by rememberSaveable { mutableStateOf<TimeField?>(null) }
    val labels = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<String>()
    }
    var labelInput by rememberSaveable { mutableStateOf("") }
    var radiusMeters by rememberSaveable { mutableFloatStateOf(DEFAULT_RADIUS_M) }
    var geo by rememberSaveable { mutableStateOf<Pair<Double, Double>?>(null) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var freezeReason by remember { mutableStateOf<String?>(null) }
    val frozen = freezeReason != null

    val suggestions by produceState(emptyList<String>()) { value = recentLabelSuggestions(container.taskDao) }

    LaunchedEffect(taskId) {
        if (taskId == 0L) return@LaunchedEffect
        val t = container.taskDao.get(taskId) ?: return@LaunchedEffect
        original = t
        // 회전 후 다시 불러올 때는 저장된 원본으로 입력 중인 값을 덮어쓰지 않는다
        if (!loaded) {
            title = t.title
            startMinute = t.windowStartMinute
            durationMinutes = t.durationMinutes
            repeatDays = t.repeatDays
            labels.clear()
            labels.addAll(t.requiredLabels())
            radiusMeters = t.geoRadiusMeters ?: DEFAULT_RADIUS_M
            geo = if (t.geoLat != null && t.geoLng != null) t.geoLat to t.geoLng else null
            enabled = t.enabled
            loaded = true
        }
    }

    // 진행 중인 회차가 끝나면 다시 편집할 수 있게 30초마다 확인한다
    val now = rememberNow(30_000)
    LaunchedEffect(original, now) {
        freezeReason = original?.let {
            editFreezeReason(it, container.taskDao, container.lockController, container.deviceClock.trustedNow())
        }
    }

    fun fetchLocation() = scope.launch {
        message = "현재 위치 확인 중…"
        val location = container.evidenceCollector.currentLocation()
        if (location == null) {
            message = "위치를 가져오지 못했어요"
        } else {
            geo = location.latitude to location.longitude
            message = null
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchLocation()
        } else {
            message = "위치 권한이 없어 장소를 지정할 수 없어요"
        }
    }

    fun addLabel(text: String) {
        val value = text.trim().trimEnd(',')
        if (value.isNotEmpty() && labels.none { it.equals(value, ignoreCase = true) }) labels += value
        labelInput = ""
    }

    fun save() {
        if (labelInput.isNotBlank() && !frozen) addLabel(labelInput)
        message = when {
            title.isBlank() -> "과제 이름을 입력하세요"
            durationMinutes !in 1..TaskSchedule.MAX_DURATION_MINUTES -> "소요 시간은 1분 이상이어야 해요"
            (repeatDays and RepeatDays.EVERY_DAY) == 0 -> "반복할 요일을 하나 이상 골라 주세요"
            else -> null
        }
        if (message != null) return

        val base = original ?: TaskEntity(title = "", windowStartMinute = 0, durationMinutes = 0)
        // 편집이 막힌 동안에는 이름만 바꾼다 (화면에서 막는 것과 별개로 저장 단계에서도 지킨다)
        val task = if (frozen) base.copy(title = title.trim()) else base.copy(
            title = title.trim(),
            windowStartMinute = startMinute,
            durationMinutes = durationMinutes,
            repeatDays = repeatDays,
            geoLat = geo?.first,
            geoLng = geo?.second,
            geoRadiusMeters = if (geo != null) radiusMeters else null,
            requiredLabel = labels.joinToString(", ").ifEmpty { null },
            enabled = enabled,
        )
        scope.launch {
            container.taskDao.upsert(task)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = if (original == null) "새 과제" else "과제 편집",
                onBack = onDone,
                actions = {
                    if (original != null && !frozen) {
                        FocusIconButton(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            onClick = { confirmDelete = true },
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                FocusButton("저장", onClick = ::save, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            freezeReason?.let { reason ->
                FocusCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionCard(title = "과제") {
                FocusTextField(
                    title, { title = it },
                    label = "과제 이름",
                    placeholder = "예: 아침 운동",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("사용", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "끄면 마감을 놓쳐도 잠기지 않아요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FocusSwitch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !frozen)
                }
            }

            SectionCard(title = "시간", trailing = { CurrentTimeText() }) {
                FocusCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = if (frozen) null else ({ picking = TimeField.START }),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "시작 시각",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                TaskSchedule.formatMinute(startMinute),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (!frozen) Icon(Icons.Default.Edit, contentDescription = "시작 시각 바꾸기")
                    }
                }

                Text("소요 시간", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DURATION_PRESETS.forEach { preset ->
                        FocusChip(
                            TaskSchedule.formatDuration(preset),
                            selected = durationMinutes == preset,
                            onClick = { durationMinutes = preset },
                            enabled = !frozen,
                        )
                    }
                    FocusChip(
                        if (durationMinutes in DURATION_PRESETS) "직접 설정" else TaskSchedule.formatDuration(durationMinutes),
                        selected = durationMinutes !in DURATION_PRESETS,
                        onClick = { picking = TimeField.DURATION },
                        leadingIcon = Icons.Default.Edit,
                        enabled = !frozen,
                    )
                }
                Text("반복 요일", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        FocusChip(
                            TaskSchedule.dayName(day),
                            selected = RepeatDays.contains(repeatDays, day),
                            onClick = { repeatDays = repeatDays xor RepeatDays.bit(day) },
                            enabled = !frozen,
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "매일" to RepeatDays.EVERY_DAY,
                        "평일" to RepeatDays.WEEKDAYS,
                        "주말" to RepeatDays.WEEKEND,
                    ).forEach { (name, mask) ->
                        FocusChip(name, selected = repeatDays == mask, onClick = { repeatDays = mask }, enabled = !frozen)
                    }
                }
                Text(
                    TaskSchedule.describeDays(repeatDays) + " · " +
                        TaskSchedule.describeWindow(startMinute, durationMinutes) + " · 마감까지 인증하지 않으면 잠겨요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "장소", subtitle = "선택 · 지정한 곳 근처에서 찍어야 통과해요") {
                val currentGeo = geo
                if (currentGeo == null) {
                    FocusButton(
                        "지금 있는 곳을 장소로 지정",
                        onClick = {
                            if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) fetchLocation()
                            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = FocusButtonStyle.Outline,
                        icon = Icons.Default.LocationOn,
                        enabled = !frozen,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "%.5f, %.5f".format(currentGeo.first, currentGeo.second),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        FocusButton(
                            "없애기", onClick = { geo = null },
                            style = FocusButtonStyle.Ghost, height = 40.dp, enabled = !frozen,
                        )
                    }
                    Text("반경", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RADIUS_PRESETS.forEach { preset ->
                            FocusChip(
                                "${preset.toInt()}m",
                                selected = radiusMeters == preset,
                                onClick = { radiusMeters = preset },
                                enabled = !frozen,
                            )
                        }
                    }
                }
            }

            SectionCard(title = "사진 조건", subtitle = "선택 · 아래 라벨 중 하나가 사진에 보이면 통과해요") {
                if (labels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        labels.forEach { label ->
                            FocusChip(
                                label,
                                selected = true,
                                onClick = { labels.remove(label) },
                                trailingIcon = if (frozen) null else Icons.Default.Close,
                                enabled = !frozen,
                            )
                        }
                    }
                }
                FocusTextField(
                    labelInput,
                    { input ->
                        // 쉼표를 치면 바로 칩으로 만든다
                        if (input.endsWith(",")) addLabel(input) else labelInput = input
                    },
                    label = "라벨 추가",
                    placeholder = "영문, 예: Dog",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addLabel(labelInput) }),
                    enabled = !frozen,
                    trailing = {
                        FocusIconButton(
                            Icons.Default.Add,
                            contentDescription = "라벨 추가",
                            onClick = { addLabel(labelInput) },
                            enabled = !frozen && labelInput.isNotBlank(),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                val available = suggestions.filter { s -> labels.none { it.equals(s, ignoreCase = true) } }
                if (available.isNotEmpty() && !frozen) {
                    Text(
                        "최근 사진에서 인식된 라벨",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        available.forEach { suggestion ->
                            FocusChip(
                                suggestion,
                                selected = false,
                                onClick = { addLabel(suggestion) },
                                leadingIcon = Icons.Default.Add,
                            )
                        }
                    }
                }
            }
        }
    }

    picking?.let { field ->
        TimeWheelDialog(
            title = if (field == TimeField.START) "시작 시각" else "소요 시간",
            initialMinute = if (field == TimeField.START) startMinute else durationMinutes,
            hourSuffix = if (field == TimeField.DURATION) "시간" else "",
            minuteSuffix = if (field == TimeField.DURATION) "분" else "",
            onDismiss = { picking = null },
            onConfirm = { minute ->
                if (field == TimeField.START) startMinute = minute
                else if (minute > 0) durationMinutes = minute
                picking = null
            },
        )
    }

    val toDelete = original
    if (confirmDelete && toDelete != null && !frozen) {
        FocusDialog(
            title = "과제를 삭제할까요?",
            onDismiss = { confirmDelete = false },
            confirmText = "삭제",
            danger = true,
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    container.taskDao.delete(toDelete)
                    onDone()
                }
            },
        ) {
            Text(
                "'${toDelete.title}' 과제가 삭제돼요. 인증 기록은 보존 기간이 지나면 함께 지워져요.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    FocusPanel(
        title = title,
        subtitle = subtitle,
        trailing = trailing,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

/** 시간창을 고를 때 기준이 되도록 현재 시각을 24시간제로 보여준다. */
@Composable
private fun CurrentTimeText() {
    val now = rememberNow()
    Text(
        "현재 ${TaskSchedule.formatMinute(now.hour * 60 + now.minute)}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** 최근 인증 사진에서 자주 인식된 라벨. 어떤 이름을 적어야 하는지 고민하지 않게 해 준다. */
private suspend fun recentLabelSuggestions(dao: TaskDao): List<String> =
    dao.recentLabelStrings(limit = 30)
        .flatMap { it.split(',') }
        .map { it.substringBeforeLast(':').trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(10)
        .map { it.key }

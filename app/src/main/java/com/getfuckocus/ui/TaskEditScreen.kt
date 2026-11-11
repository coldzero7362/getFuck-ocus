package com.getfuckocus.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfuckocus.container
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.VerifyMethod
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
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.theme.halftone
import kotlinx.coroutines.launch

private enum class TimeField { START, DURATION }

private val DURATION_PRESETS = listOf(10, 30, 60, 90, 120, 180)
private val RADIUS_PRESETS = listOf(50f, 100f, 150f, 300f, 500f)
private val REMIND_BEFORE_PRESETS = listOf(0, 5, 15, 30)
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
    // 새 과제는 가벼운 탭 체크로 시작한다. 잠금까지 걸고 싶은 일만 사진 인증으로 바꾼다
    var verifyMethod by rememberSaveable { mutableStateOf(VerifyMethod.TAP) }
    var remindAtStart by rememberSaveable { mutableStateOf(true) }
    var remindBeforeMinutes by rememberSaveable { mutableIntStateOf(15) }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }
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
            remindAtStart = t.remindAtStart
            remindBeforeMinutes = t.remindBeforeMinutes
            verifyMethod = t.verifyMethod
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
        // 알림 설정은 잠금 조건이 아니므로 편집이 막힌 동안에도 바꿀 수 있다
        val task = if (frozen) {
            base.copy(title = title.trim(), remindAtStart = remindAtStart, remindBeforeMinutes = remindBeforeMinutes)
        } else base.copy(
            remindAtStart = remindAtStart,
            remindBeforeMinutes = remindBeforeMinutes,
            verifyMethod = verifyMethod,
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

    // 저장하지 않은 변경 감지: 불러온 직후(새 과제는 처음 값)를 기준으로 삼는다
    fun snapshot(): String = listOf(
        title, startMinute, durationMinutes, repeatDays, verifyMethod, remindAtStart, remindBeforeMinutes,
        labels.toList(), labelInput, radiusMeters, geo, enabled,
    ).toString()
    var baseline by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(loaded, taskId) {
        if (baseline == null && (taskId == 0L || loaded)) baseline = snapshot()
    }
    val dirty = baseline != null && baseline != snapshot()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    fun leave() = if (dirty) confirmLeave = true else onDone()
    BackHandler(enabled = dirty) { confirmLeave = true }

    val isNew = taskId == 0L
    var placeExpanded by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val placeOpen = placeExpanded ?: (geo != null)

    Scaffold(
        topBar = {
            FocusTopBar(
                title = if (isNew) "새 과제" else "과제 편집",
                subtitle = if (isNew) "이름과 시간만 정하면 바로 만들 수 있어요" else original?.title?.let { "'$it' 설정" },
                onBack = ::leave,
                actions = {
                    if (original != null && !frozen) {
                        FocusIconButton(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            onClick = { confirmDelete = true },
                            tint = Chrome.red,
                        )
                    }
                },
            )
        },
        bottomBar = {
            // 저장 버튼은 화면 아래에 고정하고 시스템 내비게이션·키보드 위로 올린다
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Chrome.carbon)
                    .halftone()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                message?.let { FocusNotice(it, tone = if (it.endsWith("…")) Tone.Info else Tone.Danger) }
                FocusButton(
                    if (isNew) "과제 만들기" else "저장",
                    onClick = ::save,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                    icon = Icons.Default.Check,
                )
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
                FocusNotice(reason, title = "지금은 이름과 알림만 바꿀 수 있어요", tone = Tone.Warning, icon = Icons.Default.Lock)
            }

            FocusPanel(title = "기본 정보", index = "01", modifier = Modifier.fillMaxWidth()) {
                if (isNew) {
                    Text("어떤 일을 할까요?", style = MaterialTheme.typography.titleMedium)
                }
                FocusTextField(
                    title, { title = it },
                    label = "과제 이름",
                    placeholder = "예: 아침 운동",
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingRow(title = "사용", description = "끄면 마감을 놓쳐도 잠기지 않아요") {
                    FocusSwitch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !frozen)
                }
            }

            FocusPanel(title = "인증 방식", index = "02", modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MethodTile(
                        title = "탭 체크",
                        caption = "버튼 한 번",
                        icon = Icons.Default.ThumbUp,
                        selected = verifyMethod == VerifyMethod.TAP,
                        enabled = !frozen,
                        onClick = { verifyMethod = VerifyMethod.TAP },
                        modifier = Modifier.weight(1f),
                    )
                    MethodTile(
                        title = "사진 인증",
                        caption = "놓치면 잠금",
                        icon = PixelIcons.Camera,
                        selected = verifyMethod == VerifyMethod.PHOTO,
                        enabled = !frozen,
                        onClick = { verifyMethod = VerifyMethod.PHOTO },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    when (verifyMethod) {
                        VerifyMethod.TAP ->
                            "버튼 한 번이나 알림의 \"완료\"로 체크해요. 놓쳐도 잠기지 않고 기록과 알림으로만 챙겨요. " +
                                "약 먹기, 출퇴근 같은 일과에 맞아요."
                        VerifyMethod.PHOTO ->
                            "앱 카메라로 찍어서 인증해요. 마감까지 인증하지 않으면 다른 앱이 잠겨요. " +
                                "꼭 지키고 싶은 약속에 맞아요."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Chrome.chromeIndigo,
                )

                // 사진 조건은 사진 인증에만 쓴다. 탭 체크로 바꿔도 적어 둔 라벨은 지우지 않고 숨기기만 한다
                if (verifyMethod == VerifyMethod.PHOTO) {
                    SubHeader("사진 조건", "선택 · 아래 라벨 중 하나가 사진에 보이면 통과해요")
                    Text(
                        if (labels.isEmpty()) "추가한 조건이 없어요 · 무엇이 찍혀도 통과해요" else "추가한 조건 ${labels.size}개 · 누르면 빠져요",
                        style = MaterialTheme.typography.labelMedium,
                        color = Chrome.chromeIndigo,
                    )
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
                                tint = Chrome.carbon,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val available = suggestions.filter { s -> labels.none { it.equals(s, ignoreCase = true) } }
                    if (available.isNotEmpty() && !frozen) {
                        Text(
                            "최근 사진에서 인식된 라벨 · 눌러서 추가",
                            style = MaterialTheme.typography.labelMedium,
                            color = Chrome.chromeIndigo,
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

            FocusPanel(title = "일정 설정", index = "03", trailing = { CurrentTimeText() }, modifier = Modifier.fillMaxWidth()) {
                FocusCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = Chrome.white,
                    onClick = if (frozen) null else ({ picking = TimeField.START }),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("시작 시각", style = MaterialTheme.typography.labelMedium, color = Chrome.chromeIndigo)
                            Text(
                                TaskSchedule.formatMinute(startMinute),
                                style = DigitStyle.copy(fontSize = 36.sp),
                                color = Chrome.carbon,
                            )
                            Text(
                                "~ " + TaskSchedule.formatMinute((startMinute + durationMinutes) % (24 * 60)) + " 마감",
                                style = MaterialTheme.typography.bodySmall,
                                color = Chrome.chromeIndigo,
                            )
                        }
                        if (!frozen) {
                            Text("바꾸기", style = MaterialTheme.typography.labelLarge, color = Chrome.chromeIndigo)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "시작 시각 바꾸기", tint = Chrome.chromeIndigo)
                        }
                    }
                }

                SubHeader("소요 시간")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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

                SubHeader("반복 요일", "빠른 선택을 누르거나 요일을 하나씩 켜고 꺼요")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "매일" to RepeatDays.EVERY_DAY,
                        "평일" to RepeatDays.WEEKDAYS,
                        "주말" to RepeatDays.WEEKEND,
                    ).forEach { (name, mask) ->
                        FocusChip(
                            name,
                            selected = repeatDays == mask,
                            onClick = { repeatDays = mask },
                            enabled = !frozen,
                            leadingIcon = if (repeatDays == mask) Icons.Default.Check else null,
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                }
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
                FocusNotice(
                    TaskSchedule.describeDays(repeatDays) + " · " +
                        TaskSchedule.describeWindow(startMinute, durationMinutes) +
                        if (verifyMethod == VerifyMethod.PHOTO) " · 마감까지 인증하지 않으면 잠겨요" else "",
                    tone = Tone.Info,
                    icon = PixelIcons.Clock,
                )

                SubHeader("알림", "잠금과 상관없이 언제든 바꿀 수 있어요")
                SettingRow(title = "시작할 때 알림") {
                    FocusSwitch(checked = remindAtStart, onCheckedChange = { remindAtStart = it })
                }
                Text("마감 전 알림", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    REMIND_BEFORE_PRESETS.forEach { minutes ->
                        FocusChip(
                            if (minutes == 0) "끄기" else "${minutes}분 전",
                            selected = remindBeforeMinutes == minutes,
                            onClick = { remindBeforeMinutes = minutes },
                        )
                    }
                }
                if (!notificationsGranted && (remindAtStart || remindBeforeMinutes > 0)) {
                    FocusNotice(
                        "알림 권한이 없어서 알림이 오지 않아요",
                        tone = Tone.Warning,
                        icon = PixelIcons.Bell,
                        actionLabel = "허용",
                        onAction = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    )
                }
            }

            FocusPanel(
                title = "장소 설정",
                index = "04",
                subtitle = if (geo != null) "지정됨 · 반경 ${radiusMeters.toInt()}m" else "선택 · 지정한 곳 근처에서만 통과해요",
                expanded = placeOpen,
                onToggle = { placeExpanded = !placeOpen },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val currentGeo = geo
                if (currentGeo == null) {
                    Text(
                        "지금 있는 곳을 등록하면 그 근처에서 체크하거나 찍어야 통과해요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Chrome.chromeIndigo,
                    )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ChromeShapes.small)
                            .background(Chrome.white)
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Chrome.signal, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            // 좌표를 장소 이름으로 바꾸려면 위치를 외부 서버로 보내야 해서, 기기 안에서 끝나도록 좌표는 보조로만 둔다
                            Text("등록한 위치 (반경 ${radiusMeters.toInt()}m)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "%.5f, %.5f".format(currentGeo.first, currentGeo.second),
                                style = MaterialTheme.typography.bodySmall,
                                color = Chrome.chromeIndigo,
                            )
                        }
                        FocusButton(
                            "없애기", onClick = { geo = null },
                            style = FocusButtonStyle.Ghost, height = 48.dp, enabled = !frozen,
                        )
                    }
                    SubHeader("반경")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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

    if (confirmLeave) {
        FocusDialog(
            title = "저장하지 않고 나갈까요?",
            onDismiss = { confirmLeave = false },
            confirmText = "나가기",
            dismissText = "계속 편집",
            danger = true,
            onConfirm = {
                confirmLeave = false
                onDone()
            },
        ) {
            Text("바꾼 내용이 저장되지 않아요.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String? = null, control: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
            }
        }
        Spacer(Modifier.width(12.dp))
        control()
    }
}

/** 한 구역 안의 작은 제목. 위에 옅은 선을 그어 앞 내용과 나눈다. */
@Composable
private fun SubHeader(title: String, description: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Chrome.hairline.copy(alpha = 0.35f)))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
        }
    }
}

/** 인증 방식 고르기용 큰 타일. 선택되면 앰버 판으로 튀어나오고 체크 표시가 붙는다. */
@Composable
private fun MethodTile(
    title: String,
    caption: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        color = if (selected) Chrome.amber else Chrome.white,
        onClick = if (enabled) onClick else null,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(22.dp))
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "선택됨", tint = Chrome.carbon, modifier = Modifier.size(20.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
    }
}

/** 시간창을 고를 때 기준이 되도록 현재 시각을 24시간제로 보여준다. */
@Composable
private fun CurrentTimeText() {
    val now = rememberNow()
    Text(
        "현재 ${TaskSchedule.formatMinute(now.hour * 60 + now.minute)}",
        style = MaterialTheme.typography.labelLarge,
        color = Chrome.chromeIndigo,
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

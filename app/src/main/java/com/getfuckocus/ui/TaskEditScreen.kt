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
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.NagContent
import com.getfuckocus.data.nagImagePaths
import com.getfuckocus.data.nagMessages
import androidx.compose.foundation.clickable
import com.getfuckocus.data.allowedMissActions
import com.getfuckocus.data.saveProblem
import com.getfuckocus.lock.NagPolicy
import com.getfuckocus.safety.SafetyPolicy
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ContentScale
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
import com.getfuckocus.ui.components.FocusSegmented
import com.getfuckocus.ui.components.FocusTab
import androidx.compose.foundation.border
import com.getfuckocus.ui.components.SegmentItem
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
fun TaskEditScreen(
    taskId: Long,
    onDone: () -> Unit,
    onAddTag: () -> Unit,
    pickedPlace: PickedPlace?,
    onPickPlace: (current: PickedPlace?) -> Unit,
    onPlaceConsumed: () -> Unit,
) {
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
    // 새 과제는 가벼운 탭 체크 + 알림만으로 시작한다. 꼭 지킬 일만 더 센 인증과 억제 화면·잠금으로 바꾼다
    var verifyMethod by rememberSaveable { mutableStateOf(VerifyMethod.TAP) }
    var missAction by rememberSaveable { mutableStateOf(MissAction.NOTIFY) }
    var nfcTagId by rememberSaveable { mutableStateOf<Long?>(null) }
    // 억제 화면에 흘려보낼 문구·사진 (여러 개)
    val nagMessages = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<String>()
    }
    var nagInput by rememberSaveable { mutableStateOf("") }
    val nagImages = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<String>()
    }
    val nfcTags by remember { container.nfcTagDao.observeAll() }.collectAsState(initial = emptyList())
    var importingImage by remember { mutableStateOf(false) }
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
            missAction = t.missAction
            nfcTagId = t.nfcTagId
            nagMessages.clear()
            nagMessages.addAll(t.nagMessages())
            nagImages.clear()
            nagImages.addAll(t.nagImagePaths())
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

    // 지도에서 고른 장소를 받는다 (한 번 받으면 비워서 다시 들어오지 않게 한다)
    LaunchedEffect(pickedPlace) {
        val place = pickedPlace ?: return@LaunchedEffect
        if (!frozen) {
            geo = place.lat to place.lng
            radiusMeters = place.radiusMeters
            message = null
        }
        onPlaceConsumed()
    }
    fun openPlacePicker() = onPickPlace(geo?.let { PickedPlace(it.first, it.second, radiusMeters) })

    fun addLabel(text: String) {
        val value = text.trim().trimEnd(',')
        if (value.isNotEmpty() && labels.none { it.equals(value, ignoreCase = true) }) labels += value
        labelInput = ""
    }

    // 장소는 사진·위치 인증에서만 본다 (탭 체크는 버튼 한 번, NFC는 태그가 놓인 자리가 곧 장소)
    val usesPlace = verifyMethod == VerifyMethod.PHOTO || verifyMethod == VerifyMethod.LOCATION

    fun selectMethod(method: VerifyMethod) {
        verifyMethod = method
        // 탭 체크는 잠글 수 없으므로 잠금이었으면 한 단계 낮춘다
        if (missAction !in method.allowedMissActions()) missAction = MissAction.NAG
        // NFC를 고르면 태그가 하나뿐일 때 바로 골라 둔다
        if (method == VerifyMethod.NFC && nfcTagId == null) nfcTagId = nfcTags.singleOrNull()?.id
    }

    // 이번 편집에서 새로 넣은 이미지는 저장하지 않고 나가면 지운다 (원래 있던 이미지는 저장할 때 정리한다)
    fun discardNewImage(path: String) {
        if (path !in original?.nagImagePaths().orEmpty()) deleteNagImage(context, path)
    }
    fun removeNagImage(path: String) {
        nagImages.remove(path)
        discardNewImage(path)
    }
    fun addNagMessage(text: String) {
        val value = text.replace('\n', ' ').trim().take(NagContent.MAX_MESSAGE_LENGTH)
        if (value.isNotEmpty() && value !in nagMessages && nagMessages.size < NagContent.MAX_MESSAGES) nagMessages += value
        nagInput = ""
    }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(NagContent.MAX_IMAGES)
    ) { uris ->
        val picked = uris.take(NagContent.MAX_IMAGES - nagImages.size)
        if (picked.isEmpty()) return@rememberLauncherForActivityResult
        importingImage = true
        scope.launch {
            var failed = 0
            picked.forEach { uri ->
                val imported = importNagImage(context, uri)
                if (imported == null) failed++ else nagImages += imported
            }
            if (failed > 0) message = "이미지 ${failed}장을 불러오지 못했어요"
            importingImage = false
        }
    }

    fun save() {
        if (labelInput.isNotBlank() && !frozen) addLabel(labelInput)
        if (nagInput.isNotBlank()) addNagMessage(nagInput)
        message = when {
            title.isBlank() -> "과제 이름을 입력하세요"
            durationMinutes !in 1..TaskSchedule.MAX_DURATION_MINUTES -> "소요 시간은 1분 이상이어야 해요"
            (repeatDays and RepeatDays.EVERY_DAY) == 0 -> "반복할 요일을 하나 이상 골라 주세요"
            !frozen -> saveProblem(verifyMethod, hasGeoFence = geo != null, nfcTagId, missAction)
            else -> null
        }
        if (message != null) return

        val base = original ?: TaskEntity(title = "", windowStartMinute = 0, durationMinutes = 0)
        // 편집이 막힌 동안에는 이름만 바꾼다 (화면에서 막는 것과 별개로 저장 단계에서도 지킨다)
        // 알림 설정과 억제 문구·이미지는 잠금 조건이 아니므로 편집이 막힌 동안에도 바꿀 수 있다
        val shared = base.copy(
            title = title.trim(),
            remindAtStart = remindAtStart,
            remindBeforeMinutes = remindBeforeMinutes,
            nagMessage = NagContent.join(nagMessages),
            nagImagePath = NagContent.join(nagImages).ifEmpty { null },
        )
        val task = if (frozen) shared else shared.copy(
            verifyMethod = verifyMethod,
            missAction = missAction,
            nfcTagId = if (verifyMethod == VerifyMethod.NFC) nfcTagId else null,
            windowStartMinute = startMinute,
            durationMinutes = durationMinutes,
            repeatDays = repeatDays,
            geoLat = geo?.first.takeIf { usesPlace },
            geoLng = geo?.second.takeIf { usesPlace },
            geoRadiusMeters = if (geo != null && usesPlace) radiusMeters else null,
            requiredLabel = labels.joinToString(", ").ifEmpty { null },
            enabled = enabled,
        )
        scope.launch {
            container.taskDao.upsert(task)
            // 빠진 이미지의 옛 파일은 저장한 뒤에 지운다
            val kept = task.nagImagePaths().toSet()
            original?.nagImagePaths().orEmpty().filter { it !in kept }.forEach { deleteNagImage(context, it) }
            onDone()
        }
    }

    // 저장하지 않은 변경 감지: 불러온 직후(새 과제는 처음 값)를 기준으로 삼는다
    fun snapshot(): String = listOf(
        title, startMinute, durationMinutes, repeatDays, verifyMethod, remindAtStart, remindBeforeMinutes,
        labels.toList(), labelInput, radiusMeters, geo, enabled, missAction, nfcTagId,
        nagMessages.toList(), nagInput, nagImages.toList(),
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
                SettingRow(title = "사용", description = "끄면 알림도, 억제 화면도, 잠금도 멈춰요") {
                    FocusSwitch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !frozen)
                }
            }

            FocusPanel(title = "인증 방식", index = "02", modifier = Modifier.fillMaxWidth()) {
                // 두 개씩 두 줄. 타일 이름 아래에 짧은 설명을 둔다
                listOf(
                    listOf(VerifyMethod.TAP to "버튼 한 번", VerifyMethod.PHOTO to "카메라로 찍기"),
                    listOf(VerifyMethod.LOCATION to "정한 곳에서", VerifyMethod.NFC to "태그 찍기"),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (method, caption) ->
                            MethodTile(
                                title = method.title,
                                caption = caption,
                                icon = method.icon,
                                selected = verifyMethod == method,
                                enabled = !frozen,
                                onClick = { selectMethod(method) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Text(
                    when (verifyMethod) {
                        VerifyMethod.TAP ->
                            "버튼 한 번이나 알림의 \"완료\"로 체크해요. 약 먹기처럼 가벼운 일과에 맞아요."
                        VerifyMethod.PHOTO ->
                            "앱 카메라로 찍어서 인증해요. 장소와 사진에 보일 것도 정할 수 있어요."
                        VerifyMethod.LOCATION ->
                            "정해 둔 장소 근처에서 버튼을 눌러 체크해요. 출퇴근, 헬스장 가기에 맞아요. 아래에서 장소를 꼭 정해 주세요."
                        VerifyMethod.NFC ->
                            "등록한 NFC 태그를 폰에 대서 체크해요. 약통, 책상처럼 그 자리에 가야 하는 일에 맞아요."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Chrome.chromeIndigo,
                )

                if (verifyMethod == VerifyMethod.NFC) {
                    SubHeader("찍을 태그", "이 과제는 고른 태그로만 체크돼요")
                    if (nfcTags.isEmpty()) {
                        FocusNotice(
                            "등록한 태그가 없어요. 먼저 태그를 등록해 주세요",
                            tone = Tone.Warning,
                            icon = PixelIcons.Nfc,
                            actionLabel = "등록",
                            onAction = onAddTag,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            nfcTags.forEach { tag ->
                                FocusChip(
                                    tag.name,
                                    selected = nfcTagId == tag.id,
                                    onClick = { nfcTagId = tag.id },
                                    leadingIcon = if (nfcTagId == tag.id) Icons.Default.Check else null,
                                    enabled = !frozen,
                                )
                            }
                            FocusChip("새 태그", selected = false, onClick = onAddTag, leadingIcon = Icons.Default.Add, enabled = !frozen)
                        }
                    }
                }

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
                // 일곱 칸은 한 줄에 다 들어가지 않아 4 + 3 두 블록으로 나눈다
                val durationKeys = DURATION_PRESETS.map { preset ->
                    SegmentItem(
                        TaskSchedule.formatDuration(preset),
                        selected = durationMinutes == preset,
                        onClick = { durationMinutes = preset },
                        enabled = !frozen,
                    )
                } + SegmentItem(
                    if (durationMinutes in DURATION_PRESETS) "직접" else TaskSchedule.formatDuration(durationMinutes),
                    selected = durationMinutes !in DURATION_PRESETS,
                    onClick = { picking = TimeField.DURATION },
                    enabled = !frozen,
                    icon = Icons.Default.Edit,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationKeys.chunked(4).forEach { FocusSegmented(it) }
                }

                SubHeader("반복 요일", "빠른 선택을 누르거나 요일을 하나씩 켜고 꺼요")
                FocusSegmented(
                    listOf(
                        "매일" to RepeatDays.EVERY_DAY,
                        "평일" to RepeatDays.WEEKDAYS,
                        "주말" to RepeatDays.WEEKEND,
                    ).map { (name, mask) ->
                        SegmentItem(name, selected = repeatDays == mask, onClick = { repeatDays = mask }, enabled = !frozen)
                    }
                )
                // 요일은 여러 개를 켤 수 있다. 켠 요일이 눌린 키로 보인다
                FocusSegmented(
                    DayOfWeek.entries.map { day ->
                        SegmentItem(
                            TaskSchedule.dayName(day),
                            selected = RepeatDays.contains(repeatDays, day),
                            onClick = { repeatDays = repeatDays xor RepeatDays.bit(day) },
                            enabled = !frozen,
                        )
                    }
                )
                FocusNotice(
                    TaskSchedule.describeDays(repeatDays) + " · " +
                        TaskSchedule.describeWindow(startMinute, durationMinutes) +
                        when (missAction) {
                            MissAction.LOCK -> " · 마감까지 인증하지 않으면 잠겨요"
                            MissAction.NAG -> " · 마감까지 인증하지 않으면 억제 화면이 떠요"
                            MissAction.NOTIFY -> ""
                        },
                    tone = Tone.Info,
                    icon = PixelIcons.Clock,
                )

                SubHeader("알림", "잠금과 상관없이 언제든 바꿀 수 있어요")
                SettingRow(title = "시작할 때 알림") {
                    FocusSwitch(checked = remindAtStart, onCheckedChange = { remindAtStart = it })
                }
                Text("마감 전 알림", style = MaterialTheme.typography.labelLarge)
                FocusSegmented(
                    REMIND_BEFORE_PRESETS.map { minutes ->
                        SegmentItem(
                            if (minutes == 0) "끄기" else "${minutes}분 전",
                            selected = remindBeforeMinutes == minutes,
                            onClick = { remindBeforeMinutes = minutes },
                        )
                    }
                )
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

            MissActionPanel(
                method = verifyMethod,
                missAction = missAction,
                onMissAction = { missAction = it },
                frozen = frozen,
                nagMessages = nagMessages,
                nagInput = nagInput,
                onNagInput = { input ->
                    // 엔터를 치면 바로 하나로 넣는다
                    if (input.endsWith("\n")) addNagMessage(input) else nagInput = input.take(NagContent.MAX_MESSAGE_LENGTH)
                },
                onAddNagMessage = { addNagMessage(nagInput) },
                onRemoveNagMessage = { nagMessages.remove(it) },
                nagImages = nagImages,
                importingImage = importingImage,
                onPickImage = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveImage = ::removeNagImage,
            )

            // 위치 인증은 장소가 꼭 있어야 하므로 늘 펼쳐 둔다
            if (usesPlace) FocusPanel(
                title = "장소 설정",
                index = "05",
                subtitle = when {
                    geo != null -> "지정됨 · 반경 ${radiusMeters.toInt()}m"
                    verifyMethod == VerifyMethod.LOCATION -> "필수 · 위치 인증은 여기서 체크해야 통과해요"
                    else -> "선택 · 지정한 곳 근처에서만 통과해요"
                },
                expanded = placeOpen || verifyMethod == VerifyMethod.LOCATION,
                onToggle = { placeExpanded = !placeOpen },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val currentGeo = geo
                if (currentGeo == null) {
                    Text(
                        "장소를 정하면 그 근처에서 체크하거나 찍어야 통과해요. 지금 있는 곳이 아니어도 지도에서 고를 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Chrome.chromeIndigo,
                    )
                    FocusButton(
                        "지도에서 고르기",
                        onClick = ::openPlacePicker,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.LocationOn,
                        enabled = !frozen,
                    )
                    FocusButton(
                        "지금 있는 곳으로 정하기",
                        onClick = {
                            if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) fetchLocation()
                            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = FocusButtonStyle.Outline,
                        enabled = !frozen,
                    )
                } else {
                    PlaceInfoBox(
                        lat = currentGeo.first,
                        lng = currentGeo.second,
                        radiusMeters = radiusMeters,
                        enabled = !frozen,
                        onChange = ::openPlacePicker,
                        onRemove = { geo = null },
                    )
                    SubHeader("반경")
                    FocusSegmented(
                        RADIUS_PRESETS.map { preset ->
                            SegmentItem(
                                "${preset.toInt()}m",
                                selected = radiusMeters == preset,
                                onClick = { radiusMeters = preset },
                                enabled = !frozen,
                            )
                        }
                    )
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
                    (toDelete.nagImagePaths() + nagImages).distinct().forEach { deleteNagImage(context, it) }
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
                nagImages.forEach(::discardNewImage)
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


/**
 * 정해 둔 장소. 디자인 가이드의 정보 상자 모양: 흰 판 + 카본 꼬리표 머리 + 앰버 배지,
 * 좌표는 디지털 숫자로, 아래에 돌출 버튼 둘 (바꾸기는 앰버 도구 버튼, 초기화는 테두리 버튼).
 * 따뜻한 색은 누를 수 있는 버튼과 배지에만 쓴다.
 */
@Composable
private fun PlaceInfoBox(
    lat: Double,
    lng: Double,
    radiusMeters: Float,
    enabled: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ChromeShapes.small)
            .background(Chrome.white)
            .border(1.dp, Chrome.hairline, ChromeShapes.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusTab("정한 장소", icon = Icons.Default.LocationOn)
            Spacer(Modifier.weight(1f))
            Pill("반경 ${radiusMeters.toInt()}m", Chrome.amber, Chrome.carbon)
            Spacer(Modifier.width(10.dp))
        }
        Column(
            Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 좌표를 장소 이름으로 바꾸려면 위치를 외부 서버로 보내야 해서, 편집 화면에서는 좌표만 보여 준다
            Column {
                Text(
                    "위도 · 경도",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Chrome.chromeIndigo,
                )
                Text(
                    "%.5f  %.5f".format(lat, lng),
                    style = DigitStyle.copy(fontSize = 20.sp),
                    color = Chrome.carbon,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusButton(
                    "지도에서 바꾸기",
                    onClick = onChange,
                    modifier = Modifier.weight(1.4f),
                    style = FocusButtonStyle.Tonal,
                    icon = Icons.Default.LocationOn,
                    height = 44.dp,
                    enabled = enabled,
                )
                FocusButton(
                    "초기화",
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    style = FocusButtonStyle.Outline,
                    height = 44.dp,
                    enabled = enabled,
                )
            }
        }
    }
}

/** 놓쳤을 때 할 일과, 억제 화면·잠금 화면에 띄울 문구·이미지 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissActionPanel(
    method: VerifyMethod,
    missAction: MissAction,
    onMissAction: (MissAction) -> Unit,
    frozen: Boolean,
    nagMessages: List<String>,
    nagInput: String,
    onNagInput: (String) -> Unit,
    onAddNagMessage: () -> Unit,
    onRemoveNagMessage: (String) -> Unit,
    nagImages: List<String>,
    importingImage: Boolean,
    onPickImage: () -> Unit,
    onRemoveImage: (String) -> Unit,
) {
    FocusPanel(title = "놓쳤을 때", index = "04", modifier = Modifier.fillMaxWidth()) {
        val allowed = method.allowedMissActions()
        FocusSegmented(
            MissAction.entries.map { action ->
                SegmentItem(
                    action.title,
                    selected = missAction == action,
                    onClick = { onMissAction(action) },
                    enabled = !frozen && action in allowed,
                    icon = action.icon,
                )
            }
        )
        Text(
            when (missAction) {
                MissAction.NOTIFY -> "놓쳐도 막지 않아요. 알림과 기록으로만 챙겨요."
                MissAction.NAG ->
                    "놓치면 아래 문구와 이미지를 화면 가득 띄워요. 인증해야 멈추고, " +
                        "그전까지는 다른 앱을 쓰다가도 ${NagPolicy.INTERVAL_MS / 60_000}분마다 다시 덮어요."
                MissAction.LOCK ->
                    "놓치면 인증할 때까지 허용한 앱 말고는 못 써요. 화면에는 아래 문구와 이미지가 떠요."
            } + if (MissAction.LOCK !in allowed) " 탭 체크는 버튼 한 번으로 풀려서 앱 잠금은 고를 수 없어요." else "",
            style = MaterialTheme.typography.bodySmall,
            color = Chrome.chromeIndigo,
        )
        if (missAction != MissAction.NOTIFY) {
            FocusNotice(
                "긴급전화, ${SafetyPolicy.EMERGENCY_DELAY_MS / 60_000}분 비상 해제, " +
                    "${SafetyPolicy.MAX_LOCK_DURATION_MS / 3_600_000}시간 자동 해제는 항상 쓸 수 있어요",
                tone = Tone.Info,
                icon = PixelIcons.Shield,
            )
            SubHeader(
                "흘러갈 문구",
                "첫 문구는 가운데 크게, 모두 ${DANMAKU_INTERVAL_MS / 60_000}분마다 화면을 가로질러 지나가요 · " +
                    "비워 두면 \"$DEFAULT_NAG_MESSAGE\"",
            )
            if (nagMessages.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    nagMessages.forEach { text ->
                        FocusChip(text, selected = true, onClick = { onRemoveNagMessage(text) }, trailingIcon = Icons.Default.Close)
                    }
                }
            }
            if (nagMessages.size < NagContent.MAX_MESSAGES) {
                FocusTextField(
                    nagInput, onNagInput,
                    label = "문구 추가 (${nagMessages.size}/${NagContent.MAX_MESSAGES})",
                    placeholder = DEFAULT_NAG_MESSAGE,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddNagMessage() }),
                    trailing = {
                        FocusIconButton(
                            Icons.Default.Add,
                            contentDescription = "문구 추가",
                            onClick = onAddNagMessage,
                            enabled = nagInput.isNotBlank(),
                            tint = Chrome.carbon,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SubHeader("흘러갈 사진", "선택 · 최대 ${NagContent.MAX_IMAGES}장 · 문구와 함께 크기가 제각각으로 지나가요")
            if (nagImages.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    nagImages.forEach { path -> NagThumbnail(path, onRemove = { onRemoveImage(path) }) }
                }
            }
            if (nagImages.size < NagContent.MAX_IMAGES) {
                FocusButton(
                    if (importingImage) "불러오는 중…" else "사진 고르기 (${nagImages.size}/${NagContent.MAX_IMAGES})",
                    onClick = onPickImage,
                    modifier = Modifier.fillMaxWidth(),
                    style = FocusButtonStyle.Outline,
                    icon = Icons.Default.Add,
                    height = 48.dp,
                    enabled = !importingImage,
                )
            }
        }
    }
}

/** 고른 사진 작은 칸. 오른쪽 위 X로 뺀다 */
@Composable
private fun NagThumbnail(path: String, onRemove: () -> Unit) {
    val image = rememberNagImage(path, maxSide = 240)
    Box(
        Modifier
            .size(76.dp)
            .clip(ChromeShapes.small)
            .background(Chrome.sky)
            .border(1.dp, Chrome.hairline, ChromeShapes.small),
    ) {
        image?.let {
            Image(it, contentDescription = "고른 사진", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(26.dp)
                .background(Chrome.carbon)
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "사진 빼기", tint = Chrome.white, modifier = Modifier.size(16.dp))
        }
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

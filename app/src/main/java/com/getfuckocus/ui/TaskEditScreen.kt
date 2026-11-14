package com.getfuckocus.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.getfuckocus.ui.components.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.getfuckocus.container
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.NagContent
import com.getfuckocus.data.nagImagePaths
import com.getfuckocus.data.nagMessages
import com.getfuckocus.data.allowedMissActions
import com.getfuckocus.data.saveProblem
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.collectAsState
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.requiredLabels
import com.getfuckocus.hasPermission
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.task.editFreezeReason
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusDialog
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.TimeWheelDialog
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.halftone
import kotlinx.coroutines.launch


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
    var iconKey by rememberSaveable { mutableStateOf<String?>(null) }
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
            iconKey = t.iconKey
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
            iconKey = iconKey,
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
        title, iconKey, startMinute, durationMinutes, repeatDays, verifyMethod, remindAtStart, remindBeforeMinutes,
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

            BasicInfoPanel(
                isNew = isNew,
                title = title,
                onTitle = { title = it },
                iconKey = iconKey,
                onIcon = { iconKey = it },
                enabled = enabled,
                onEnabled = { enabled = it },
                frozen = frozen,
            )

            VerifyMethodPanel(
                verifyMethod = verifyMethod,
                onSelectMethod = ::selectMethod,
                frozen = frozen,
                nfcTags = nfcTags,
                nfcTagId = nfcTagId,
                onPickTag = { nfcTagId = it },
                onAddTag = onAddTag,
                labels = labels,
                labelInput = labelInput,
                onLabelInput = { labelInput = it },
                onAddLabel = ::addLabel,
                onRemoveLabel = { labels.remove(it) },
                suggestions = suggestions,
            )

            SchedulePanel(
                startMinute = startMinute,
                durationMinutes = durationMinutes,
                onDuration = { durationMinutes = it },
                onPick = { picking = it },
                repeatDays = repeatDays,
                onRepeatDays = { repeatDays = it },
                missAction = missAction,
                frozen = frozen,
                remindAtStart = remindAtStart,
                onRemindAtStart = { remindAtStart = it },
                remindBeforeMinutes = remindBeforeMinutes,
                onRemindBefore = { remindBeforeMinutes = it },
                notificationsGranted = notificationsGranted,
                onRequestNotifications = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )

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
            if (usesPlace) PlacePanel(
                verifyMethod = verifyMethod,
                geo = geo,
                radiusMeters = radiusMeters,
                onRadius = { radiusMeters = it },
                expanded = placeOpen || verifyMethod == VerifyMethod.LOCATION,
                onToggle = { placeExpanded = !placeOpen },
                frozen = frozen,
                onPickOnMap = ::openPlacePicker,
                onUseCurrentLocation = {
                    if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) fetchLocation()
                    else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onClearPlace = { geo = null },
            )
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
                    container.achievementDao.deleteForTask(toDelete.id)
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

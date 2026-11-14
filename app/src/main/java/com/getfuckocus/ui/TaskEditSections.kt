package com.getfuckocus.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.NagContent
import com.getfuckocus.data.nagMessages
import androidx.compose.foundation.clickable
import com.getfuckocus.data.allowedMissActions
import com.getfuckocus.safety.SafetyPolicy
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import java.time.DayOfWeek
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusChip
import com.getfuckocus.ui.components.FocusSegmented
import com.getfuckocus.ui.components.FocusTab
import androidx.compose.foundation.border
import com.getfuckocus.ui.components.SegmentItem
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.components.FocusSwitch
import com.getfuckocus.ui.components.FocusTextField
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.recessed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.data.NfcTagEntity
import com.getfuckocus.ui.components.TaskIconMark
import com.getfuckocus.ui.theme.TaskIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

// 과제 편집 화면의 구역들. 상태는 TaskEditScreen이 들고, 여기서는 그리기와 이벤트 전달만 한다

internal enum class TimeField { START, DURATION }

private val DURATION_PRESETS = listOf(10, 30, 60, 90, 120, 180)
private val RADIUS_PRESETS = listOf(50f, 100f, 150f, 300f, 500f)
private val REMIND_BEFORE_PRESETS = listOf(0, 5, 15, 30)
internal const val DEFAULT_RADIUS_M = 150f

/** 01 기본 정보: 이름, 아이콘, 사용 여부 */
@Composable
internal fun BasicInfoPanel(
    isNew: Boolean,
    title: String,
    onTitle: (String) -> Unit,
    iconKey: String?,
    onIcon: (String?) -> Unit,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    frozen: Boolean,
) {
    FocusPanel(title = "기본 정보", index = "01", modifier = Modifier.fillMaxWidth()) {
        if (isNew) {
            Text("어떤 일을 할까요?", style = MaterialTheme.typography.titleMedium)
        }
        FocusTextField(
            title, onTitle,
            label = "과제 이름",
            placeholder = "예: 아침 운동",
            modifier = Modifier.fillMaxWidth(),
        )
        IconPicker(selected = TaskIcon.fromKey(iconKey), onSelect = { onIcon(it?.name) })
        SettingRow(title = "사용", description = "끄면 알림·억제 화면·잠금이 모두 멈춰요") {
            FocusSwitch(checked = enabled, onCheckedChange = onEnabled, enabled = !frozen)
        }
    }
}

/** 02 인증 방식: 방식 타일, NFC 태그 고르기, 사진 조건(라벨) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VerifyMethodPanel(
    verifyMethod: VerifyMethod,
    onSelectMethod: (VerifyMethod) -> Unit,
    frozen: Boolean,
    nfcTags: List<NfcTagEntity>,
    nfcTagId: Long?,
    onPickTag: (Long) -> Unit,
    onAddTag: () -> Unit,
    labels: List<String>,
    labelInput: String,
    onLabelInput: (String) -> Unit,
    onAddLabel: (String) -> Unit,
    onRemoveLabel: (String) -> Unit,
    suggestions: List<String>,
) {
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
                        onClick = { onSelectMethod(method) },
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
                            onClick = { onPickTag(tag.id) },
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
                            onClick = { onRemoveLabel(label) },
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
                    if (input.endsWith(",")) onAddLabel(input) else onLabelInput(input)
                },
                label = "라벨 추가",
                placeholder = "영문, 예: Dog",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddLabel(labelInput) }),
                enabled = !frozen,
                trailing = {
                    FocusIconButton(
                        Icons.Default.Add,
                        contentDescription = "라벨 추가",
                        onClick = { onAddLabel(labelInput) },
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
                            onClick = { onAddLabel(suggestion) },
                            leadingIcon = Icons.Default.Add,
                        )
                    }
                }
            }
        }
    }
}

/** 03 일정 설정: 시작 시각, 소요 시간, 반복 요일, 알림 */
@Composable
internal fun SchedulePanel(
    startMinute: Int,
    durationMinutes: Int,
    onDuration: (Int) -> Unit,
    onPick: (TimeField) -> Unit,
    repeatDays: Int,
    onRepeatDays: (Int) -> Unit,
    missAction: MissAction,
    frozen: Boolean,
    remindAtStart: Boolean,
    onRemindAtStart: (Boolean) -> Unit,
    remindBeforeMinutes: Int,
    onRemindBefore: (Int) -> Unit,
    notificationsGranted: Boolean,
    onRequestNotifications: () -> Unit,
) {
    FocusPanel(title = "일정 설정", index = "03", trailing = { CurrentTimeText() }, modifier = Modifier.fillMaxWidth()) {
        FocusCard(
            modifier = Modifier.fillMaxWidth(),
            color = Chrome.white,
            onClick = if (frozen) null else ({ onPick(TimeField.START) }),
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
                onClick = { onDuration(preset) },
                enabled = !frozen,
            )
        } + SegmentItem(
            if (durationMinutes in DURATION_PRESETS) "직접" else TaskSchedule.formatDuration(durationMinutes),
            selected = durationMinutes !in DURATION_PRESETS,
            onClick = { onPick(TimeField.DURATION) },
            enabled = !frozen,
            icon = Icons.Default.Edit,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            durationKeys.chunked(4).forEach { FocusSegmented(it) }
        }

        SubHeader("반복 요일", "요일을 눌러 켜고 꺼요")
        FocusSegmented(
            listOf(
                "매일" to RepeatDays.EVERY_DAY,
                "평일" to RepeatDays.WEEKDAYS,
                "주말" to RepeatDays.WEEKEND,
            ).map { (name, mask) ->
                SegmentItem(name, selected = repeatDays == mask, onClick = { onRepeatDays(mask) }, enabled = !frozen)
            }
        )
        // 요일은 여러 개를 켤 수 있다. 켠 요일이 눌린 키로 보인다
        FocusSegmented(
            DayOfWeek.entries.map { day ->
                SegmentItem(
                    TaskSchedule.dayName(day),
                    selected = RepeatDays.contains(repeatDays, day),
                    onClick = { onRepeatDays(repeatDays xor RepeatDays.bit(day)) },
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
            FocusSwitch(checked = remindAtStart, onCheckedChange = onRemindAtStart)
        }
        Text("마감 전 알림", style = MaterialTheme.typography.labelLarge)
        FocusSegmented(
            REMIND_BEFORE_PRESETS.map { minutes ->
                SegmentItem(
                    if (minutes == 0) "끄기" else "${minutes}분 전",
                    selected = remindBeforeMinutes == minutes,
                    onClick = { onRemindBefore(minutes) },
                )
            }
        )
        if (!notificationsGranted && (remindAtStart || remindBeforeMinutes > 0)) {
            FocusNotice(
                "알림 권한이 없어서 알림이 오지 않아요",
                tone = Tone.Warning,
                icon = PixelIcons.Bell,
                actionLabel = "허용",
                onAction = onRequestNotifications,
            )
        }
    }
}

/** 05 장소 설정: 지도·현재 위치로 정하기, 반경 */
@Composable
internal fun PlacePanel(
    verifyMethod: VerifyMethod,
    geo: Pair<Double, Double>?,
    radiusMeters: Float,
    onRadius: (Float) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    frozen: Boolean,
    onPickOnMap: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onClearPlace: () -> Unit,
) {
    FocusPanel(
        title = "장소 설정",
        index = "05",
        subtitle = when {
            geo != null -> "지정됨 · 반경 ${radiusMeters.toInt()}m"
            verifyMethod == VerifyMethod.LOCATION -> "필수 · 위치 인증은 여기서 체크해야 통과해요"
            else -> "선택 · 지정한 곳 근처에서만 통과해요"
        },
        expanded = expanded,
        onToggle = onToggle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val currentGeo = geo
        if (currentGeo == null) {
            Text(
                "TIP. 지금 있는 곳이 아니어도 지도에서 고를 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = Chrome.chromeIndigo,
            )
            FocusButton(
                "지도에서 고르기",
                onClick = onPickOnMap,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.LocationOn,
                enabled = !frozen,
            )
            FocusButton(
                "지금 있는 곳으로 정하기",
                onClick = onUseCurrentLocation,
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
                onChange = onPickOnMap,
                onRemove = onClearPlace,
            )
            SubHeader("반경")
            FocusSegmented(
                RADIUS_PRESETS.map { preset ->
                    SegmentItem(
                        "${preset.toInt()}m",
                        selected = radiusMeters == preset,
                        onClick = { onRadius(preset) },
                        enabled = !frozen,
                    )
                }
            )
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
internal fun MissActionPanel(
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
                MissAction.NOTIFY -> "놓쳐도 막지 않고 알림과 기록으로만 챙겨요."
                MissAction.NAG ->
                    "폰은 그대로 쓸 수 있어요. 대신 인증할 때까지 " +
                        "${DANMAKU_INTERVAL_MS / 60_000}분마다 아래 문구와 사진이 화면을 지나가요."
                MissAction.LOCK ->
                    "인증할 때까지 허용한 앱만 쓸 수 있어요. 잠금 화면에 아래 문구가 떠요."
            } + if (MissAction.LOCK !in allowed) " 탭 체크는 버튼 한 번이면 풀려서 앱 잠금은 고를 수 없어요." else "",
            style = MaterialTheme.typography.bodySmall,
            color = Chrome.chromeIndigo,
        )
        if (missAction != MissAction.NOTIFY) {
            Text(
                "TIP. 긴급전화, ${SafetyPolicy.EMERGENCY_DELAY_MS / 60_000}분 비상 해제, " +
                    "${SafetyPolicy.MAX_LOCK_DURATION_MS / 3_600_000}시간 자동 해제는 언제든 쓸 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = Chrome.carbon,
            )
            SubHeader(
                "흘러갈 문구",
                "첫 문구가 가장 크게 지나가요 · 비워 두면 \"$DEFAULT_NAG_MESSAGE\"",
            )
            // 입력칸을 위에 두고, 넣은 문구는 그 아래에 한 줄씩 쌓는다
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
            if (nagMessages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    nagMessages.forEachIndexed { index, text ->
                        NagMessageRow(index + 1, text, onRemove = { onRemoveNagMessage(text) })
                    }
                }
            }

            SubHeader("흘러갈 사진", "선택 · 최대 ${NagContent.MAX_IMAGES}장")
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

/**
 * 넣은 문구 한 줄: 판에 파인 칸에 순번 + 문구 + 빼기 버튼.
 * 칸보다 긴 문구는 글자가 천천히 옆으로 흘러가며 가려진 뒷부분까지 되풀이해 보여 준다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NagMessageRow(number: Int, text: String, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .recessed(Chrome.platinum)
            .padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(number),
            style = DigitStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
            color = Chrome.chromeIndigo,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Chrome.carbon,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 1_200, initialDelayMillis = 1_000),
        )
        FocusIconButton(Icons.Default.Close, contentDescription = "'$text' 빼기", onClick = onRemove, tint = Chrome.chromeIndigo)
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

/** 과제 아이콘 고르기. 업적 알림에도 이 아이콘이 나온다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: TaskIcon?, onSelect: (TaskIcon?) -> Unit) {
    SubHeader("아이콘", "연속 달성 업적을 받으면 이 아이콘에 별과 오라가 붙어요")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IconTile(selected == null, "아이콘 없음", onClick = { onSelect(null) }) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Chrome.chromeIndigo, modifier = Modifier.size(20.dp))
        }
        TaskIcon.entries.forEach { icon ->
            IconTile(selected == icon, icon.label, onClick = { onSelect(icon) }) {
                TaskIconMark(icon, Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun IconTile(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(ChromeShapes.small)
            .background(if (selected) Chrome.amber else Chrome.white)
            .border(2.dp, if (selected) Chrome.carbon else Chrome.platinum, ChromeShapes.small)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) { content() }
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
internal suspend fun recentLabelSuggestions(dao: TaskDao): List<String> =
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

package com.getfuckocus.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.getfuckocus.ui.theme.plate
import kotlinx.coroutines.delay
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getfuckocus.container
import com.getfuckocus.data.NfcTagEntity
import com.getfuckocus.nfc.NfcAvailability
import com.getfuckocus.nfc.NfcTagScanner
import com.getfuckocus.nfc.NfcTags
import com.getfuckocus.nfc.ScanState
import com.getfuckocus.nfc.isDebuggable
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusDialog
import com.getfuckocus.ui.components.FocusEmptyState
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.components.FocusFab
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusTextField
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.Dimens
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.theme.bevel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val REGISTERED_FORMAT = DateTimeFormatter.ofPattern("yyyy.M.d 등록")

/**
 * 기록 옆 쪽. 등록한 NFC 태그를 보여 주고, + 버튼으로 새 태그를 등록한다.
 * 이 쪽이 보이는 동안([active]) 태그를 찍으면 해당 카드에 초록 스캔 효과가 지나간다.
 */
@Composable
fun NfcTagListScreen(onAdd: () -> Unit, active: Boolean = true) {
    val context = LocalContext.current
    val dao = context.container.nfcTagDao
    val taskDao = context.container.taskDao
    val tags by remember { dao.observeAll() }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf<NfcTagEntity?>(null) }
    val listState = rememberLazyListState()

    // 찍힌 태그: (태그 id, 몇 번째 찍힘). 같은 태그를 또 찍어도 효과가 다시 돌도록 번호를 올린다
    var scanned by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var unknownNotice by remember { mutableStateOf<Int?>(null) }
    var scanCount by remember { mutableIntStateOf(0) }
    fun onScanned(uid: String) {
        val list = tags.orEmpty()
        val index = list.indexOfFirst { it.uid == uid }
        scanCount++
        if (index < 0) {
            unknownNotice = scanCount
            return
        }
        scanned = list[index].id to scanCount
        // 목록 밖에 있던 카드면 보이는 곳까지 굴려 준다 (빈 상태 칸이 없으니 카드 순서 = 항목 순서)
        scope.launch { listState.animateScrollToItem(index) }
    }
    // 다른 쪽(홈·기록)을 보고 있을 때는 리더 모드를 켜지 않는다
    NfcTagScanner(enabled = active, onTag = ::onScanned)
    LaunchedEffect(unknownNotice) {
        if (unknownNotice != null) {
            delay(UNKNOWN_NOTICE_MS)
            unknownNotice = null
        }
    }
    val debug = remember { context.isDebuggable() }

    Scaffold(
        topBar = {
            FocusTopBar(
                "NFC 태그",
                subtitle = tags?.let { "${it.size}개 등록 · 체크할 때 찍을 태그예요" },
            )
        },
        floatingActionButton = {
            FocusFab(Icons.Default.Add, contentDescription = "NFC 태그 추가", onClick = onAdd)
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = Dimens.fabClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tags?.isEmpty() == true) {
                item {
                    FocusEmptyState(
                        title = "아직 등록한 태그가 없어요",
                        body = "약통, 책상, 현관처럼 과제하는 곳에 NFC 스티커를 붙여 두세요. " +
                            "교통카드나 출입카드도 돼요",
                        icon = PixelIcons.Nfc,
                        actionLabel = "첫 태그 등록하기",
                        onAction = onAdd,
                    )
                }
            }
            items(tags.orEmpty(), key = { it.id }) { tag ->
                TagCard(
                    tag,
                    scanKey = scanned?.takeIf { it.first == tag.id }?.second,
                    onDelete = { confirmDelete = tag },
                    // 실물 태그 없이 효과를 시험하는 디버그 빌드 전용: 카드를 길게 누르면 찍은 것처럼
                    onDebugScan = if (debug) ({ onScanned(tag.uid) }) else null,
                )
            }
        }
        AnimatedVisibility(
            visible = unknownNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
        ) {
            FocusNotice("등록하지 않은 태그예요. + 를 눌러 등록할 수 있어요", tone = Tone.Warning, icon = PixelIcons.Nfc)
        }
        }
    }

    confirmDelete?.let { tag ->
        // 과제가 쓰는 태그를 지우면 그 과제를 인증할 방법이 사라지므로 먼저 과제에서 바꾸게 한다
        val usedBy by produceState<List<String>?>(null, tag) { value = taskDao.titlesUsingTag(tag.id) }
        val users = usedBy ?: return@let
        if (users.isNotEmpty()) {
            FocusDialog(
                title = "'${tag.name}' 태그는 지울 수 없어요",
                onDismiss = { confirmDelete = null },
                confirmText = "확인",
                dismissText = "닫기",
                onConfirm = { confirmDelete = null },
            ) {
                Text(
                    "${users.joinToString(", ") { "'$it'" }} 과제에서 쓰는 중이에요. " +
                        "과제 편집에서 다른 태그나 인증 방식으로 바꾼 뒤 지워 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@let
        }
        FocusDialog(
            title = "'${tag.name}' 태그를 지울까요?",
            onDismiss = { confirmDelete = null },
            confirmText = "지우기",
            danger = true,
            onConfirm = {
                confirmDelete = null
                scope.launch { dao.delete(tag) }
            },
        ) {
            Text("다시 쓰려면 태그를 새로 등록해야 해요.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 등록되지 않은 태그를 찍었을 때 안내를 보여 주는 시간 */
private const val UNKNOWN_NOTICE_MS = 2_500L
/** 스캔 효과: 초록 띠가 왼쪽에서 오른쪽으로 지나가는 시간, 그 뒤 카드가 한 번 깜빡이고 사라지는 시간 */
private const val SCAN_SWEEP_MS = 520
private const val SCAN_BLINK_MS = 420

/**
 * 등록한 태그 한 장. 한 줄에 아이콘 · 이름/등록일·ID · 삭제 아이콘 버튼을 놓아 높이를 줄였다.
 * [scanKey]가 바뀔 때마다(이 태그가 찍힐 때마다) 초록 스캔 효과를 한 번 보여 준다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagCard(tag: NfcTagEntity, scanKey: Int?, onDelete: () -> Unit, onDebugScan: (() -> Unit)?) {
    val sweep = remember { Animatable(0f) }
    val blink = remember { Animatable(0f) }
    LaunchedEffect(scanKey) {
        if (scanKey == null) return@LaunchedEffect
        blink.snapTo(0f)
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(SCAN_SWEEP_MS, easing = FastOutSlowInEasing))
        // 다 지나가면 카드 전체가 초록으로 한 번 반짝이고 사라진다
        blink.snapTo(1f)
        sweep.snapTo(0f)
        blink.animateTo(0f, tween(SCAN_BLINK_MS))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .plate(Chrome.platinum)
            .then(
                if (onDebugScan != null) Modifier.combinedClickable(onClick = {}, onLongClick = onDebugScan)
                else Modifier
            )
            .drawWithContent {
                drawContent()
                val p = sweep.value
                if (p > 0f && p < 1f) {
                    // 초록 띠: 카드 왼쪽 밖에서 들어와 오른쪽 밖으로 나간다
                    val band = size.width * 0.45f
                    val x = -band + (size.width + band) * p
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.7f to SCAN_GREEN.copy(alpha = 0.55f),
                            1f to SCAN_GREEN.copy(alpha = 0.9f),
                            startX = x,
                            endX = x + band,
                        ),
                        // 띠 범위만 칠한다 (그라데이션 끝 색이 띠 오른쪽까지 번지지 않게)
                        topLeft = Offset(x, 0f),
                        size = Size(band, size.height),
                    )
                    drawRect(SCAN_GREEN, topLeft = Offset(x + band - 2.dp.toPx(), 0f), size = Size(2.dp.toPx(), size.height))
                }
                if (blink.value > 0f) drawRect(SCAN_GREEN.copy(alpha = 0.35f * blink.value))
            }
            .padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SdCardIcon()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(tag.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                Instant.ofEpochMilli(tag.createdAt).atZone(ZoneId.systemDefault()).format(REGISTERED_FORMAT) +
                    "  ·  " + tag.uid,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Chrome.chromeIndigo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        DeleteIconButton(label = "'${tag.name}' 태그 지우기", onClick = onDelete)
    }
}

/** SD 카드 모양: 오른쪽 위 모서리를 비스듬히 자른 세로 카드 */
private val SdCardShape = CutCornerShape(topEnd = 11.dp, topStart = 2.dp, bottomStart = 2.dp, bottomEnd = 2.dp)

/**
 * 태그 카드 왼쪽의 아이콘 칸. 저장 매체(SD 카드)처럼 생긴 판에 금색 접점을 박고 NFC 표시를 찍는다.
 * 위쪽 접점 줄, 아래쪽 라벨 자리(NFC 아이콘), 아래 그림자 선으로 끼워 넣는 카드 느낌을 낸다.
 */
@Composable
private fun SdCardIcon() {
    Box(
        Modifier
            .size(width = 36.dp, height = 44.dp)
            .clip(SdCardShape)
            .background(Chrome.carbon)
            .padding(start = 1.5.dp, top = 1.5.dp, end = 1.5.dp, bottom = 3.dp)
            .clip(SdCardShape)
            .background(Chrome.sky),
    ) {
        // 접점: 자른 모서리를 피해 왼쪽부터 금색 막대를 나란히
        Row(
            Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(SD_PINS) {
                Box(Modifier.size(width = 3.dp, height = 7.dp).background(SD_GOLD))
            }
        }
        // 라벨 자리: 접점 아래 흰 스티커에 NFC 표시
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
                .size(width = 26.dp, height = 24.dp)
                .clip(ChromeShapes.control)
                .background(Chrome.white.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PixelIcons.Nfc, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(18.dp))
        }
    }
}

private const val SD_PINS = 4
private val SD_GOLD = Color(0xFFE9B94A)

/** 휴지통 아이콘만 있는 작은 입체 버튼. 누르면 아래로 눌려 들어간다 */
@Composable
private fun DeleteIconButton(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 2.dp else 0.dp, label = "deleteSink")
    Box(
        Modifier
            .size(44.dp)
            .offset(y = sink)
            .clip(ChromeShapes.small)
            .background(Chrome.redSoft)
            .bevel(
                highlight = if (pressed) Color.Transparent else Chrome.highlight,
                shadow = Chrome.red.copy(alpha = 0.6f),
                shadowWidth = if (pressed) 1.dp else 3.dp,
            )
            .border(1.dp, Chrome.red.copy(alpha = 0.45f), ChromeShapes.small)
            .clickable(interaction, indication = null, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = Chrome.redInk, modifier = Modifier.size(22.dp))
    }
}

/** 스캔 효과 초록: 앱의 성공 초록보다 밝게 해서 회색 카드 위에서 잘 보이게 한다 */
private val SCAN_GREEN = Color(0xFF3DDC84)

/**
 * 태그를 두 번 찍어 ID가 같은지 확인하고, 이름을 붙여 저장한다.
 * 디버그 빌드에서는 실물 태그 없이 시험할 수 있게 "가짜 태그 찍기" 버튼이 보인다.
 */
@Composable
fun NfcTagAddScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val dao = context.container.nfcTagDao
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanState>(ScanState.Waiting()) }
    var name by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    fun handleScan(uid: String) = scope.launch {
        val registered = dao.findByUid(uid)?.name
        state = NfcTags.onScan(state, uid, registered)
    }

    val availability = NfcTagScanner(enabled = state !is ScanState.Ready, onTag = { handleScan(it) })

    // 이름을 비워 두지 않도록, 확인이 끝나면 "태그 N"을 미리 채워 둔다
    val ready = state as? ScanState.Ready
    LaunchedEffect(ready) {
        if (ready != null && name.isBlank()) name = "태그 ${dao.count() + 1}"
    }

    fun save(uid: String) {
        if (saving) return
        saving = true
        scope.launch {
            // 저장하기 전에 다른 곳에서 같은 태그를 등록했을 수 있으니 한 번 더 확인한다
            val registered = dao.findByUid(uid)
            if (registered != null) {
                state = ScanState.Waiting(notice = "이미 '${registered.name}'(으)로 등록된 태그예요")
            } else {
                dao.insert(NfcTagEntity(uid = uid, name = name.trim()))
                onDone()
            }
            saving = false
        }
    }

    Scaffold(topBar = { FocusTopBar("NFC 태그 추가", onBack = onDone) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val step = when (state) {
                is ScanState.Waiting -> 0
                is ScanState.Confirming -> 1
                is ScanState.Ready -> 2
            }
            StepIndicator(step)

            when (availability) {
                // 쓸 수 없는 기기에서는 대기 안내 대신 이유를 크게 보여 준다
                NfcAvailability.Unsupported -> if (state !is ScanState.Ready) FocusEmptyState(
                    title = "이 기기에서는 NFC를 쓸 수 없어요",
                    body = "NFC가 없는 기기예요. NFC가 있는 폰에서 등록해 주세요",
                    icon = PixelIcons.Nfc,
                    tone = Tone.Danger,
                )
                NfcAvailability.Off -> FocusNotice(
                    "켜고 돌아오면 바로 읽을 수 있어요",
                    title = "NFC가 꺼져 있어요",
                    tone = Tone.Warning,
                    icon = PixelIcons.Nfc,
                    actionLabel = "NFC 설정",
                    onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                )
                NfcAvailability.Ready -> Unit
            }

            if (availability != NfcAvailability.Unsupported || state is ScanState.Ready) when (val s = state) {
                is ScanState.Waiting -> ScanPanel(
                    title = "폰 뒷면에 태그를 대 주세요",
                    body = "보통 카메라 아래쪽에 NFC 안테나가 있어요. 태그를 대고 1초쯤 기다려 주세요",
                    notice = s.notice,
                    scanning = availability == NfcAvailability.Ready,
                )
                is ScanState.Confirming -> ScanPanel(
                    title = "한 번 더 대 주세요",
                    body = "태그를 폰에서 뗐다가 다시 대 주세요. 같은 태그인지 확인해요",
                    uid = s.uid,
                    scanning = availability == NfcAvailability.Ready,
                )
                is ScanState.Ready -> FocusPanel(title = "인식 완료", subtitle = "이름을 붙여 주세요", icon = Icons.Default.CheckCircle) {
                    FocusNotice(s.uid, title = "같은 태그인지 확인했어요", tone = Tone.Success)
                    FocusTextField(
                        value = name,
                        onValueChange = { name = it.take(NAME_MAX_LENGTH) },
                        label = "태그 이름",
                        placeholder = "예: 책상, 약통, 현관",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) save(s.uid) }),
                    )
                    FocusButton(
                        "저장",
                        onClick = { save(s.uid) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank() && !saving,
                        height = 56.dp,
                    )
                    FocusButton(
                        "다시 찍기",
                        onClick = { state = ScanState.Waiting() },
                        modifier = Modifier.fillMaxWidth(),
                        style = FocusButtonStyle.Outline,
                        icon = Icons.Default.Refresh,
                        height = 48.dp,
                    )
                }
            }

            if (context.isDebuggable() && state !is ScanState.Ready) {
                // 실물 태그가 없을 때 등록 흐름을 시험하는 용도. 출시 빌드에는 보이지 않는다.
                // NFC가 없는 에뮬레이터에서 쓰는 도구라 NFC 미지원이어도 켜 둔다
                FocusButton(
                    "가짜 태그 찍기 (디버그 빌드 전용)",
                    onClick = {
                        val current = state
                        handleScan(if (current is ScanState.Confirming) current.uid else NfcTags.fakeUid())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = FocusButtonStyle.Outline,
                )
            }
        }
    }
}

private const val NAME_MAX_LENGTH = 20

@Composable
private fun ScanPanel(
    title: String,
    body: String,
    scanning: Boolean,
    notice: String? = null,
    uid: String? = null,
) {
    FocusPanel(title = title, subtitle = if (scanning) "인식 대기 중" else "NFC를 켜면 인식을 시작해요", icon = PixelIcons.Nfc) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 인식 대기: 태그 아이콘 주위로 링이 돈다
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                if (scanning) FocusSpinner(Modifier.size(112.dp), strokeWidth = 6.dp)
                Box(
                    Modifier.size(84.dp).clip(CircleShape).background(if (scanning) Chrome.sky else Chrome.white),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        PixelIcons.Nfc, contentDescription = null,
                        tint = if (scanning) Chrome.carbon else Chrome.hairline,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            uid?.let { UidText(it) }
        }
        notice?.let { FocusNotice(it, tone = Tone.Danger, icon = Icons.Default.Warning) }
    }
}

/** 1 태그 대기 → 2 한 번 더 → 완료, 세 칸짜리 진행 표시 */
@Composable
private fun StepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("1 태그 대기", "2 한 번 더", "완료").forEachIndexed { index, label ->
            val done = index < step
            val current = index == step
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(ChromeShapes.control)
                    .background(
                        when {
                            done -> Chrome.ice
                            current -> Chrome.amber
                            else -> Chrome.periwinkle
                        }
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Chrome.teal, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done) Chrome.teal else Chrome.carbon,
                )
            }
        }
    }
}

@Composable
private fun UidText(uid: String) {
    Text(
        uid,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

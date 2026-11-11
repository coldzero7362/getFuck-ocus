package com.getfuckocus.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.getfuckocus.ui.components.FocusFab
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusTextField
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.theme.Chrome
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val REGISTERED_FORMAT = DateTimeFormatter.ofPattern("yyyy.M.d 등록")

/** 기록 옆 쪽. 등록한 NFC 태그를 보여 주고, + 버튼으로 새 태그를 등록한다. */
@Composable
fun NfcTagListScreen(onBack: () -> Unit, onAdd: () -> Unit) {
    val dao = LocalContext.current.container.nfcTagDao
    val tags by remember { dao.observeAll() }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf<NfcTagEntity?>(null) }

    Scaffold(
        topBar = {
            FocusTopBar(
                "NFC 태그",
                subtitle = tags?.let { "${it.size}개 등록 · 체크할 때 찍을 태그예요" },
                onBack = onBack,
            )
        },
        floatingActionButton = {
            FocusFab(Icons.Default.Add, contentDescription = "NFC 태그 추가", onClick = onAdd)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tags?.isEmpty() == true) {
                item { TagEmptyState(onAdd) }
            }
            items(tags.orEmpty(), key = { it.id }) { tag ->
                TagCard(tag, onDelete = { confirmDelete = tag })
            }
        }
    }

    confirmDelete?.let { tag ->
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

@Composable
private fun TagCard(tag: NfcTagEntity, onDelete: () -> Unit) {
    FocusCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    tag.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tag.uid,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Instant.ofEpochMilli(tag.createdAt).atZone(ZoneId.systemDefault()).format(REGISTERED_FORMAT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FocusIconButton(Icons.Default.Delete, contentDescription = "${tag.name} 지우기", onClick = onDelete)
        }
    }
}

@Composable
private fun TagEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("아직 등록한 태그가 없어요", style = MaterialTheme.typography.titleMedium)
        Text(
            "책상, 약통, 현관처럼 과제를 하는 곳에 NFC 스티커를 붙이고 등록해 두세요.\n" +
                "교통카드나 출입카드도 쓸 수 있어요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        FocusButton("첫 태그 등록하기", onClick = onAdd)
    }
}

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
            when (availability) {
                NfcAvailability.Unsupported -> NoticeCard("이 기기에는 NFC가 없어요. 태그를 읽을 수 없어요")
                NfcAvailability.Off -> NoticeCard(
                    "NFC가 꺼져 있어요. 켜고 돌아오면 바로 읽을 수 있어요",
                    action = "NFC 설정 열기" to {
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    },
                )
                NfcAvailability.Ready -> Unit
            }

            when (val s = state) {
                is ScanState.Waiting -> ScanPanel(
                    step = "1 / 2",
                    title = "폰 뒷면에 태그를 대 주세요",
                    body = "보통 카메라 아래쪽에 NFC 안테나가 있어요. 태그를 대고 1초쯤 기다려 주세요",
                    notice = s.notice,
                    scanning = availability == NfcAvailability.Ready,
                )
                is ScanState.Confirming -> ScanPanel(
                    step = "2 / 2",
                    title = "한 번 더 대 주세요",
                    body = "태그를 폰에서 뗐다가 다시 대 주세요. 같은 태그인지 확인해요",
                    uid = s.uid,
                    scanning = availability == NfcAvailability.Ready,
                )
                is ScanState.Ready -> FocusPanel(title = "확인했어요", subtitle = "이름을 붙여 저장하세요") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Chrome.teal)
                        Spacer(Modifier.width(8.dp))
                        UidText(s.uid)
                    }
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
                    )
                    FocusButton(
                        "다시 찍기",
                        onClick = { state = ScanState.Waiting() },
                        modifier = Modifier.fillMaxWidth(),
                        style = FocusButtonStyle.Ghost,
                        icon = Icons.Default.Refresh,
                        height = 40.dp,
                    )
                }
            }

            if (context.isDebuggable() && state !is ScanState.Ready) {
                // 실물 태그가 없을 때 등록 흐름을 시험하는 용도. 출시 빌드에는 보이지 않는다
                FocusButton(
                    "가짜 태그 찍기 (디버그)",
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
    step: String,
    title: String,
    body: String,
    scanning: Boolean,
    notice: String? = null,
    uid: String? = null,
) {
    FocusPanel(title = title, subtitle = step) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (scanning) FocusSpinner(Modifier.size(48.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            uid?.let { UidText(it) }
        }
        notice?.let {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Chrome.red, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Chrome.red)
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

@Composable
private fun NoticeCard(text: String, action: Pair<String, () -> Unit>? = null) {
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
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            action?.let { (label, onClick) ->
                FocusButton(
                    label, onClick = onClick,
                    style = FocusButtonStyle.Ghost,
                    height = 40.dp,
                    contentColor = colors.onSecondaryContainer,
                )
            }
        }
    }
}

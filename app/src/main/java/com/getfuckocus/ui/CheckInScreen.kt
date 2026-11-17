package com.getfuckocus.ui

import com.getfuckocus.devtools.CheckInTestButtons
import com.getfuckocus.data.nfcTagIdList
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.getfuckocus.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfuckocus.container
import com.getfuckocus.data.NfcTagEntity
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.hasPermission
import com.getfuckocus.nfc.NfcAvailability
import com.getfuckocus.nfc.NfcTagScanner
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.verify.CheckInResult
import com.getfuckocus.verify.Verdict
import kotlinx.coroutines.launch

/**
 * 카메라 없는 인증 화면 (탭 체크, 위치, NFC). 잠금·억제 화면과 알림에서도 여기로 온다.
 * - 탭 체크: 버튼을 누르면 체크
 * - 위치: 버튼을 누르면 지금 위치를 재서 정한 장소 근처인지 본다
 * - NFC: 화면이 떠 있는 동안 태그를 기다렸다가, 이 과제에 고른 태그인지 본다
 */
@Composable
fun CheckInScreen(taskId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    val task by produceState<TaskEntity?>(null, taskId) { value = container.taskDao.get(taskId) }
    val tags by produceState(emptyList<NfcTagEntity>(), task) {
        value = task?.nfcTagIdList().orEmpty().mapNotNull { container.nfcTagDao.get(it) }
    }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CheckInResult?>(null) }
    // 장소 밖 실패 화면을 닫았는지. 다시 체크하면 새 결과로 또 띄운다
    var missDismissed by remember { mutableStateOf(false) }

    fun run(t: TaskEntity, scannedUid: String? = null) {
        if (checking) return
        checking = true
        scope.launch {
            result = container.verificationFlow.checkIn(t, scannedUid)
            missDismissed = false
            checking = false
        }
    }

    var permissionDenied by remember { mutableStateOf(false) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) task?.let { run(it) }
    }
    fun checkHere(t: TaskEntity) {
        if (t.verifyMethod == VerifyMethod.LOCATION && !context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            run(t)
        }
    }

    val t = task
    val passed = result?.verdict == Verdict.Pass
    // NFC는 결과가 나오기 전까지 태그를 기다린다. 실패하면 다시 찍을 수 있게 계속 켜 둔다
    val nfcAvailability = NfcTagScanner(enabled = t?.verifyMethod == VerifyMethod.NFC && !passed && !checking) { uid ->
        t?.let { run(it, uid) }
    }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = t?.title ?: "체크",
                subtitle = t?.let { "${it.verifyMethod.title} · ${TaskSchedule.describeWindow(it)}" },
                onBack = onBack,
            )
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
            if (t == null) return@Column
            val r = result
            when {
                r != null && r.verdict == Verdict.Pass -> PassPanel(r, onBack)
                else -> {
                    when (t.verifyMethod) {
                        VerifyMethod.NFC -> NfcWaitPanel(tags, nfcAvailability, checking)
                        VerifyMethod.LOCATION -> ActionPanel(
                            title = "정한 장소에서 체크해요",
                            body = "누르면 지금 위치가 정한 장소 " +
                                "${t.geoRadiusMeters?.toInt() ?: 0}m 안인지 한 번 확인해요",
                            label = t.verifyMethod.actionLabel,
                            icon = t.verifyMethod.actionIcon,
                            busy = checking,
                            onClick = { checkHere(t) },
                        )
                        else -> ActionPanel(
                            title = "했으면 눌러 주세요",
                            body = "버튼 한 번으로 이번 회차를 기록해요",
                            label = t.verifyMethod.actionLabel,
                            icon = t.verifyMethod.actionIcon,
                            busy = checking,
                            onClick = { checkHere(t) },
                        )
                    }
                    if (permissionDenied) {
                        FocusNotice("위치 권한이 없어 장소를 확인할 수 없어요", tone = Tone.Danger)
                    }
                    (r?.verdict as? Verdict.Fail)?.let { fail ->
                        val miss = r.placeMiss
                        if (miss != null && !missDismissed) {
                            PlaceMissDialog(
                                taskTitle = t.title,
                                miss = miss,
                                otherReasons = placeMissOthers(r),
                                checking = checking,
                                onRetry = { checkHere(t) },
                                onDismiss = { missDismissed = true },
                            )
                        }
                        FocusNotice(
                            fail.reasons.joinToString("\n"),
                            title = "체크하지 못했어요",
                            tone = Tone.Danger,
                            icon = Icons.Default.Warning,
                        )
                    }
                    // 실물 태그 없이 시험하는 버튼. 디버그 빌드(src/debug)에만 있고 배포 빌드에서는 아무것도 그리지 않는다
                    if (t.verifyMethod == VerifyMethod.NFC) {
                        CheckInTestButtons(tags.map { it.uid }) { uid -> run(t, uid) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPanel(
    title: String,
    body: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    busy: Boolean,
    onClick: () -> Unit,
) {
    FocusPanel(title = title, modifier = Modifier.fillMaxWidth()) {
        Text(body, style = MaterialTheme.typography.bodyMedium)
        FocusButton(
            if (busy) "확인하는 중…" else label,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            icon = icon,
            height = 60.dp,
            enabled = !busy,
        )
    }
}

@Composable
private fun NfcWaitPanel(tags: List<NfcTagEntity>, availability: NfcAvailability, busy: Boolean) {
    when (availability) {
        NfcAvailability.Unsupported -> FocusNotice("이 기기에는 NFC가 없어 태그를 읽을 수 없어요", tone = Tone.Warning)
        NfcAvailability.Off -> FocusNotice("NFC가 꺼져 있어요. 설정에서 켜고 돌아오면 바로 읽어요", tone = Tone.Warning)
        NfcAvailability.Ready -> Unit
    }
    FocusPanel(title = "태그를 폰 뒷면에 대 주세요", subtitle = tags.takeIf { it.isNotEmpty() }?.let { list ->
            if (list.size == 1) "'${list[0].name}' 태그" else list.joinToString(", ") { "'${it.name}'" } + " 중 하나"
        }, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (availability == NfcAvailability.Ready || busy) FocusSpinner(Modifier.size(48.dp))
            Text(
                when {
                    busy -> "확인하는 중…"
                    tags.size > 1 -> "고른 태그 중 아무거나 하나를 찍으면 바로 체크돼요"
                    else -> "이 과제에 고른 태그를 찍으면 바로 체크돼요"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PassPanel(result: CheckInResult, onDone: () -> Unit) {
    FocusPanel(title = if (result.late) "늦었지만 기록했어요" else "완료했어요", modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Chrome.teal, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (result.releasedLock) "잠금이 풀렸어요. 다음엔 제때 해 봐요" else "잘했어요! 오늘 회차를 마쳤어요",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        FocusButton("닫기", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

package com.getfuckocus.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.getfuckocus.container
import com.getfuckocus.hasPermission
import com.getfuckocus.lock.BlockerServiceStatus
import com.getfuckocus.lock.NotificationHelper
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.FocusSectionLabel
import com.getfuckocus.ui.components.FocusStatusBadge
import com.getfuckocus.ui.components.FocusSwitch
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.Dimens
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.theme.bevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 설정. 잠금 관리 · 앱 관리 · 인증 관리 · 기타 네 묶음으로 나누고,
 * 항목마다 지금 상태(켜짐/꺼짐, 개수)를 배지로 바로 보여 준다.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAllowedApps: () -> Unit, onOpenTags: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val serviceEnabled = rememberBlockerServiceEnabled()
    val tags by remember { container.nfcTagDao.observeAll() }.collectAsState(initial = null)
    // 잠금 예외 앱 화면과 같은 기준: 설치된 앱 중 허용한 것만 센다 (기본 목록에는 없는 앱도 있다)
    var allowedCount by remember { mutableStateOf<Int?>(null) }
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    LaunchedEffect(resumes) {
        val allowed = container.whitelist.userPackages()
        allowedCount = withContext(Dispatchers.Default) {
            container.whitelist.launchableApps().count { it.packageName in allowed }
        }
    }

    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (granted && serviceEnabled) NotificationHelper.showServiceActive(context)
    }

    Scaffold(topBar = { FocusTopBar(title = "설정", subtitle = "항목을 누르면 자세히 바꿀 수 있어요", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            item { FocusSectionLabel("잠금 관리") }
            item {
                FocusCard(Modifier.fillMaxWidth()) {
                    SettingHeader(
                        icon = PixelIcons.Shield,
                        title = "잠금 서비스",
                        description = "마감을 놓쳤을 때 다른 앱을 잠가요",
                        badge = { OnOffBadge(serviceEnabled) },
                    ) {
                        // 접근성 설정은 시스템 화면에서만 바꿀 수 있어서 스위치는 그 화면으로 보내는 역할만 한다
                        FocusSwitch(checked = serviceEnabled, onCheckedChange = { BlockerServiceStatus.openSettings(context) })
                    }
                    if (!serviceEnabled) {
                        FocusNotice(
                            "꺼져 있으면 마감을 놓쳐도 앱이 잠기지 않고 알림만 와요",
                            tone = Tone.Warning,
                            actionLabel = "켜기",
                            onAction = { BlockerServiceStatus.openSettings(context) },
                        )
                    }
                    Text(
                        "접근성 권한으로 지금 열린 앱의 이름만 확인해요. 화면 내용이나 입력한 글자는 읽지 않고, " +
                            "어떤 데이터도 기기 밖으로 보내지 않아요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Chrome.chromeIndigo,
                    )
                }
            }

            item { FocusSectionLabel("앱 관리") }
            item {
                NavigationCard(
                    icon = PixelIcons.Apps,
                    title = "잠금 예외 앱",
                    description = "잠금 중에도 열 수 있는 앱을 골라요",
                    badge = { allowedCount?.let { FocusStatusBadge("${it}개 허용", Tone.Info, icon = PixelIcons.Apps) } },
                    onClick = onOpenAllowedApps,
                )
            }

            item { FocusSectionLabel("인증 관리") }
            item {
                NavigationCard(
                    icon = PixelIcons.Nfc,
                    title = "NFC 태그",
                    description = "체크할 때 찍을 태그를 등록하고 지워요",
                    badge = {
                        tags?.let { FocusStatusBadge("${it.size}개 등록", Tone.Info, icon = PixelIcons.Nfc) }
                    },
                    onClick = onOpenTags,
                )
            }

            item { FocusSectionLabel("기타") }
            item {
                FocusCard(Modifier.fillMaxWidth()) {
                    SettingHeader(
                        icon = PixelIcons.Bell,
                        title = "알림",
                        description = if (notificationsGranted) "잠금 상태와 과제 알림을 받아요" else "허용하면 시작·마감 알림과 잠금 상태를 알려 줘요",
                        badge = { OnOffBadge(notificationsGranted, onText = "허용됨", offText = "꺼짐") },
                    ) {}
                    if (!notificationsGranted) {
                        FocusNotice(
                            "알림이 꺼져 있으면 마감이 다가와도 알 수 없어요",
                            tone = Tone.Warning,
                            icon = PixelIcons.Bell,
                            actionLabel = "허용",
                            onAction = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                }
            }
            item {
                NavigationCard(
                    icon = PixelIcons.Info,
                    title = "앱 정보 · 삭제",
                    description = "시스템 앱 정보 화면을 열어요. 언제든 지울 수 있어요",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )
            }
        }
    }
}

/** 아이콘 칸 + 제목·설명 + 상태 배지 + 오른쪽 조작부 */
@Composable
private fun SettingHeader(
    icon: ImageVector,
    title: String,
    description: String,
    badge: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
            badge?.let {
                Spacer(Modifier.height(2.dp))
                it()
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun NavigationCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    badge: (@Composable () -> Unit)? = null,
) {
    FocusCard(Modifier.fillMaxWidth(), onClick = onClick) {
        SettingHeader(icon = icon, title = title, description = description, badge = badge) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Chrome.chromeIndigo,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** 게임 메뉴 아이콘 칸: 라이트 블루 판에 점 아이콘 */
@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(44.dp)
            .clip(ChromeShapes.small)
            .background(Chrome.sky)
            .bevel(shadowWidth = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Chrome.carbon, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun OnOffBadge(on: Boolean, onText: String = "켜짐", offText: String = "꺼짐") {
    if (on) FocusStatusBadge(onText, Tone.Success, icon = Icons.Default.CheckCircle)
    else FocusStatusBadge(offText, Tone.Danger, icon = Icons.Default.Close)
}

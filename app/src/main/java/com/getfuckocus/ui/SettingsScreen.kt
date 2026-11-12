package com.getfuckocus.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.material.icons.filled.LocationOn
import androidx.core.app.ActivityCompat
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
 * 설정. 잠금 관리 · 앱 관리 · 권한 · 기타 네 묶음으로 나누고,
 * 항목마다 지금 상태(켜짐/꺼짐, 개수)를 배지로 바로 보여 준다.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAllowedApps: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val serviceEnabled = rememberBlockerServiceEnabled()
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

    fun notificationsAllowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    var notificationsGranted by remember { mutableStateOf(notificationsAllowed()) }
    var cameraGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.CAMERA)) }
    var locationGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) }
    // 시스템 설정에서 권한을 바꾸고 돌아오면 다시 확인한다
    LifecycleResumeEffect(Unit) {
        notificationsGranted = notificationsAllowed()
        cameraGranted = context.hasPermission(Manifest.permission.CAMERA)
        locationGranted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberPermissionRequest { granted ->
        notificationsGranted = granted
        if (granted && serviceEnabled) NotificationHelper.showServiceActive(context)
    }
    val cameraPermission = rememberPermissionRequest { cameraGranted = it }
    val locationPermission = rememberPermissionRequest { locationGranted = it }

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
                        description = "마감을 놓쳤을 때 앱을 잠그거나 억제 화면을 띄워요",
                        badge = { OnOffBadge(serviceEnabled) },
                    ) {
                        // 접근성 설정은 시스템 화면에서만 바꿀 수 있어서 스위치는 그 화면으로 보내는 역할만 한다
                        FocusSwitch(checked = serviceEnabled, onCheckedChange = { BlockerServiceStatus.openSettings(context) })
                    }
                    if (!serviceEnabled) {
                        // 바로 위 스위치가 켜는 역할을 하므로 안내에는 버튼을 따로 달지 않는다
                        FocusNotice(
                            "꺼져 있으면 마감을 놓쳐도 잠금이나 억제 화면 없이 알림만 와요",
                            tone = Tone.Warning,
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

            // 권한은 앱이 직접 끌 수 없어서, 허용 전에는 요청하고 허용 뒤에는 시스템 설정 화면으로 보낸다
            item { FocusSectionLabel("권한") }
            item {
                PermissionCard(
                    icon = PixelIcons.Bell,
                    title = "알림",
                    description = "시작·마감 알림과 잠금 상태를 알려 줘요",
                    granted = notificationsGranted,
                    grantedHint = "누르면 시스템 알림 설정에서 과제 알림, 잠금 알림을 따로 끌 수 있어요",
                    deniedWarning = "알림이 꺼져 있으면 마감이 다가와도 알 수 없어요",
                    onClick = {
                        if (notificationsGranted) openNotificationSettings(context)
                        else notificationPermission(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }
            item {
                PermissionCard(
                    icon = PixelIcons.Camera,
                    title = "카메라",
                    description = "사진 인증 과제를 찍을 때만 써요",
                    granted = cameraGranted,
                    grantedHint = "누르면 시스템 앱 정보에서 권한을 끌 수 있어요",
                    deniedWarning = "카메라가 없으면 사진 인증을 할 수 없어요",
                    onClick = {
                        if (cameraGranted) openAppDetails(context)
                        else cameraPermission(Manifest.permission.CAMERA)
                    },
                )
            }
            item {
                PermissionCard(
                    icon = Icons.Default.LocationOn,
                    title = "위치",
                    description = "위치 인증, 장소를 정한 사진 인증을 할 때만 한 번 재요. 백그라운드 위치는 쓰지 않아요",
                    granted = locationGranted,
                    grantedHint = "누르면 시스템 앱 정보에서 권한을 끌 수 있어요",
                    deniedWarning = "위치가 없으면 장소를 확인하는 인증을 할 수 없어요",
                    onClick = {
                        if (locationGranted) openAppDetails(context)
                        else locationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                )
            }

            item { FocusSectionLabel("기타") }
            item {
                NavigationCard(
                    icon = PixelIcons.Info,
                    title = "앱 정보 · 삭제",
                    description = "시스템 앱 정보 화면을 열어요. 언제든 지울 수 있어요",
                    onClick = { openAppDetails(context) },
                )
            }
        }
    }
}

/**
 * 권한 하나. 허용 전에는 누르면 권한을 요청하고, 허용 뒤에는 시스템 설정 화면을 연다
 * (권한을 거두는 것은 시스템 화면에서만 할 수 있다).
 */
@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    grantedHint: String,
    deniedWarning: String,
    onClick: () -> Unit,
) {
    FocusCard(Modifier.fillMaxWidth(), onClick = onClick) {
        SettingHeader(
            icon = icon,
            title = title,
            description = description,
            badge = { OnOffBadge(granted, onText = "허용됨", offText = "허용 안 함") },
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Chrome.chromeIndigo,
                modifier = Modifier.size(28.dp),
            )
        }
        if (granted) {
            Text(grantedHint, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo)
        } else {
            FocusNotice(deniedWarning, tone = Tone.Warning, icon = icon, actionLabel = "허용", onAction = onClick)
        }
    }
}

private fun openAppDetails(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/**
 * 권한 요청 결과를 받는다. 두 번 거절해서 시스템이 더 묻지 않으면(다시 묻기 안내도 없으면)
 * 요청 창이 뜨지 않으므로, 앱 정보 화면을 열어 직접 켤 수 있게 한다.
 */
@Composable
private fun rememberPermissionRequest(onResult: (Boolean) -> Unit): (permission: String) -> Unit {
    val context = LocalContext.current
    var requested by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onResult(granted)
        val permission = requested
        val activity = context.findActivity()
        if (!granted && permission != null && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        ) {
            openAppDetails(context)
        }
    }
    return { permission ->
        requested = permission
        launcher.launch(permission)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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

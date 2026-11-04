package com.getfuckocus.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getfuckocus.hasPermission
import com.getfuckocus.lock.BlockerServiceStatus
import com.getfuckocus.lock.NotificationHelper
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusSwitch
import com.getfuckocus.ui.components.FocusTopBar

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAllowedApps: () -> Unit) {
    val context = LocalContext.current
    val serviceEnabled = rememberBlockerServiceEnabled()

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

    Scaffold(topBar = { FocusTopBar(title = "설정", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FocusCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("잠금 서비스", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (serviceEnabled) "켜짐" else "꺼짐",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (serviceEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            )
                        }
                        // 접근성 설정은 시스템 화면에서만 바꿀 수 있어서 스위치는 그 화면으로 보내는 역할만 한다
                        FocusSwitch(checked = serviceEnabled, onCheckedChange = { BlockerServiceStatus.openSettings(context) })
                    }
                    Text(
                        "접근성 권한으로 지금 열린 앱의 이름만 확인해요. 화면 내용이나 입력한 글자는 읽지 않고, " +
                            "어떤 데이터도 기기 밖으로 보내지 않아요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!notificationsGranted) {
                        FocusButton(
                            "상태 알림 허용",
                            onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            style = FocusButtonStyle.Tonal,
                            height = 44.dp,
                        )
                    }
                    FocusButton(
                        "앱 정보 · 삭제",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        style = FocusButtonStyle.Ghost,
                        height = 40.dp,
                    )
                }
            }

            item {
                FocusCard(Modifier.fillMaxWidth(), onClick = onOpenAllowedApps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("잠금 예외 앱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "잠금 중에도 열 수 있는 앱을 골라요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

package com.getfuckocus.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.getfuckocus.container
import com.getfuckocus.safety.InstalledApp
import com.getfuckocus.ui.components.FocusCard
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusSwitch
import com.getfuckocus.ui.components.FocusTextField
import com.getfuckocus.ui.components.FocusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 잠금 중에도 열 수 있게 할 앱을 고르는 화면. */
@Composable
fun AllowedAppsScreen(onBack: () -> Unit) {
    val whitelist = LocalContext.current.container.whitelist

    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var allowed by remember { mutableStateOf(whitelist.userPackages()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.Default) { whitelist.launchableApps() } }

    // 화면을 열 때 이미 허용돼 있던 앱만 위로 올린다. 켜고 끌 때마다 순서가 바뀌면 누르던 줄이 사라진다
    val initiallyAllowed = remember { whitelist.userPackages() }
    val shown = apps.orEmpty()
        .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
        .sortedByDescending { it.packageName in initiallyAllowed }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = "잠금 예외 앱",
                // 기본 허용 목록에는 설치되지 않은 앱도 있어서, 설치된 앱 중 허용한 것만 센다
                subtitle = "${apps.orEmpty().count { it.packageName in allowed }}개 허용 · 전화, 문자, 알람, 설정, 홈 화면은 항상 허용",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FocusTextField(
                    query, { query = it },
                    label = "앱 검색",
                    placeholder = "앱 이름",
                    trailing = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            if (apps == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { FocusSpinner() }
                }
            }
            items(shown, key = { it.packageName }) { app ->
                val checked = app.packageName in allowed
                val toggle = { value: Boolean ->
                    whitelist.setUserAllowed(app.packageName, value)
                    allowed = whitelist.userPackages()
                }
                FocusCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = { toggle(!checked) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app.packageName)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        FocusSwitch(checked = checked, onCheckedChange = toggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(width = 96, height = 96).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

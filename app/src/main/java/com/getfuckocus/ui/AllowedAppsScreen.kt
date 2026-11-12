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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.getfuckocus.container
import com.getfuckocus.safety.InstalledApp
import com.getfuckocus.ui.components.FocusEmptyState
import com.getfuckocus.ui.components.FocusIconButton
import com.getfuckocus.ui.components.FocusSectionLabel
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusSwitch
import com.getfuckocus.ui.components.FocusTextField
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.bevel
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
    val matched = apps.orEmpty().filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    val pinned = matched.filter { it.packageName in initiallyAllowed }
    val rest = matched.filter { it.packageName !in initiallyAllowed }
    val allowedCount = apps.orEmpty().count { it.packageName in allowed }
    val toggle: (InstalledApp, Boolean) -> Unit = { app, value ->
        whitelist.setUserAllowed(app.packageName, value)
        allowed = whitelist.userPackages()
    }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = "잠금 예외 앱",
                // 기본 허용 목록에는 설치되지 않은 앱도 있어서, 설치된 앱 중 허용한 것만 센다
                subtitle = "${allowedCount}개 허용 · 전화, 문자, 알람, 설정, 홈 화면은 항상 허용",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 검색창은 목록과 함께 스크롤되지 않게 위에 고정한다
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Chrome.canvas)
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
            ) {
                FocusTextField(
                    query, { query = it },
                    label = "앱 검색",
                    placeholder = "앱 이름",
                    trailing = {
                        if (query.isEmpty()) Icon(Icons.Default.Search, contentDescription = null)
                        else FocusIconButton(Icons.Default.Close, contentDescription = "검색어 지우기", onClick = { query = "" })
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val loaded = apps
                when {
                    loaded == null -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FocusSpinner()
                            Text("설치된 앱을 불러오는 중…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    loaded.isEmpty() -> item {
                        FocusEmptyState(
                            title = "앱 목록을 불러오지 못했어요",
                            body = "다시 열어 보거나, 기기의 앱 목록 권한을 확인해 주세요",
                            icon = Icons.Default.Warning,
                            tone = Tone.Warning,
                        )
                    }
                    matched.isEmpty() -> item {
                        FocusEmptyState(
                            title = "\u201C$query\u201D와 맞는 앱이 없어요",
                            body = "앱 이름 일부만 입력해도 찾을 수 있어요",
                            icon = Icons.Default.Search,
                            actionLabel = "검색어 지우기",
                            onAction = { query = "" },
                        )
                    }
                }
                if (query.isNotBlank() && matched.isNotEmpty()) {
                    item { FocusSectionLabel("검색 결과", count = matched.size, modifier = Modifier.padding(bottom = 6.dp)) }
                    appRows(matched, allowed, toggle)
                } else {
                    if (pinned.isNotEmpty()) {
                        item { FocusSectionLabel("허용한 앱", count = pinned.size, modifier = Modifier.padding(bottom = 6.dp)) }
                        appRows(pinned, allowed, toggle)
                    }
                    if (rest.isNotEmpty()) {
                        item {
                            FocusSectionLabel(
                                "다른 앱", count = rest.size,
                                modifier = Modifier.padding(top = if (pinned.isEmpty()) 0.dp else 12.dp, bottom = 6.dp),
                            )
                        }
                        appRows(rest, allowed, toggle)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.appRows(apps: List<InstalledApp>, allowed: Set<String>, toggle: (InstalledApp, Boolean) -> Unit) {
    items(apps, key = { it.packageName }) { app ->
        AppRow(app, checked = app.packageName in allowed, onToggle = { toggle(app, it) })
    }
}

/** 한 줄 = 앱 아이콘 + 이름 + 상태 + 스위치. 줄 전체가 스위치처럼 눌린다 (패키지명은 숨긴다). */
@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(ChromeShapes.control)
            .background(if (checked) Chrome.ice else Chrome.platinum)
            .bevel(shadowWidth = 1.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName)
        Spacer(Modifier.width(14.dp))
        // 켜고 끈 상태는 스위치로 충분히 보이므로 설명 줄은 두지 않는다
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        FocusSwitch(checked = checked, onCheckedChange = onToggle)
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
            .clip(ChromeShapes.small)
            .background(Chrome.white),
        contentAlignment = Alignment.Center,
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

package com.getfuckocus.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.getfuckocus.container
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.data.requiredLabels
import com.getfuckocus.hasPermission
import com.getfuckocus.task.TaskSchedule
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.verify.DetectedLabel
import com.getfuckocus.verify.VerificationResult
import com.getfuckocus.verify.Verdict
import kotlinx.coroutines.launch

/** 앱 카메라로만 인증한다. 갤러리에서 사진을 고르는 경로는 일부러 두지 않는다. */
@Composable
fun CaptureScreen(taskId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()

    var task by remember { mutableStateOf<TaskEntity?>(null) }
    var cameraGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.CAMERA)) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VerificationResult?>(null) }
    // 패널이 닫히는 애니메이션 동안에도 내용을 유지하기 위한 마지막 결과
    var shownResult by remember { mutableStateOf<VerificationResult?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted -> cameraGranted = granted[Manifest.permission.CAMERA] == true }

    LaunchedEffect(taskId) {
        val t = container.taskDao.get(taskId) ?: return@LaunchedEffect
        task = t
        val needed = buildList {
            add(Manifest.permission.CAMERA)
            if (t.hasGeoFence()) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.filterNot { context.hasPermission(it) }
        if (needed.isNotEmpty()) permissions.launch(needed.toTypedArray())
    }
    LaunchedEffect(result) { result?.let { shownResult = it } }
    BackHandler(enabled = result != null) { result = null }

    Scaffold(
        topBar = {
            FocusTopBar(
                title = task?.title ?: "인증",
                subtitle = task?.let { TaskSchedule.describeWindow(it) },
                onBack = onBack,
            )
        },
    ) { padding ->
        val t = task
        if (t == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { FocusSpinner() }
            return@Scaffold
        }

        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ConditionRow(t)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cameraGranted) {
                        CameraPreview(imageCapture, Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("카메라 권한이 필요해요", color = Color.White)
                            FocusButton(
                                "권한 허용",
                                onClick = { permissions.launch(arrayOf(Manifest.permission.CAMERA)) },
                                height = 44.dp,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ShutterButton(
                        enabled = cameraGranted && !busy,
                        busy = busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                result = try {
                                    container.verificationFlow.run(t, imageCapture)
                                } catch (e: Exception) {
                                    VerificationResult(
                                        Verdict.Fail(listOf("촬영에 실패했어요: ${e.message}")),
                                        labels = emptyList(),
                                        releasedLock = false,
                                    )
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    )
                    Text(
                        if (busy) "확인 중…" else "촬영해서 인증",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 결과 패널: 어두운 배경 + 아래에서 올라오는 카드
            AnimatedVisibility(visible = result != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(remember { MutableInteractionSource() }, indication = null) { result = null }
                )
            }
            AnimatedVisibility(
                visible = result != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                shownResult?.let { r ->
                    ResultPanel(
                        task = t,
                        result = r,
                        onRetry = { result = null },
                        onDone = {
                            result = null
                            onBack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionRow(task: TaskEntity) {
    val labels = task.requiredLabels()
    if (!task.hasGeoFence() && labels.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (task.hasGeoFence()) {
            Pill("지정 장소 ${task.geoRadiusMeters!!.toInt()}m 이내", colors.surfaceContainerHigh, colors.onSurface, icon = Icons.Default.LocationOn)
        }
        if (labels.isNotEmpty()) {
            Pill("${labels.joinToString(" · ")} 중 하나", colors.surfaceContainerHigh, colors.onSurface, icon = Icons.Default.Search)
        }
    }
}

/** 원형 셔터. 확인 중에는 바깥 테두리를 따라 꼬리가 달린 호가 빙글빙글 돈다. */
@Composable
private fun ShutterButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = enabled || busy
    val rotation by rememberInfiniteTransition(label = "shutterSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing)),
        label = "shutterRotation",
    )

    Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        if (busy) {
            val head = colors.secondary
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 5.dp.toPx()
                rotate(rotation) {
                    drawArc(
                        brush = Brush.sweepGradient(0f to Color.Transparent, 0.75f to head, 0.75f to Color.Transparent),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .border(4.dp, if (active) colors.primary else colors.outlineVariant, CircleShape)
                .padding(8.dp)
                .clip(CircleShape)
                .background(if (active) colors.primary else colors.outlineVariant)
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "촬영해서 인증" },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultPanel(
    task: TaskEntity,
    result: VerificationResult,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val passed = result.verdict is Verdict.Pass
    // 과제 조건과 맞고 기준을 넘은 라벨만 보여준다
    val passedLabels = result.labels
        .filter { label -> label.isAccepted(task) }
        .sortedByDescending { it.confidence }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(colors.surfaceContainerLow)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }
            .navigationBarsPadding()
            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.outlineVariant)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (passed) colors.tertiary else colors.error,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (passed) "인증 성공" else "인증 실패",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        when (val verdict = result.verdict) {
            Verdict.Pass -> Text(
                if (result.releasedLock) "잠금이 해제됐어요" else "이번 회차를 완료했어요",
                style = MaterialTheme.typography.bodyLarge,
            )
            is Verdict.Fail -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                verdict.reasons.forEach { reason ->
                    Row {
                        Text("•  ", color = colors.error)
                        Text(reason, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (passedLabels.isNotEmpty()) {
            Text("통과한 라벨", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                passedLabels.forEach { label ->
                    Pill(
                        "${label.text} ${(label.confidence * 100).toInt()}%",
                        colors.tertiaryContainer,
                        colors.onTertiaryContainer,
                        icon = Icons.Default.CheckCircle,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        if (passed) {
            FocusButton("확인", onClick = onDone, modifier = Modifier.fillMaxWidth())
        } else {
            FocusButton("다시 찍기", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            FocusButton("나가기", onClick = onDone, modifier = Modifier.fillMaxWidth(), style = FocusButtonStyle.Ghost, height = 44.dp)
        }
    }
}

private fun DetectedLabel.isAccepted(task: TaskEntity): Boolean =
    confidence >= task.minLabelConfidence && task.requiredLabels().any { it.equals(text, ignoreCase = true) }

@Composable
private fun CameraPreview(imageCapture: ImageCapture, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (providerFuture.isDone) providerFuture.get().unbindAll()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

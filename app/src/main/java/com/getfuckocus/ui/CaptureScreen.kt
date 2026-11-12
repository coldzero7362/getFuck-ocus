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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.getfuckocus.ui.components.Text
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
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.PixelIcons
import com.getfuckocus.ui.theme.bevel
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.circleBevel
import com.getfuckocus.ui.theme.plate
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
                        // 카본 베젤 안에 끼운 화면
                        .plate(Chrome.carbon, ChromeShapes.panel)
                        .padding(6.dp)
                        .clip(ChromeShapes.small)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cameraGranted) {
                        CameraPreview(imageCapture, Modifier.fillMaxSize())
                        // 인증 진행 중: 미리보기 위에 상태 띠를 띄워 촬영 결과와 헷갈리지 않게 한다
                        if (busy) {
                            Row(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                                    .clip(ChromeShapes.small)
                                    .background(Chrome.carbon.copy(alpha = 0.85f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FocusSpinner(Modifier.size(18.dp), strokeWidth = 3.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("사진을 확인하는 중…", style = MaterialTheme.typography.labelLarge, color = Chrome.white)
                            }
                        }
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
                        if (busy) "확인 중…" else "눌러서 찍기",
                        style = MaterialTheme.typography.labelLarge,
                        color = Chrome.carbon,
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

/** 이 사진이 통과하려면 맞아야 할 조건. 좁은 화면에서는 줄을 바꿔 잘리지 않게 한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionRow(task: TaskEntity) {
    val labels = task.requiredLabels()
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("통과 조건", style = MaterialTheme.typography.labelMedium, color = Chrome.carbon)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Pill("마감 전 촬영", Chrome.white, Chrome.carbon, icon = PixelIcons.Clock)
            if (task.hasGeoFence()) {
                Pill("지정 장소 ${task.geoRadiusMeters!!.toInt()}m 이내", Chrome.white, Chrome.carbon, icon = Icons.Default.LocationOn)
            }
            if (labels.isNotEmpty()) {
                Pill("${labels.joinToString(" · ")} 중 하나", Chrome.white, Chrome.carbon, icon = Icons.Default.Search)
            }
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

    // 회전 링이 굵어도 셔터 테두리와 겹치지 않게 바깥 칸을 넉넉히 둔다
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        if (busy) {
            val head = colors.secondary
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 9.dp.toPx()
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
                .border(4.dp, if (active) Chrome.chromeIndigo else colors.outlineVariant, CircleShape)
                .padding(8.dp)
                .clip(CircleShape)
                .background(if (active) Chrome.signal else colors.outlineVariant)
                .circleBevel()
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

    val tone = if (passed) Tone.Success else Tone.Danger
    val reasons = (result.verdict as? Verdict.Fail)?.reasons.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .plate(Chrome.platinum, ChromeShapes.sheet)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }
            .navigationBarsPadding(),
    ) {
        // 결과 머리띠: 성공은 초록, 실패는 빨강 + 아이콘 + 글자로 한눈에 구분한다
        Row(
            Modifier
                .fillMaxWidth()
                .background(tone.container)
                .bevel(shadow = tone.accent, shadowWidth = 3.dp)
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(tone.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (passed) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Chrome.white,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (passed) "인증 성공" else "인증 실패",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = tone.content,
                )
                Text(
                    when {
                        passed && result.releasedLock -> "잠금이 해제됐어요"
                        passed -> "이번 회차를 완료했어요"
                        else -> "맞지 않은 조건 ${reasons.size}개"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = tone.content,
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 실패 이유는 항목마다 따로 칸을 나눈다
            reasons.forEachIndexed { index, reason ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(ChromeShapes.small)
                        .background(Chrome.white)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Chrome.white,
                        modifier = Modifier
                            .clip(ChromeShapes.control)
                            .background(Chrome.red)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = Chrome.carbon)
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
                            Chrome.ice,
                            Chrome.teal,
                            icon = Icons.Default.CheckCircle,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            if (passed) {
                FocusButton("확인", onClick = onDone, modifier = Modifier.fillMaxWidth(), height = 56.dp, icon = Icons.Default.Check)
            } else {
                FocusButton("다시 찍기", onClick = onRetry, modifier = Modifier.fillMaxWidth(), height = 56.dp, icon = PixelIcons.Camera)
                Spacer(Modifier.height(4.dp))
                FocusButton("나가기", onClick = onDone, modifier = Modifier.fillMaxWidth(), style = FocusButtonStyle.Outline, height = 48.dp)
            }
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

package com.getfuckocus.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.getfuckocus.ui.components.FocusPanel
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.circleBevel
import com.getfuckocus.ui.theme.halftone
import com.getfuckocus.ui.theme.plate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.getfuckocus.container
import com.getfuckocus.hasPermission
import com.getfuckocus.place.PlaceAddress
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusChip
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.FocusSpinner
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import java.io.File

/** 장소 조건: 중심 좌표와 반경. 화면 사이에 문자열로 주고받는다. */
data class PickedPlace(val lat: Double, val lng: Double, val radiusMeters: Float) {
    fun encode(): String = "$lat,$lng,$radiusMeters"

    companion object {
        fun decode(text: String?): PickedPlace? {
            val parts = text?.split(',')?.takeIf { it.size == 3 } ?: return null
            val lat = parts[0].toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: return null
            val lng = parts[1].toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: return null
            val radius = parts[2].toFloatOrNull()?.takeIf { it > 0 } ?: return null
            return PickedPlace(lat, lng, radius)
        }
    }
}

val PLACE_RADIUS_PRESETS = listOf(50f, 100f, 150f, 300f, 500f)
const val DEFAULT_PLACE_RADIUS_M = 150f

/** 처음 열 때 위치를 모르면 보여 줄 곳 (서울시청) */
private val FALLBACK_CENTER = GeoPoint(37.5665, 126.9780)
private const val PICK_ZOOM = 17.0
/** 지도가 이만큼 멈춰 있으면 이동이 끝난 것으로 보고 주소를 찾는다 */
private const val SETTLE_MS = 600L

private sealed interface AddressState {
    data object Loading : AddressState
    data class Found(val address: PlaceAddress) : AddressState
    data object Failed : AddressState
}

/**
 * 지도를 움직여 가운데 고정 핀으로 장소를 고르고 반경을 정한다. 지금 있는 곳이 아니어도 어디든 고를 수 있다.
 * 지도 그림은 OpenStreetMap에서, 핀 위치의 주소는 OSM Nominatim에서 받는다. 고른 좌표는 기기 안에만 저장한다.
 */
@Composable
fun PlacePickerScreen(initial: PickedPlace?, onPicked: (PickedPlace) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    var radius by remember { mutableFloatStateOf(initial?.radiusMeters ?: DEFAULT_PLACE_RADIUS_M) }
    var center by remember { mutableStateOf(initial?.let { GeoPoint(it.lat, it.lng) } ?: FALLBACK_CENTER) }
    var address by remember { mutableStateOf<AddressState>(AddressState.Loading) }
    var locating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<LocationError?>(null) }
    var confirming by remember { mutableStateOf(false) }

    val circle = remember { mutableStateOf<Polygon?>(null) }
    val mapView = remember { createMapView(context, center) }

    fun redrawCircle() {
        circle.value?.points = Polygon.pointsAsCircle(center, radius.toDouble())
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        val polygon = Polygon(mapView).apply {
            // 게임기 화면의 녹색 톤에 맞춘 초록 원. 녹색 지도 위에서도 보이게 테두리는 짙게 한다
            fillPaint.color = Chrome.success.copy(alpha = 0.22f).toArgb()
            outlinePaint.color = Chrome.teal.toArgb()
            outlinePaint.strokeWidth = 6f
            // 원을 눌러도 말풍선이 뜨지 않게
            infoWindow = null
        }
        mapView.overlays.add(polygon)
        circle.value = polygon
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = onMove()
            override fun onZoom(event: ZoomEvent?): Boolean = onMove()
            private fun onMove(): Boolean {
                val c = mapView.mapCenter
                center = GeoPoint(c.latitude, c.longitude)
                redrawCircle()
                return false
            }
        }
        mapView.addMapListener(listener)
        redrawCircle()
        onDispose {
            mapView.removeMapListener(listener)
            mapView.onDetach()
        }
    }
    LifecycleResumeEffect(mapView) {
        mapView.onResume()
        onPauseOrDispose { mapView.onPause() }
    }
    // 반경을 바꾸면 중심은 그대로 두고 원만 바로 다시 그린다
    LaunchedEffect(radius) { redrawCircle() }

    // 지도가 멈추면 주소를 찾는다. 다시 움직이면 이전 조회는 취소되어 늦게 온 결과가 새 주소를 덮어쓰지 않는다
    LaunchedEffect(center) {
        address = AddressState.Loading
        delay(SETTLE_MS)
        val found = container.reverseGeocoder.lookup(center.latitude, center.longitude)
        address = found?.let { AddressState.Found(it) } ?: AddressState.Failed
    }

    fun moveToCurrent() {
        if (locating) return
        locating = true
        locationError = null
        scope.launch {
            val location = container.evidenceCollector.currentLocation()
            if (location == null) {
                locationError = if (locationServiceOn(context)) LocationError.Failed else LocationError.ServiceOff
            } else {
                mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude), PICK_ZOOM, 600L)
            }
            locating = false
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) moveToCurrent() else locationError = LocationError.NoPermission
    }
    fun goToCurrent() {
        if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) moveToCurrent()
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // 처음 고를 때는 권한이 이미 있으면 지금 위치에서 시작한다 (없으면 묻지 않고 기본 위치에서 시작)
    LaunchedEffect(Unit) {
        if (initial == null && context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) moveToCurrent()
    }

    val validCenter = center.latitude in -90.0..90.0 && center.longitude in -180.0..180.0
    fun confirm() {
        if (confirming || !validCenter) return
        confirming = true
        onPicked(PickedPlace(center.latitude, center.longitude, radius))
    }

    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    val openLocationSettings = {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // 휴대용 게임기처럼: 위는 카본 베젤에 박힌 지도 "화면", 아래는 판 위의 정보창과 카본 조작 띠
    Scaffold(
        topBar = { FocusTopBar("장소 선택", onBack = onBack) },
        bottomBar = {
            CommandBar(
                locationError = locationError,
                onOpenAppSettings = openAppSettings,
                onOpenLocationSettings = openLocationSettings,
                validCenter = validCenter,
                confirming = confirming,
                onConfirm = ::confirm,
            )
        },
        // 아래 내비게이션 영역은 조작 띠가 직접 비켜 간다 (카본 띠가 화면 끝까지 닿게)
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenBezel(
                center = center,
                busy = address is AddressState.Loading || locating,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                MapGuide(Modifier.align(Alignment.TopStart).padding(8.dp))
                CenterPin(Modifier.align(Alignment.Center))
                MyLocationButton(
                    loading = locating,
                    onClick = ::goToCurrent,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                )
            }
            ReadoutPanel(address = address, radius = radius, onRadius = { radius = it })
        }
    }
}

private enum class LocationError { NoPermission, ServiceOff, Failed }

/**
 * 지도 "화면"을 감싼 카본 베젤. 위에는 전원 등과 각인 문구, 고른 좌표를 디지털 숫자로,
 * 아래에는 지도 출처를 새겨 넣는다 (지도를 가리지 않으면서 OpenStreetMap 출처 표시를 지킨다).
 */
@Composable
private fun ScreenBezel(
    center: GeoPoint,
    busy: Boolean,
    modifier: Modifier = Modifier,
    screen: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier
            .plate(Chrome.carbon, ChromeShapes.panel)
            .halftone()
            .padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PowerLed(busy)
            Spacer(Modifier.width(8.dp))
            Text(
                "MAP · SCAN",
                style = BezelLabel,
                color = Chrome.sky,
                modifier = Modifier.weight(1f),
            )
            Text(
                "%.5f  %.5f".format(center.latitude, center.longitude),
                style = DigitStyle.copy(fontSize = 13.sp),
                color = Chrome.sky,
                maxLines = 1,
            )
        }
        // 화면 유리: 베젤 안쪽으로 파인 테두리
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(ChromeShapes.control)
                .border(2.dp, Chrome.chromeIndigo, ChromeShapes.control),
            content = screen,
        )
        Text(
            "© OpenStreetMap contributors",
            style = BezelLabel.copy(fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = Chrome.sky.copy(alpha = 0.85f),
        )
    }
}

/** 전원 등: 평소에는 초록, 주소를 찾거나 위치를 재는 동안에는 앰버로 깜박인다 */
@Composable
private fun PowerLed(busy: Boolean) {
    val blink = rememberInfiniteTransition(label = "led")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "ledAlpha",
    )
    Box(
        Modifier
            .size(9.dp)
            .graphicsLayer { this.alpha = if (busy) alpha else 1f }
            .clip(CircleShape)
            .background(if (busy) Chrome.amber else Chrome.success)
            .border(1.dp, Chrome.highlight, CircleShape),
    )
}

/** 지도 화면 위 안내: 각진 카본 꼬리표 (경고·행동색이 아니라 차가운 하늘색 글자) */
@Composable
private fun MapGuide(modifier: Modifier = Modifier) {
    Text(
        "지도를 움직여 원하는 위치를 선택하세요",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Chrome.sky,
        modifier = modifier
            .background(Chrome.carbon.copy(alpha = 0.85f), ChromeShapes.control)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

/**
 * 베젤 아래 정보창: 선택한 위치(주소 → 보조 주소)와 적용 반경.
 * 앱의 다른 화면과 같은 패널(하늘색 머리띠 + 플래티넘 판)을 쓴다.
 */
@Composable
private fun ReadoutPanel(address: AddressState, radius: Float, onRadius: (Float) -> Unit) {
    FocusPanel(title = "선택한 위치", icon = Icons.Default.LocationOn, modifier = Modifier.fillMaxWidth()) {
        val (title, detail) = when (address) {
            AddressState.Loading -> "주소 확인 중…" to null
            AddressState.Failed -> "주소를 확인할 수 없어요" to null
            is AddressState.Found -> address.address.title to address.address.detail
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (address is AddressState.Found) Chrome.carbon else Chrome.mutedIndigo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Chrome.chromeIndigo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DottedDivider()
        Text("적용 반경", style = BezelLabel, color = Chrome.chromeIndigo)
        RadiusChips(radius, onRadius)
    }
}

/** 디자인 가이드의 점선 구분선 */
@Composable
private fun DottedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            Chrome.mutedIndigo,
            Offset(0f, 0f),
            Offset(size.width, 0f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())),
        )
    }
}

/** 화면 아래 카본 조작 띠: 오류 안내와 주 버튼. 과제 편집 화면의 저장 띠와 같은 모양이다 */
@Composable
private fun CommandBar(
    locationError: LocationError?,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    validCenter: Boolean,
    confirming: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Chrome.carbon)
            .halftone()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (locationError) {
            LocationError.NoPermission -> FocusNotice(
                "위치 권한이 필요해요. 기기 설정에서 위치 권한을 허용해 주세요",
                tone = Tone.Warning, actionLabel = "설정", onAction = onOpenAppSettings,
            )
            LocationError.ServiceOff -> FocusNotice(
                "기기의 위치 서비스가 꺼져 있어요",
                tone = Tone.Warning, actionLabel = "켜기", onAction = onOpenLocationSettings,
            )
            LocationError.Failed -> FocusNotice("현재 위치를 확인하지 못했어요. 다시 시도해 주세요", tone = Tone.Warning)
            null -> Unit
        }
        if (!validCenter) FocusNotice("좌표가 올바르지 않아요. 지도를 다시 움직여 주세요", tone = Tone.Danger)
        FocusButton(
            "이 장소로 정하기",
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
            enabled = validCenter && !confirming,
        )
    }
}

/** 반경 버튼. 넓은 화면은 한 줄, 좁은 화면(330dp 미만)은 3 + 2 두 줄. 선택된 것은 앰버 판으로 튀어나와 구분된다 */
@Composable
private fun RadiusChips(radius: Float, onRadius: (Float) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rows = if (maxWidth < 330.dp) PLACE_RADIUS_PRESETS.chunked(3) else listOf(PLACE_RADIUS_PRESETS)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { preset ->
                        FocusChip(
                            "${preset.toInt()}m",
                            selected = radius == preset,
                            onClick = { onRadius(preset) },
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                    // 두 줄로 나눌 때 아랫줄 버튼이 너무 넓어지지 않게 빈 칸을 채운다
                    repeat(rows.first().size - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 베젤에 새긴 글자: 대문자 + 자간, 실크스크린 각인 느낌 */
private val BezelLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)

/**
 * 도트 핀 그림. 글자 하나가 픽셀 하나다.
 * K 짙은 테두리, O 주황 몸통, H 밝은 반사광, W 흰 가운데, . 빈칸. 맨 아래 가운데 픽셀이 핀 끝이다.
 */
private val PIN_PIXELS = listOf(
    "....KKKKK....",
    "..KKOOOOOKK..",
    ".KOHHOOOOOOK.",
    ".KHHOOOOOOOK.",
    "KOHOOKKKOOOOK",
    "KOOOKWWWKOOOK",
    "KOOOKWWWKOOOK",
    "KOOOKWWWKOOOK",
    "KOOOOKKKOOOOK",
    ".KOOOOOOOOOK.",
    ".KOOOOOOOOOK.",
    "..KOOOOOOOK..",
    "...KOOOOOK...",
    "....KOOOK....",
    ".....KOK.....",
    ".....KOK.....",
    "......K......",
)
private val PIN_PIXEL = 3.5.dp

/**
 * 가운데 고정 핀. 옛 게임기 화면처럼 네모 픽셀로 찍는다.
 * 맨 아래 가운데 픽셀(핀 끝)이 정확히 지도 가운데(고른 좌표)에 오도록 그림 높이의 절반만큼 올린다.
 */
@Composable
private fun CenterPin(modifier: Modifier = Modifier) {
    val columns = PIN_PIXELS.first().length
    val width = PIN_PIXEL * columns
    val height = PIN_PIXEL * PIN_PIXELS.size
    Canvas(
        modifier
            .size(width, height)
            .offset(y = -height / 2)
            .semantics { contentDescription = "고를 장소" },
    ) {
        val px = size.width / columns
        // 픽셀 사이에 틈이 보이지 않게 아주 조금 겹쳐 그린다
        val cell = androidx.compose.ui.geometry.Size(px + 0.5f, px + 0.5f)
        PIN_PIXELS.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                val color = when (ch) {
                    'K' -> Chrome.carbon
                    'O' -> Chrome.signal
                    'H' -> Chrome.amber
                    'W' -> Chrome.white
                    else -> null
                } ?: return@forEachIndexed
                drawRect(color, topLeft = Offset(col * px, row * px), size = cell)
            }
        }
    }
}

/**
 * 지도 오른쪽 아래 현재 위치 버튼. 가이드의 둥근 오렌지 행동 버튼처럼 원형에 단단한 테두리를 단다.
 * 찾는 동안에는 돌아가는 표시를 보이고 다시 누르지 못하게 한다.
 */
@Composable
private fun MyLocationButton(loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Chrome.signal)
            .circleBevel()
            .border(2.dp, Chrome.carbon, CircleShape)
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (loading) "현재 위치 찾는 중" else "현재 위치로 이동" },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            FocusSpinner(Modifier.size(24.dp), color = Chrome.carbon, strokeWidth = 3.dp)
        } else {
            // 위치 타깃: 가운데 점 + 고리 + 네 방향 눈금
            Canvas(Modifier.size(24.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val stroke = 2.dp.toPx()
                drawCircle(Chrome.carbon, radius = size.width * 0.3f, center = c, style = Stroke(stroke))
                drawCircle(Chrome.carbon, radius = size.width * 0.13f, center = c)
                val inner = size.width * 0.3f
                val outer = size.width * 0.5f
                listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                    drawLine(Chrome.carbon, c + d * inner, c + d * outer, strokeWidth = stroke)
                }
            }
        }
    }
}

private fun locationServiceOn(context: Context): Boolean {
    val manager = context.getSystemService(LocationManager::class.java) ?: return false
    return runCatching { manager.isLocationEnabled }.getOrDefault(true)
}

/**
 * 지도 뷰. 지도 그림 캐시는 앱 전용 캐시 폴더에 두고(다른 앱이 못 읽고 지우기 쉬움),
 * OpenStreetMap 이용 조건에 맞게 앱 패키지명을 User-Agent로 보낸다.
 * 기본 지도 색이 흐려 도로·건물 구분이 약해서 대비와 채도를 조금 올린다.
 */
private fun createMapView(context: Context, start: GeoPoint): MapView {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        isVerticalMapRepetitionEnabled = false
        minZoomLevel = 5.0
        maxZoomLevel = 19.0
        controller.setZoom(PICK_ZOOM)
        controller.setCenter(start)
        overlayManager.tilesOverlay.setColorFilter(mapContrastFilter())
    }
}

private const val GREEN_TINT_RB = 0.82f
private const val GREEN_TINT_LIFT = 10f

private fun mapContrastFilter(): ColorMatrixColorFilter {
    val contrast = 1.12f
    val shift = (1 - contrast) * 128
    val matrix = ColorMatrix().apply { setSaturation(1.2f) }
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, shift,
                0f, contrast, 0f, 0f, shift,
                0f, 0f, contrast, 0f, shift,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
    // 휴대용 게임기 화면처럼 녹색 기운을 얹는다: 빨강·파랑을 조금 줄이고 초록을 조금 띄운다.
    // 원래 색 구분(도로, 공원, 건물)은 남도록 약하게만 건다
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                GREEN_TINT_RB, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, GREEN_TINT_LIFT,
                0f, 0f, GREEN_TINT_RB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
    return ColorMatrixColorFilter(matrix)
}

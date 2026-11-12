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
            fillPaint.color = Chrome.signal.copy(alpha = 0.16f).toArgb()
            outlinePaint.color = Chrome.signal.toArgb()
            outlinePaint.strokeWidth = 5f
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

    // 아래 내비게이션 영역은 하단 패널이 직접 비켜 가도록(흰 배경이 화면 끝까지 닿게) Scaffold 여백에서 뺀다
    Scaffold(
        topBar = { FocusTopBar("장소 선택", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                MapGuide(Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
                CenterPin(Modifier.align(Alignment.Center))
                // OpenStreetMap 이용 조건: 지도 위에 출처를 보인다
                Text(
                    "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = Chrome.carbon,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Chrome.white.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                MyLocationButton(
                    loading = locating,
                    onClick = ::goToCurrent,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                )
            }
            BottomPanel(
                address = address,
                center = center,
                radius = radius,
                onRadius = { radius = it },
                locationError = locationError,
                onOpenAppSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onOpenLocationSettings = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                validCenter = validCenter,
                confirming = confirming,
                onConfirm = ::confirm,
            )
        }
    }
}

private enum class LocationError { NoPermission, ServiceOff, Failed }

@Composable
private fun BottomPanel(
    address: AddressState,
    center: GeoPoint,
    radius: Float,
    onRadius: (Float) -> Unit,
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
            .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Chrome.white, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 선택한 위치: 주소가 먼저, 좌표는 작은 보조 정보
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("선택한 위치", style = MaterialTheme.typography.labelMedium, color = Chrome.mutedIndigo)
            val (title, detail) = when (address) {
                AddressState.Loading -> "주소 확인 중…" to null
                AddressState.Failed -> "주소를 확인할 수 없어요" to null
                is AddressState.Found -> address.address.title to address.address.detail
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (address is AddressState.Found) Chrome.carbon else Chrome.mutedIndigo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Chrome.carbon, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "%.5f, %.5f".format(center.latitude, center.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = Chrome.mutedIndigo,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("적용 반경", style = MaterialTheme.typography.labelMedium, color = Chrome.mutedIndigo)
            RadiusChips(radius, onRadius)
        }

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
            icon = Icons.Default.Check,
            height = 56.dp,
            enabled = validCenter && !confirming,
        )
    }
}

/** 반경 버튼. 넓은 화면은 한 줄, 좁은 화면(360dp 미만)은 3 + 2 두 줄. 선택된 것은 색과 체크 표시로 함께 구분한다 */
@Composable
private fun RadiusChips(radius: Float, onRadius: (Float) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rows = if (maxWidth < 360.dp) PLACE_RADIUS_PRESETS.chunked(3) else listOf(PLACE_RADIUS_PRESETS)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { preset ->
                        FocusChip(
                            "${preset.toInt()}m",
                            selected = radius == preset,
                            onClick = { onRadius(preset) },
                            leadingIcon = if (radius == preset) Icons.Default.Check else null,
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

/** 지도 위쪽 안내. 지도를 많이 가리지 않게 작은 반투명 판으로 띄운다 */
@Composable
private fun MapGuide(modifier: Modifier = Modifier) {
    Text(
        "지도를 움직여 원하는 위치를 선택하세요",
        style = MaterialTheme.typography.labelLarge,
        color = Chrome.white,
        modifier = modifier
            .background(Chrome.carbon.copy(alpha = 0.78f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * 가운데 고정 핀. 핀 끝이 정확히 지도 가운데(고른 좌표)에 오도록 그림의 아래 끝을 중심에 맞춘다.
 * 주황 몸통에 짙은 테두리를 둘러 어떤 지도 색 위에서도 보이게 한다.
 */
@Composable
private fun CenterPin(modifier: Modifier = Modifier) {
    val width = 36.dp
    val height = 48.dp
    Canvas(
        modifier
            .size(width, height)
            // 그림의 아래 끝(핀 끝)이 Box 가운데에 오도록 높이의 절반만큼 올린다
            .offset(y = -height / 2)
            .semantics { contentDescription = "고를 장소" },
    ) {
        val w = size.width
        val h = size.height
        val r = w / 2
        val body = Path().apply {
            moveTo(w / 2, h)
            cubicTo(w * 0.2f, h * 0.62f, 0f, r * 1.45f, 0f, r)
            arcTo(androidx.compose.ui.geometry.Rect(0f, 0f, w, w), 180f, 180f, false)
            cubicTo(w, r * 1.45f, w * 0.8f, h * 0.62f, w / 2, h)
            close()
        }
        drawPath(body, Chrome.signal)
        drawPath(body, Chrome.carbon, style = Stroke(width = 2.5.dp.toPx()))
        drawCircle(Chrome.white, radius = r * 0.36f, center = Offset(w / 2, r))
        drawCircle(Chrome.carbon, radius = r * 0.36f, center = Offset(w / 2, r), style = Stroke(width = 1.5.dp.toPx()))
    }
}

/** 지도 오른쪽 아래 현재 위치 버튼. 찾는 동안에는 돌아가는 표시를 보이고 다시 누르지 못하게 한다 */
@Composable
private fun MyLocationButton(loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(52.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Chrome.white)
            .border(1.dp, Chrome.hairline.copy(alpha = 0.4f), CircleShape)
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (loading) "현재 위치 찾는 중" else "현재 위치로 이동" },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            FocusSpinner(Modifier.size(24.dp), strokeWidth = 3.dp)
        } else {
            // 위치 타깃: 가운데 점 + 고리 + 네 방향 눈금
            Canvas(Modifier.size(24.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val stroke = 2.dp.toPx()
                drawCircle(Chrome.chromeIndigo, radius = size.width * 0.3f, center = c, style = Stroke(stroke))
                drawCircle(Chrome.chromeIndigo, radius = size.width * 0.13f, center = c)
                val inner = size.width * 0.3f
                val outer = size.width * 0.5f
                listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                    drawLine(Chrome.chromeIndigo, c + d * inner, c + d * outer, strokeWidth = stroke)
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
    return ColorMatrixColorFilter(matrix)
}

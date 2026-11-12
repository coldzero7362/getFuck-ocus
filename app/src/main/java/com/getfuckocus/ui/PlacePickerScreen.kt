package com.getfuckocus.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.getfuckocus.container
import com.getfuckocus.hasPermission
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusChip
import com.getfuckocus.ui.components.FocusNotice
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Tone
import com.getfuckocus.ui.theme.Chrome
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

/**
 * 지도를 움직여 가운데 핀을 원하는 곳에 맞추고 반경을 골라 장소를 정한다.
 * 지금 있는 곳이 아니어도 어디든 고를 수 있다.
 * 지도 그림만 OpenStreetMap에서 받고, 고른 좌표는 기기 안에만 저장한다.
 */
@Composable
fun PlacePickerScreen(initial: PickedPlace?, onPicked: (PickedPlace) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    var radius by remember { mutableFloatStateOf(initial?.radiusMeters ?: DEFAULT_PLACE_RADIUS_M) }
    var center by remember { mutableStateOf(initial?.let { GeoPoint(it.lat, it.lng) } ?: FALLBACK_CENTER) }
    var locating by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val circle = remember { mutableStateOf<Polygon?>(null) }
    val mapView = remember { createMapView(context, center) }

    fun redrawCircle() {
        circle.value?.points = Polygon.pointsAsCircle(center, radius.toDouble())
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        val polygon = Polygon(mapView).apply {
            fillPaint.color = Chrome.signal.copy(alpha = 0.18f).toArgb()
            outlinePaint.color = Chrome.signal.toArgb()
            outlinePaint.strokeWidth = 4f
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
    LaunchedEffect(radius) { redrawCircle() }

    fun moveToCurrent() = scope.launch {
        locating = true
        notice = null
        val location = container.evidenceCollector.currentLocation()
        if (location == null) {
            notice = "지금 위치를 가져오지 못했어요"
        } else {
            mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude), PICK_ZOOM, 600L)
        }
        locating = false
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) moveToCurrent() else notice = "위치 권한이 없어 지금 위치로 갈 수 없어요. 지도를 움직여 골라 주세요"
    }
    fun goToCurrent() {
        if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) moveToCurrent()
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // 처음 고를 때는 권한이 이미 있으면 지금 위치에서 시작한다 (없으면 묻지 않고 기본 위치에서 시작)
    LaunchedEffect(Unit) {
        if (initial == null && context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) moveToCurrent()
    }

    Scaffold(
        topBar = { FocusTopBar("장소 고르기", subtitle = "지도를 움직여 핀을 원하는 곳에 맞추세요", onBack = onBack) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                // 핀 끝이 지도 가운데에 오도록 아이콘 높이의 절반만큼 올린다
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "고를 장소",
                    tint = Chrome.red,
                    modifier = Modifier.align(Alignment.Center).offset(y = (-22).dp).size(44.dp),
                )
                // OpenStreetMap 이용 조건: 지도 위에 출처를 보인다
                Text(
                    "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = Chrome.carbon,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Chrome.white.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Chrome.platinum)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "%.5f, %.5f · 반경 %dm".format(center.latitude, center.longitude, radius.toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = Chrome.chromeIndigo,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PLACE_RADIUS_PRESETS.forEach { preset ->
                        FocusChip(
                            "${preset.toInt()}m",
                            selected = radius == preset,
                            onClick = { radius = preset },
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                }
                notice?.let { FocusNotice(it, tone = Tone.Warning) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FocusButton(
                        if (locating) "찾는 중…" else "지금 위치로",
                        onClick = ::goToCurrent,
                        modifier = Modifier.weight(1f),
                        style = FocusButtonStyle.Outline,
                        icon = Icons.Default.LocationOn,
                        enabled = !locating,
                    )
                    FocusButton(
                        "이 장소로 정하기",
                        onClick = { onPicked(PickedPlace(center.latitude, center.longitude, radius)) },
                        modifier = Modifier.weight(1.3f),
                        icon = Icons.Default.Check,
                    )
                }
            }
        }
    }
}

/**
 * 지도 뷰. 지도 그림 캐시는 앱 전용 캐시 폴더에 두고(다른 앱이 못 읽고 지우기 쉬움),
 * OpenStreetMap 이용 조건에 맞게 앱 패키지명을 User-Agent로 보낸다.
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
    }
}

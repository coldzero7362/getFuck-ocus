package com.getfuckocus.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.FocusTopBar
import com.getfuckocus.ui.components.Text
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import com.getfuckocus.ui.theme.DigitStyle
import com.getfuckocus.ui.theme.plate
import com.getfuckocus.verify.PlaceMiss
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

/**
 * 위치 체크가 장소 밖이라 실패했을 때 띄우는 전체 화면.
 * 지도에 지금 위치와 정한 장소(반경 원)를 함께 놓고 직선으로 잇고, 거리·방향·남은 거리를 크게 보여 준다.
 * @param otherReasons 장소 말고도 걸린 조건 (예: 인증 시간이 아님)
 */
@Composable
fun PlaceMissDialog(
    taskTitle: String,
    miss: PlaceMiss,
    otherReasons: List<String>,
    checking: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 대화상자 창에는 시스템 바 인셋이 오지 않아서, 앱 화면에서 잰 내비게이션 바 높이만큼 아래를 비운다
    val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Chrome.canvas),
        ) {
            FocusTopBar(
                title = "장소에서 벗어났어요",
                subtitle = "'$taskTitle' 위치 체크",
                onBack = onDismiss,
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScreenBezel(
                    center = GeoPoint(miss.hereLat, miss.hereLng),
                    busy = checking,
                    label = "MAP · MISS",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    MissMap(miss, Modifier.fillMaxSize())
                    MapSkinOverlay(Modifier.fillMaxSize())
                    MapLegend(Modifier.align(Alignment.TopStart).padding(8.dp))
                }
                DistanceReadout(miss, otherReasons)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Chrome.carbon)
                    .padding(bottom = navBottom)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FocusButton(
                    "닫기", onClick = onDismiss,
                    style = FocusButtonStyle.Outline, height = 56.dp,
                    modifier = Modifier.weight(1f),
                )
                FocusButton(
                    if (checking) "확인하는 중…" else "다시 확인하기",
                    onClick = onRetry,
                    enabled = !checking,
                    icon = VerifyMethod.LOCATION.actionIcon,
                    height = 56.dp,
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}

/** 지도 위 범례: 주황 점 = 지금 위치, 초록 원 = 정한 장소 */
@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Chrome.carbon.copy(alpha = 0.82f), ChromeShapes.control)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(Chrome.signal, CircleShape).border(1.5.dp, Color.White, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text("지금 위치", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(10.dp).background(Chrome.success.copy(alpha = 0.4f), CircleShape).border(1.5.dp, Chrome.teal, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text("정한 장소", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** 지금 위치와 장소를 한 화면에 담도록 맞춰 둔 지도. 손가락으로 움직이거나 확대할 수는 있다 */
@Composable
private fun MissMap(miss: PlaceMiss, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val here = GeoPoint(miss.hereLat, miss.hereLng)
    val target = GeoPoint(miss.targetLat, miss.targetLng)
    val mapView = remember(miss) {
        createMapView(context, here).apply {
            // 아주 멀리 있어도 두 점이 한 화면에 들어오게 장소 고르기보다 더 멀리 뺄 수 있게 한다
            minZoomLevel = 3.0
        }
    }

    DisposableEffect(mapView) {
        val circlePoints = Polygon.pointsAsCircle(target, miss.radiusMeters.toDouble())
        val area = Polygon(mapView).apply {
            points = circlePoints
            fillPaint.color = Chrome.success.copy(alpha = 0.22f).toArgb()
            outlinePaint.color = Chrome.teal.toArgb()
            outlinePaint.strokeWidth = 6f
            infoWindow = null
        }
        val accuracy = Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(here, miss.accuracyMeters.toDouble().coerceAtLeast(1.0))
            fillPaint.color = Chrome.signal.copy(alpha = 0.16f).toArgb()
            outlinePaint.color = Chrome.signal.copy(alpha = 0.5f).toArgb()
            outlinePaint.strokeWidth = 2f
            infoWindow = null
        }
        val line = Polyline(mapView).apply {
            setPoints(listOf(here, target))
            outlinePaint.color = Chrome.carbon.toArgb()
            outlinePaint.strokeWidth = with(density) { 3.dp.toPx() }
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(with(density) { 8.dp.toPx() }, with(density) { 6.dp.toPx() }), 0f)
            infoWindow = null
        }
        fun dot(fill: Int, ring: Int, sizeDp: Int): BitmapDrawable {
            val px = with(density) { sizeDp.dp.toPx() }.roundToInt()
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = ring
            c.drawCircle(px / 2f, px / 2f, px / 2f, p)
            p.color = fill
            c.drawCircle(px / 2f, px / 2f, px / 2f - px * 0.18f, p)
            return BitmapDrawable(context.resources, bmp)
        }
        val targetMarker = Marker(mapView).apply {
            position = target
            icon = dot(Chrome.teal.toArgb(), Color.White.toArgb(), 14)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
        }
        val hereMarker = Marker(mapView).apply {
            position = here
            icon = dot(Chrome.signal.toArgb(), Color.White.toArgb(), 20)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
        }
        mapView.overlays.addAll(listOf(area, accuracy, line, targetMarker, hereMarker))

        val box = BoundingBox.fromGeoPointsSafe(circlePoints + here)
        val border = with(density) { 36.dp.toPx() }.roundToInt()
        mapView.addOnFirstLayoutListener { _, _, _, _, _ -> mapView.zoomToBoundingBox(box, false, border) }
        onDispose { mapView.onDetach() }
    }
    LifecycleResumeEffect(mapView) {
        mapView.onResume()
        onPauseOrDispose { mapView.onPause() }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

@Composable
private fun DistanceReadout(miss: PlaceMiss, otherReasons: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .plate(Chrome.platinum)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("정한 장소까지 직선거리", style = MaterialTheme.typography.labelLarge, color = Chrome.chromeIndigo)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatDistance(miss.distanceMeters),
                style = DigitStyle.copy(fontSize = 44.sp),
                color = Chrome.redInk,
                maxLines = 1,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${directionOf(miss.bearingDegrees)} 쪽",
                style = MaterialTheme.typography.titleMedium,
                color = Chrome.carbon,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            "반경 ${miss.radiusMeters.roundToInt()}m 안까지 ${formatDistance(miss.toEdgeMeters)} 더 가야 해요",
            style = MaterialTheme.typography.bodyLarge,
            color = Chrome.carbon,
        )
        Text(
            "지금 위치 오차 ±${miss.accuracyMeters.roundToInt()}m",
            style = MaterialTheme.typography.bodySmall,
            color = Chrome.chromeIndigo,
        )
        otherReasons.forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Chrome.redInk)
        }
    }
}

/** 장소 말고 걸린 이유들. 장소 사유("…m 떨어져 있어요")는 화면이 대신 보여 주므로 뺀다 */
internal fun placeMissOthers(result: com.getfuckocus.verify.CheckInResult): List<String> =
    (result.verdict as? com.getfuckocus.verify.Verdict.Fail)?.reasons.orEmpty()
        .filterNot { it.startsWith("지정한 장소에서") }

/** 1km 아래는 m, 그 위는 km (100km 아래는 소수 한 자리) */
internal fun formatDistance(meters: Float): String = when {
    meters < 1000f -> "${meters.roundToInt()}m"
    meters < 100_000f -> "%.1fkm".format(meters / 1000f)
    else -> "${(meters / 1000f).roundToInt()}km"
}

/** 방위각(북=0, 시계 방향)을 여덟 방향 이름으로 */
internal fun directionOf(bearing: Float): String {
    val names = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")
    val normalized = ((bearing % 360f) + 360f) % 360f
    return names[((normalized + 22.5f) / 45f).toInt() % 8]
}

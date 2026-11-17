package com.getfuckocus.verify

import android.location.Location
import android.os.Build
import com.getfuckocus.data.NfcTagEntity
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.task.TaskSchedule
import kotlin.math.abs
import kotlin.math.min

/** 인증 방식들이 함께 쓰는 판정: 시간창, 장소, NFC 태그. */
object ConditionChecks {
    private const val LOCATION_FRESHNESS_MS = 5 * 60 * 1000L
    private const val MAX_ACCURACY_SLACK_M = 50f

    fun windowReason(task: TaskEntity, inWindow: Boolean): String? =
        if (inWindow) null else "지금은 인증 가능 시간이 아니에요 · ${TaskSchedule.describeWindow(task)}"

    /** 장소 조건을 판정할 수 있는 위치인지: 있고, 모의 위치가 아니고, 측정 시각이 [atMillis]와 가깝다. */
    fun usable(location: Location?, atMillis: Long): Boolean =
        location != null && !location.isMockLocation() && abs(location.time - atMillis) <= LOCATION_FRESHNESS_MS

    /**
     * 장소 조건 판정. 장소 조건이 없는 과제면 빈 목록.
     * @param atMillis 인증 시각(기기 시계). 위치 측정 시각과 크게 다르면 시계 조작을 의심한다.
     */
    fun placeReasons(task: TaskEntity, location: Location?, atMillis: Long): List<String> {
        val lat = task.geoLat ?: return emptyList()
        val lng = task.geoLng ?: return emptyList()
        val radius = task.geoRadiusMeters ?: return emptyList()
        return when {
            location == null -> listOf("위치를 확인할 수 없어요")
            location.isMockLocation() -> listOf("모의 위치는 인정되지 않아요")
            abs(location.time - atMillis) > LOCATION_FRESHNESS_MS -> listOf("위치 정보 시각이 인증 시각과 맞지 않아요")
            else -> {
                val distance = distanceMeters(location.latitude, location.longitude, lat, lng)
                val allowed = radius + min(location.accuracy, MAX_ACCURACY_SLACK_M)
                if (distance > allowed) listOf("지정한 장소에서 ${distance.toInt()}m 떨어져 있어요") else emptyList()
            }
        }
    }

    /**
     * 장소에서 벗어나 실패했을 때, 지금 위치와 정한 장소가 얼마나 떨어졌는지. 실패 화면의 지도와 거리 표시에 쓴다.
     * 위치를 믿을 수 없거나 반경 안이면 null.
     */
    fun placeMiss(task: TaskEntity, location: Location?, atMillis: Long): PlaceMiss? {
        val lat = task.geoLat ?: return null
        val lng = task.geoLng ?: return null
        val radius = task.geoRadiusMeters ?: return null
        if (!usable(location, atMillis)) return null
        location!!
        val distance = distanceMeters(location.latitude, location.longitude, lat, lng)
        val slack = min(location.accuracy, MAX_ACCURACY_SLACK_M)
        if (distance <= radius + slack) return null
        val result = FloatArray(2)
        Location.distanceBetween(location.latitude, location.longitude, lat, lng, result)
        return PlaceMiss(
            hereLat = location.latitude,
            hereLng = location.longitude,
            targetLat = lat,
            targetLng = lng,
            radiusMeters = radius,
            distanceMeters = distance,
            accuracyMeters = location.accuracy,
            bearingDegrees = result[1],
        )
    }

    /**
     * NFC 인증 판정. [expected]는 과제에 고른 태그 (지워졌으면 null), [scannedUid]는 방금 찍은 태그 ID.
     */
    /** 고른 태그([expected]) 중 하나를 찍었으면 null, 아니면 실패 이유 */
    fun tagReason(expected: List<NfcTagEntity>, scannedUid: String?): String? = when {
        expected.isEmpty() -> "이 과제에 고른 태그를 찾을 수 없어요. 과제 편집에서 태그를 다시 골라 주세요"
        scannedUid == null -> "태그를 찍지 않았어요"
        expected.none { it.uid == scannedUid } ->
            "${expected.joinToString(", ") { "'${it.name}'" }} 태그가 아니에요. 이 과제에 고른 태그를 찍어 주세요"
        else -> null
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    private fun Location.isMockLocation(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock
        else @Suppress("DEPRECATION") isFromMockProvider
}

/**
 * 정한 장소 밖에서 체크했을 때의 위치 차이.
 * [distanceMeters]는 지금 위치에서 장소 가운데까지의 직선거리, [bearingDegrees]는 지금 위치에서 본 장소 쪽 방위(북=0, 시계 방향).
 */
data class PlaceMiss(
    val hereLat: Double,
    val hereLng: Double,
    val targetLat: Double,
    val targetLng: Double,
    val radiusMeters: Float,
    val distanceMeters: Float,
    val accuracyMeters: Float,
    val bearingDegrees: Float,
) {
    /** 반경 가장자리까지 더 가야 하는 거리 */
    val toEdgeMeters: Float get() = (distanceMeters - radiusMeters).coerceAtLeast(0f)
}

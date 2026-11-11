package com.getfuckocus.verify

import android.location.Location
import android.os.Build
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.task.TaskSchedule
import kotlin.math.abs
import kotlin.math.min

/** 사진 인증과 탭 체크가 함께 쓰는 판정: 시간창과 장소. */
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

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    private fun Location.isMockLocation(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock
        else @Suppress("DEPRECATION") isFromMockProvider
}

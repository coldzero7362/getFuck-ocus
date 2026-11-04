package com.getfuckocus.verify

import android.location.Location
import android.os.Build
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.data.requiredLabels
import com.getfuckocus.task.TaskSchedule
import kotlin.math.abs
import kotlin.math.min

/**
 * 과제 조건과 촬영 증거를 비교한다.
 * 완벽한 위조 방지는 목표가 아니다. 자기 약속을 깨기 귀찮을 만큼의 마찰이면 충분하다.
 */
class PhotoVerifier {

    /**
     * @param inWindow 촬영 시각이 어느 회차의 시간창 안이면 true.
     *   이 과제 때문에 잠긴 상태라면 마감 뒤라도 true로 넘겨 늦은 인증으로 잠금을 풀 수 있게 한다.
     */
    fun verify(task: TaskEntity, evidence: Evidence, inWindow: Boolean): Verdict {
        val reasons = mutableListOf<String>()
        val capturedMillis = evidence.capturedAt.wallMillis

        // 1. 시간창
        if (!inWindow) {
            reasons += "지금은 인증 가능 시간이 아니에요 · ${TaskSchedule.describeWindow(task)}"
        }

        // 2. EXIF 교차검증: 파일에 기록된 촬영 시각이 셔터 시점과 크게 다르면 거부
        evidence.exif.dateTimeOriginalMillis?.let { exifMillis ->
            if (abs(exifMillis - capturedMillis) > EXIF_TIME_TOLERANCE_MS) {
                reasons += "사진 메타데이터의 촬영 시각이 일치하지 않아요"
            }
        }

        // 3. 위치
        if (task.hasGeoFence()) {
            val location = evidence.location
            when {
                location == null -> reasons += "위치를 확인할 수 없어요"
                location.isMockLocation() -> reasons += "모의 위치는 인정되지 않아요"
                // 위치 측정 시각(위성/네트워크 기준)과 기기 시계가 크게 다르면 시계 조작을 의심한다
                abs(location.time - capturedMillis) > LOCATION_FRESHNESS_MS ->
                    reasons += "위치 정보 시각이 촬영 시각과 맞지 않아요"
                else -> {
                    val distance = distanceMeters(location.latitude, location.longitude, task.geoLat!!, task.geoLng!!)
                    val allowed = task.geoRadiusMeters!! + min(location.accuracy, MAX_ACCURACY_SLACK_M)
                    if (distance > allowed) reasons += "지정한 장소에서 ${distance.toInt()}m 떨어져 있어요"
                    if (evidence.exif.latitude != null && evidence.exif.longitude != null) {
                        val exifGap = distanceMeters(
                            evidence.exif.latitude, evidence.exif.longitude,
                            location.latitude, location.longitude,
                        )
                        if (exifGap > EXIF_LOCATION_TOLERANCE_M) reasons += "사진 메타데이터의 위치가 일치하지 않아요"
                    }
                }
            }
        }

        // 4. 이미지 내용 (ML Kit 라벨)
        val requiredLabels = task.requiredLabels()
        if (requiredLabels.isNotEmpty()) {
            val found = evidence.labels.any { detected ->
                detected.confidence >= task.minLabelConfidence &&
                    requiredLabels.any { it.equals(detected.text, ignoreCase = true) }
            }
            val nearMiss = evidence.labels.filter { detected ->
                requiredLabels.any { it.equals(detected.text, ignoreCase = true) }
            }
            if (!found && nearMiss.isNotEmpty()) {
                // 라벨은 맞는데 신뢰도가 기준에 못 미친 경우: 무엇이 부족한지 그대로 알려준다
                val threshold = (task.minLabelConfidence * 100).toInt()
                reasons += nearMiss.joinToString(", ") { "'${it.text}' ${(it.confidence * 100).toInt()}%" } +
                    "로 인식됐지만 기준(${threshold}%)보다 낮아요. 대상을 더 크게, 밝게 찍어 보세요"
            } else if (!found) {
                // 실제로 인식된 라벨 목록은 촬영 화면이 칩으로 보여준다
                reasons += "사진에서 ${requiredLabels.joinToString(", ") { "'$it'" }} 중 어느 것도 찾지 못했어요"
                if (evidence.labels.isEmpty()) reasons += "사진에서 인식된 라벨이 없어요"
            }
        }

        return if (reasons.isEmpty()) Verdict.Pass else Verdict.Fail(reasons)
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    private fun Location.isMockLocation(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock
        else @Suppress("DEPRECATION") isFromMockProvider

    private companion object {
        const val EXIF_TIME_TOLERANCE_MS = 2 * 60 * 1000L
        const val LOCATION_FRESHNESS_MS = 5 * 60 * 1000L
        const val MAX_ACCURACY_SLACK_M = 50f
        const val EXIF_LOCATION_TOLERANCE_M = 50f
    }
}

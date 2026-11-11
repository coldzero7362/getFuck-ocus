package com.getfuckocus.verify

import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.data.requiredLabels
import kotlin.math.abs

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
        ConditionChecks.windowReason(task, inWindow)?.let { reasons += it }

        // 2. EXIF 교차검증: 파일에 기록된 촬영 시각이 셔터 시점과 크게 다르면 거부
        evidence.exif.dateTimeOriginalMillis?.let { exifMillis ->
            if (abs(exifMillis - capturedMillis) > EXIF_TIME_TOLERANCE_MS) {
                reasons += "사진 메타데이터의 촬영 시각이 일치하지 않아요"
            }
        }

        // 3. 위치 (탭 체크와 같은 판정) + 사진 EXIF 위치 교차검증
        if (task.hasGeoFence()) {
            val location = evidence.location
            reasons += ConditionChecks.placeReasons(task, location, capturedMillis)
            if (location != null && ConditionChecks.usable(location, capturedMillis) &&
                evidence.exif.latitude != null && evidence.exif.longitude != null
            ) {
                val exifGap = ConditionChecks.distanceMeters(
                    evidence.exif.latitude, evidence.exif.longitude,
                    location.latitude, location.longitude,
                )
                if (exifGap > EXIF_LOCATION_TOLERANCE_M) reasons += "사진 메타데이터의 위치가 일치하지 않아요"
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

    private companion object {
        const val EXIF_TIME_TOLERANCE_MS = 2 * 60 * 1000L
        const val EXIF_LOCATION_TOLERANCE_M = 50f
    }
}

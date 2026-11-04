package com.getfuckocus.verify

import android.location.Location
import com.getfuckocus.safety.Stamp
import java.io.File

data class Evidence(
    val photo: File,
    /** 셔터를 누른 순간 앱이 기록한 시각. 파일의 EXIF가 아니라 이 값을 기준으로 판정한다. */
    val capturedAt: Stamp,
    val location: Location?,
    val labels: List<DetectedLabel>,
    /** 저장된 파일의 EXIF. 앱 기록과 교차검증하는 보조 자료다. */
    val exif: ExifSummary,
)

data class DetectedLabel(val text: String, val confidence: Float)

data class ExifSummary(
    val dateTimeOriginalMillis: Long?,
    val latitude: Double?,
    val longitude: Double?,
)

sealed interface Verdict {
    data object Pass : Verdict
    data class Fail(val reasons: List<String>) : Verdict
}

/** 촬영 화면에 보여줄 인증 결과. */
data class VerificationResult(
    val verdict: Verdict,
    val labels: List<DetectedLabel>,
    /** 이 인증으로 잠금이 풀렸는지 */
    val releasedLock: Boolean,
)

package com.getfuckocus.verify

import androidx.camera.core.ImageCapture
import com.getfuckocus.data.AttemptEntity
import com.getfuckocus.data.NfcTagDao
import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.lock.LockController
import com.getfuckocus.safety.DeviceClock
import com.getfuckocus.task.AchievementRecorder
import com.getfuckocus.task.CheckInTarget
import com.getfuckocus.task.TaskSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 사진 인증: 촬영 → 판정 → 기록 → (통과 시) 잠금 해제. 탭·위치·NFC: 판정 → 기록 → (통과 시) 잠금 해제. */
class VerificationFlow(
    private val dao: TaskDao,
    private val nfcTags: NfcTagDao,
    private val collector: EvidenceCollector,
    private val verifier: PhotoVerifier,
    private val lock: LockController,
    private val clock: DeviceClock,
    private val achievements: AchievementRecorder,
) {
    suspend fun run(task: TaskEntity, imageCapture: ImageCapture): VerificationResult {
        // 장소 조건이 있는 과제만 위치를 측정한다
        val location = if (task.hasGeoFence()) collector.currentLocation() else null
        // 시계를 뒤로 돌려 시간창 안인 척하지 못하게, 믿을 수 있는 시각과 기기 시계의 차이만큼 보정한다
        val clockShift = clock.trustedNowMillis() - System.currentTimeMillis()
        val evidence = collector.capture(imageCapture, location)

        val capturedMillis = evidence.capturedAt.wallMillis + clockShift
        val captured = Instant.ofEpochMilli(capturedMillis).atZone(ZoneId.systemDefault())
        // 시간창 안이면 그 회차, 이 과제 때문에 잠겨 있으면 잠금의 원인이 된 회차로 늦은 인증을 받는다
        val lockedByThis = lock.current()?.takeIf { it.taskId == task.id }
        val occurrence = TaskSchedule.occurrenceContaining(task, captured)
            ?: lockedByThis?.let { LocalDate.parse(it.dayKey) }
        val verdict = verifier.verify(task, evidence, inWindow = occurrence != null)
        val passed = verdict is Verdict.Pass

        dao.insertAttempt(
            AttemptEntity(
                taskId = task.id,
                dayKey = TaskSchedule.dayKey(occurrence ?: captured.toLocalDate()),
                capturedAt = capturedMillis,
                photoPath = evidence.photo.absolutePath,
                latitude = location?.latitude,
                longitude = location?.longitude,
                labels = evidence.labels.joinToString(",") { "${it.text}:${"%.2f".format(it.confidence)}" },
                passed = passed,
                reasons = (verdict as? Verdict.Fail)?.reasons?.joinToString("\n").orEmpty(),
            )
        )

        if (passed) {
            lock.releaseIfVerified(task.id)
            achievements.onPassed(task)
        }
        return VerificationResult(verdict, evidence.labels, releasedLock = passed && lockedByThis != null)
    }

    /**
     * 카메라 없는 인증 (탭 체크, 위치, NFC): 조건을 판정해 회차를 기록하고, 통과하면 이 과제로 걸린 잠금·억제 화면을 푼다.
     * 마감 뒤에도 늦은 체크로 기록할 수 있다.
     * 장소 조건이 있으면 위치 권한이 필요하다 (호출하는 쪽에서 먼저 받는다).
     * @param scannedUid NFC 인증에서 방금 찍은 태그 ID
     */
    suspend fun checkIn(task: TaskEntity, scannedUid: String? = null): CheckInResult {
        val wallNow = System.currentTimeMillis()
        val trustedNow = clock.trustedNowMillis()
        val now = Instant.ofEpochMilli(trustedNow).atZone(ZoneId.systemDefault())
        // 시간창 안이면 그 회차, 이 과제 때문에 잠겨 있으면 잠금의 원인이 된 회차, 아니면 가장 최근에 놓친 회차
        val lockedByThis = lock.current()?.takeIf { it.taskId == task.id }
        val target = TaskSchedule.occurrenceContaining(task, now)?.let { CheckInTarget(it, late = false) }
            ?: lockedByThis?.let { CheckInTarget(LocalDate.parse(it.dayKey), late = true) }
            ?: TaskSchedule.checkInOccurrence(task, now)
        // 탭 체크는 장소를 보지 않는다. NFC는 태그가 놓인 자리가 곧 장소다
        val checksPlace = task.verifyMethod == VerifyMethod.LOCATION || task.verifyMethod == VerifyMethod.PHOTO
        val location = if (checksPlace && task.hasGeoFence()) collector.currentLocation() else null

        val reasons = buildList {
            ConditionChecks.windowReason(task, inWindow = target != null)?.let { add(it) }
            if (task.verifyMethod == VerifyMethod.LOCATION && !task.hasGeoFence()) {
                add("장소가 지정되지 않았어요. 과제 편집에서 장소를 정해 주세요")
            }
            if (checksPlace) addAll(ConditionChecks.placeReasons(task, location, wallNow))
            if (task.verifyMethod == VerifyMethod.NFC) {
                ConditionChecks.tagReason(task.nfcTagId?.let { nfcTags.get(it) }, scannedUid)?.let { add(it) }
            }
        }
        val verdict = if (reasons.isEmpty()) Verdict.Pass else Verdict.Fail(reasons)
        val passed = verdict is Verdict.Pass

        dao.insertAttempt(
            AttemptEntity(
                taskId = task.id,
                dayKey = TaskSchedule.dayKey(target?.date ?: now.toLocalDate()),
                capturedAt = trustedNow,
                photoPath = "",
                latitude = location?.latitude,
                longitude = location?.longitude,
                labels = "",
                passed = passed,
                reasons = reasons.joinToString("\n"),
                method = task.verifyMethod,
            )
        )
        if (passed) {
            lock.releaseIfVerified(task.id)
            achievements.onPassed(task)
        }
        return CheckInResult(verdict, late = target?.late == true, releasedLock = passed && lockedByThis != null)
    }
}

/**
 * 카메라 없는 인증의 결과. [late]면 마감 뒤에 기록했다 (기록 화면에서 "늦게 통과"로 보인다).
 * [releasedLock]이면 이 인증으로 잠금이나 억제 화면이 풀렸다.
 */
data class CheckInResult(val verdict: Verdict, val late: Boolean, val releasedLock: Boolean = false)

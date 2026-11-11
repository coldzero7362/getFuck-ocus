package com.getfuckocus.verify

import androidx.camera.core.ImageCapture
import com.getfuckocus.data.AttemptEntity
import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.lock.LockController
import com.getfuckocus.safety.DeviceClock
import com.getfuckocus.task.TaskSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 사진 인증: 촬영 → 판정 → 기록 → (통과 시) 잠금 해제. 탭 체크: 판정 → 기록. */
class VerificationFlow(
    private val dao: TaskDao,
    private val collector: EvidenceCollector,
    private val verifier: PhotoVerifier,
    private val lock: LockController,
    private val clock: DeviceClock,
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

        if (passed) lock.releaseIfVerified(task.id)
        return VerificationResult(verdict, evidence.labels, releasedLock = passed && lockedByThis != null)
    }

    /**
     * 탭 체크: 버튼 한 번으로 회차를 기록한다. 장소 조건이 있으면 위치만 확인한다.
     * 탭 체크 과제는 잠그지 않으므로 잠금 해제는 하지 않는다. 마감 뒤에도 늦은 체크로 기록할 수 있다.
     * 장소 조건이 있으면 위치 권한이 필요하다 (호출하는 쪽에서 먼저 받는다).
     */
    suspend fun checkIn(task: TaskEntity): CheckInResult {
        val wallNow = System.currentTimeMillis()
        val trustedNow = clock.trustedNowMillis()
        val now = Instant.ofEpochMilli(trustedNow).atZone(ZoneId.systemDefault())
        val target = TaskSchedule.checkInOccurrence(task, now)
        val location = if (task.hasGeoFence()) collector.currentLocation() else null

        val reasons = buildList {
            ConditionChecks.windowReason(task, inWindow = target != null)?.let(::add)
            addAll(ConditionChecks.placeReasons(task, location, wallNow))
        }
        val verdict = if (reasons.isEmpty()) Verdict.Pass else Verdict.Fail(reasons)

        dao.insertAttempt(
            AttemptEntity(
                taskId = task.id,
                dayKey = TaskSchedule.dayKey(target?.date ?: now.toLocalDate()),
                capturedAt = trustedNow,
                photoPath = "",
                latitude = location?.latitude,
                longitude = location?.longitude,
                labels = "",
                passed = verdict is Verdict.Pass,
                reasons = reasons.joinToString("\n"),
                method = VerifyMethod.TAP,
            )
        )
        return CheckInResult(verdict, late = target?.late == true)
    }
}

/** 탭 체크 결과. [late]면 마감 뒤에 기록했다 (기록 화면에서 "늦게 통과"로 보인다). */
data class CheckInResult(val verdict: Verdict, val late: Boolean)

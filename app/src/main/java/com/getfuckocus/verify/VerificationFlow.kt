package com.getfuckocus.verify

import androidx.camera.core.ImageCapture
import com.getfuckocus.data.AttemptEntity
import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.data.hasGeoFence
import com.getfuckocus.lock.LockController
import com.getfuckocus.task.TaskSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 촬영 → 판정 → 기록 → (통과 시) 잠금 해제. */
class VerificationFlow(
    private val dao: TaskDao,
    private val collector: EvidenceCollector,
    private val verifier: PhotoVerifier,
    private val lock: LockController,
) {
    suspend fun run(task: TaskEntity, imageCapture: ImageCapture): VerificationResult {
        // 장소 조건이 있는 과제만 위치를 측정한다
        val location = if (task.hasGeoFence()) collector.currentLocation() else null
        val evidence = collector.capture(imageCapture, location)

        val captured = Instant.ofEpochMilli(evidence.capturedAt.wallMillis).atZone(ZoneId.systemDefault())
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
                capturedAt = evidence.capturedAt.wallMillis,
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
}

package com.getfuckocus.safety

import com.getfuckocus.data.TaskDao
import java.io.File
import java.util.concurrent.TimeUnit

/** 보존 기간이 지난 인증 사진과 기록을 지운다. */
class PhotoRetention(private val dao: TaskDao) {
    suspend fun purgeExpired(now: Long = System.currentTimeMillis()) {
        val cutoff = now - TimeUnit.DAYS.toMillis(SafetyPolicy.PHOTO_RETENTION_DAYS)
        dao.attemptsBefore(cutoff).forEach { File(it.photoPath).delete() }
        dao.deleteAttemptsBefore(cutoff)
    }
}

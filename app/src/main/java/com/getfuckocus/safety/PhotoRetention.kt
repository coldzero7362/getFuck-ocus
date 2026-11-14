package com.getfuckocus.safety

import android.content.Context
import com.getfuckocus.data.TaskDao
import com.getfuckocus.verify.EvidenceCollector
import java.util.concurrent.TimeUnit

/** 보존 기간이 지난 인증 사진을 지운다. 성공·실패 기록은 남겨 기록 화면에서 계속 보이게 한다. */
class PhotoRetention(private val context: Context, private val dao: TaskDao) {
    suspend fun purgeExpired(now: Long = System.currentTimeMillis()) {
        val cutoff = now - TimeUnit.DAYS.toMillis(SafetyPolicy.PHOTO_RETENTION_DAYS)
        dao.photosBefore(cutoff).forEach { EvidenceCollector.deleteEvidence(context, it.photoPath) }
        dao.clearPhotosBefore(cutoff)
    }
}

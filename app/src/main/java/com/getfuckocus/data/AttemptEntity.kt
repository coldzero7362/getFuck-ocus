package com.getfuckocus.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 인증 시도 한 번의 기록. 사진은 앱 내부 저장소에만 있고 보존 기간이 지나면 지워진다.
 * 기록 자체는 남겨서 기록 화면의 달력과 연속 달성에 쓴다 (그때 photoPath는 빈 문자열).
 */
@Entity(tableName = "attempts", indices = [Index("taskId", "dayKey")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    /** 회차의 시작 날짜 (yyyy-MM-dd). 자정을 넘는 과제는 촬영 날짜와 다를 수 있다. */
    val dayKey: String,
    val capturedAt: Long,
    val photoPath: String,
    val latitude: Double?,
    val longitude: Double?,
    /** "Book:0.92,Table:0.71" 형식 */
    val labels: String,
    val passed: Boolean,
    /** 실패 사유, 줄바꿈으로 구분 */
    val reasons: String,
)

/** 어느 과제의 어느 회차(시작 날짜)가 통과했는지. */
data class PassedOccurrence(val taskId: Long, val dayKey: String)

/** 회차를 처음 통과한 시각. */
data class PassRecord(val taskId: Long, val dayKey: String, val firstPassedAt: Long)

package com.getfuckocus.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY windowStartMinute")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE enabled = 1")
    suspend fun enabledTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    /** 이 NFC 태그로 인증하는 과제 이름들. 쓰는 중인 태그는 지우지 못하게 한다. */
    @Query("SELECT title FROM tasks WHERE nfcTagId = :tagId")
    suspend fun titlesUsingTag(tagId: Long): List<String>

    @Query("UPDATE tasks SET startedAt = :startedAt WHERE id = :id")
    suspend fun markStarted(id: Long, startedAt: Long)

    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM attempts WHERE taskId = :taskId AND dayKey = :dayKey AND passed = 1)")
    suspend fun passedOn(taskId: Long, dayKey: String): Boolean

    @Query("SELECT DISTINCT taskId, dayKey FROM attempts WHERE passed = 1 AND dayKey IN (:dayKeys)")
    fun observePassed(dayKeys: List<String>): Flow<List<PassedOccurrence>>

    /** 최근 인증 사진들에서 인식된 라벨 문자열 ("Book:0.92,Table:0.71" 형식) */
    @Query("SELECT labels FROM attempts ORDER BY id DESC LIMIT :limit")
    suspend fun recentLabelStrings(limit: Int): List<String>

    /** 회차마다 처음 통과한 시각. 기록 화면에서 제때 통과했는지, 늦게 통과했는지 가른다. */
    @Query("SELECT taskId, dayKey, MIN(capturedAt) AS firstPassedAt FROM attempts WHERE passed = 1 GROUP BY taskId, dayKey")
    fun observePassRecords(): Flow<List<PassRecord>>

    @Query("SELECT * FROM attempts WHERE dayKey = :dayKey ORDER BY capturedAt")
    fun observeAttemptsOn(dayKey: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE capturedAt < :before AND photoPath != ''")
    suspend fun photosBefore(before: Long): List<AttemptEntity>

    /** 사진 파일만 지우고 기록은 남긴다 (기록 화면에서 결과는 계속 보이게) */
    @Query("UPDATE attempts SET photoPath = '' WHERE capturedAt < :before")
    suspend fun clearPhotosBefore(before: Long)
}

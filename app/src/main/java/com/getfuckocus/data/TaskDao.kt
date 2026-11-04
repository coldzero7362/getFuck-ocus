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

    @Query("SELECT * FROM attempts WHERE capturedAt < :before")
    suspend fun attemptsBefore(before: Long): List<AttemptEntity>

    @Query("DELETE FROM attempts WHERE capturedAt < :before")
    suspend fun deleteAttemptsBefore(before: Long)
}

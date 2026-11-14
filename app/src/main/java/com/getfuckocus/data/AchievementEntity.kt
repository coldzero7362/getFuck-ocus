package com.getfuckocus.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 과제 하나가 연속 달성 [milestone]회에 처음 닿은 기록. 한 과제의 같은 단계는 한 번만 받는다.
 * [seen]이 false면 아직 화면 위 알림으로 보여 주지 않았다 (알림 버튼으로 체크해 앱이 꺼져 있을 때 등).
 */
@Entity(tableName = "achievements", primaryKeys = ["taskId", "milestone"])
data class AchievementEntity(
    val taskId: Long,
    val milestone: Int,
    val achievedAt: Long,
    val seen: Boolean = false,
)

@Dao
interface AchievementDao {
    /** 이미 받은 단계면 무시한다 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Query("SELECT milestone FROM achievements WHERE taskId = :taskId")
    suspend fun milestonesOf(taskId: Long): List<Int>

    @Query("SELECT * FROM achievements WHERE seen = 0 ORDER BY achievedAt, milestone")
    fun observeUnseen(): Flow<List<AchievementEntity>>

    @Query("UPDATE achievements SET seen = 1 WHERE taskId = :taskId AND milestone = :milestone")
    suspend fun markSeen(taskId: Long, milestone: Int)

    @Query("DELETE FROM achievements WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)
}

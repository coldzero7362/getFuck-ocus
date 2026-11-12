package com.getfuckocus.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 체크할 때 찍는 NFC 태그. 태그 안의 데이터는 읽지도 쓰지도 않고, 태그 고유 ID(UID)만 기억한다.
 * 같은 태그를 두 번 등록하지 않도록 uid는 겹치지 않는다.
 */
@Entity(tableName = "nfc_tags", indices = [Index(value = ["uid"], unique = true)])
data class NfcTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "04:A2:3F:..." 형식의 대문자 16진수 */
    val uid: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface NfcTagDao {
    @Query("SELECT * FROM nfc_tags ORDER BY createdAt")
    fun observeAll(): Flow<List<NfcTagEntity>>

    @Query("SELECT * FROM nfc_tags WHERE id = :id")
    suspend fun get(id: Long): NfcTagEntity?

    @Query("SELECT * FROM nfc_tags ORDER BY createdAt")
    suspend fun all(): List<NfcTagEntity>

    @Query("SELECT * FROM nfc_tags WHERE uid = :uid")
    suspend fun findByUid(uid: String): NfcTagEntity?

    @Query("SELECT COUNT(*) FROM nfc_tags")
    suspend fun count(): Int

    @Insert
    suspend fun insert(tag: NfcTagEntity)

    @Delete
    suspend fun delete(tag: NfcTagEntity)
}

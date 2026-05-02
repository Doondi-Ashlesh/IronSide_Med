package com.ironsidemedical.connect.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironsidemedical.connect.data.local.database.entities.VitalSignsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalSignsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VitalSignsEntity): Long

    @Query("SELECT * FROM vital_signs WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getBySession(sessionId: String): Flow<List<VitalSignsEntity>>

    @Query("SELECT * FROM vital_signs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<VitalSignsEntity>>

    @Query("""
        SELECT * FROM vital_signs
        WHERE timestamp BETWEEN :fromMs AND :toMs
        ORDER BY timestamp ASC
    """)
    suspend fun getBetween(fromMs: Long, toMs: Long): List<VitalSignsEntity>

    @Query("DELETE FROM vital_signs WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("SELECT COUNT(*) FROM vital_signs WHERE session_id = :sessionId")
    suspend fun countForSession(sessionId: String): Int
}

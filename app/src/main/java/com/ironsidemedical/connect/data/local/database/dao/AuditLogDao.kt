package com.ironsidemedical.connect.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironsidemedical.connect.data.local.database.entities.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AuditLogEntity): Long

    @Query("SELECT * FROM audit_log ORDER BY timestamp ASC")
    fun getAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT chain_hash FROM audit_log ORDER BY id DESC LIMIT 1")
    suspend fun getLastHash(): String?

    @Query("SELECT * FROM audit_log ORDER BY id DESC LIMIT 1")
    suspend fun getLatestEntry(): AuditLogEntity?

    @Query("SELECT COUNT(*) FROM audit_log")
    suspend fun count(): Int
}

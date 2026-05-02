package com.ironsidemedical.connect.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironsidemedical.connect.data.local.database.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceEntity)

    @Query("SELECT * FROM paired_devices ORDER BY last_connected_epoch_ms DESC")
    fun getAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM paired_devices WHERE address = :address")
    suspend fun getByAddress(address: String): DeviceEntity?

    @Query("DELETE FROM paired_devices WHERE address = :address")
    suspend fun deleteByAddress(address: String)
}

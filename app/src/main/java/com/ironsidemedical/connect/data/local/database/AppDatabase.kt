package com.ironsidemedical.connect.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ironsidemedical.connect.data.local.database.dao.AuditLogDao
import com.ironsidemedical.connect.data.local.database.dao.DeviceDao
import com.ironsidemedical.connect.data.local.database.dao.VitalSignsDao
import com.ironsidemedical.connect.data.local.database.entities.AuditLogEntity
import com.ironsidemedical.connect.data.local.database.entities.DeviceEntity
import com.ironsidemedical.connect.data.local.database.entities.VitalSignsEntity

/**
 * Room database root.
 *
 * The actual SQLite file is encrypted with SQLCipher using a key derived from
 * the Android Keystore — see [com.ironsidemedical.connect.di.AppModule].
 * This ensures PHI at rest is protected even on rooted devices.
 *
 * Migration policy: all schema changes must include a Room [Migration] that
 * preserves existing patient data — destructive fallbacks are disabled.
 * IEC 62304 §5.5.3 requires that data integrity is maintained across updates.
 */
@Database(
    entities = [
        VitalSignsEntity::class,
        AuditLogEntity::class,
        DeviceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vitalSignsDao(): VitalSignsDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        const val DATABASE_NAME = "ironside_connect.db"
    }
}

package com.ironsidemedical.connect.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ironsidemedical.connect.domain.model.AuditEvent
import java.time.Instant

@Entity(
    tableName = "audit_log",
    indices = [Index("timestamp"), Index("event_type")]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestampEpochMs: Long,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "detail") val detail: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "device_address") val deviceAddress: String?,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "chain_hash") val chainHash: String,
) {
    fun toDomain() = AuditEvent(
        id = id,
        timestamp = Instant.ofEpochMilli(timestampEpochMs),
        eventType = eventType,
        detail = detail,
        userId = userId,
        deviceAddress = deviceAddress,
        sessionId = sessionId,
        chainHash = chainHash,
    )

    companion object {
        fun fromDomain(domain: AuditEvent) = AuditLogEntity(
            id = domain.id,
            timestampEpochMs = domain.timestamp.toEpochMilli(),
            eventType = domain.eventType,
            detail = domain.detail,
            userId = domain.userId,
            deviceAddress = domain.deviceAddress,
            sessionId = domain.sessionId,
            chainHash = domain.chainHash,
        )
    }
}

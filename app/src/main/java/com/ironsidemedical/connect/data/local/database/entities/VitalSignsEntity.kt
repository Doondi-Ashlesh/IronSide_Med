package com.ironsidemedical.connect.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ironsidemedical.connect.domain.model.VitalSigns
import java.time.Instant

@Entity(
    tableName = "vital_signs",
    indices = [
        Index("session_id"),
        Index("timestamp"),
        Index("device_address"),
    ]
)
data class VitalSignsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestampEpochMs: Long,
    @ColumnInfo(name = "heart_rate_bpm") val heartRateBpm: Double,
    @ColumnInfo(name = "spo2_percent") val spo2Percent: Double,
    @ColumnInfo(name = "systolic_mmhg") val systolicMmHg: Double,
    @ColumnInfo(name = "diastolic_mmhg") val diastolicMmHg: Double,
    @ColumnInfo(name = "temperature_celsius") val temperatureCelsius: Double,
    @ColumnInfo(name = "device_address") val deviceAddress: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
) {
    fun toDomain() = VitalSigns(
        id = id,
        timestamp = Instant.ofEpochMilli(timestampEpochMs),
        heartRateBpm = heartRateBpm,
        spo2Percent = spo2Percent,
        systolicMmHg = systolicMmHg,
        diastolicMmHg = diastolicMmHg,
        temperatureCelsius = temperatureCelsius,
        deviceAddress = deviceAddress,
        sessionId = sessionId,
    )

    companion object {
        fun fromDomain(domain: VitalSigns) = VitalSignsEntity(
            id = domain.id,
            timestampEpochMs = domain.timestamp.toEpochMilli(),
            heartRateBpm = domain.heartRateBpm,
            spo2Percent = domain.spo2Percent,
            systolicMmHg = domain.systolicMmHg,
            diastolicMmHg = domain.diastolicMmHg,
            temperatureCelsius = domain.temperatureCelsius,
            deviceAddress = domain.deviceAddress,
            sessionId = domain.sessionId,
        )
    }
}

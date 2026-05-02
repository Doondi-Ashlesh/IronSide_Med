package com.ironsidemedical.connect.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ironsidemedical.connect.domain.model.Device
import java.time.Instant

@Entity(tableName = "paired_devices")
data class DeviceEntity(
    @PrimaryKey val address: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "model_number") val modelNumber: String?,
    @ColumnInfo(name = "serial_number") val serialNumber: String?,
    @ColumnInfo(name = "firmware_version") val firmwareVersion: String?,
    @ColumnInfo(name = "last_connected_epoch_ms") val lastConnectedEpochMs: Long?,
) {
    fun toDomain() = Device(
        address = address,
        name = name,
        modelNumber = modelNumber,
        serialNumber = serialNumber,
        firmwareVersion = firmwareVersion,
        isPaired = true,
        lastConnectedAt = lastConnectedEpochMs?.let { Instant.ofEpochMilli(it) },
    )

    companion object {
        fun fromDomain(domain: Device) = DeviceEntity(
            address = domain.address,
            name = domain.name,
            modelNumber = domain.modelNumber,
            serialNumber = domain.serialNumber,
            firmwareVersion = domain.firmwareVersion,
            lastConnectedEpochMs = domain.lastConnectedAt?.toEpochMilli(),
        )
    }
}

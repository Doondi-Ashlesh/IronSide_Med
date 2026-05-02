package com.ironsidemedical.connect.domain.usecase

import com.ironsidemedical.connect.data.bluetooth.BleGattManager
import com.ironsidemedical.connect.domain.model.Device
import com.ironsidemedical.connect.domain.repository.IDeviceRepository
import com.ironsidemedical.connect.util.AuditLogger
import com.ironsidemedical.connect.util.SessionManager
import java.time.Instant
import javax.inject.Inject

class ConnectDeviceUseCase @Inject constructor(
    private val bleGattManager: BleGattManager,
    private val deviceRepository: IDeviceRepository,
    private val auditLogger: AuditLogger,
    private val sessionManager: SessionManager,
) {
    suspend operator fun invoke(device: Device): Result<Unit> {
        auditLogger.log(AuditLogger.Event.BLE_CONNECT_ATTEMPT, "address=${device.address}")
        val result = bleGattManager.connect(device)
        result.onSuccess {
            auditLogger.log(AuditLogger.Event.BLE_CONNECTED, "address=${device.address} name=${device.name}")
            auditLogger.log(AuditLogger.Event.BLE_AUTHENTICATED, "address=${device.address}")
            sessionManager.onDeviceConnected(device.address)
            // Update last-connected timestamp in local DB
            deviceRepository.saveDevice(device.copy(isPaired = true, lastConnectedAt = Instant.now()))
        }
        result.onFailure { e ->
            auditLogger.log(AuditLogger.Event.BLE_ERROR, "address=${device.address} error=${e.message}")
        }
        return result
    }
}

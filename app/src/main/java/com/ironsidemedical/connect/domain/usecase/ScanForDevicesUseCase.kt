package com.ironsidemedical.connect.domain.usecase

import com.ironsidemedical.connect.data.bluetooth.BleDeviceScanner
import com.ironsidemedical.connect.domain.model.Device
import com.ironsidemedical.connect.util.AuditLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class ScanForDevicesUseCase @Inject constructor(
    private val scanner: BleDeviceScanner,
    private val auditLogger: AuditLogger,
) {
    operator fun invoke(): Flow<Device> = scanner.scan()
        .onStart { auditLogger.log(AuditLogger.Event.BLE_SCAN_START) }
        .map { event ->
            when (event) {
                is BleDeviceScanner.ScanEvent.Found -> {
                    auditLogger.log(
                        AuditLogger.Event.BLE_DEVICE_FOUND,
                        "address=${event.device.address} rssi=${event.rssi}"
                    )
                    event.device
                }
            }
        }
        .onCompletion { auditLogger.log(AuditLogger.Event.BLE_SCAN_STOP) }
}

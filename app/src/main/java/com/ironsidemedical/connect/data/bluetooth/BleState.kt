package com.ironsidemedical.connect.data.bluetooth

import com.ironsidemedical.connect.domain.model.Device

/** Complete state machine for a BLE device connection lifecycle. */
sealed class BleState {
    data object Idle : BleState()
    data object Scanning : BleState()
    data class Connecting(val device: Device) : BleState()
    data class Connected(val device: Device) : BleState()
    data class Authenticating(val device: Device) : BleState()
    data class Ready(val device: Device) : BleState()
    data class Disconnecting(val device: Device) : BleState()
    data class Error(val device: Device?, val code: BleError) : BleState()
    data object BluetoothDisabled : BleState()
    data object PermissionDenied : BleState()
}

enum class BleError(val code: Int, val description: String) {
    GATT_FAILURE(0x0085, "GATT operation failed"),
    CONNECTION_TIMEOUT(0x0008, "Connection attempt timed out"),
    AUTHENTICATION_FAILED(0x0501, "Device-level authentication rejected"),
    PAIRING_FAILED(0x0502, "BLE pairing procedure failed"),
    SERVICE_NOT_FOUND(0x0601, "Required GATT service not found on device"),
    MTU_NEGOTIATION_FAILED(0x0701, "MTU negotiation failed"),
    UNEXPECTED_DISCONNECT(0x0099, "Unexpected disconnection during active session"),
    PERMISSION_DENIED(0x0001, "Required BLE permission not granted"),
}

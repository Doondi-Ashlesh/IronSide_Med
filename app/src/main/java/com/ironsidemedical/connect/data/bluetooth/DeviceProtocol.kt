package com.ironsidemedical.connect.data.bluetooth

import java.util.UUID

/**
 * IronSide proprietary GATT service and characteristic UUIDs.
 *
 * These are assigned UUIDs from the IronSide Bluetooth SIG company block.
 * Any change here must be reflected in the device firmware and the software
 * requirements specification (SRS-BT-001 through SRS-BT-020).
 */
object DeviceProtocol {

    // ── Service UUIDs ────────────────────────────────────────────────────────
    val SERVICE_VITALS: UUID = UUID.fromString("12345678-0001-1000-8000-00805f9b34fb")
    val SERVICE_DEVICE_INFO: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb") // Standard DIS
    val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")    // Standard BAS
    val SERVICE_SECURITY: UUID = UUID.fromString("12345678-0002-1000-8000-00805f9b34fb")

    // ── Vitals Characteristics (SERVICE_VITALS) ───────────────────────────────
    /** Notify: 20-byte measurement packet, 1 Hz */
    val CHAR_VITAL_MEASUREMENT: UUID = UUID.fromString("12345678-0011-1000-8000-00805f9b34fb")
    /** Read/Write: sensor configuration bitfield */
    val CHAR_SENSOR_CONFIG: UUID = UUID.fromString("12345678-0012-1000-8000-00805f9b34fb")
    /** Read: device timestamp (Unix epoch, little-endian uint64) */
    val CHAR_DEVICE_TIMESTAMP: UUID = UUID.fromString("12345678-0013-1000-8000-00805f9b34fb")

    // ── Device Information Characteristics (SERVICE_DEVICE_INFO / standard) ──
    val CHAR_MODEL_NUMBER: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    val CHAR_SERIAL_NUMBER: UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    val CHAR_FIRMWARE_REV: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    val CHAR_HARDWARE_REV: UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
    val CHAR_MANUFACTURER: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")

    // ── Security Characteristics (SERVICE_SECURITY) ───────────────────────────
    /** Write: 32-byte session token (HMAC-SHA256 challenge response) */
    val CHAR_AUTH_CHALLENGE: UUID = UUID.fromString("12345678-0021-1000-8000-00805f9b34fb")
    /** Read/Notify: auth status */
    val CHAR_AUTH_STATUS: UUID = UUID.fromString("12345678-0022-1000-8000-00805f9b34fb")

    // ── Standard Descriptor UUID ──────────────────────────────────────────────
    val DESC_CLIENT_CHARACTERISTIC_CONFIG: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ── Protocol Constants ────────────────────────────────────────────────────
    const val TARGET_MTU = 247            // BLE 4.2 maximum DLE payload
    const val SCAN_DEVICE_NAME_PREFIX = "ISM-"
    const val CONNECT_TIMEOUT_MS = 10_000L
    const val OPERATION_TIMEOUT_MS = 5_000L

    /**
     * Parse a raw 20-byte measurement packet from [CHAR_VITAL_MEASUREMENT].
     *
     * Byte layout (little-endian):
     *  [0-1]   : flags bitfield
     *  [2-3]   : heart rate (uint16, bpm × 10)
     *  [4-5]   : SpO2 (uint16, % × 10)
     *  [6-9]   : systolic BP (uint32, mmHg × 10)
     *  [10-13] : diastolic BP (uint32, mmHg × 10)
     *  [14-17] : temperature (uint32, °C × 100)
     *  [18-19] : checksum (CRC-16/CCITT)
     */
    fun parseMeasurementPacket(data: ByteArray): RawMeasurement? {
        if (data.size < 20) return null
        val flags = (data[1].toInt() shl 8) or data[0].toUByte().toInt()
        val checksum = ((data[19].toInt() shl 8) or data[18].toUByte().toInt()).toUShort()
        if (!verifyCrc16(data.copyOf(18), checksum)) return null

        return RawMeasurement(
            flags = flags,
            heartRateBpm = ((data[3].toInt() shl 8) or data[2].toUByte().toInt()) / 10.0,
            spo2Percent = ((data[5].toInt() shl 8) or data[4].toUByte().toInt()) / 10.0,
            systolicMmHg = readUInt32LE(data, 6) / 10.0,
            diastolicMmHg = readUInt32LE(data, 10) / 10.0,
            temperatureCelsius = readUInt32LE(data, 14) / 100.0,
        )
    }

    private fun readUInt32LE(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    /** CRC-16/CCITT-FALSE — poly 0x1021, init 0xFFFF, no reflection */
    fun verifyCrc16(data: ByteArray, expected: UShort): Boolean {
        var crc = 0xFFFF
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
            }
        }
        return (crc and 0xFFFF).toUShort() == expected
    }

    data class RawMeasurement(
        val flags: Int,
        val heartRateBpm: Double,
        val spo2Percent: Double,
        val systolicMmHg: Double,
        val diastolicMmHg: Double,
        val temperatureCelsius: Double,
    )
}

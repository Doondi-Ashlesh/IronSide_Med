package com.ironsidemedical.connect.bluetooth

import com.ironsidemedical.connect.data.bluetooth.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Requirement trace: SRS-BT-010 (Measurement packet parsing)
 * Validates the binary protocol parser against known-good byte sequences.
 */
class DeviceProtocolTest {

    @Test
    fun `parseMeasurementPacket returns null for packet shorter than 20 bytes`() {
        val short = ByteArray(15)
        assertNull(DeviceProtocol.parseMeasurementPacket(short))
    }

    @Test
    fun `parseMeasurementPacket parses valid packet correctly`() {
        val packet = buildTestPacket(
            heartRate = 720,      // 72.0 bpm × 10
            spo2 = 980,           // 98.0% × 10
            systolic = 1200,      // 120.0 mmHg × 10
            diastolic = 800,      // 80.0 mmHg × 10
            temperature = 3660,   // 36.60°C × 100
        )

        val result = DeviceProtocol.parseMeasurementPacket(packet)

        assertNotNull(result)
        assertEquals(72.0, result!!.heartRateBpm, 0.001)
        assertEquals(98.0, result.spo2Percent, 0.001)
        assertEquals(120.0, result.systolicMmHg, 0.001)
        assertEquals(80.0, result.diastolicMmHg, 0.001)
        assertEquals(36.60, result.temperatureCelsius, 0.001)
    }

    @Test
    fun `parseMeasurementPacket returns null when CRC is wrong`() {
        val packet = buildTestPacket(heartRate = 720, spo2 = 980, systolic = 1200, diastolic = 800, temperature = 3660)
        packet[18] = (packet[18].toInt() xor 0xFF).toByte()  // corrupt checksum
        assertNull(DeviceProtocol.parseMeasurementPacket(packet))
    }

    @Test
    fun `verifyCrc16 returns true for valid data`() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val crc = computeCrc16(data)
        assert(DeviceProtocol.verifyCrc16(data, crc))
    }

    @Test
    fun `verifyCrc16 returns false after single-bit flip`() {
        val data = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val crc = computeCrc16(data)
        data[1] = (data[1].toInt() xor 0x01).toByte()
        assertFalse(DeviceProtocol.verifyCrc16(data, crc))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun assertFalse(value: Boolean) = assertEquals(false, value)

    /** Builds a 20-byte packet with a valid CRC-16 checksum. */
    private fun buildTestPacket(
        heartRate: Int,
        spo2: Int,
        systolic: Int,
        diastolic: Int,
        temperature: Int,
    ): ByteArray {
        val data = ByteArray(20)
        data[0] = 0x00; data[1] = 0x00  // flags
        data[2] = (heartRate and 0xFF).toByte(); data[3] = (heartRate shr 8).toByte()
        data[4] = (spo2 and 0xFF).toByte(); data[5] = (spo2 shr 8).toByte()
        writeUInt32LE(data, 6, systolic.toLong())
        writeUInt32LE(data, 10, diastolic.toLong())
        writeUInt32LE(data, 14, temperature.toLong())
        val crc = computeCrc16(data.copyOf(18))
        data[18] = (crc.toInt() and 0xFF).toByte()
        data[19] = (crc.toInt() shr 8).toByte()
        return data
    }

    private fun writeUInt32LE(buf: ByteArray, offset: Int, value: Long) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = (value shr 8 and 0xFF).toByte()
        buf[offset + 2] = (value shr 16 and 0xFF).toByte()
        buf[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun computeCrc16(data: ByteArray): UShort {
        var crc = 0xFFFF
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
            }
        }
        return (crc and 0xFFFF).toUShort()
    }
}

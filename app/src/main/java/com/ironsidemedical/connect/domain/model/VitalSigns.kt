package com.ironsidemedical.connect.domain.model

import java.time.Instant

/**
 * A single vital-signs measurement captured from the connected device.
 * All numeric fields use SI units as defined in the device ICD.
 */
data class VitalSigns(
    val id: Long = 0,
    val timestamp: Instant,
    val heartRateBpm: Double,
    val spo2Percent: Double,
    val systolicMmHg: Double,
    val diastolicMmHg: Double,
    val temperatureCelsius: Double,
    val deviceAddress: String = "",
    val sessionId: String = "",
) {
    // Clinical alert thresholds — ref: IronSide Clinical Requirements Spec CRS-001
    val isHeartRateAbnormal: Boolean get() = heartRateBpm < 40.0 || heartRateBpm > 180.0
    val isSpo2Low: Boolean get() = spo2Percent < 90.0
    val isBloodPressureAbnormal: Boolean get() =
        systolicMmHg > 180.0 || systolicMmHg < 70.0 || diastolicMmHg > 120.0
    val isTempAbnormal: Boolean get() =
        temperatureCelsius < 35.0 || temperatureCelsius > 40.0

    val hasAnyAlert: Boolean get() =
        isHeartRateAbnormal || isSpo2Low || isBloodPressureAbnormal || isTempAbnormal
}

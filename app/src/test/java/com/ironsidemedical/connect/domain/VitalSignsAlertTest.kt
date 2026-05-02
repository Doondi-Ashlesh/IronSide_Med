package com.ironsidemedical.connect.domain

import com.ironsidemedical.connect.domain.model.VitalSigns
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Requirement trace: CRS-001 (Clinical Requirements Spec)
 * Covers alert threshold logic for all four vital sign types.
 */
class VitalSignsAlertTest {

    // ── Heart rate ────────────────────────────────────────────────────────────
    @Test fun `heart rate below 40 triggers alert`() {
        assertTrue(vitals(heartRate = 39.9).isHeartRateAbnormal)
    }

    @Test fun `heart rate at 40 does not trigger alert`() {
        assertFalse(vitals(heartRate = 40.0).isHeartRateAbnormal)
    }

    @Test fun `heart rate above 180 triggers alert`() {
        assertTrue(vitals(heartRate = 180.1).isHeartRateAbnormal)
    }

    @Test fun `heart rate at 180 does not trigger alert`() {
        assertFalse(vitals(heartRate = 180.0).isHeartRateAbnormal)
    }

    // ── SpO2 ─────────────────────────────────────────────────────────────────
    @Test fun `spo2 below 90 triggers alert`() {
        assertTrue(vitals(spo2 = 89.9).isSpo2Low)
    }

    @Test fun `spo2 at exactly 90 does not trigger alert`() {
        assertFalse(vitals(spo2 = 90.0).isSpo2Low)
    }

    // ── Blood pressure ────────────────────────────────────────────────────────
    @Test fun `systolic above 180 triggers alert`() {
        assertTrue(vitals(systolic = 181.0).isBloodPressureAbnormal)
    }

    @Test fun `systolic below 70 triggers alert`() {
        assertTrue(vitals(systolic = 69.0).isBloodPressureAbnormal)
    }

    @Test fun `diastolic above 120 triggers alert`() {
        assertTrue(vitals(diastolic = 121.0).isBloodPressureAbnormal)
    }

    @Test fun `normal blood pressure does not trigger alert`() {
        assertFalse(vitals(systolic = 120.0, diastolic = 80.0).isBloodPressureAbnormal)
    }

    // ── Temperature ───────────────────────────────────────────────────────────
    @Test fun `temperature below 35 triggers alert`() {
        assertTrue(vitals(temp = 34.9).isTempAbnormal)
    }

    @Test fun `temperature above 40 triggers alert`() {
        assertTrue(vitals(temp = 40.1).isTempAbnormal)
    }

    @Test fun `normal temperature does not trigger alert`() {
        assertFalse(vitals(temp = 37.0).isTempAbnormal)
    }

    // ── Composite ────────────────────────────────────────────────────────────
    @Test fun `all normal vitals have no alert`() {
        assertFalse(vitals().hasAnyAlert)
    }

    @Test fun `any abnormal vital sets hasAnyAlert`() {
        assertTrue(vitals(spo2 = 85.0).hasAnyAlert)
    }

    private fun vitals(
        heartRate: Double = 72.0,
        spo2: Double = 98.0,
        systolic: Double = 120.0,
        diastolic: Double = 80.0,
        temp: Double = 36.6,
    ) = VitalSigns(
        timestamp = Instant.now(),
        heartRateBpm = heartRate,
        spo2Percent = spo2,
        systolicMmHg = systolic,
        diastolicMmHg = diastolic,
        temperatureCelsius = temp,
    )
}

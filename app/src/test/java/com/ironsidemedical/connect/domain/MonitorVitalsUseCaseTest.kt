package com.ironsidemedical.connect.domain

import app.cash.turbine.test
import com.ironsidemedical.connect.data.bluetooth.BleGattManager
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.domain.repository.IVitalSignsRepository
import com.ironsidemedical.connect.domain.usecase.MonitorVitalsUseCase
import com.ironsidemedical.connect.util.AuditLogger
import com.ironsidemedical.connect.util.SessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class MonitorVitalsUseCaseTest {

    private val bleGattManager: BleGattManager = mockk()
    private val vitalSignsRepository: IVitalSignsRepository = mockk()
    private val auditLogger: AuditLogger = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk()

    private lateinit var useCase: MonitorVitalsUseCase

    @Before
    fun setUp() {
        every { sessionManager.connectedDeviceAddress } returns "AA:BB:CC:DD:EE:FF"
        useCase = MonitorVitalsUseCase(bleGattManager, vitalSignsRepository, auditLogger, sessionManager)
    }

    @Test
    fun `emits vitals and saves them to repository`() = runTest {
        val channel = Channel<VitalSigns>()
        every { bleGattManager.measurementFlow() } returns channel
        coEvery { vitalSignsRepository.saveVitalSigns(any()) } returns Unit

        val normal = testVitals(heartRate = 72.0, spo2 = 98.0)

        useCase("session-123").test {
            channel.send(normal)
            val emitted = awaitItem()
            assertEquals("session-123", emitted.sessionId)
            assertEquals("AA:BB:CC:DD:EE:FF", emitted.deviceAddress)
            assertEquals(72.0, emitted.heartRateBpm, 0.001)
            channel.close()
            awaitComplete()
        }

        coVerify(exactly = 1) { vitalSignsRepository.saveVitalSigns(any()) }
    }

    @Test
    fun `logs alert when vital signs are abnormal`() = runTest {
        val channel = Channel<VitalSigns>()
        every { bleGattManager.measurementFlow() } returns channel
        coEvery { vitalSignsRepository.saveVitalSigns(any()) } returns Unit

        val critical = testVitals(heartRate = 200.0, spo2 = 85.0)   // both abnormal

        useCase("session-xyz").test {
            channel.send(critical)
            awaitItem()
            channel.close()
            awaitComplete()
        }

        verify(exactly = 1) {
            auditLogger.log(AuditLogger.Event.ALERT_TRIGGERED, any())
        }
    }

    @Test
    fun `does NOT log alert when vitals are normal`() = runTest {
        val channel = Channel<VitalSigns>()
        every { bleGattManager.measurementFlow() } returns channel
        coEvery { vitalSignsRepository.saveVitalSigns(any()) } returns Unit

        val normal = testVitals(heartRate = 65.0, spo2 = 99.0)

        useCase("session-abc").test {
            channel.send(normal)
            awaitItem()
            channel.close()
            awaitComplete()
        }

        verify(exactly = 0) {
            auditLogger.log(AuditLogger.Event.ALERT_TRIGGERED, any())
        }
    }

    private fun testVitals(heartRate: Double, spo2: Double) = VitalSigns(
        timestamp = Instant.now(),
        heartRateBpm = heartRate,
        spo2Percent = spo2,
        systolicMmHg = 120.0,
        diastolicMmHg = 80.0,
        temperatureCelsius = 36.6,
    )
}

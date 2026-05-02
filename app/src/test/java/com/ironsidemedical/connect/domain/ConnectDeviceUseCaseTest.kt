package com.ironsidemedical.connect.domain

import com.ironsidemedical.connect.data.bluetooth.BleGattManager
import com.ironsidemedical.connect.domain.model.Device
import com.ironsidemedical.connect.domain.repository.IDeviceRepository
import com.ironsidemedical.connect.domain.usecase.ConnectDeviceUseCase
import com.ironsidemedical.connect.util.AuditLogger
import com.ironsidemedical.connect.util.SessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConnectDeviceUseCaseTest {

    private val bleGattManager: BleGattManager = mockk()
    private val deviceRepository: IDeviceRepository = mockk(relaxed = true)
    private val auditLogger: AuditLogger = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private lateinit var useCase: ConnectDeviceUseCase

    private val testDevice = Device(
        address = "AA:BB:CC:DD:EE:FF",
        name = "ISM-001",
        modelNumber = "ISM-X1",
        serialNumber = "SN12345",
        firmwareVersion = "2.1.0",
    )

    @Before
    fun setUp() {
        useCase = ConnectDeviceUseCase(bleGattManager, deviceRepository, auditLogger, sessionManager)
    }

    @Test
    fun `successful connection saves device and notifies session manager`() = runTest {
        coEvery { bleGattManager.connect(testDevice) } returns Result.success(Unit)

        val result = useCase(testDevice)

        assertTrue(result.isSuccess)
        coVerify { deviceRepository.saveDevice(any()) }
        verify { sessionManager.onDeviceConnected(testDevice.address) }
        verify { auditLogger.log(AuditLogger.Event.BLE_CONNECTED, any()) }
        verify { auditLogger.log(AuditLogger.Event.BLE_AUTHENTICATED, any()) }
    }

    @Test
    fun `failed connection logs error and does not save device`() = runTest {
        val error = RuntimeException("GATT failure")
        coEvery { bleGattManager.connect(testDevice) } returns Result.failure(error)

        val result = useCase(testDevice)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { deviceRepository.saveDevice(any()) }
        verify(exactly = 0) { sessionManager.onDeviceConnected(any()) }
        verify { auditLogger.log(AuditLogger.Event.BLE_ERROR, any()) }
    }

    @Test
    fun `connection attempt is always audited even on failure`() = runTest {
        coEvery { bleGattManager.connect(testDevice) } returns Result.failure(RuntimeException())

        useCase(testDevice)

        verify { auditLogger.log(AuditLogger.Event.BLE_CONNECT_ATTEMPT, any()) }
    }
}

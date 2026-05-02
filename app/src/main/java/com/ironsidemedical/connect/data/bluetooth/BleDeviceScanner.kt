package com.ironsidemedical.connect.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.ironsidemedical.connect.domain.model.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android BLE scanning APIs as a cold [Flow].
 *
 * Scanning stops automatically when the collector cancels, preventing the
 * common lifecycle leak of forgetting to call stopScan() on rotation.
 */
@Singleton
class BleDeviceScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothAdapter by lazy {
        context.getSystemService(BluetoothManager::class.java).adapter
    }

    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true

    /**
     * Emits [ScanEvent]s for every IronSide device discovered.
     * Uses SCAN_MODE_LOW_LATENCY while the UI is visible; the caller should
     * switch to a low-power WorkManager job for background scanning.
     */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<ScanEvent> = callbackFlow {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
            ?: run { close(IllegalStateException("BLE not available")); return@callbackFlow }

        val filters = listOf(
            ScanFilter.Builder()
                .setDeviceNamePrefix(DeviceProtocol.SCAN_DEVICE_NAME_PREFIX)
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.toDevice() ?: return
                trySend(ScanEvent.Found(device, result.rssi))
            }

            override fun onScanFailed(errorCode: Int) {
                Timber.e("BLE scan failed: errorCode=$errorCode")
                close(BleException.ScanFailed(errorCode))
            }
        }

        Timber.d("Starting BLE scan")
        scanner.startScan(filters, settings, callback)

        awaitClose {
            Timber.d("Stopping BLE scan")
            scanner.stopScan(callback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun ScanResult.toDevice(): Device? {
        val address = device.address ?: return null
        val name = device.name ?: scanRecord?.deviceName ?: return null
        return Device(
            address = address,
            name = name,
            modelNumber = null,
            serialNumber = null,
            firmwareVersion = null,
        )
    }

    sealed class ScanEvent {
        data class Found(val device: Device, val rssi: Int) : ScanEvent()
    }
}

sealed class BleException(message: String) : Exception(message) {
    class ScanFailed(code: Int) : BleException("BLE scan failed with code $code")
    class ConnectionTimeout(address: String) : BleException("Connection to $address timed out")
    class AuthenticationFailed(address: String) : BleException("Device $address rejected auth")
    class ServiceNotFound(uuid: String) : BleException("GATT service $uuid not found")
}

package com.ironsidemedical.connect.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.ironsidemedical.connect.domain.model.Device
import com.ironsidemedical.connect.domain.model.VitalSigns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the full BLE GATT connection lifecycle for a single device.
 *
 * All GATT callbacks arrive on the Android BLE callback thread. This class
 * bridges them into Kotlin Coroutines so callers can use structured concurrency.
 *
 * Design notes:
 * - Only one device connected at a time (matches IronSide hardware constraint)
 * - MTU negotiated to [DeviceProtocol.TARGET_MTU] immediately after connection
 * - Device-level HMAC authentication runs before any data is consumed
 * - Measurements are buffered in a [Channel] to avoid back-pressure on the callback thread
 */
@Singleton
@SuppressLint("MissingPermission")
class BleGattManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: com.ironsidemedical.connect.data.local.security.KeystoreManager,
) {
    private val _bleState = MutableStateFlow<BleState>(BleState.Idle)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // CompletableDeferred used for one-shot GATT operations (MTU, service discovery, writes)
    private var mtuDeferred: CompletableDeferred<Int>? = null
    private var serviceDiscoveryDeferred: CompletableDeferred<Boolean>? = null
    private var writeDeferred: CompletableDeferred<Boolean>? = null
    private var readDeferred: CompletableDeferred<ByteArray>? = null

    private val measurementChannel = Channel<VitalSigns>(capacity = Channel.BUFFERED)

    private val bluetoothAdapter by lazy {
        context.getSystemService(BluetoothManager::class.java).adapter
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun connect(device: Device): Result<Unit> = runCatching {
        val btDevice = bluetoothAdapter.getRemoteDevice(device.address)
        _bleState.value = BleState.Connecting(device)

        withTimeout(DeviceProtocol.CONNECT_TIMEOUT_MS) {
            val connected = CompletableDeferred<Unit>()
            gatt = btDevice.connectGatt(context, false, buildCallback(device, connected), BluetoothDevice.TRANSPORT_LE)
            connected.await()
        }

        negotiateMtu()
        discoverServices(device)
        authenticateDevice(device)
        enableMeasurementNotifications()
        _bleState.value = BleState.Ready(device)
        Timber.i("BLE ready: ${device.name}")
    }.onFailure { e ->
        Timber.e(e, "BLE connection failed for ${device.name}")
        _bleState.value = BleState.Error(device, classifyError(e))
        cleanupGatt()
    }

    suspend fun disconnect() {
        val current = (_bleState.value as? BleState.Ready)?.device
            ?: (_bleState.value as? BleState.Connected)?.device
        if (current != null) {
            _bleState.value = BleState.Disconnecting(current)
        }
        cleanupGatt()
        _bleState.value = BleState.Idle
        managerScope.cancel()
        managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        measurementChannel.close()
    }

    fun measurementFlow() = measurementChannel

    // ── GATT Operations ────────────────────────────────────────────────────────

    private suspend fun negotiateMtu() {
        mtuDeferred = CompletableDeferred()
        gatt?.requestMtu(DeviceProtocol.TARGET_MTU)
        withTimeout(DeviceProtocol.OPERATION_TIMEOUT_MS) { mtuDeferred!!.await() }
    }

    private suspend fun discoverServices(device: Device) {
        serviceDiscoveryDeferred = CompletableDeferred()
        _bleState.value = BleState.Connecting(device)
        gatt?.discoverServices()
        val success = withTimeout(DeviceProtocol.OPERATION_TIMEOUT_MS) {
            serviceDiscoveryDeferred!!.await()
        }
        if (!success) throw BleException.ServiceNotFound(DeviceProtocol.SERVICE_VITALS.toString())
    }

    private suspend fun authenticateDevice(device: Device) {
        _bleState.value = BleState.Authenticating(device)

        // Read the auth challenge nonce from the device
        val challengeChar = gatt
            ?.getService(DeviceProtocol.SERVICE_SECURITY)
            ?.getCharacteristic(DeviceProtocol.CHAR_AUTH_CHALLENGE)
            ?: throw BleException.AuthenticationFailed(device.address)

        // Compute HMAC-SHA256 response using the per-device key from Android Keystore
        val nonce = readCharacteristic(challengeChar)
        val response = keystoreManager.computeHmac(nonce)

        // Write the response back
        writeDeferred = CompletableDeferred()
        @Suppress("DEPRECATION")
        challengeChar.value = response
        gatt?.writeCharacteristic(challengeChar)
        val accepted = withTimeout(DeviceProtocol.OPERATION_TIMEOUT_MS) { writeDeferred!!.await() }
        if (!accepted) throw BleException.AuthenticationFailed(device.address)

        Timber.i("Device authentication successful")
    }

    private fun enableMeasurementNotifications() {
        val char = gatt
            ?.getService(DeviceProtocol.SERVICE_VITALS)
            ?.getCharacteristic(DeviceProtocol.CHAR_VITAL_MEASUREMENT)
            ?: return
        gatt?.setCharacteristicNotification(char, true)
        val descriptor = char.getDescriptor(DeviceProtocol.DESC_CLIENT_CHARACTERISTIC_CONFIG)
        @Suppress("DEPRECATION")
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt?.writeDescriptor(descriptor)
    }

    private suspend fun readCharacteristic(char: BluetoothGattCharacteristic): ByteArray {
        readDeferred = CompletableDeferred()
        gatt?.readCharacteristic(char)
        return withTimeout(DeviceProtocol.OPERATION_TIMEOUT_MS) { readDeferred!!.await() }
    }

    // ── GATT Callback ─────────────────────────────────────────────────────────

    private fun buildCallback(
        device: Device,
        connectedSignal: CompletableDeferred<Unit>,
    ) = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            Timber.d("onConnectionStateChange: status=$status newState=$newState")
            when {
                newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS -> {
                    _bleState.value = BleState.Connected(device)
                    connectedSignal.complete(Unit)
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasReady = _bleState.value is BleState.Ready
                    _bleState.value = if (wasReady) {
                        BleState.Error(device, BleError.UNEXPECTED_DISCONNECT)
                    } else {
                        BleState.Idle
                    }
                    connectedSignal.completeExceptionally(
                        BleException.ConnectionTimeout(device.address)
                    )
                    cleanupGatt()
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            Timber.d("MTU negotiated to $mtu")
            mtuDeferred?.complete(mtu)
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val hasVitals = g.getService(DeviceProtocol.SERVICE_VITALS) != null
            serviceDiscoveryDeferred?.complete(hasVitals)
        }

        @Deprecated("Deprecated in GATT API 33 - handled via onCharacteristicRead(gatt, char, value, status)")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            @Suppress("DEPRECATION")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readDeferred?.complete(characteristic.value ?: byteArrayOf())
            } else {
                readDeferred?.completeExceptionally(BleException.ConnectionTimeout(device.address))
            }
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readDeferred?.complete(value)
            } else {
                readDeferred?.completeExceptionally(BleException.ConnectionTimeout(device.address))
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            writeDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            handleMeasurement(characteristic.value)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleMeasurement(value)
        }
    }

    private fun handleMeasurement(data: ByteArray) {
        val raw = DeviceProtocol.parseMeasurementPacket(data) ?: return
        val vitals = VitalSigns(
            timestamp = Instant.now(),
            heartRateBpm = raw.heartRateBpm,
            spo2Percent = raw.spo2Percent,
            systolicMmHg = raw.systolicMmHg,
            diastolicMmHg = raw.diastolicMmHg,
            temperatureCelsius = raw.temperatureCelsius,
        )
        managerScope.launch { measurementChannel.send(vitals) }
    }

    private fun classifyError(e: Throwable): BleError = when (e) {
        is BleException.ConnectionTimeout -> BleError.CONNECTION_TIMEOUT
        is BleException.AuthenticationFailed -> BleError.AUTHENTICATION_FAILED
        is BleException.ServiceNotFound -> BleError.SERVICE_NOT_FOUND
        else -> BleError.GATT_FAILURE
    }

    private fun cleanupGatt() {
        gatt?.close()
        gatt = null
    }
}

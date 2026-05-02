package com.ironsidemedical.connect.presentation.screens.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironsidemedical.connect.data.bluetooth.BleDeviceScanner
import com.ironsidemedical.connect.domain.model.Device
import com.ironsidemedical.connect.domain.usecase.ConnectDeviceUseCase
import com.ironsidemedical.connect.domain.usecase.ScanForDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceScanViewModel @Inject constructor(
    private val scanForDevicesUseCase: ScanForDevicesUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val bleScanner: BleDeviceScanner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceScanUiState())
    val uiState: StateFlow<DeviceScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (!bleScanner.isBluetoothEnabled) {
            _uiState.update { it.copy(error = "Bluetooth is disabled. Please enable it in Settings.") }
            return
        }
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList(), error = null) }
        scanJob = viewModelScope.launch {
            scanForDevicesUseCase()
                .catch { e ->
                    Timber.e(e, "Scan error")
                    _uiState.update { it.copy(isScanning = false, error = e.message) }
                }
                .collect { device ->
                    _uiState.update { current ->
                        val updated = (current.discoveredDevices + device).distinctBy { it.address }
                        current.copy(discoveredDevices = updated)
                    }
                }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun connectTo(device: Device) {
        viewModelScope.launch {
            _uiState.update { it.copy(connectingTo = device.address, error = null) }
            connectDeviceUseCase(device)
                .onSuccess {
                    _uiState.update { it.copy(connectingTo = null, connectedDevice = device) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(connectingTo = null, error = "Failed to connect: ${e.message}") }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}

data class DeviceScanUiState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<Device> = emptyList(),
    val connectingTo: String? = null,
    val connectedDevice: Device? = null,
    val error: String? = null,
)

package com.ironsidemedical.connect.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironsidemedical.connect.data.bluetooth.BleState
import com.ironsidemedical.connect.data.bluetooth.BleGattManager
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.domain.usecase.MonitorVitalsUseCase
import com.ironsidemedical.connect.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val monitorVitalsUseCase: MonitorVitalsUseCase,
    private val bleGattManager: BleGattManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val bleState: StateFlow<BleState> = bleGattManager.bleState
        .stateIn(viewModelScope, SharingStarted.Eagerly, BleState.Idle)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeBleState()
    }

    private fun observeBleState() {
        viewModelScope.launch {
            bleGattManager.bleState.collect { state ->
                _uiState.update { it.copy(bleState = state) }
                if (state is BleState.Ready) {
                    startMonitoring(state.device.address)
                }
            }
        }
    }

    private fun startMonitoring(deviceAddress: String) {
        val sessionId = sessionManager.currentSessionId ?: return
        viewModelScope.launch {
            monitorVitalsUseCase(sessionId).collect { vitals ->
                _uiState.update { current ->
                    current.copy(
                        latestVitals = vitals,
                        activeAlerts = buildAlertList(vitals),
                    )
                }
            }
        }
    }

    private fun buildAlertList(v: VitalSigns): List<String> = buildList {
        if (v.isHeartRateAbnormal) add("Heart rate ${v.heartRateBpm.toInt()} bpm is out of normal range")
        if (v.isSpo2Low) add("SpO₂ ${v.spo2Percent.toInt()}% is below safe threshold")
        if (v.isBloodPressureAbnormal) add("Blood pressure ${v.systolicMmHg.toInt()}/${v.diastolicMmHg.toInt()} mmHg is abnormal")
        if (v.isTempAbnormal) add("Temperature ${v.temperatureCelsius}°C is out of range")
    }
}

data class DashboardUiState(
    val bleState: BleState = BleState.Idle,
    val latestVitals: VitalSigns? = null,
    val activeAlerts: List<String> = emptyList(),
)

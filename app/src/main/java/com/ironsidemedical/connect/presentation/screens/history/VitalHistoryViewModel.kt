package com.ironsidemedical.connect.presentation.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.domain.repository.IVitalSignsRepository
import com.ironsidemedical.connect.domain.usecase.ExportSessionDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VitalHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vitalSignsRepository: IVitalSignsRepository,
    private val exportSessionDataUseCase: ExportSessionDataUseCase,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(VitalHistoryUiState())
    val uiState: StateFlow<VitalHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            vitalSignsRepository.getVitalSignsForSession(sessionId).collect { vitals ->
                _uiState.update { it.copy(vitals = vitals) }
            }
        }
    }

    fun exportSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            exportSessionDataUseCase(sessionId)
                .onSuccess { file ->
                    Timber.i("Exported to ${file.absolutePath}")
                    _uiState.update { it.copy(isExporting = false, exportPath = file.absolutePath) }
                }
                .onFailure { e ->
                    Timber.e(e, "Export failed")
                    _uiState.update { it.copy(isExporting = false, exportError = e.message) }
                }
        }
    }
}

data class VitalHistoryUiState(
    val vitals: List<VitalSigns> = emptyList(),
    val isExporting: Boolean = false,
    val exportPath: String? = null,
    val exportError: String? = null,
)

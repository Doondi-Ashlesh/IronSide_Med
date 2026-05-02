package com.ironsidemedical.connect.presentation.screens.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironsidemedical.connect.domain.model.AuditEvent
import com.ironsidemedical.connect.domain.repository.IAuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class IntegrityStatus { UNKNOWN, VALID, COMPROMISED }

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditRepository: IAuditRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auditRepository.getAll().collect { events ->
                _uiState.update { it.copy(events = events) }
            }
        }
    }

    fun verifyIntegrity() {
        viewModelScope.launch {
            val isValid = auditRepository.verifyChainIntegrity()
            _uiState.update {
                it.copy(integrityStatus = if (isValid) IntegrityStatus.VALID else IntegrityStatus.COMPROMISED)
            }
        }
    }
}

data class AuditLogUiState(
    val events: List<AuditEvent> = emptyList(),
    val integrityStatus: IntegrityStatus = IntegrityStatus.UNKNOWN,
)

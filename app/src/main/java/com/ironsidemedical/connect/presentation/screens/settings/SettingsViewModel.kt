package com.ironsidemedical.connect.presentation.screens.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironsidemedical.connect.util.AuditLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    val settings = dataStore.data.map { prefs ->
        AppSettings(
            alertsEnabled = prefs[KEY_ALERTS_ENABLED] ?: true,
            requireBiometricOnResume = prefs[KEY_BIOMETRIC_ON_RESUME] ?: true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_ALERTS_ENABLED] = enabled }
            auditLogger.log(AuditLogger.Event.SETTINGS_CHANGED, "alertsEnabled=$enabled")
        }
    }

    fun setBiometricOnResume(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BIOMETRIC_ON_RESUME] = enabled }
            auditLogger.log(AuditLogger.Event.SETTINGS_CHANGED, "biometricOnResume=$enabled")
        }
    }

    companion object {
        private val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val KEY_BIOMETRIC_ON_RESUME = booleanPreferencesKey("biometric_on_resume")
    }
}

data class AppSettings(
    val alertsEnabled: Boolean = true,
    val requireBiometricOnResume: Boolean = true,
)

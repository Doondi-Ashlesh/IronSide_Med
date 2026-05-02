package com.ironsidemedical.connect.util

import com.ironsidemedical.connect.data.local.security.KeystoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the current authenticated user and device session.
 *
 * Sessions are ephemeral — they survive process death only as far as the
 * audit log is concerned. On restart the user must re-authenticate via
 * biometrics or PIN (satisfying FDA MFA guidance for medical apps).
 */
@Singleton
class SessionManager @Inject constructor(
    private val keystoreManager: KeystoreManager,
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    var currentUserId: String = "anonymous"
        private set
    var currentSessionId: String? = null
        private set
    var connectedDeviceAddress: String? = null
        private set

    fun onUserAuthenticated(userId: String) {
        currentUserId = userId
        currentSessionId = keystoreManager.generateSessionToken()
        _sessionState.value = SessionState.Authenticated(userId, Instant.now())
    }

    fun onDeviceConnected(address: String) {
        connectedDeviceAddress = address
    }

    fun onDeviceDisconnected() {
        connectedDeviceAddress = null
    }

    fun onUserLogout() {
        currentUserId = "anonymous"
        currentSessionId = null
        connectedDeviceAddress = null
        _sessionState.value = SessionState.Unauthenticated
    }

    val isAuthenticated: Boolean
        get() = _sessionState.value is SessionState.Authenticated
}

sealed class SessionState {
    data object Unauthenticated : SessionState()
    data class Authenticated(val userId: String, val since: Instant) : SessionState()
}

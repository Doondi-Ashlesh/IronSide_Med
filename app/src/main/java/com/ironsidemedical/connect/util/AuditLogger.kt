package com.ironsidemedical.connect.util

import com.ironsidemedical.connect.data.repository.AuditRepository
import com.ironsidemedical.connect.domain.model.AuditEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Append-only audit logger.
 *
 * Every call appends a hash-chained [AuditEvent] to the encrypted database.
 * The chain hash is computed over (prevHash || eventType || detail || timestamp)
 * so any retroactive edit or deletion is detectable.
 *
 * Calls are fire-and-forget from the caller's perspective — logging errors are
 * swallowed and reported via Timber to avoid cascading failures in clinical code.
 * In production, a circuit-breaker would escalate persistent logging failures.
 *
 * Required by: FDA 21 CFR Part 11 / Part 820, IEC 62304 §5.8
 */
@Singleton
class AuditLogger @Inject constructor(
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager,
) {
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(event: Event, detail: String = "") {
        logScope.launch {
            try {
                val prevHash = auditRepository.getLastHash()
                val now = Instant.now()
                val chainHash = AuditRepository.computeChainHash(
                    prevHash = prevHash,
                    eventType = event.key,
                    detail = detail,
                    timestampEpochSec = now.epochSecond,
                )
                auditRepository.append(
                    AuditEvent(
                        timestamp = now,
                        eventType = event.key,
                        detail = detail,
                        userId = sessionManager.currentUserId,
                        deviceAddress = sessionManager.connectedDeviceAddress,
                        sessionId = sessionManager.currentSessionId,
                        chainHash = chainHash,
                    )
                )
                Timber.d("AUDIT [${event.key}] $detail")
            } catch (e: Exception) {
                Timber.e(e, "Failed to write audit log entry: ${event.key}")
                // TODO: queue for retry; escalate after N failures
            }
        }
    }

    /** Typed event constants — adding new events here enforces a compile-time audit vocabulary. */
    enum class Event(val key: String) {
        APP_LAUNCH("APP_LAUNCH"),
        USER_AUTHENTICATED("USER_AUTHENTICATED"),
        USER_AUTH_FAILED("USER_AUTH_FAILED"),
        USER_LOGOUT("USER_LOGOUT"),
        BLE_SCAN_START("BLE_SCAN_START"),
        BLE_SCAN_STOP("BLE_SCAN_STOP"),
        BLE_DEVICE_FOUND("BLE_DEVICE_FOUND"),
        BLE_CONNECT_ATTEMPT("BLE_CONNECT_ATTEMPT"),
        BLE_CONNECTED("BLE_CONNECTED"),
        BLE_AUTHENTICATED("BLE_AUTHENTICATED"),
        BLE_DISCONNECTED("BLE_DISCONNECTED"),
        BLE_ERROR("BLE_ERROR"),
        SESSION_START("SESSION_START"),
        SESSION_END("SESSION_END"),
        ALERT_TRIGGERED("ALERT_TRIGGERED"),
        ALERT_ACKNOWLEDGED("ALERT_ACKNOWLEDGED"),
        DATA_EXPORT("DATA_EXPORT"),
        DEVICE_PAIRED("DEVICE_PAIRED"),
        DEVICE_REMOVED("DEVICE_REMOVED"),
        SETTINGS_CHANGED("SETTINGS_CHANGED"),
        FIRMWARE_UPDATE_START("FIRMWARE_UPDATE_START"),
        FIRMWARE_UPDATE_COMPLETE("FIRMWARE_UPDATE_COMPLETE"),
        FIRMWARE_UPDATE_FAILED("FIRMWARE_UPDATE_FAILED"),
        INTEGRITY_CHECK_PASS("INTEGRITY_CHECK_PASS"),
        INTEGRITY_CHECK_FAIL("INTEGRITY_CHECK_FAIL"),
    }
}

package com.ironsidemedical.connect.domain.model

import java.time.Instant

/**
 * Immutable audit record.
 *
 * FDA 21 CFR Part 820 and IEC 62304 §5.8 require a tamper-evident audit trail.
 * Each record stores a SHA-256 hash of (previousHash + eventType + detail + timestamp)
 * to form a hash chain that detects any retroactive modification.
 */
data class AuditEvent(
    val id: Long = 0,
    val timestamp: Instant,
    val eventType: String,
    val detail: String,
    val userId: String,
    val deviceAddress: String?,
    val sessionId: String?,
    /** SHA-256 hex of (prevHash || eventType || detail || timestamp.epochSecond) */
    val chainHash: String,
)

package com.ironsidemedical.connect.domain.model

import java.time.Instant
import java.util.UUID

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val deviceAddress: String,
    val deviceName: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val measurementCount: Int = 0,
    val alertCount: Int = 0,
) {
    val isActive: Boolean get() = endedAt == null
    val durationSeconds: Long? get() = endedAt?.let {
        it.epochSecond - startedAt.epochSecond
    }
}

package com.ironsidemedical.connect.domain.model

/**
 * Domain model for an IronSide medical device.
 * Immutable — updates produce a new copy.
 */
data class Device(
    val address: String,
    val name: String,
    val modelNumber: String?,
    val serialNumber: String?,
    val firmwareVersion: String?,
    val isPaired: Boolean = false,
    val lastConnectedAt: java.time.Instant? = null,
) {
    val displayName: String get() = if (name.isNotBlank()) name else address
}

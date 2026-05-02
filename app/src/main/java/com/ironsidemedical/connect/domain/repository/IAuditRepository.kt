package com.ironsidemedical.connect.domain.repository

import com.ironsidemedical.connect.domain.model.AuditEvent
import kotlinx.coroutines.flow.Flow

interface IAuditRepository {
    suspend fun append(event: AuditEvent)
    fun getAll(): Flow<List<AuditEvent>>
    suspend fun getLastHash(): String
    suspend fun verifyChainIntegrity(): Boolean
}

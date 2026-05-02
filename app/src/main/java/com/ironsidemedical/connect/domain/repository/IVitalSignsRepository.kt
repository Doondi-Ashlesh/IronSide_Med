package com.ironsidemedical.connect.domain.repository

import com.ironsidemedical.connect.domain.model.VitalSigns
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface IVitalSignsRepository {
    suspend fun saveVitalSigns(vitals: VitalSigns)
    fun getVitalSignsForSession(sessionId: String): Flow<List<VitalSigns>>
    fun getRecentVitalSigns(limit: Int = 100): Flow<List<VitalSigns>>
    suspend fun getVitalSignsBetween(from: Instant, to: Instant): List<VitalSigns>
    suspend fun deleteSessionData(sessionId: String)
}

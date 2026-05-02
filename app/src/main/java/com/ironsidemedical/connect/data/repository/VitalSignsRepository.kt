package com.ironsidemedical.connect.data.repository

import com.ironsidemedical.connect.data.local.database.dao.VitalSignsDao
import com.ironsidemedical.connect.data.local.database.entities.VitalSignsEntity
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.domain.repository.IVitalSignsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalSignsRepository @Inject constructor(
    private val dao: VitalSignsDao,
) : IVitalSignsRepository {

    override suspend fun saveVitalSigns(vitals: VitalSigns) {
        dao.insert(VitalSignsEntity.fromDomain(vitals))
    }

    override fun getVitalSignsForSession(sessionId: String): Flow<List<VitalSigns>> =
        dao.getBySession(sessionId).map { list -> list.map { it.toDomain() } }

    override fun getRecentVitalSigns(limit: Int): Flow<List<VitalSigns>> =
        dao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getVitalSignsBetween(from: Instant, to: Instant): List<VitalSigns> =
        dao.getBetween(from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun deleteSessionData(sessionId: String) =
        dao.deleteBySession(sessionId)
}

package com.ironsidemedical.connect.data.repository

import com.ironsidemedical.connect.data.local.database.dao.DeviceDao
import com.ironsidemedical.connect.data.local.database.entities.DeviceEntity
import com.ironsidemedical.connect.domain.model.Device
import com.ironsidemedical.connect.domain.repository.IDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val dao: DeviceDao,
) : IDeviceRepository {

    override fun getPairedDevices(): Flow<List<Device>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveDevice(device: Device) =
        dao.upsert(DeviceEntity.fromDomain(device))

    override suspend fun removeDevice(address: String) =
        dao.deleteByAddress(address)

    override suspend fun getDevice(address: String): Device? =
        dao.getByAddress(address)?.toDomain()
}

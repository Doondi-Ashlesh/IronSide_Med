package com.ironsidemedical.connect.domain.repository

import com.ironsidemedical.connect.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface IDeviceRepository {
    fun getPairedDevices(): Flow<List<Device>>
    suspend fun saveDevice(device: Device)
    suspend fun removeDevice(address: String)
    suspend fun getDevice(address: String): Device?
}

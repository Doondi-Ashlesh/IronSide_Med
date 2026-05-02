package com.ironsidemedical.connect.domain.usecase

import com.ironsidemedical.connect.data.bluetooth.BleGattManager
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.domain.repository.IVitalSignsRepository
import com.ironsidemedical.connect.util.AuditLogger
import com.ironsidemedical.connect.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Streams live vital-sign measurements from the connected device.
 *
 * Each measurement is persisted to the encrypted database and alerts are
 * audited. The flow is cold - collection starts the pipeline and cancellation
 * stops it cleanly without leaking the BLE channel.
 */
class MonitorVitalsUseCase @Inject constructor(
    private val bleGattManager: BleGattManager,
    private val vitalSignsRepository: IVitalSignsRepository,
    private val auditLogger: AuditLogger,
    private val sessionManager: SessionManager,
) {
    operator fun invoke(sessionId: String): Flow<VitalSigns> {
        val channel = bleGattManager.measurementFlow()
        return kotlinx.coroutines.flow.flow {
            for (vitals in channel) {
                val enriched = vitals.copy(
                    sessionId = sessionId,
                    deviceAddress = sessionManager.connectedDeviceAddress ?: "",
                )
                vitalSignsRepository.saveVitalSigns(enriched)
                if (enriched.hasAnyAlert) {
                    auditLogger.log(
                        AuditLogger.Event.ALERT_TRIGGERED,
                        buildAlertDetail(enriched)
                    )
                }
                emit(enriched)
            }
        }
    }

    private fun buildAlertDetail(v: VitalSigns) = buildString {
        if (v.isHeartRateAbnormal) append("HR=${v.heartRateBpm}bpm ")
        if (v.isSpo2Low) append("SpO2=${v.spo2Percent}% ")
        if (v.isBloodPressureAbnormal) append("BP=${v.systolicMmHg}/${v.diastolicMmHg}mmHg ")
        if (v.isTempAbnormal) append("Temp=${v.temperatureCelsius}°C")
    }.trim()
}

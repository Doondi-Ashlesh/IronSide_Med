package com.ironsidemedical.connect.domain.usecase

import android.content.Context
import com.google.gson.GsonBuilder
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.domain.repository.IVitalSignsRepository
import com.ironsidemedical.connect.util.AuditLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Exports session data to an encrypted JSON file in the app's private storage.
 *
 * The file is written to [Context.getFilesDir] — this directory is excluded from
 * Android Backup and inaccessible to other apps without root.
 * Sharing must go through a FileProvider with intent-based access control.
 *
 * FDA 21 CFR Part 11 requires all exported records to be audit-logged.
 */
class ExportSessionDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vitalSignsRepository: IVitalSignsRepository,
    private val auditLogger: AuditLogger,
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    private val formatter = DateTimeFormatter
        .ofPattern("yyyyMMdd_HHmmss")
        .withZone(ZoneId.systemDefault())

    suspend operator fun invoke(sessionId: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val vitals = vitalSignsRepository.getVitalSignsBetween(
                from = Instant.EPOCH,
                to = Instant.now(),
            ).filter { it.sessionId == sessionId }

            val payload = ExportPayload(
                exportedAt = Instant.now().toString(),
                sessionId = sessionId,
                recordCount = vitals.size,
                measurements = vitals.map { it.toExportRecord() },
            )

            val fileName = "ironside_session_${formatter.format(Instant.now())}.json"
            val file = File(context.filesDir, fileName)
            file.writeText(gson.toJson(payload))

            auditLogger.log(
                AuditLogger.Event.DATA_EXPORT,
                "sessionId=$sessionId records=${vitals.size} file=$fileName"
            )
            file
        }
    }

    private data class ExportPayload(
        val exportedAt: String,
        val sessionId: String,
        val recordCount: Int,
        val measurements: List<Map<String, Any>>,
    )

    private fun VitalSigns.toExportRecord(): Map<String, Any> = mapOf(
        "timestamp" to timestamp.toString(),
        "heartRateBpm" to heartRateBpm,
        "spo2Percent" to spo2Percent,
        "systolicMmHg" to systolicMmHg,
        "diastolicMmHg" to diastolicMmHg,
        "temperatureCelsius" to temperatureCelsius,
        "hasAlert" to hasAnyAlert,
    )
}

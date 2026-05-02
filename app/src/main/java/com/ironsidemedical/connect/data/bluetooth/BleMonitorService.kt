package com.ironsidemedical.connect.data.bluetooth

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ironsidemedical.connect.IronSideApp
import com.ironsidemedical.connect.MainActivity
import com.ironsidemedical.connect.R
import com.ironsidemedical.connect.data.repository.VitalSignsRepository
import com.ironsidemedical.connect.util.AuditLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that keeps BLE session alive when the app is backgrounded.
 *
 * Android requires a foreground service with a visible notification for any
 * long-running BLE connection — this satisfies that requirement while also
 * persisting measurement data to the encrypted database when the UI is gone.
 *
 * IEC 62304 note: the service restarts if killed (START_STICKY) to preserve
 * data continuity, but will stop cleanly when the user ends the session.
 */
@AndroidEntryPoint
class BleMonitorService : Service() {

    @Inject lateinit var bleGattManager: BleGattManager
    @Inject lateinit var vitalSignsRepository: VitalSignsRepository
    @Inject lateinit var auditLogger: AuditLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME) ?: "Unknown Device"
        startForeground(NOTIFICATION_ID, buildNotification(deviceName))
        collectMeasurements(deviceName)
        monitorConnectionState()
        return START_STICKY
    }

    private fun collectMeasurements(deviceName: String) {
        serviceScope.launch {
            for (vitals in bleGattManager.measurementFlow()) {
                vitalSignsRepository.saveVitalSigns(vitals)
            }
            Timber.d("Measurement channel closed, stopping service")
            stopSelf()
        }
    }

    private fun monitorConnectionState() {
        serviceScope.launch {
            bleGattManager.bleState
                .filterIsInstance<BleState.Error>()
                .collect { error ->
                    auditLogger.log(
                        AuditLogger.Event.BLE_ERROR,
                        "error=${error.code}, device=${error.device?.name}"
                    )
                    stopSelf()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(deviceName: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, IronSideApp.CHANNEL_BLE_SESSION)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_monitoring_text, deviceName))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val EXTRA_DEVICE_NAME = "device_name"
        private const val NOTIFICATION_ID = 1001
    }
}

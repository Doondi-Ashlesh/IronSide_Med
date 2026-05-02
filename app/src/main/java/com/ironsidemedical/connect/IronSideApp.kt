package com.ironsidemedical.connect

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ironsidemedical.connect.util.AuditLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Hilt component root, logging initialization, and notification channel setup
 * are all performed here so every dependent component is ready before any
 * Activity or Service starts.
 */
@HiltAndroidApp
class IronSideApp : Application() {

    @Inject lateinit var auditLogger: AuditLogger

    override fun onCreate() {
        super.onCreate()
        initLogging()
        createNotificationChannels()
        auditLogger.log(AuditLogger.Event.APP_LAUNCH, "version=${BuildConfig.SOFTWARE_VERSION}")
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // In release builds no debug tree is planted — Timber calls become no-ops.
        // This satisfies FDA cybersecurity requirement to disable verbose logging in production.
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val bleChannel = NotificationChannel(
                CHANNEL_BLE_SESSION,
                getString(R.string.notification_channel_ble),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_ble_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(bleChannel)
        }
    }

    companion object {
        const val CHANNEL_BLE_SESSION = "ble_session"
    }
}

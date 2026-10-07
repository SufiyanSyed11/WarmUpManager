package com.warmup.manager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service and monitor responsible for active app status and emergency safety tripwires
 * (captchas, "action blocked" dialogs, and verification barriers).
 *
 * Provides [stopForWarning] to immediately abort gesture automation and notify the user.
 */
class AssistMonitorService : Service() {

    companion object {
        const val ALERT_CHANNEL_ID = "warmup_assist_monitor_channel"
        const val ALERT_NOTIFICATION_ID = 4001

        private val _safetyHaltedReason = MutableStateFlow<String?>(null)
        val safetyHaltedReason: StateFlow<String?> = _safetyHaltedReason.asStateFlow()

        var isMonitoring = false
            private set

        /**
         * Static warning-stop entry point: halts active gestures, updates safety state,
         * and notifies the user.
         */
        fun stopForWarning(reason: String = "Platform warning or captcha triggered") {
            _safetyHaltedReason.value = reason
            isMonitoring = false
            WarmUpAccessibilityService.stopForWarning(reason)
        }

        fun resetSafetyHalt() {
            _safetyHaltedReason.value = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Instance method to stop assist for warning
     */
    fun stopForWarning(reason: String = "Platform warning or captcha triggered") {
        Companion.stopForWarning(reason)
        dispatchWarningNotification(reason)
    }

    private fun dispatchWarningNotification(reason: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Assist Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("WarmUp Assist Paused: Safety Alert")
            .setContentText(reason)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$reason All gestures terminated immediately to protect account standing. Complete verification manually on your device.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}

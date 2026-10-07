package com.warmup.manager.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.warmup.manager.WarmUpApp
import com.warmup.manager.data.model.Platform
import com.warmup.manager.data.model.WarmUpSessionEntity
import com.warmup.manager.engine.HumanBehaviorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * READ-ONLY Accessibility Safety Monitor.
 *
 * Adheres strictly to security requirements:
 * - NEVER performs automated gestures, swipes, likes, or bot actions.
 * - ONLY passively observes screen accessibility text in supported apps (TikTok, Instagram, YouTube)
 *   to detect safety barriers (CAPTCHAs, Login Required, Action Blocked, Try Again Later, Restrictions).
 * - When a safety trigger is identified, immediately halts the active WarmUp Manager session,
 *   logs the warning, and alerts the user.
 * - NEVER dismisses, bypasses, or interacts with the warning screen.
 */
class WarmUpAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isSessionActive = false
    private var activeAccountId: Long = 0
    private var activeUsername: String = ""
    private var activePlatform: Platform = Platform.TIKTOK
    private var targetDailyMinutes: Int = 30
    private var sessionStartTime: Long = 0

    companion object {
        const val ALERT_CHANNEL_ID = "warmup_safety_alert_channel"
        const val ALERT_NOTIFICATION_ID = 3001
        const val TARGET_CHANNEL_ID = "warmup_target_channel"
        const val TARGET_NOTIFICATION_ID = 3002

        var isServiceConnected = false
            private set

        var instance: WarmUpAccessibilityService? = null
            private set

        var lastDetectedWarning: String? = null
            private set

        fun stopForWarning(reason: String = "Platform safety alert triggered") {
            if (lastDetectedWarning == reason && instance?.isSessionActive == false) return
            lastDetectedWarning = reason
            instance?.triggerEmergencySafetyHalt(reason)
        }

        fun startSession(accountId: Long, username: String, platform: Platform, targetMinutes: Int) {
            instance?.startObservingSession(accountId, username, platform, targetMinutes)
        }

        fun stopSession(reason: String = "User ended session") {
            instance?.stopObservingSession(reason)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        val pkg = event?.packageName?.toString() ?: return

        val targetPackages = listOf(
            "com.zhiliaoapp.musically",      // TikTok
            "com.ss.android.ugc.trill",     // TikTok Global
            "com.instagram.android",        // Instagram
            "com.google.android.youtube"    // YouTube
        )

        if (targetPackages.none { pkg.contains(it) }) {
            return
        }

        // Read-only inspection of active window for security barriers
        val safetyCheck = HumanBehaviorEngine.detectSafetyBlockTrigger(rootNode)
        if (safetyCheck.isBlocked) {
            lastDetectedWarning = safetyCheck.reason
            triggerEmergencySafetyHalt(safetyCheck.reason)
        }
    }

    override fun onInterrupt() {
        stopObservingSession(reason = "Interrupted by system")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        if (instance == this) {
            instance = null
        }
        stopObservingSession(reason = "Service destroyed")
    }

    fun startObservingSession(
        accountId: Long,
        username: String,
        platform: Platform,
        targetMinutes: Int
    ) {
        if (isSessionActive) return
        isSessionActive = true
        activeAccountId = accountId
        activeUsername = username
        activePlatform = platform
        targetDailyMinutes = targetMinutes
        sessionStartTime = System.currentTimeMillis()
        lastDetectedWarning = null
    }

    fun stopObservingSession(reason: String = "Manual stop") {
        if (!isSessionActive) return
        isSessionActive = false

        val durationMinutes = ((System.currentTimeMillis() - sessionStartTime) / 60000L).toInt()
        if (durationMinutes > 0 && activeAccountId > 0) {
            val app = applicationContext as? WarmUpApp
            val repository = app?.repository
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val isWarning = reason != "Manual stop" && reason != "User ended session" && reason != "Service destroyed"
            serviceScope.launch {
                repository?.insertSession(
                    WarmUpSessionEntity(
                        accountId = activeAccountId,
                        startTime = sessionStartTime,
                        endTime = System.currentTimeMillis(),
                        durationMinutes = durationMinutes,
                        likesCount = 0, // Manual human mode: user in control of engagement
                        savesCount = 0,
                        dateString = dateStr,
                        warningReason = if (isWarning) reason else null
                    )
                )
            }
        }
    }

    /**
     * Halts automated tracking when a safety barrier (Captcha / Block / Restriction) is spotted.
     */
    fun stopForWarning(reason: String = "Platform safety alert triggered") {
        triggerEmergencySafetyHalt(reason)
    }

    private fun triggerEmergencySafetyHalt(reason: String) {
        stopObservingSession(reason = reason)
        if (AssistMonitorService.safetyHaltedReason.value != reason) {
            AssistMonitorService.stopForWarning(reason)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Safety Alerts",
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
                    .bigText("$reason Session monitoring halted immediately. Complete any verification manually in the app.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}

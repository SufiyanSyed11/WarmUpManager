package com.warmup.manager.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.warmup.manager.MainActivity
import com.warmup.manager.WarmUpApp
import com.warmup.manager.data.model.AssistSettings
import com.warmup.manager.data.model.VideoReaction
import com.warmup.manager.data.model.WarmUpSessionEntity
import com.warmup.manager.engine.HumanBehaviorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class WarmUpAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var isAssistRunning = false
    private var activeAccountId: Long = 0
    private var activeAccountUsername: String = ""
    private var settings = AssistSettings()

    private var screenWidth = 1080
    private var screenHeight = 2400

    private var sessionStartTime = 0L
    private var videosWatchedCount = 0
    private var videosSinceLastAction = 0
    private var likesDoneCount = 0
    private var savesDoneCount = 0

    companion object {
        const val ALERT_CHANNEL_ID = "warmup_safety_alert_channel"
        const val ALERT_NOTIFICATION_ID = 3001

        var isServiceConnected = false
            private set

        var instance: WarmUpAccessibilityService? = null
            private set

        /**
         * Stops active assist session and fires emergency safety alert
         */
        fun stopForWarning(reason: String = "Platform safety alert triggered") {
            instance?.stopForWarning(reason)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        instance = this

        val displayMetrics: DisplayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAssistRunning) return

        val pkg = event?.packageName?.toString() ?: return
        val targetPackages = listOf(
            "com.zhiliaoapp.musically",      // TikTok
            "com.ss.android.ugc.trill",     // TikTok Global
            "com.instagram.android",        // Instagram
            "com.google.android.youtube"    // YouTube
        )

        if (targetPackages.none { pkg.contains(it) }) {
            // Not currently in target app, idle until user switches to social app
            return
        }

        // Safety verification: check for captcha or action blocked dialogs
        val safetyCheck = HumanBehaviorEngine.detectSafetyBlockTrigger(rootInActiveWindow)
        if (safetyCheck.isBlocked) {
            triggerEmergencySafetyHalt(safetyCheck.reason)
        }
    }

    override fun onInterrupt() {
        stopAssistSession(reason = "Interrupted by system")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        if (instance == this) {
            instance = null
        }
        stopAssistSession(reason = "Service destroyed")
    }

    /**
     * Halts automated actions due to a safety warning or captcha trigger.
     */
    fun stopForWarning(reason: String = "Platform safety alert triggered") {
        triggerEmergencySafetyHalt(reason)
    }

    fun startAssistSession(accountId: Long, username: String, assistSettings: AssistSettings) {
        if (isAssistRunning) return
        isAssistRunning = true
        activeAccountId = accountId
        activeAccountUsername = username
        settings = assistSettings

        sessionStartTime = System.currentTimeMillis()
        videosWatchedCount = 0
        videosSinceLastAction = 5
        likesDoneCount = 0
        savesDoneCount = 0

        scheduleNextVideoCycle()
    }

    fun stopAssistSession(reason: String = "User requested stop") {
        if (!isAssistRunning) return
        isAssistRunning = false
        handler.removeCallbacksAndMessages(null)

        val durationMinutes = ((System.currentTimeMillis() - sessionStartTime) / 60000L).toInt()
        if (durationMinutes > 0 && activeAccountId > 0) {
            val app = applicationContext as? WarmUpApp
            val repository = app?.repository
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            serviceScope.launch {
                repository?.insertSession(
                    WarmUpSessionEntity(
                        accountId = activeAccountId,
                        startTime = sessionStartTime,
                        endTime = System.currentTimeMillis(),
                        durationMinutes = durationMinutes,
                        likesCount = likesDoneCount,
                        savesCount = savesDoneCount,
                        dateString = dateStr
                    )
                )
            }
        }
    }

    private fun scheduleNextVideoCycle() {
        if (!isAssistRunning) return

        // 1. Roll video reaction
        val reaction = HumanBehaviorEngine.rollVideoReaction(settings)
        val watchDuration = HumanBehaviorEngine.calculateWatchDurationMillis(reaction, matchedNiche = false)

        videosWatchedCount++
        videosSinceLastAction++

        // 2. Schedule actions for this video (Like / Save) during watch time
        val likeDecision = HumanBehaviorEngine.evaluateLikeDecision(
            reaction = reaction,
            settings = settings,
            videosSinceLastAction = videosSinceLastAction,
            currentLikesToday = likesDoneCount
        )

        if (likeDecision.shouldLike) {
            handler.postDelayed({
                if (isAssistRunning) {
                    performLikeGesture(likeDecision.useDoubleTap)
                    likesDoneCount++
                    videosSinceLastAction = 0

                    // Should save?
                    val shouldSave = HumanBehaviorEngine.evaluateSaveDecision(
                        wasLiked = true,
                        reaction = reaction,
                        settings = settings,
                        currentSavesToday = savesDoneCount
                    )
                    if (shouldSave) {
                        handler.postDelayed({
                            if (isAssistRunning) {
                                performSaveGesture()
                                savesDoneCount++
                            }
                        }, Random.nextLong(1200L, 2500L))
                    }
                }
            }, likeDecision.preActionDelayMillis)
        }

        // 3. After watch duration, perform human curved swipe to next video
        handler.postDelayed({
            if (isAssistRunning) {
                // Occasional human idle pause (3-15 seconds)
                val extraPause = if (Random.nextFloat() < 0.12f) Random.nextLong(3000L, 12000L) else 0L

                handler.postDelayed({
                    if (isAssistRunning) {
                        performHumanSwipeToNextVideo()
                        // Schedule next cycle
                        scheduleNextVideoCycle()
                    }
                }, extraPause)
            }
        }, watchDuration)
    }

    private fun performHumanSwipeToNextVideo() {
        val swipe = HumanBehaviorEngine.generateHumanSwipePath(screenWidth, screenHeight, isScrollUp = false)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(swipe.path, 0, swipe.durationMillis))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun performLikeGesture(useDoubleTap: Boolean) {
        val centerX = screenWidth * 0.5f + (Random.nextFloat() * 40f - 20f)
        val centerY = screenHeight * 0.5f + (Random.nextFloat() * 60f - 30f)

        if (useDoubleTap) {
            val tapPath1 = Path().apply { moveTo(centerX, centerY) }
            val tapPath2 = Path().apply { moveTo(centerX + 2f, centerY + 2f) }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(tapPath1, 0, 50))
                .addStroke(GestureDescription.StrokeDescription(tapPath2, 120, 50))
                .build()

            dispatchGesture(gesture, null, null)
        } else {
            // Heart button tap on right side of screen
            val heartX = screenWidth * 0.90f
            val heartY = screenHeight * 0.58f
            val heartPath = Path().apply { moveTo(heartX, heartY) }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(heartPath, 0, 80))
                .build()

            dispatchGesture(gesture, null, null)
        }
    }

    private fun performSaveGesture() {
        // Bookmark button tap on right side
        val saveX = screenWidth * 0.90f
        val saveY = screenHeight * 0.67f
        val savePath = Path().apply { moveTo(saveX, saveY) }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(savePath, 0, 80))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun triggerEmergencySafetyHalt(reason: String) {
        stopAssistSession(reason = reason)

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
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason + " Automated actions immediately stopped to safeguard your account. Complete any verification manually."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}

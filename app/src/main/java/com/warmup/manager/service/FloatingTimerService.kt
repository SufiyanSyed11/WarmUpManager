package com.warmup.manager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.warmup.manager.MainActivity
import com.warmup.manager.WarmUpApp
import com.warmup.manager.data.model.WarmUpSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FloatingTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    private var accountId: Long = 0
    private var username: String = ""
    private var targetMinutes: Int = 30
    private var platformName: String = "TIKTOK"

    private var startTimeMillis: Long = 0
    private var elapsedSeconds: Int = 0
    private var likesCount: Int = 0
    private var savesCount: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++
            updateTimerDisplay()
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        const val CHANNEL_ID = "warmup_floating_timer_channel"
        const val NOTIFICATION_ID = 2001

        const val EXTRA_ACCOUNT_ID = "extra_account_id"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_TARGET_MINS = "extra_target_mins"
        const val EXTRA_PLATFORM = "extra_platform"

        fun start(context: Context, accountId: Long, username: String, targetMins: Int, platform: String) {
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                putExtra(EXTRA_ACCOUNT_ID, accountId)
                putExtra(EXTRA_USERNAME, username)
                putExtra(EXTRA_TARGET_MINS, targetMins)
                putExtra(EXTRA_PLATFORM, platform)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingTimerService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            accountId = it.getLongExtra(EXTRA_ACCOUNT_ID, 0)
            username = it.getStringExtra(EXTRA_USERNAME) ?: "Account"
            targetMinutes = it.getIntExtra(EXTRA_TARGET_MINS, 30)
            platformName = it.getStringExtra(EXTRA_PLATFORM) ?: "TIKTOK"
        }

        startTimeMillis = System.currentTimeMillis()
        elapsedSeconds = 0
        likesCount = 0
        savesCount = 0

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        handler.post(tickerRunnable)
        createFloatingOverlayIfPermitted()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Warmup Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live timer during account warmup"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Warming up $username")
            .setContentText("Elapsed: $timeString • Likes: $likesCount • Saves: $savesCount")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun updateTimerDisplay() {
        // Updated on tickerRunnable tick
    }

    private fun createFloatingOverlayIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Can't draw overlays without permission, user can still use notification controls
            return
        }
        // Floating window parameters
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickerRunnable)

        // Save session if elapsed time was at least 1 minute or non-zero
        val elapsedMins = (elapsedSeconds / 60).coerceAtLeast(if (elapsedSeconds >= 30) 1 else 0)
        if (accountId > 0 && elapsedMins > 0) {
            val app = application as? WarmUpApp
            val repository = app?.repository
            val now = System.currentTimeMillis()
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            serviceScope.launch {
                repository?.insertSession(
                    WarmUpSessionEntity(
                        accountId = accountId,
                        startTime = startTimeMillis,
                        endTime = now,
                        durationMinutes = elapsedMins,
                        likesCount = likesCount,
                        savesCount = savesCount,
                        dateString = dateStr
                    )
                )
                serviceScope.cancel()
            }
        } else {
            serviceScope.cancel()
        }

        floatingView?.let {
            windowManager?.removeView(it)
        }
    }
}

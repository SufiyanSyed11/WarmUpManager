package com.warmup.manager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.warmup.manager.MainActivity
import com.warmup.manager.WarmUpApp
import com.warmup.manager.data.model.WarmUpStatus
import com.warmup.manager.util.WarmUpEngine
import java.time.LocalDate

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "warmup_reminders_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? WarmUpApp ?: return Result.failure()
        val repository = app.repository
        val database = app.database

        val accounts = database.accountDao().getAllAccounts()
        val allSessions = database.sessionDao().getAllSessions()

        // Synchronously collect or query current state
        val today = LocalDate.now()
        val uncompletedAccounts = mutableListOf<String>()

        val accountList = database.accountDao().getAllAccountsDirect()
        for (account in accountList) {
            val sessions = database.sessionDao().getSessionsForAccountDirect(account.id)
            val calc = WarmUpEngine.calculateAccountProgress(account, sessions, today)

            // If account is not already fully warmed up and hasn't met today's minutes
            if (calc.status !is WarmUpStatus.WarmedUp && !calc.todayTargetMet) {
                val remaining = account.targetDailyMinutes - calc.todayMinutes
                uncompletedAccounts.add("${account.username} (${remaining}m remaining)")
            }
        }

        if (uncompletedAccounts.isNotEmpty()) {
            sendReminderNotification(uncompletedAccounts)
        }

        return Result.success()
    }

    private fun sendReminderNotification(accountsNeedingTime: List<String>) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Warmup Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when accounts need warmup time to maintain streak"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (accountsNeedingTime.size == 1) {
            "${accountsNeedingTime.first()} still needs warmup today!"
        } else {
            "${accountsNeedingTime.size} accounts need warmup today:\n" + accountsNeedingTime.take(3).joinToString(", ")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("WarmUp Manager: Time to Warm Up")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

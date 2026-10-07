package com.warmup.manager.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WarmUpWorkManager {
    private const val WORK_NAME = "daily_warmup_reminder_work"

    fun scheduleDailyReminders(context: Context) {
        // Run periodic reminder every 6 hours to check if today's warmup goals are met
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(6, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

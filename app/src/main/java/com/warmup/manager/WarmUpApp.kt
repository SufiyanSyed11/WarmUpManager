package com.warmup.manager

import android.app.Application
import com.warmup.manager.data.db.AppDatabase
import com.warmup.manager.data.repository.WarmUpRepository

class WarmUpApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: WarmUpRepository by lazy {
        WarmUpRepository(database.accountDao(), database.sessionDao())
    }

    override fun onCreate() {
        super.onCreate()
        com.warmup.manager.worker.WarmUpWorkManager.scheduleDailyReminders(this)
    }
}

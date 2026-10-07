package com.warmup.manager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val platform: Platform,
    val nicheTag: String = "",
    val notes: String = "",
    val targetDailyMinutes: Int = 30, // Default 30 min/day
    val targetDays: Int = 5,          // Default 5 days (suggested 5, option 3-7)
    val streakResetCount: Int = 0,    // tracks streak reset history
    val isExplicitlyWarmedUp: Boolean = false,
    val isConnected: Boolean = false, // Connection status (Tracked vs Connected)
    val createdAt: Long = System.currentTimeMillis()
)

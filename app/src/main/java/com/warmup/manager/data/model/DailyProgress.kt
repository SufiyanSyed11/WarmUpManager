package com.warmup.manager.data.model

data class DailyProgress(
    val dateString: String,
    val totalMinutes: Int,
    val totalSessions: Int,
    val totalLikes: Int,
    val totalSaves: Int,
    val targetMinutes: Int,
    val isTargetMet: Boolean
)

data class AccountWarmUpCalculation(
    val account: AccountEntity,
    val status: WarmUpStatus,
    val qualifyingDaysCount: Int,
    val consecutiveStreakDays: Int,
    val hasMissedDay: Boolean,
    val todayMinutes: Int,
    val todayTargetMet: Boolean,
    val dailyProgressList: List<DailyProgress>,
    val totalMinutesAllTime: Int,
    val totalLikesAllTime: Int,
    val totalSavesAllTime: Int
)

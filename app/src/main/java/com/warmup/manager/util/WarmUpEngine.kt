package com.warmup.manager.util

import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.data.model.AccountWarmUpCalculation
import com.warmup.manager.data.model.DailyProgress
import com.warmup.manager.data.model.WarmUpSessionEntity
import com.warmup.manager.data.model.WarmUpStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WarmUpEngine {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateAccountProgress(
        account: AccountEntity,
        sessions: List<WarmUpSessionEntity>,
        currentDate: LocalDate = LocalDate.now()
    ): AccountWarmUpCalculation {
        // Group sessions by date
        val sessionsByDate = sessions.groupBy { it.dateString }

        // Compute daily aggregates
        val dailyProgressMap = mutableMapOf<String, DailyProgress>()
        sessionsByDate.forEach { (dateStr, daySessions) ->
            val totalMins = daySessions.sumOf { it.durationMinutes }
            val likes = daySessions.sumOf { it.likesCount }
            val saves = daySessions.sumOf { it.savesCount }
            dailyProgressMap[dateStr] = DailyProgress(
                dateString = dateStr,
                totalMinutes = totalMins,
                totalSessions = daySessions.size,
                totalLikes = likes,
                totalSaves = saves,
                targetMinutes = account.targetDailyMinutes,
                isTargetMet = totalMins >= account.targetDailyMinutes
            )
        }

        val sortedDailyList = dailyProgressMap.values.sortedByDescending { it.dateString }

        val totalMinutesAll = sessions.sumOf { it.durationMinutes }
        val totalLikesAll = sessions.sumOf { it.likesCount }
        val totalSavesAll = sessions.sumOf { it.savesCount }

        val todayStr = currentDate.format(dateFormatter)
        val todayProgress = dailyProgressMap[todayStr]
        val todayMinutes = todayProgress?.totalMinutes ?: 0
        val todayTargetMet = todayMinutes >= account.targetDailyMinutes

        // Total qualifying days (any day where daily minutes >= target)
        val qualifyingDaysCount = dailyProgressMap.values.count { it.isTargetMet }

        // Consecutive streak calculation backwards from today or yesterday
        var consecutiveStreak = 0
        var hasMissedDay = false

        // Check if today is completed
        val checkStartDate = if (todayTargetMet) currentDate else currentDate.minusDays(1)
        val yesterdayStr = currentDate.minusDays(1).format(dateFormatter)
        val yesterdayProgress = dailyProgressMap[yesterdayStr]

        // Count streak backwards
        var cursor = checkStartDate
        while (true) {
            val cursorStr = cursor.format(dateFormatter)
            val progress = dailyProgressMap[cursorStr]
            if (progress != null && progress.isTargetMet) {
                consecutiveStreak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }

        // Missed day detection: If account has had activity before yesterday, but yesterday was NOT met,
        // and today is not yet the only day, warn user of streak interruption.
        if (qualifyingDaysCount > 0 && !todayTargetMet) {
            val hadActivityBefore = dailyProgressMap.keys.any { it < yesterdayStr }
            if (hadActivityBefore && (yesterdayProgress == null || !yesterdayProgress.isTargetMet)) {
                hasMissedDay = true
            }
        }

        // Effective streak or qualifying count considering manual reset
        val effectiveProgressCount = if (account.streakResetCount > 0) {
            consecutiveStreak
        } else {
            // By default, if consecutive days meet target, or qualifying days meet target
            consecutiveStreak.coerceAtLeast(if (!hasMissedDay) qualifyingDaysCount else consecutiveStreak)
        }

        // Determine Status
        val status: WarmUpStatus = when {
            account.isExplicitlyWarmedUp || effectiveProgressCount >= account.targetDays -> {
                WarmUpStatus.WarmedUp
            }
            effectiveProgressCount > 0 -> {
                WarmUpStatus.WarmingUp(
                    currentDay = effectiveProgressCount,
                    targetDays = account.targetDays
                )
            }
            else -> {
                WarmUpStatus.NotStarted
            }
        }

        return AccountWarmUpCalculation(
            account = account,
            status = status,
            qualifyingDaysCount = qualifyingDaysCount,
            consecutiveStreakDays = consecutiveStreak,
            hasMissedDay = hasMissedDay,
            todayMinutes = todayMinutes,
            todayTargetMet = todayTargetMet,
            dailyProgressList = sortedDailyList,
            totalMinutesAllTime = totalMinutesAll,
            totalLikesAllTime = totalLikesAll,
            totalSavesAllTime = totalSavesAll
        )
    }
}

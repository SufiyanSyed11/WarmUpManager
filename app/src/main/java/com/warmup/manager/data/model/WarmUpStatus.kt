package com.warmup.manager.data.model

sealed class WarmUpStatus {
    data object NotStarted : WarmUpStatus()
    data class WarmingUp(val currentDay: Int, val targetDays: Int) : WarmUpStatus() {
        val progress: Float
            get() = (currentDay.toFloat() / targetDays.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    }
    data object WarmedUp : WarmUpStatus()

    fun getBadgeText(): String = when (this) {
        is NotStarted -> "Not started"
        is WarmingUp -> "Warming up - Day $currentDay of $targetDays"
        is WarmedUp -> "Warmed up"
    }
}

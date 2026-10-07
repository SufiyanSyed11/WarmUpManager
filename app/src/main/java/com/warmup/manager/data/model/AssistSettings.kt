package com.warmup.manager.data.model

enum class VideoReaction(val displayName: String, val watchDescription: String) {
    INSTANT_SKIP("Instant Skip (~25%)", "Swipe away after 1-3 seconds"),
    QUICK_GLANCE("Quick Glance (~20%)", "Watch 20-40%, then swipe"),
    PARTIAL_WATCH("Partial Watch (~25%)", "Watch 50-80%, then swipe"),
    FULL_WATCH("Full Watch (~20%)", "Watch to end, loop once"),
    REWATCH("Rewatch (~10%)", "Watch fully and loop 2-3 times")
}

enum class SessionMood(val displayName: String, val likeMultiplier: Float) {
    LOW("Low Engagement", 0.3f),     // Many sessions have 0 likes
    MEDIUM("Medium Engagement", 1.0f),
    HIGH("High Engagement", 1.6f)
}

data class AssistSettings(
    val isEnabled: Boolean = false,
    val sessionMood: SessionMood = SessionMood.MEDIUM,

    // Watch duration reaction probabilities (sum to 1.0)
    val instantSkipProb: Float = 0.25f,
    val quickGlanceProb: Float = 0.20f,
    val partialWatchProb: Float = 0.25f,
    val fullWatchProb: Float = 0.20f,
    val rewatchProb: Float = 0.10f,

    // Engagement probabilities
    val likeProbability: Float = 0.08f,    // 6-10% (only on full watch / rewatch)
    val saveProbability: Float = 0.03f,    // 2-4% (only after like or rewatch)
    val unlikeProbability: Float = 0.01f,  // ~1% like then unlike
    val minGapVideos: Int = 4,             // 3-8 videos gap between actions

    // Safety and limits
    val dailyLikeCap: Int = 20,
    val dailySaveCap: Int = 8,
    val autoStopOnCaptcha: Boolean = true,
    val nicheKeywords: List<String> = emptyList()
) {
    /**
     * Calculates mathematical estimation of actions in a typical 20-minute session
     */
    fun calculateExpected20MinMetrics(): ExpectedSessionMetrics {
        // Average video length assumed ~20-25s
        // Weighted average view duration based on probabilities
        // Instant skip: ~2s
        // Quick glance: ~7s
        // Partial watch: ~14s
        // Full watch: ~22s
        // Rewatch: ~45s
        val avgSecondsPerVideo = (instantSkipProb * 2f) +
                (quickGlanceProb * 7f) +
                (partialWatchProb * 14f) +
                (fullWatchProb * 22f) +
                (rewatchProb * 45f) + 1.5f // + swipe transition time

        val totalVideosIn20Min = (1200f / avgSecondsPerVideo.coerceAtLeast(3f)).toInt()
        val qualifyingVideos = (totalVideosIn20Min * (fullWatchProb + rewatchProb)).toInt()

        val expectedLikes = if (sessionMood == SessionMood.LOW && Math.random() < 0.4) {
            0
        } else {
            (qualifyingVideos * likeProbability * sessionMood.likeMultiplier).toInt().coerceAtMost(dailyLikeCap)
        }

        val expectedSaves = (expectedLikes * saveProbability * 3f).toInt().coerceAtMost(dailySaveCap)

        return ExpectedSessionMetrics(
            estimatedVideosWatched = totalVideosIn20Min,
            estimatedLikes = expectedLikes,
            estimatedSaves = expectedSaves,
            averageVideoSeconds = avgSecondsPerVideo
        )
    }
}

data class ExpectedSessionMetrics(
    val estimatedVideosWatched: Int,
    val estimatedLikes: Int,
    val estimatedSaves: Int,
    val averageVideoSeconds: Float
)

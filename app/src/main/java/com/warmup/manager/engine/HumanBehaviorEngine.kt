package com.warmup.manager.engine

import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import com.warmup.manager.data.model.AssistSettings
import com.warmup.manager.data.model.SessionMood
import com.warmup.manager.data.model.VideoReaction
import java.util.Locale
import kotlin.random.Random

object HumanBehaviorEngine {

    /**
     * Re-rolls a video reaction using weighted probabilities
     */
    fun rollVideoReaction(settings: AssistSettings): VideoReaction {
        val r = Random.nextFloat()
        var cumulative = settings.instantSkipProb
        if (r < cumulative) return VideoReaction.INSTANT_SKIP

        cumulative += settings.quickGlanceProb
        if (r < cumulative) return VideoReaction.QUICK_GLANCE

        cumulative += settings.partialWatchProb
        if (r < cumulative) return VideoReaction.PARTIAL_WATCH

        cumulative += settings.fullWatchProb
        if (r < cumulative) return VideoReaction.FULL_WATCH

        return VideoReaction.REWATCH
    }

    /**
     * Calculates realistic watch duration in milliseconds based on reaction and niche keywords
     */
    fun calculateWatchDurationMillis(
        reaction: VideoReaction,
        matchedNiche: Boolean
    ): Long {
        val baseDuration = when (reaction) {
            VideoReaction.INSTANT_SKIP -> Random.nextLong(1000L, 3000L) // 1-3s
            VideoReaction.QUICK_GLANCE -> Random.nextLong(4000L, 8000L) // 20-40% of ~20s
            VideoReaction.PARTIAL_WATCH -> Random.nextLong(10000L, 16000L) // 50-80%
            VideoReaction.FULL_WATCH -> Random.nextLong(18000L, 25000L) // full + maybe loop once
            VideoReaction.REWATCH -> Random.nextLong(35000L, 55000L) // loops 2-3 times
        }

        // If video matches account niche tag, give it 1.3x - 1.6x longer watch interest
        return if (matchedNiche && reaction != VideoReaction.INSTANT_SKIP) {
            (baseDuration * (1.3f + Random.nextFloat() * 0.3f)).toLong()
        } else {
            baseDuration
        }
    }

    /**
     * Determines whether to like this video
     * Rules:
     * - Only 6-10% overall
     * - Only from Full watch or Rewatch. NEVER like a skipped or glanced video.
     * - Respect minimum gap of 3-8 videos between actions.
     * - Randomize mood scaling. Some low mood sessions have zero likes.
     */
    fun evaluateLikeDecision(
        reaction: VideoReaction,
        settings: AssistSettings,
        videosSinceLastAction: Int,
        currentLikesToday: Int
    ): LikeDecision {
        // Hard daily cap
        if (currentLikesToday >= settings.dailyLikeCap) {
            return LikeDecision.NO_LIKE
        }

        // Quiet gap restriction (3-8 videos)
        if (videosSinceLastAction < settings.minGapVideos) {
            return LikeDecision.NO_LIKE
        }

        // Only Full watch or Rewatch can ever trigger a like
        if (reaction != VideoReaction.FULL_WATCH && reaction != VideoReaction.REWATCH) {
            return LikeDecision.NO_LIKE
        }

        // Low engagement sessions might have zero likes
        if (settings.sessionMood == SessionMood.LOW && Random.nextFloat() < 0.40f) {
            return LikeDecision.NO_LIKE
        }

        val effectiveLikeProb = (settings.likeProbability * settings.sessionMood.likeMultiplier).coerceIn(0.01f, 0.20f)
        if (Random.nextFloat() < effectiveLikeProb) {
            val isDoubleTap = Random.nextBoolean() // Mix double-tap vs heart button
            val delayMillis = Random.nextLong(1000L, 4000L) // 1-4s delay before liking
            val shouldUnlike = Random.nextFloat() < settings.unlikeProbability // ~1% like then unlike

            return LikeDecision(
                shouldLike = true,
                useDoubleTap = isDoubleTap,
                preActionDelayMillis = delayMillis,
                shouldUnlikeShortlyAfter = shouldUnlike
            )
        }

        return LikeDecision.NO_LIKE
    }

    /**
     * Determines whether to save/bookmark this video
     * Rules:
     * - Only ~2-4%
     * - Only after like or rewatch
     */
    fun evaluateSaveDecision(
        wasLiked: Boolean,
        reaction: VideoReaction,
        settings: AssistSettings,
        currentSavesToday: Int
    ): Boolean {
        if (currentSavesToday >= settings.dailySaveCap) return false
        if (!wasLiked && reaction != VideoReaction.REWATCH) return false

        return Random.nextFloat() < settings.saveProbability
    }

    /**
     * Generates a natural human swipe gesture Path with Bezier curvature and tremor jitter
     */
    fun generateHumanSwipePath(
        screenWidth: Int,
        screenHeight: Int,
        isScrollUp: Boolean = false
    ): HumanSwipeData {
        // Vary start and end positions
        val startX = (screenWidth * (0.45f + (Random.nextFloat() * 0.15f - 0.075f)))
        val endX = (screenWidth * (0.45f + (Random.nextFloat() * 0.18f - 0.09f)))

        val swipeDistanceFraction = 0.65f + Random.nextFloat() * 0.20f // 65-85% screen height
        val startY = if (!isScrollUp) {
            (screenHeight * (0.75f + Random.nextFloat() * 0.10f))
        } else {
            (screenHeight * (0.25f - Random.nextFloat() * 0.10f))
        }

        val endY = if (!isScrollUp) {
            startY - (screenHeight * swipeDistanceFraction)
        } else {
            startY + (screenHeight * swipeDistanceFraction)
        }

        // Bezier control point with slight curved deflection
        val controlX = (startX + endX) / 2f + (Random.nextFloat() * 60f - 30f)
        val controlY = (startY + endY) / 2f + (Random.nextFloat() * 40f - 20f)

        val path = Path().apply {
            moveTo(startX, startY)
            quadTo(controlX, controlY, endX, endY)
        }

        // Vary swipe speed (150-600ms)
        val duration = Random.nextLong(180L, 550L)

        return HumanSwipeData(
            path = path,
            durationMillis = duration
        )
    }

    /**
     * Scans accessibility tree for Captchas, "Action Blocked", "Try Again Later", "Login Required" or restriction warnings.
     * READ-ONLY inspection to detect safety barriers.
     */
    fun detectSafetyBlockTrigger(rootNode: AccessibilityNodeInfo?): SafetyBlockResult {
        if (rootNode == null) return SafetyBlockResult.CLEAN

        val warningPatterns = listOf(
            // 1. CAPTCHA
            "captcha",
            "verify you're human",
            "slide to complete",
            "puzzle",
            "drag the slider",
            "not a robot",
            "security verification",
            // 2. Login required
            "login required",
            "please log in",
            "log in to continue",
            "session expired",
            "sign in required",
            "logged out",
            // 3. Action blocked
            "action blocked",
            "temporarily blocked",
            "action restricted",
            "feature unavailable",
            // 4. Try again later
            "try again later",
            "we limit how often",
            "wait a few minutes",
            "too many attempts",
            // 5. Warning/restriction screens
            "account restricted",
            "warning screen",
            "unusual activity",
            "suspicious login",
            "confirm your identity",
            "security check",
            "community guidelines",
            "protect our community",
            "temporarily locked",
            "verification required"
        )

        for (pattern in warningPatterns) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByText(pattern)
            if (matchingNodes.isNotEmpty()) {
                return SafetyBlockResult(
                    isBlocked = true,
                    reason = "Platform safety alert triggered: '$pattern' detected on screen."
                )
            }
        }

        // Secondary recursive scan on text & contentDescription for exact or partial matches
        var detectedReason: String? = null
        fun scanNode(node: AccessibilityNodeInfo?) {
            if (node == null || detectedReason != null) return
            val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""
            for (pattern in warningPatterns) {
                if (nodeText.contains(pattern) || contentDesc.contains(pattern)) {
                    detectedReason = "Platform safety alert triggered: '$pattern' detected on screen."
                    return
                }
            }
            for (i in 0 until node.childCount) {
                scanNode(node.getChild(i))
                if (detectedReason != null) return
            }
        }
        scanNode(rootNode)

        if (detectedReason != null) {
            return SafetyBlockResult(isBlocked = true, reason = detectedReason!!)
        }

        return SafetyBlockResult.CLEAN
    }
}

data class LikeDecision(
    val shouldLike: Boolean,
    val useDoubleTap: Boolean = true,
    val preActionDelayMillis: Long = 1500L,
    val shouldUnlikeShortlyAfter: Boolean = false
) {
    companion object {
        val NO_LIKE = LikeDecision(shouldLike = false)
    }
}

data class HumanSwipeData(
    val path: Path,
    val durationMillis: Long
)

data class SafetyBlockResult(
    val isBlocked: Boolean,
    val reason: String = ""
) {
    companion object {
        val CLEAN = SafetyBlockResult(isBlocked = false)
    }
}

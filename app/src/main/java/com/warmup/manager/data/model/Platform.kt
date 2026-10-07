package com.warmup.manager.data.model

enum class Platform(val displayName: String, val hexColor: Long) {
    TIKTOK("TikTok", 0xFFEE1D52),
    INSTAGRAM("Instagram", 0xFFE1306C),
    YOUTUBE("YouTube", 0xFFFF0000);

    companion object {
        fun fromString(name: String): Platform {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: TIKTOK
        }
    }
}

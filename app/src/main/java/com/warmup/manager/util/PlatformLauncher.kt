package com.warmup.manager.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.warmup.manager.data.model.Platform

object PlatformLauncher {

    fun getPackageNames(platform: Platform): List<String> {
        return when (platform) {
            Platform.TIKTOK -> listOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill"
            )
            Platform.INSTAGRAM -> listOf(
                "com.instagram.android"
            )
            Platform.YOUTUBE -> listOf(
                "com.google.android.youtube"
            )
        }
    }

    fun isAppInstalled(context: Context, platform: Platform): Boolean {
        val pm = context.packageManager
        val packages = getPackageNames(platform)
        for (pkg in packages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed
            }
        }
        return false
    }

    /**
     * Launches the official social media app on the device.
     * Uses deep linking to the specified account handle when possible,
     * or falls back to opening the platform profile in the browser.
     */
    fun openPlatform(context: Context, platform: Platform, username: String? = null): Boolean {
        val cleanHandle = username?.trim()?.removePrefix("@") ?: ""
        val pm = context.packageManager
        val packages = getPackageNames(platform)

        // 1. Try launching the official app directly or via deep link
        for (pkg in packages) {
            try {
                pm.getPackageInfo(pkg, 0)

                val intent: Intent? = when (platform) {
                    Platform.TIKTOK -> {
                        if (cleanHandle.isNotEmpty()) {
                            Intent(Intent.ACTION_VIEW, Uri.parse("snssdk1233://user/profile/$cleanHandle")).apply {
                                setPackage(pkg)
                            }
                        } else {
                            pm.getLaunchIntentForPackage(pkg)
                        }
                    }
                    Platform.INSTAGRAM -> {
                        if (cleanHandle.isNotEmpty()) {
                            Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/$cleanHandle")).apply {
                                setPackage(pkg)
                            }
                        } else {
                            pm.getLaunchIntentForPackage(pkg)
                        }
                    }
                    Platform.YOUTUBE -> {
                        if (cleanHandle.isNotEmpty()) {
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@$cleanHandle")).apply {
                                setPackage(pkg)
                            }
                        } else {
                            pm.getLaunchIntentForPackage(pkg)
                        }
                    }
                }

                val finalIntent = intent ?: pm.getLaunchIntentForPackage(pkg)
                if (finalIntent != null) {
                    finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(finalIntent)
                    return true
                }
            } catch (_: Exception) {
                // Try next package or fallback
            }
        }

        // 2. Fallback to web browser URL
        val webUrl = when (platform) {
            Platform.TIKTOK -> if (cleanHandle.isNotEmpty()) "https://www.tiktok.com/@$cleanHandle" else "https://www.tiktok.com"
            Platform.INSTAGRAM -> if (cleanHandle.isNotEmpty()) "https://www.instagram.com/$cleanHandle" else "https://www.instagram.com"
            Platform.YOUTUBE -> if (cleanHandle.isNotEmpty()) "https://www.youtube.com/@$cleanHandle" else "https://www.youtube.com"
        }

        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open ${platform.displayName}: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

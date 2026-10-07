package com.warmup.manager.ui.screens.assist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.data.model.AccountWarmUpCalculation
import com.warmup.manager.data.model.AssistSettings
import com.warmup.manager.data.model.Platform
import com.warmup.manager.data.model.SessionMood
import com.warmup.manager.data.model.WarmUpSessionEntity
import com.warmup.manager.service.AssistMonitorService
import com.warmup.manager.service.WarmUpAccessibilityService
import com.warmup.manager.ui.theme.DarkBackground
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.DarkSurfaceVariant
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.TextMuted
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary
import com.warmup.manager.util.PlatformLauncher
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class TelemetryLogEntry(
    val id: String,
    val time: String,
    val text: String,
    val type: LogType
)

enum class LogType {
    INFO, LIKE, SAVE, SKIP, ALERT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistEngineScreen(
    accounts: List<AccountEntity> = emptyList(),
    calculations: List<AccountWarmUpCalculation> = emptyList(),
    activeTimerAccount: AccountEntity? = null,
    elapsedSeconds: Int = 0,
    onStartRealSession: (AccountEntity) -> Unit = {},
    onStopRealSession: () -> Unit = {},
    onUpdateAccount: (AccountEntity) -> Unit = {},
    currentSettings: AssistSettings,
    onSaveSettings: (AssistSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Observe reactive safety alert states from AssistMonitorService
    val safetyHaltedState by AssistMonitorService.safetyHaltedReason.collectAsState()

    // Real Device Session Assistant Selection State
    var selectedPlatform by remember { mutableStateOf(Platform.TIKTOK) }
    val platformAccounts = remember(accounts, selectedPlatform) {
        accounts.filter { it.platform == selectedPlatform }
    }

    var selectedAccountId by remember(platformAccounts, activeTimerAccount) {
        val initialId = activeTimerAccount?.id
            ?: platformAccounts.firstOrNull()?.id
            ?: accounts.firstOrNull()?.id
            ?: 0L
        mutableStateOf(initialId)
    }

    val selectedAccount = remember(accounts, selectedAccountId) {
        accounts.find { it.id == selectedAccountId } ?: platformAccounts.firstOrNull() ?: accounts.firstOrNull()
    }

    val selectedCalculation = remember(calculations, selectedAccount) {
        selectedAccount?.let { acc -> calculations.find { it.account.id == acc.id } }
    }

    val isRunningThisAccount = activeTimerAccount != null && selectedAccount != null && activeTimerAccount.id == selectedAccount.id

    // Assist Master Enable Switch
    var isEnabled by remember { mutableStateOf(currentSettings.isEnabled) }
    var mood by remember { mutableStateOf(currentSettings.sessionMood) }

    var instantSkip by remember { mutableFloatStateOf(currentSettings.instantSkipProb) }
    var quickGlance by remember { mutableFloatStateOf(currentSettings.quickGlanceProb) }
    var partialWatch by remember { mutableFloatStateOf(currentSettings.partialWatchProb) }
    var fullWatch by remember { mutableFloatStateOf(currentSettings.fullWatchProb) }
    var rewatch by remember { mutableFloatStateOf(currentSettings.rewatchProb) }

    var likeProb by remember { mutableFloatStateOf(currentSettings.likeProbability) }
    var saveProb by remember { mutableFloatStateOf(currentSettings.saveProbability) }
    var dailyLikeCap by remember { mutableIntStateOf(currentSettings.dailyLikeCap) }
    var dailySaveCap by remember { mutableIntStateOf(currentSettings.dailySaveCap) }
    var nicheKeywordsText by remember { mutableStateOf(currentSettings.nicheKeywords.joinToString(", ")) }

    val liveSettings = remember(
        isEnabled, mood, instantSkip, quickGlance, partialWatch, fullWatch, rewatch,
        likeProb, saveProb, dailyLikeCap, dailySaveCap, nicheKeywordsText
    ) {
        val keywords = nicheKeywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        AssistSettings(
            isEnabled = isEnabled,
            sessionMood = mood,
            instantSkipProb = instantSkip,
            quickGlanceProb = quickGlance,
            partialWatchProb = partialWatch,
            fullWatchProb = fullWatch,
            rewatchProb = rewatch,
            likeProbability = likeProb,
            saveProbability = saveProb,
            dailyLikeCap = dailyLikeCap,
            dailySaveCap = dailySaveCap,
            nicheKeywords = keywords
        )
    }

    val liveMetrics = remember(liveSettings) {
        liveSettings.calculateExpected20MinMetrics()
    }

    // Local Behavioral Simulator State (isolated simulation as required)
    var isSimulatorRunning by remember { mutableStateOf(false) }
    var simSafetyHalted by remember { mutableStateOf<String?>(null) }
    var simVideoIndex by remember { mutableIntStateOf(1) }
    var simCurrentReaction by remember { mutableStateOf("Partial Watch (~25%)") }
    var simWatchSeconds by remember { mutableIntStateOf(0) }
    var simTargetSeconds by remember { mutableIntStateOf(12) }
    var simLastAction by remember { mutableStateOf("Watching current video in simulation player") }
    var simSwipeTrajectory by remember { mutableStateOf("Bézier swipe: duration 320ms, curve offset -8px, distance 76% height") }
    var simLikesInSession by remember { mutableIntStateOf(0) }
    var simSavesInSession by remember { mutableIntStateOf(0) }
    var simVideosSinceAction by remember { mutableIntStateOf(3) }

    val simLogs = remember {
        mutableStateListOf(
            TelemetryLogEntry(
                id = "init",
                time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
                text = "Assist Safety Engine initialized. Ready to observe live session.",
                type = LogType.INFO
            )
        )
    }

    // Sync safety alert from real monitor into telemetry log
    LaunchedEffect(safetyHaltedState) {
        safetyHaltedState?.let { reason ->
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            simLogs.add(0, TelemetryLogEntry(
                id = System.currentTimeMillis().toString(),
                time = timeStr,
                text = "SAFETY ALERT: $reason -> Session stopped immediately.",
                type = LogType.ALERT
            ))
        }
    }

    // Local Simulator Step logic
    fun triggerNextSimVideo() {
        if (simSafetyHalted != null) return
        val nextIndex = simVideoIndex + 1
        simVideoIndex = nextIndex

        val r = Random.nextFloat()
        val reaction: String
        val targetSecs: Int
        val isFullOrRewatch: Boolean

        if (r < liveSettings.instantSkipProb) {
            reaction = "Instant Skip (~25%)"
            targetSecs = Random.nextInt(2, 4)
            isFullOrRewatch = false
        } else if (r < liveSettings.instantSkipProb + liveSettings.quickGlanceProb) {
            reaction = "Quick Glance (~20%)"
            targetSecs = Random.nextInt(5, 9)
            isFullOrRewatch = false
        } else if (r < liveSettings.instantSkipProb + liveSettings.quickGlanceProb + liveSettings.partialWatchProb) {
            reaction = "Partial Watch (~25%)"
            targetSecs = Random.nextInt(10, 16)
            isFullOrRewatch = false
        } else if (r < liveSettings.instantSkipProb + liveSettings.quickGlanceProb + liveSettings.partialWatchProb + liveSettings.fullWatchProb) {
            reaction = "Full Watch (~20%)"
            targetSecs = Random.nextInt(18, 24)
            isFullOrRewatch = true
        } else {
            reaction = "Rewatch (~10%)"
            targetSecs = Random.nextInt(28, 42)
            isFullOrRewatch = true
        }

        simCurrentReaction = reaction
        simTargetSeconds = targetSecs

        val speedMs = Random.nextInt(200, 520)
        val curvature = Random.nextInt(-18, 18)
        simSwipeTrajectory = "Bézier trajectory: duration ${speedMs}ms, curve offset ${curvature}px, distance 76% height"

        val moodMult = liveSettings.sessionMood.likeMultiplier
        val currentGap = simVideosSinceAction + 1
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

        if (reaction.contains("Skip")) {
            val desc = "Simulated swipe away after ${targetSecs}s. (Skip forbids likes)"
            simLastAction = desc
            simVideosSinceAction = currentGap
            simLogs.add(0, TelemetryLogEntry(id = System.currentTimeMillis().toString(), time = timeStr, text = "Sim Video #$nextIndex: $reaction ($targetSecs s) -> $desc", type = LogType.SKIP))
        } else if (isFullOrRewatch && currentGap >= liveSettings.minGapVideos && Random.nextFloat() < (liveSettings.likeProbability * moodMult)) {
            val isDoubleTap = Random.nextBoolean()
            val likeDelay = String.format(Locale.US, "%.1f", Random.nextFloat() * 2f + 1.2f)
            simLikesInSession++
            simVideosSinceAction = 0
            var desc = "Simulated like (${if (isDoubleTap) "double-tap" else "heart"}) after ${likeDelay}s."
            if (Random.nextFloat() < (liveSettings.saveProbability * 3f)) {
                simSavesInSession++
                desc += " * Bookmarked."
            }
            simLastAction = desc
            simLogs.add(0, TelemetryLogEntry(id = System.currentTimeMillis().toString(), time = timeStr, text = "Sim Video #$nextIndex: $reaction -> $desc", type = LogType.LIKE))
        } else {
            simVideosSinceAction = currentGap
            val desc = "Watched ${targetSecs}s ($reaction). Gap: $currentGap videos."
            simLastAction = desc
            simLogs.add(0, TelemetryLogEntry(id = System.currentTimeMillis().toString(), time = timeStr, text = "Sim Video #$nextIndex: $desc", type = LogType.INFO))
        }

        while (simLogs.size > 30) {
            simLogs.removeAt(simLogs.lastIndex)
        }
    }

    LaunchedEffect(isSimulatorRunning, simSafetyHalted, simTargetSeconds, simVideoIndex) {
        if (!isSimulatorRunning || simSafetyHalted != null) return@LaunchedEffect
        while (isSimulatorRunning && simSafetyHalted == null) {
            delay(600L)
            if (simWatchSeconds < simTargetSeconds) {
                simWatchSeconds++
            } else {
                simWatchSeconds = 0
                triggerNextSimVideo()
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Assist Mode Engine",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF4F46E5).copy(alpha = 0.25f))
                                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "STAGE 3 ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                            }
                            Text(
                                text = "Real Session Assistant & Read-Only Safety Monitor",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "Active" else "Off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEnabled) StatusGreen else TextMuted,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                onSaveSettings(liveSettings.copy(isEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6366F1),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================================
            // 1. REAL ON-DEVICE SESSION ASSISTANT CONTROLLER
            // =========================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF10B981))
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = Color(0xFFA5B4FC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Real Device Session Assistant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Launches official app, tracks duration & monitors safety",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Running pulse indicator
                            if (isRunningThisAccount) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusGreen.copy(alpha = 0.2f))
                                        .border(1.dp, StatusGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "LIVE SESSION",
                                        color = StatusGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Platform Selector Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Platform.entries.forEach { p ->
                                val isSelected = selectedPlatform == p
                                val count = accounts.count { it.platform == p }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(p.hexColor).copy(alpha = 0.25f) else Color(0xFF0F172A)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(p.hexColor) else DarkSurfaceVariant,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            selectedPlatform = p
                                            val firstAcc = accounts.firstOrNull { it.platform == p }
                                            if (firstAcc != null) {
                                                selectedAccountId = firstAcc.id
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${p.displayName} ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }

                        // Account Selector Dropdown / Selection
                        if (platformAccounts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No ${selectedPlatform.displayName} accounts tracked yet. Add one from the Dashboard.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            var accountDropdownExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = accountDropdownExpanded,
                                onExpandedChange = { accountDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedAccount?.let { "${it.username} ${if (it.isConnected) "(Connected)" else "(Tracked)"}" } ?: "Select Account",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Active Warmup Account", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF020617),
                                        unfocusedContainerColor = Color(0xFF020617),
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = accountDropdownExpanded,
                                    onDismissRequest = { accountDropdownExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceVariant)
                                ) {
                                    platformAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(acc.username, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text(
                                                        if (acc.isConnected) "Connected" else "Tracked",
                                                        fontSize = 11.sp,
                                                        color = if (acc.isConnected) StatusGreen else TextMuted
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedAccountId = acc.id
                                                accountDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Account Details & Real Session Live Status
                        selectedAccount?.let { acc ->
                            val calc = selectedCalculation
                            val targetMins = acc.targetDailyMinutes
                            val todayMins = calc?.todayMinutes ?: 0
                            val sessionElapsedMins = if (isRunningThisAccount) elapsedSeconds / 60 else 0
                            val totalEffectiveToday = todayMins + sessionElapsedMins
                            val remainingMins = (targetMins - totalEffectiveToday).coerceAtLeast(0)
                            val isTargetMet = totalEffectiveToday >= targetMins

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Row 1: Platform & Connection Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = acc.platform.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(acc.platform.hexColor)
                                            )
                                            Text("•", color = TextMuted)
                                            Text(
                                                text = acc.username,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }

                                        // Connection Badge
                                        if (acc.isConnected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(StatusGreen.copy(alpha = 0.2f))
                                                    .border(1.dp, StatusGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(10.dp))
                                                    Text("CONNECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF334155).copy(alpha = 0.6f))
                                                    .border(1.dp, Color(0xFF64748B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        onUpdateAccount(acc.copy(isConnected = true))
                                                        Toast.makeText(context, "${acc.username} marked as Connected", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("TRACKED (Tap to Connect)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                            }
                                        }
                                    }

                                    // Row 2: Live Metrics Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Elapsed Session Timer
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isRunningThisAccount) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                val mins = if (isRunningThisAccount) elapsedSeconds / 60 else 0
                                                val secs = if (isRunningThisAccount) elapsedSeconds % 60 else 0
                                                Text(
                                                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = if (isRunningThisAccount) Color(0xFF818CF8) else TextMuted
                                                )
                                                Text("Session Elapsed", fontSize = 9.sp, color = TextSecondary)
                                            }
                                        }

                                        // Today's Target
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "${targetMins}m",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color.White
                                                )
                                                Text("Daily Target", fontSize = 9.sp, color = TextSecondary)
                                            }
                                        }

                                        // Today's Completed
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "${totalEffectiveToday}m",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (isTargetMet) StatusGreen else Color(0xFF38BDF8)
                                                )
                                                Text("Completed", fontSize = 9.sp, color = TextSecondary)
                                            }
                                        }

                                        // Remaining
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = if (isTargetMet) "DONE" else "${remainingMins}m",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (isTargetMet) StatusGreen else Color(0xFFFBBF24)
                                                )
                                                Text("Remaining", fontSize = 9.sp, color = TextSecondary)
                                            }
                                        }
                                    }

                                    // Row 3: Safety Monitor Status Pill
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (safetyHaltedState != null)
                                                    Color(0xFF881337).copy(alpha = 0.4f)
                                                else if (isRunningThisAccount)
                                                    Color(0xFF065F46).copy(alpha = 0.3f)
                                                else
                                                    Color(0xFF1E293B).copy(alpha = 0.5f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (safetyHaltedState != null) Icons.Default.Warning else Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = if (safetyHaltedState != null) Color(0xFFFDA4AF) else if (isRunningThisAccount) StatusGreen else Color(0xFFA5B4FC),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (safetyHaltedState != null)
                                                    "Safety Trigger Tripped: $safetyHaltedState"
                                                else if (isRunningThisAccount)
                                                    "Safety Monitor: Active & Guarding (Read-Only)"
                                                else
                                                    "Safety Monitor: Ready (Read-Only Guard)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (safetyHaltedState != null) Color(0xFFFECDD3) else if (isRunningThisAccount) StatusGreen else TextSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        if (safetyHaltedState != null) {
                                            Text(
                                                text = "Reset",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFDA4AF),
                                                modifier = Modifier.clickable {
                                                    AssistMonitorService.resetSafetyHalt()
                                                }
                                            )
                                        }
                                    }

                                    // Target Reached Alert Banner
                                    if (isTargetMet) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(StatusGreen.copy(alpha = 0.15f))
                                                .border(1.dp, StatusGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "★ Daily target of ${targetMins}m reached today! Warmup streak preserved.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusGreen
                                                )
                                            }
                                        }
                                    }

                                    // Row 4: Action Buttons [Open Platform] [Start Session] [Stop Session]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // [Open Platform]
                                        OutlinedButton(
                                            onClick = {
                                                PlatformLauncher.openPlatform(context, acc.platform, acc.username)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open ${acc.platform.displayName}", fontSize = 11.sp)
                                        }

                                        if (isRunningThisAccount) {
                                            // [Stop Session]
                                            Button(
                                                onClick = onStopRealSession,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Stop Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            // [Start Session]
                                            Button(
                                                onClick = {
                                                    onStartRealSession(acc)
                                                    // Open official platform app immediately
                                                    PlatformLauncher.openPlatform(context, acc.platform, acc.username)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Start Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Divider / Section Title for Simulation & Mathematical Modeling
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Behavioral Simulator & Live Forecast",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOCAL MODEL ONLY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Live 20-minute Session Forecast Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sparkles,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "LIVE 20-MIN SESSION FORECAST (FROM SLIDERS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Mood: ${liveSettings.sessionMood.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA5B4FC)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ForecastMetricBox(
                                modifier = Modifier.weight(1f),
                                value = "~${liveMetrics.estimatedVideosWatched}",
                                label = "Videos Scrolled",
                                subtext = "avg ${String.format(Locale.US, "%.1f", liveMetrics.averageVideoSeconds)}s/vid",
                                valueColor = Color.White
                            )
                            ForecastMetricBox(
                                modifier = Modifier.weight(1f),
                                value = "~${liveMetrics.estimatedLikes}",
                                label = "Expected Likes",
                                subtext = "6-10% full watch",
                                valueColor = Color(0xFFFB7185)
                            )
                            ForecastMetricBox(
                                modifier = Modifier.weight(1f),
                                value = "~${liveMetrics.estimatedSaves}",
                                label = "Expected Saves",
                                subtext = "2-4% after like",
                                valueColor = Color(0xFFFBBF24)
                            )
                            ForecastMetricBox(
                                modifier = Modifier.weight(1f),
                                value = "0%",
                                label = "Robotic Pattern",
                                subtext = "Dynamic Bézier",
                                valueColor = StatusGreen
                            )
                        }
                    }
                }
            }

            // Real-Time Behavioral Simulator (Isolated local preview)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Real-Time Behavioral Simulator (Read-Only Preview)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Simulates reaction distributions, Bézier swipe kinetics, and quiet gap rules locally.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        // Simulator controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (simSafetyHalted != null) {
                                        simSafetyHalted = null
                                        AssistMonitorService.resetSafetyHalt()
                                    }
                                    if (!isSimulatorRunning) {
                                        isSimulatorRunning = true
                                        triggerNextSimVideo()
                                    } else {
                                        isSimulatorRunning = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSimulatorRunning) StatusOrange else Color(0xFF059669)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSimulatorRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSimulatorRunning) "Pause Simulator" else "Start Feed Simulation",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    isSimulatorRunning = false
                                    val reason = "Security verification / Slide captcha identified in active window."
                                    simSafetyHalted = reason
                                    AssistMonitorService.stopForWarning(reason)
                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                                    simLogs.add(0, TelemetryLogEntry(
                                        id = System.currentTimeMillis().toString(),
                                        time = timeStr,
                                        text = "SAFETY HALT: Captcha / Action Blocked triggered! All automated gestures stopped.",
                                        type = LogType.ALERT
                                    ))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF881337)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFDA4AF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test Captcha Tripwire",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFDA4AF)
                                )
                            }
                        }

                        // Emergency Safety Alert Banner if tripped
                        simSafetyHalted?.let { reason ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF4C0519).copy(alpha = 0.8f))
                                    .border(1.dp, Color(0xFFE11D48), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFB7185),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "EMERGENCY SAFETY HALT TRIPPED!",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color(0xFFFECDD3)
                                        )
                                    }
                                    Text(
                                        text = "$reason Session terminated immediately to protect account standing. Complete verification manually on your device.",
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        lineHeight = 15.sp
                                    )
                                    Button(
                                        onClick = {
                                            simSafetyHalted = null
                                            AssistMonitorService.resetSafetyHalt()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Reset Tripwire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Mock Phone Feed Player Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Smartphone,
                                            contentDescription = null,
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Sim Video #$simVideoIndex",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "Target: ${selectedPlatform.displayName}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF4F46E5).copy(alpha = 0.25f))
                                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = simCurrentReaction,
                                            color = Color(0xFFA5B4FC),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "${simWatchSeconds}s / ${simTargetSeconds}s",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = TextPrimary
                                    )

                                    val progressFraction = if (simTargetSeconds > 0) {
                                        (simWatchSeconds.toFloat() / simTargetSeconds.toFloat()).coerceIn(0f, 1f)
                                    } else 0f

                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF6366F1),
                                        trackColor = DarkSurfaceVariant
                                    )

                                    Text(
                                        text = simLastAction,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                }

                                Text(
                                    text = "Trajectory: $simSwipeTrajectory",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        // Behavioral Telemetry Log Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Behavioral Telemetry Log",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Sim Session: $simLikesInSession likes • $simSavesInSession saves",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    simLogs.take(5).forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (log.type) {
                                                        LogType.LIKE -> Color(0xFF4C0519).copy(alpha = 0.4f)
                                                        LogType.ALERT -> Color(0xFF881337).copy(alpha = 0.7f)
                                                        LogType.SKIP -> Color(0xFF0F172A)
                                                        else -> Color(0xFF1E293B).copy(alpha = 0.4f)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "[${log.time}] ",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = log.text,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = when (log.type) {
                                                    LogType.LIKE -> Color(0xFFFECDD3)
                                                    LogType.ALERT -> Color(0xFFF87171)
                                                    LogType.SKIP -> TextSecondary
                                                    else -> TextPrimary
                                                },
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sliders: Viewing Reaction Weights
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "VIEWING REACTION WEIGHTS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        ReactionSliderRow(
                            label = "Instant Skip (1-3s)",
                            value = instantSkip,
                            onValueChange = {
                                instantSkip = it
                                onSaveSettings(liveSettings.copy(instantSkipProb = it))
                            }
                        )

                        ReactionSliderRow(
                            label = "Quick Glance (20-40%)",
                            value = quickGlance,
                            onValueChange = {
                                quickGlance = it
                                onSaveSettings(liveSettings.copy(quickGlanceProb = it))
                            }
                        )

                        ReactionSliderRow(
                            label = "Partial Watch (50-80%)",
                            value = partialWatch,
                            onValueChange = {
                                partialWatch = it
                                onSaveSettings(liveSettings.copy(partialWatchProb = it))
                            }
                        )

                        ReactionSliderRow(
                            label = "Full Watch (end + loop)",
                            value = fullWatch,
                            onValueChange = {
                                fullWatch = it
                                onSaveSettings(liveSettings.copy(fullWatchProb = it))
                            }
                        )

                        ReactionSliderRow(
                            label = "Rewatch (loops 2-3x)",
                            value = rewatch,
                            onValueChange = {
                                rewatch = it
                                onSaveSettings(liveSettings.copy(rewatchProb = it))
                            }
                        )
                    }
                }
            }

            // Sliders: Engagement & Safety Caps
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ENGAGEMENT & SAFETY CAPS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Mood selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Session Mood Scaling",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SessionMood.entries.forEach { m ->
                                    val isSelected = mood == m
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF818CF8) else DarkSurfaceVariant,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                mood = m
                                                onSaveSettings(liveSettings.copy(sessionMood = m))
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = m.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        ReactionSliderRow(
                            label = "Like Probability (6-10%)",
                            value = likeProb,
                            accentColor = Color(0xFFFB7185),
                            onValueChange = {
                                likeProb = it
                                onSaveSettings(liveSettings.copy(likeProbability = it))
                            }
                        )

                        ReactionSliderRow(
                            label = "Save Probability (2-4%)",
                            value = saveProb,
                            accentColor = Color(0xFFFBBF24),
                            onValueChange = {
                                saveProb = it
                                onSaveSettings(liveSettings.copy(saveProbability = it))
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Like Cap",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = dailyLikeCap.toString(),
                                    onValueChange = {
                                        val num = it.toIntOrNull() ?: 20
                                        dailyLikeCap = num
                                        onSaveSettings(liveSettings.copy(dailyLikeCap = num))
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF020617),
                                        unfocusedContainerColor = Color(0xFF020617),
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Save Cap",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = dailySaveCap.toString(),
                                    onValueChange = {
                                        val num = it.toIntOrNull() ?: 8
                                        dailySaveCap = num
                                        onSaveSettings(liveSettings.copy(dailySaveCap = num))
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF020617),
                                        unfocusedContainerColor = Color(0xFF020617),
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Niche Keywords
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Niche Keywords (comma-separated)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            OutlinedTextField(
                                value = nicheKeywordsText,
                                onValueChange = {
                                    nicheKeywordsText = it
                                    val kwList = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                                    onSaveSettings(liveSettings.copy(nicheKeywords = kwList))
                                },
                                placeholder = { Text("e.g. AI tools, productivity, streetwear", fontSize = 12.sp, color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF020617),
                                    unfocusedContainerColor = Color(0xFF020617),
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = DarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Android System Accessibility Settings Link
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (WarmUpAccessibilityService.isServiceConnected)
                                        StatusGreen.copy(alpha = 0.2f)
                                    else
                                        StatusOrange.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = null,
                                tint = if (WarmUpAccessibilityService.isServiceConnected) StatusGreen else StatusOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (WarmUpAccessibilityService.isServiceConnected)
                                    "Accessibility Safety Guard Active"
                                else
                                    "Accessibility Guard Permission Needed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (WarmUpAccessibilityService.isServiceConnected)
                                    "Read-only screen observer is watching for captchas and warnings."
                                else
                                    "Enable WarmUp Manager in Android Settings -> Accessibility for automatic safety stop.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (WarmUpAccessibilityService.isServiceConnected)
                                    Color(0xFF1E293B)
                                else
                                    Color(0xFF4F46E5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (WarmUpAccessibilityService.isServiceConnected) "Manage" else "Enable",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ForecastMetricBox(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    subtext: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1
            )
            Text(
                text = subtext,
                fontSize = 8.sp,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReactionSliderRow(
    label: String,
    value: Float,
    accentColor: Color = Color(0xFF818CF8),
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.01f..0.50f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

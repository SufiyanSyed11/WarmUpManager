package com.warmup.manager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmup.manager.data.model.AccountWarmUpCalculation
import com.warmup.manager.data.model.DailyProgress
import com.warmup.manager.data.model.WarmUpSessionEntity
import com.warmup.manager.data.model.WarmUpStatus
import com.warmup.manager.ui.components.MissedStreakBanner
import com.warmup.manager.ui.components.StatusBadge
import com.warmup.manager.ui.theme.DarkBackground
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.DarkSurfaceVariant
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusGrey
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.TextMuted
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.util.PlatformLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    calc: AccountWarmUpCalculation,
    sessions: List<WarmUpSessionEntity>,
    onBack: () -> Unit,
    onStartTimer: () -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit = {},
    onAddSession: (WarmUpSessionEntity) -> Unit,
    onDeleteSession: (WarmUpSessionEntity) -> Unit,
    onContinueStreak: () -> Unit,
    onResetStreak: () -> Unit
) {
    val context = LocalContext.current
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = calc.account.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onStartTimer) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Start Live Timer",
                            tint = Color(calc.account.platform.hexColor)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartTimer,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(calc.account.platform.hexColor)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Timer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                FloatingActionButton(
                    onClick = { showAddSessionDialog = true },
                    containerColor = DarkSurfaceVariant,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Log Session")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Missed Streak Banner if detected
            if (calc.hasMissedDay) {
                item {
                    MissedStreakBanner(
                        onContinueStreak = onContinueStreak,
                        onResetStreak = onResetStreak
                    )
                }
            }

            // Overview Card
            item {
                AccountHeaderCard(
                    calc = calc,
                    onConnect = { showConnectDialog = true },
                    onDisconnect = {
                        onUpdateAccount(calc.account.copy(isConnected = false))
                        Toast.makeText(context, "${calc.account.username} set to Tracked", Toast.LENGTH_SHORT).show()
                    },
                    onOpenPlatform = {
                        PlatformLauncher.openPlatform(context, calc.account.platform, calc.account.username)
                    }
                )
            }

            // Stats Quick Row
            item {
                StatsSummaryRow(calc = calc)
            }

            // Daily History Section
            item {
                Text(
                    text = "Daily Progress History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (calc.dailyProgressList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sessions logged yet. Tap + to record a warmup session.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(calc.dailyProgressList, key = { it.dateString }) { daily ->
                    DailyProgressCard(
                        daily = daily,
                        targetMinutes = calc.account.targetDailyMinutes
                    )
                }
            }

            // Individual Sessions History
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Individual Sessions (${sessions.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(sessions, key = { it.id }) { session ->
                SessionItemCard(
                    session = session,
                    onDelete = { onDeleteSession(session) }
                )
            }
        }
    }

    if (showAddSessionDialog) {
        LogSessionDialog(
            accountId = calc.account.id,
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { session ->
                onAddSession(session)
                showAddSessionDialog = false
            }
        )
    }

    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(calc.account.platform.hexColor))
                    Text("Connect ${calc.account.username}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Security Guarantee:",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "WarmUp Manager never requests, intercepts, or stores your social media password or credentials.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "To connect, verify that ${calc.account.username} is actively logged into the official ${calc.account.platform.displayName} app on this device.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    OutlinedButton(
                        onClick = {
                            PlatformLauncher.openPlatform(context, calc.account.platform, calc.account.username)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open ${calc.account.platform.displayName} to Verify", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAccount(calc.account.copy(isConnected = true))
                        Toast.makeText(context, "${calc.account.username} marked as Connected", Toast.LENGTH_SHORT).show()
                        showConnectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text("Confirm Connected")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun AccountHeaderCard(
    calc: AccountWarmUpCalculation,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenPlatform: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = calc.account.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${calc.account.platform.displayName} • Target: ${calc.account.targetDailyMinutes}m/day (${calc.account.targetDays} days)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (calc.account.nicheTag.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = calc.account.nicheTag,
                        color = Color(0xFF818CF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (calc.account.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = calc.account.notes,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Connection status and action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (calc.account.isConnected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusGreen.copy(alpha = 0.2f))
                            .border(1.dp, StatusGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
                            Text("CONNECTED ACCOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF334155).copy(alpha = 0.6f))
                            .border(1.dp, Color(0xFF64748B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("TRACKED ACCOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenPlatform,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open ${calc.account.platform.displayName}", fontSize = 11.sp)
                    }

                    if (!calc.account.isConnected) {
                        Button(
                            onClick = onConnect,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Disconnect", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            StatusBadge(status = calc.status, showProgressBar = true)
        }
    }
}

@Composable
private fun StatsSummaryRow(calc: AccountWarmUpCalculation) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Minutes
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Schedule,
            label = "Total Time",
            value = "${calc.totalMinutesAllTime}m",
            accentColor = Color(0xFF38BDF8)
        )
        // Likes
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Favorite,
            label = "Likes",
            value = "${calc.totalLikesAllTime}",
            accentColor = Color(0xFFF43F5E)
        )
        // Saves
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bookmark,
            label = "Saves",
            value = "${calc.totalSavesAllTime}",
            accentColor = Color(0xFFF59E0B)
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun DailyProgressCard(
    daily: DailyProgress,
    targetMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = daily.dateString,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (daily.isTargetMet) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x2610B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Target Met",
                                color = StatusGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x26F97316))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Partial",
                                color = StatusOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${daily.totalSessions} sessions • ${daily.totalLikes} likes • ${daily.totalSaves} saves",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${daily.totalMinutes}/$targetMinutes m",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (daily.isTargetMet) StatusGreen else StatusOrange
                )
                Text(
                    text = if (daily.isTargetMet) "Qualifies" else "Needs ${targetMinutes - daily.totalMinutes}m",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SessionItemCard(
    session: WarmUpSessionEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${session.durationMinutes} minutes session",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${session.dateString} • ${session.likesCount} likes, ${session.savesCount} saves",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (!session.warningReason.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Safety Halt: ${session.warningReason}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFCA5A5),
                            maxLines = 1
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete session",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LogSessionDialog(
    accountId: Long,
    onDismiss: () -> Unit,
    onConfirm: (WarmUpSessionEntity) -> Unit
) {
    var durationMinutes by remember { mutableIntStateOf(15) }
    var likesCount by remember { mutableIntStateOf(2) }
    var savesCount by remember { mutableIntStateOf(1) }
    var dateString by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Warmup Session", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = DarkSurfaceVariant
                    )
                )

                Text("Session Duration: $durationMinutes min", color = TextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10, 15, 20, 30).forEach { mins ->
                        Button(
                            onClick = { durationMinutes = mins },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (durationMinutes == mins) Color(0xFF6366F1) else DarkSurfaceVariant
                            )
                        ) {
                            Text("${mins}m", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = likesCount.toString(),
                        onValueChange = { likesCount = it.toIntOrNull() ?: 0 },
                        label = { Text("Likes") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = DarkSurfaceVariant
                        )
                    )
                    OutlinedTextField(
                        value = savesCount.toString(),
                        onValueChange = { savesCount = it.toIntOrNull() ?: 0 },
                        label = { Text("Saves") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    onConfirm(
                        WarmUpSessionEntity(
                            accountId = accountId,
                            startTime = now - (durationMinutes * 60 * 1000),
                            endTime = now,
                            durationMinutes = durationMinutes,
                            likesCount = likesCount,
                            savesCount = savesCount,
                            dateString = dateString
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

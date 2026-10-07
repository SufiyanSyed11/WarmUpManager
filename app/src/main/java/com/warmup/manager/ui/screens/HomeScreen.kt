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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmup.manager.data.model.AccountWarmUpCalculation
import com.warmup.manager.data.model.Platform
import com.warmup.manager.data.model.WarmUpStatus
import com.warmup.manager.ui.components.PlatformCard
import com.warmup.manager.ui.theme.DarkBackground
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.DarkSurfaceVariant
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusGrey
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.TextMuted
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    calculations: List<AccountWarmUpCalculation>,
    onSelectPlatform: (Platform) -> Unit,
    onStartTimer: (AccountWarmUpCalculation) -> Unit,
    onOpenAssistEngine: () -> Unit,
    onOpenSettings: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onTestNotification: () -> Unit
) {
    val totalAccounts = calculations.size
    val warmedAccounts = calculations.count { it.status is WarmUpStatus.WarmedUp }
    val inProgressAccounts = calculations.count { it.status is WarmUpStatus.WarmingUp }
    val notStartedAccounts = calculations.count { it.status is WarmUpStatus.NotStarted }

    // Accounts needing minutes today
    val queueAccounts = calculations.filter {
        it.status !is WarmUpStatus.WarmedUp && !it.todayTargetMet
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WarmUp Manager",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Stage 3: Full Automation & Assist Engine",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAssistEngine) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Assist Engine",
                            tint = Color(0xFFA5B4FC)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Assist Settings",
                            tint = Color(0xFF818CF8)
                        )
                    }
                    IconButton(onClick = onTestNotification) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Test Reminder Notification",
                            tint = Color(0xFF60A5FA)
                        )
                    }
                    IconButton(onClick = onImportCsv) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import CSV",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onExportCsv) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = TextSecondary
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
            // Dashboard Header Summary Card
            item {
                DashboardOverviewCard(
                    total = totalAccounts,
                    warmed = warmedAccounts,
                    inProgress = inProgressAccounts,
                    notStarted = notStartedAccounts
                )
            }

            // Stage 3 Assist Engine Navigation Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAssistEngine),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Assist Mode Engine",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF4F46E5).copy(alpha = 0.35f))
                                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "STAGE 3",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                            }
                            Text(
                                text = "Live behavior simulator, Bézier swipes, 20-min forecasts & captcha tripwires.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Assist Engine",
                            tint = Color(0xFF818CF8)
                        )
                    }
                }
            }

            // Today's Action Queue (Accounts Needing Warmup Today)
            if (queueAccounts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Warmup Queue (${queueAccounts.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Target: 30m each",
                                fontSize = 11.sp,
                                color = StatusOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            queueAccounts.take(3).forEach { calc ->
                                val remaining = calc.account.targetDailyMinutes - calc.todayMinutes
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(calc.account.platform.hexColor))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = calc.account.username,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                            Text(
                                                text = "${calc.todayMinutes}/${calc.account.targetDailyMinutes}m today • ${remaining}m remaining",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        androidx.compose.material3.Button(
                                            onClick = { onStartTimer(calc) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = Color(calc.account.platform.hexColor)
                                            )
                                        ) {
                                            Text("Start Timer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Platforms",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap to manage accounts",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            // Platform Cards: TikTok, Instagram, YouTube
            Platform.entries.forEach { platform ->
                item {
                    val platformCalcs = calculations.filter { it.account.platform == platform }
                    PlatformCard(
                        platform = platform,
                        totalAccounts = platformCalcs.size,
                        warmedCount = platformCalcs.count { it.status is WarmUpStatus.WarmedUp },
                        inProgressCount = platformCalcs.count { it.status is WarmUpStatus.WarmingUp },
                        notStartedCount = platformCalcs.count { it.status is WarmUpStatus.NotStarted },
                        onClick = { onSelectPlatform(platform) }
                    )
                }
            }

            // Safe Warmup Advisory Footer Note
            item {
                AdvisoryCard()
            }
        }
    }
}

@Composable
private fun DashboardOverviewCard(
    total: Int,
    warmed: Int,
    inProgress: Int,
    notStarted: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Warmup Progress Overview",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular completion indicator
                val ratio = if (total > 0) warmed.toFloat() / total.toFloat() else 0f
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(68.dp),
                        color = DarkSurfaceVariant,
                        strokeWidth = 6.dp
                    )
                    CircularProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.size(68.dp),
                        color = StatusGreen,
                        strokeWidth = 6.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(ratio * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Counts breakdown
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricBox(title = "Warmed", count = warmed, color = StatusGreen)
                    MetricBox(title = "In Progress", count = inProgress, color = StatusOrange)
                    MetricBox(title = "Not Started", count = notStarted, color = StatusGrey)
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AdvisoryCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Recommended Warmup Protocol",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A default 5-day cycle at 30 min/day simulates genuine organic user interest. Split sessions (e.g. 2 x 15 min) match authentic mobile usage habits without triggering platform rate alerts.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

package com.warmup.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmup.manager.data.model.Platform
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.DarkSurfaceVariant
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusGrey
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.TextMuted
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary

@Composable
fun PlatformCard(
    platform: Platform,
    totalAccounts: Int,
    warmedCount: Int,
    inProgressCount: Int,
    notStartedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandGradient = when (platform) {
        Platform.TIKTOK -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFFE2C55), Color(0xFF25F4EE).copy(alpha = 0.8f))
        )
        Platform.INSTAGRAM -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
        )
        Platform.YOUTUBE -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF0000), Color(0xFFB30000))
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Brand badge & arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (platform) {
                                Platform.TIKTOK -> Icons.Default.VideoLibrary
                                Platform.INSTAGRAM -> Icons.Default.PlayCircle
                                Platform.YOUTUBE -> Icons.Default.PlayCircle
                            },
                            contentDescription = platform.displayName,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = platform.displayName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "$totalAccounts ${if (totalAccounts == 1) "account" else "accounts"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open platform",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Warmed up
                StatColumn(label = "Warmed", count = warmedCount, color = StatusGreen)
                // In progress
                StatColumn(label = "Warming Up", count = inProgressCount, color = StatusOrange)
                // Not started
                StatColumn(label = "Not Started", count = notStartedCount, color = StatusGrey)
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

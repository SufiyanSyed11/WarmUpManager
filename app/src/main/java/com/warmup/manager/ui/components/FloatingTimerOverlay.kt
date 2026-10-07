package com.warmup.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary

@Composable
fun FloatingTimerOverlay(
    account: AccountEntity,
    elapsedSeconds: Int,
    likesCount: Int,
    savesCount: Int,
    onAddLike: () -> Unit,
    onAddSave: () -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMinimized by remember { mutableStateOf(false) }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val progress = (minutes.toFloat() / account.targetDailyMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    if (isMinimized) {
        // Minimized floating pill
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                .border(1.5.dp, Color(account.platform.hexColor), RoundedCornerShape(24.dp))
                .clickable { isMinimized = false }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(StatusOrange)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${account.username} • $formattedTime",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    } else {
        // Expanded floating controller card
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(account.platform.hexColor).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with account name and minimize icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(account.platform.hexColor))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = account.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${account.platform.displayName})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = "Minimize",
                        fontSize = 11.sp,
                        color = Color(0xFF818CF8),
                        modifier = Modifier.clickable { isMinimized = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Timer Counter & Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = formattedTime,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Target: ${account.targetDailyMinutes} mins (${(progress * 100).toInt()}% completed)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(52.dp),
                            color = Color(0xFF334155),
                            strokeWidth = 4.dp
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(52.dp),
                            color = if (progress >= 1f) StatusGreen else StatusOrange,
                            strokeWidth = 4.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Action buttons (+1 Like, +1 Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddLike,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261928))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+1 Like ($likesCount)",
                            fontSize = 12.sp,
                            color = Color(0xFFF43F5E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onAddSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2215))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Save",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+1 Save ($savesCount)",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stop & Save Button
                Button(
                    onClick = onStopSession,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stop & Save Warmup Session",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

package com.warmup.manager.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmup.manager.data.model.WarmUpStatus
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusGreenBg
import com.warmup.manager.ui.theme.StatusGrey
import com.warmup.manager.ui.theme.StatusGreyBg
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.StatusOrangeBg

@Composable
fun StatusBadge(
    status: WarmUpStatus,
    modifier: Modifier = Modifier,
    showProgressBar: Boolean = true
) {
    val (bgColor, textColor, dotColor) = when (status) {
        is WarmUpStatus.NotStarted -> Triple(StatusGreyBg, StatusGrey, StatusGrey)
        is WarmUpStatus.WarmingUp -> Triple(StatusOrangeBg, StatusOrange, StatusOrange)
        is WarmUpStatus.WarmedUp -> Triple(StatusGreenBg, StatusGreen, StatusGreen)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status.getBadgeText(),
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showProgressBar && status is WarmUpStatus.WarmingUp) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = StatusOrange,
                trackColor = Color(0xFF334155),
            )
        }
    }
}

package com.warmup.manager.ui.screens.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmup.manager.data.model.AssistSettings
import com.warmup.manager.data.model.SessionMood
import com.warmup.manager.ui.theme.DarkBackground
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.DarkSurfaceVariant
import com.warmup.manager.ui.theme.StatusGreen
import com.warmup.manager.ui.theme.StatusOrange
import com.warmup.manager.ui.theme.TextMuted
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistSettingsScreen(
    currentSettings: AssistSettings,
    onSaveSettings: (AssistSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

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

    // Assemble current live settings object
    val liveSettings = remember(
        isEnabled, mood, instantSkip, quickGlance, partialWatch, fullWatch, rewatch,
        likeProb, saveProb, dailyLikeCap, dailySaveCap
    ) {
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
            dailySaveCap = dailySaveCap
        )
    }

    val liveMetrics = remember(liveSettings) {
        liveSettings.calculateExpected20MinMetrics()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assist Mode & Probabilities",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onSaveSettings(liveSettings)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveSettings(liveSettings)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save", fontSize = 12.sp)
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
            // Master Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assist Mode (Human Behavior Engine)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Automates organic randomized scroll timing and reactions using Android AccessibilityService",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6366F1)
                            )
                        )
                    }
                }
            }

            // Android Accessibility Service Permission Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2436))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Android Accessibility Permission",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF818CF8)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To allow the engine to dispatch human swipes, enable 'WarmUp Manager' in your device's Accessibility settings.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3730A3)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Accessibility Settings", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Live Preview Card (Expected per 20-min session)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16252C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sparkles,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live 20-Minute Session Forecast",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ForecastColumn(
                                label = "Videos Watched",
                                value = "~${liveMetrics.estimatedVideosWatched}",
                                subtext = "avg ${(liveMetrics.averageVideoSeconds).toInt()}s/video"
                            )
                            ForecastColumn(
                                label = "Expected Likes",
                                value = "~${liveMetrics.estimatedLikes}",
                                subtext = "capped at $dailyLikeCap"
                            )
                            ForecastColumn(
                                label = "Expected Saves",
                                value = "~${liveMetrics.estimatedSaves}",
                                subtext = "capped at $dailySaveCap"
                            )
                        }
                    }
                }
            }

            // Session Mood Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Session Mood Scaling",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Randomized per session to prevent predictable algorithmic footprints",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SessionMood.entries.forEach { m ->
                                val isSelected = mood == m
                                Button(
                                    onClick = { mood = m },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF6366F1) else DarkSurfaceVariant,
                                        contentColor = if (isSelected) Color.White else TextSecondary
                                    )
                                ) {
                                    Text(m.displayName.replace(" Engagement", ""), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Video Reaction Distribution Sliders
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Watch Reaction Weights",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        ProbabilitySlider(
                            label = "Instant Skip (1-3s)",
                            value = instantSkip,
                            onValueChange = { instantSkip = it }
                        )

                        ProbabilitySlider(
                            label = "Quick Glance (20-40%)",
                            value = quickGlance,
                            onValueChange = { quickGlance = it }
                        )

                        ProbabilitySlider(
                            label = "Partial Watch (50-80%)",
                            value = partialWatch,
                            onValueChange = { partialWatch = it }
                        )

                        ProbabilitySlider(
                            label = "Full Watch (100% + loop)",
                            value = fullWatch,
                            onValueChange = { fullWatch = it }
                        )

                        ProbabilitySlider(
                            label = "Rewatch (2-3 loops)",
                            value = rewatch,
                            onValueChange = { rewatch = it }
                        )
                    }
                }
            }

            // Engagement Probabilities & Caps
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Engagement Probabilities & Safety Caps",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        ProbabilitySlider(
                            label = "Like Probability (on full/rewatch only)",
                            value = likeProb,
                            onValueChange = { likeProb = it }
                        )

                        ProbabilitySlider(
                            label = "Save Probability (after like only)",
                            value = saveProb,
                            onValueChange = { saveProb = it }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = dailyLikeCap.toString(),
                                onValueChange = { dailyLikeCap = it.toIntOrNull() ?: 20 },
                                label = { Text("Daily Like Cap") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = DarkSurfaceVariant
                                )
                            )

                            OutlinedTextField(
                                value = dailySaveCap.toString(),
                                onValueChange = { dailySaveCap = it.toIntOrNull() ?: 8 },
                                label = { Text("Daily Save Cap") },
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
                }
            }
        }
    }
}

@Composable
private fun ForecastColumn(label: String, value: String, subtext: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = subtext, fontSize = 9.sp, color = TextMuted)
    }
}

@Composable
private fun ProbabilitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF818CF8)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..0.50f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6366F1),
                activeTrackColor = Color(0xFF6366F1),
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

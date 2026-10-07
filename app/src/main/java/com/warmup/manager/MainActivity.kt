package com.warmup.manager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.warmup.manager.data.model.Platform
import com.warmup.manager.ui.screens.AccountDetailScreen
import com.warmup.manager.ui.screens.AccountListScreen
import com.warmup.manager.ui.screens.HomeScreen
import com.warmup.manager.ui.screens.assist.AssistEngineScreen
import com.warmup.manager.ui.theme.DarkBackground
import com.warmup.manager.ui.theme.WarmUpManagerTheme
import com.warmup.manager.util.CsvHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WarmUpApp
        val repository = app.repository

        setContent {
            WarmUpManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    WarmUpNavHost(
                        activity = this,
                        app = app
                    )
                }
            }
        }
    }
}

@Composable
fun WarmUpNavHost(
    activity: ComponentActivity,
    app: WarmUpApp
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val repository = app.repository

    val allCalculations by repository.getAllCalculations().collectAsState(initial = emptyList())
    val allAccounts by repository.getAllAccounts().collectAsState(initial = emptyList())

    // Active live timer state
    var activeTimerAccount by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.warmup.manager.data.model.AccountEntity?>(null) }
    var elapsedSeconds by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var timerLikes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var timerSaves by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var timerStartMillis by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    fun dispatchTargetReachedNotification(username: String, targetMins: Int) {
        val nm = activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "warmup_target_channel",
                "WarmUp Targets",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
        val notif = androidx.core.app.NotificationCompat.Builder(activity, "warmup_target_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("★ WarmUp Daily Target Reached!")
            .setContentText("$username reached today's target of ${targetMins}m! Streak preserved.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(3002, notif)
    }

    fun stopAndSaveActiveSession(warningReason: String? = null) {
        val acc = activeTimerAccount
        if (acc != null) {
            val durationMins = (elapsedSeconds / 60).coerceAtLeast(if (elapsedSeconds >= 20) 1 else 0)
            if (durationMins > 0) {
                val now = System.currentTimeMillis()
                val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                scope.launch(Dispatchers.IO) {
                    repository.insertSession(
                        com.warmup.manager.data.model.WarmUpSessionEntity(
                            accountId = acc.id,
                            startTime = timerStartMillis,
                            endTime = now,
                            durationMinutes = durationMins,
                            likesCount = timerLikes,
                            savesCount = timerSaves,
                            dateString = dateStr,
                            warningReason = warningReason
                        )
                    )
                }
                if (warningReason != null) {
                    Toast.makeText(activity, "Session halted: $warningReason", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Saved ${durationMins}m warmup session for ${acc.username}!", Toast.LENGTH_LONG).show()
                }
            }
            com.warmup.manager.service.FloatingTimerService.stop(activity)
            com.warmup.manager.service.WarmUpAccessibilityService.stopSession(warningReason ?: "Manual stop")
            activeTimerAccount = null
        }
    }

    // Ticker when timer is active
    androidx.compose.runtime.LaunchedEffect(activeTimerAccount) {
        val acc = activeTimerAccount
        if (acc != null) {
            elapsedSeconds = 0
            timerLikes = 0
            timerSaves = 0
            timerStartMillis = System.currentTimeMillis()
            var hasNotifiedTargetReached = false

            while (activeTimerAccount != null) {
                kotlinx.coroutines.delay(1000L)
                elapsedSeconds++

                if (!hasNotifiedTargetReached && (elapsedSeconds / 60) >= acc.targetDailyMinutes) {
                    hasNotifiedTargetReached = true
                    dispatchTargetReachedNotification(acc.username, acc.targetDailyMinutes)
                    Toast.makeText(activity, "★ Daily target of ${acc.targetDailyMinutes}m reached for ${acc.username}!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Auto-stop active session when safety monitor trips
    val safetyHaltedState by com.warmup.manager.service.AssistMonitorService.safetyHaltedReason.collectAsState()
    androidx.compose.runtime.LaunchedEffect(safetyHaltedState) {
        safetyHaltedState?.let { reason ->
            if (activeTimerAccount != null) {
                stopAndSaveActiveSession(warningReason = reason)
            }
        }
    }

    var assistSettings by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.warmup.manager.data.model.AssistSettings()) }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            // HOME SCREEN
            composable("home") {
                HomeScreen(
                    calculations = allCalculations,
                    onSelectPlatform = { platform ->
                        navController.navigate("platform/${platform.name}")
                    },
                    onStartTimer = { calc ->
                        activeTimerAccount = calc.account
                        Toast.makeText(activity, "Started warmup timer for ${calc.account.username}", Toast.LENGTH_SHORT).show()
                    },
                    onOpenAssistEngine = {
                        navController.navigate("assist")
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onExportCsv = {
                        scope.launch {
                            val csv = withContext(Dispatchers.IO) {
                                CsvHelper.exportAccountsToCsv(allAccounts)
                            }
                            Toast.makeText(activity, "Exported ${allAccounts.size} accounts to CSV", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onImportCsv = {
                        Toast.makeText(activity, "CSV import dialog", Toast.LENGTH_SHORT).show()
                    },
                    onTestNotification = {
                        scope.launch(Dispatchers.IO) {
                            val request = androidx.work.OneTimeWorkRequestBuilder<com.warmup.manager.worker.DailyReminderWorker>().build()
                            androidx.work.WorkManager.getInstance(activity).enqueue(request)
                        }
                        Toast.makeText(activity, "Triggered warmup reminder notification check", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // STAGE 3 ASSIST ENGINE SCREEN (REAL SESSION CONTROLLER + SIMULATOR & TELEMETRY)
            composable("assist") {
                AssistEngineScreen(
                    accounts = allAccounts,
                    calculations = allCalculations,
                    activeTimerAccount = activeTimerAccount,
                    elapsedSeconds = elapsedSeconds,
                    onStartRealSession = { acc ->
                        activeTimerAccount = acc
                        timerStartMillis = System.currentTimeMillis()
                        elapsedSeconds = 0
                        com.warmup.manager.service.FloatingTimerService.start(
                            context = activity,
                            accountId = acc.id,
                            username = acc.username,
                            targetMins = acc.targetDailyMinutes,
                            platform = acc.platform.name
                        )
                        com.warmup.manager.service.WarmUpAccessibilityService.startSession(
                            accountId = acc.id,
                            username = acc.username,
                            platform = acc.platform,
                            targetMinutes = acc.targetDailyMinutes
                        )
                        Toast.makeText(activity, "Started live session for ${acc.username}", Toast.LENGTH_SHORT).show()
                    },
                    onStopRealSession = {
                        stopAndSaveActiveSession()
                    },
                    onUpdateAccount = { acc ->
                        scope.launch(Dispatchers.IO) {
                            repository.updateAccount(acc)
                        }
                    },
                    currentSettings = assistSettings,
                    onSaveSettings = { updated ->
                        assistSettings = updated
                        Toast.makeText(activity, "Updated Assist Engine configuration", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // SETTINGS SCREEN (STAGE 3 ASSIST MODE & PROBABILITIES)
            composable("settings") {
                com.warmup.manager.ui.screens.settings.AssistSettingsScreen(
                    currentSettings = assistSettings,
                    onSaveSettings = { updated ->
                        assistSettings = updated
                        Toast.makeText(activity, "Saved Assist Mode settings", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // PLATFORM ACCOUNT LIST
            composable(
                route = "platform/{platformName}",
                arguments = listOf(navArgument("platformName") { type = NavType.StringType })
            ) { backStackEntry ->
                val platformName = backStackEntry.arguments?.getString("platformName") ?: Platform.TIKTOK.name
                val platform = Platform.fromString(platformName)
                val platformCalculations = allCalculations.filter { it.account.platform == platform }

                AccountListScreen(
                    platform = platform,
                    calculations = platformCalculations,
                    onBack = { navController.popBackStack() },
                    onSelectAccount = { accountId ->
                        navController.navigate("account/$accountId")
                    },
                    onStartSession = { acc ->
                        activeTimerAccount = acc
                        timerStartMillis = System.currentTimeMillis()
                        elapsedSeconds = 0
                        com.warmup.manager.util.PlatformLauncher.openPlatform(activity, acc.platform, acc.username)
                        com.warmup.manager.service.FloatingTimerService.start(
                            context = activity,
                            accountId = acc.id,
                            username = acc.username,
                            targetMins = acc.targetDailyMinutes,
                            platform = acc.platform.name
                        )
                        com.warmup.manager.service.WarmUpAccessibilityService.startSession(
                            accountId = acc.id,
                            username = acc.username,
                            platform = acc.platform,
                            targetMinutes = acc.targetDailyMinutes
                        )
                        Toast.makeText(activity, "Started warmup session for ${acc.username}", Toast.LENGTH_SHORT).show()
                    },
                    onAddAccount = { account ->
                        scope.launch(Dispatchers.IO) {
                            repository.insertAccount(account)
                        }
                    },
                    onUpdateAccount = { account ->
                        scope.launch(Dispatchers.IO) {
                            repository.updateAccount(account)
                        }
                    },
                    onDeleteAccount = { account ->
                        scope.launch(Dispatchers.IO) {
                            repository.deleteAccount(account)
                        }
                    }
                )
            }

            // ACCOUNT DETAIL & HISTORY
            composable(
                route = "account/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
                val calc = allCalculations.find { it.account.id == accountId }
                val sessionsFlow = repository.getSessionsForAccount(accountId)
                val sessions by sessionsFlow.collectAsState(initial = emptyList())

                if (calc != null) {
                    AccountDetailScreen(
                        calc = calc,
                        sessions = sessions,
                        onBack = { navController.popBackStack() },
                        onStartTimer = {
                            activeTimerAccount = calc.account
                            com.warmup.manager.util.PlatformLauncher.openPlatform(activity, calc.account.platform, calc.account.username)
                            Toast.makeText(activity, "Timer running for ${calc.account.username}", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateAccount = { updatedAcc ->
                            scope.launch(Dispatchers.IO) {
                                repository.updateAccount(updatedAcc)
                            }
                        },
                        onAddSession = { session ->
                            scope.launch(Dispatchers.IO) {
                                repository.insertSession(session)
                            }
                        },
                        onDeleteSession = { session ->
                            scope.launch(Dispatchers.IO) {
                                repository.deleteSession(session)
                            }
                        },
                        onContinueStreak = {
                            scope.launch(Dispatchers.IO) {
                                val updated = calc.account.copy(streakResetCount = 0)
                                repository.updateAccount(updated)
                            }
                        },
                        onResetStreak = {
                            scope.launch(Dispatchers.IO) {
                                val updated = calc.account.copy(streakResetCount = calc.account.streakResetCount + 1)
                                repository.updateAccount(updated)
                            }
                        }
                    )
                }
            }
        }

        // Floating Timer Overlay when session is running
        activeTimerAccount?.let { acc ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                com.warmup.manager.ui.components.FloatingTimerOverlay(
                    account = acc,
                    elapsedSeconds = elapsedSeconds,
                    likesCount = timerLikes,
                    savesCount = timerSaves,
                    onAddLike = { timerLikes++ },
                    onAddSave = { timerSaves++ },
                    onStopSession = {
                        val durationMins = (elapsedSeconds / 60).coerceAtLeast(if (elapsedSeconds >= 20) 1 else 0)
                        if (durationMins > 0) {
                            val now = System.currentTimeMillis()
                            val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            scope.launch(Dispatchers.IO) {
                                repository.insertSession(
                                    com.warmup.manager.data.model.WarmUpSessionEntity(
                                        accountId = acc.id,
                                        startTime = timerStartMillis,
                                        endTime = now,
                                        durationMinutes = durationMins,
                                        likesCount = timerLikes,
                                        savesCount = timerSaves,
                                        dateString = dateStr
                                    )
                                )
                            }
                            Toast.makeText(activity, "Saved ${durationMins}m warmup session!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(activity, "Session cancelled (too short)", Toast.LENGTH_SHORT).show()
                        }
                        activeTimerAccount = null
                    }
                )
            }
        }
    }
}

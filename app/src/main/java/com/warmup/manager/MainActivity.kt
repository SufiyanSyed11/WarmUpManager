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

    // Ticker when timer is active
    androidx.compose.runtime.LaunchedEffect(activeTimerAccount) {
        if (activeTimerAccount != null) {
            elapsedSeconds = 0
            timerLikes = 0
            timerSaves = 0
            timerStartMillis = System.currentTimeMillis()
            while (activeTimerAccount != null) {
                kotlinx.coroutines.delay(1000L)
                elapsedSeconds++
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

            // STAGE 3 ASSIST ENGINE SCREEN (LIVE SIMULATOR & TELEMETRY)
            composable("assist") {
                AssistEngineScreen(
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
                            Toast.makeText(activity, "Timer running for ${calc.account.username}", Toast.LENGTH_SHORT).show()
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

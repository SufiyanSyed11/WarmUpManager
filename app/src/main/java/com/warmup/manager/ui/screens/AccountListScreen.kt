package com.warmup.manager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.data.model.AccountWarmUpCalculation
import com.warmup.manager.data.model.Platform
import com.warmup.manager.ui.components.StatusBadge
import com.warmup.manager.ui.theme.DarkBackground
import com.warmup.manager.ui.theme.DarkSurface
import com.warmup.manager.ui.theme.DarkSurfaceVariant
import com.warmup.manager.ui.theme.TextMuted
import com.warmup.manager.ui.theme.TextPrimary
import com.warmup.manager.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    platform: Platform,
    calculations: List<AccountWarmUpCalculation>,
    onBack: () -> Unit,
    onSelectAccount: (Long) -> Unit,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${platform.displayName} Accounts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${calculations.size} total accounts",
                            fontSize = 11.sp,
                            color = TextSecondary
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
            FloatingActionButton(
                onClick = {
                    accountToEdit = null
                    showAddEditDialog = true
                },
                containerColor = Color(platform.hexColor),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        if (calculations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No ${platform.displayName} accounts added yet",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap + to register an account and start tracking warmup",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(calculations, key = { it.account.id }) { calc ->
                    AccountRowCard(
                        calc = calc,
                        onClick = { onSelectAccount(calc.account.id) },
                        onEdit = {
                            accountToEdit = calc.account
                            showAddEditDialog = true
                        },
                        onDelete = {
                            accountToDelete = calc.account
                        }
                    )
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditAccountDialog(
            platform = platform,
            existingAccount = accountToEdit,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { account ->
                if (accountToEdit == null) {
                    onAddAccount(account)
                } else {
                    onUpdateAccount(account)
                }
                showAddEditDialog = false
            }
        )
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove ${account.username}? All warmup history and recorded sessions for this account will be erased.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount(account)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun AccountRowCard(
    calc: AccountWarmUpCalculation,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateAddedString = remember(calc.account.createdAt) {
        dateFormat.format(Date(calc.account.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = calc.account.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Added $dateAddedString",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Account", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (calc.account.nicheTag.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = calc.account.nicheTag,
                        fontSize = 11.sp,
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge with progress bar
            StatusBadge(
                status = calc.status,
                showProgressBar = true
            )
        }
    }
}

@Composable
fun AddEditAccountDialog(
    platform: Platform,
    existingAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    var username by remember { mutableStateOf(existingAccount?.username ?: "") }
    var nicheTag by remember { mutableStateOf(existingAccount?.nicheTag ?: "") }
    var notes by remember { mutableStateOf(existingAccount?.notes ?: "") }
    var targetMinutes by remember { mutableIntStateOf(existingAccount?.targetDailyMinutes ?: 30) }
    var targetDays by remember { mutableIntStateOf(existingAccount?.targetDays ?: 5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingAccount == null) "Add ${platform.displayName} Account" else "Edit Account",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Handle") },
                    placeholder = { Text("@your_channel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Color(platform.hexColor),
                        unfocusedBorderColor = DarkSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = nicheTag,
                    onValueChange = { nicheTag = it },
                    label = { Text("Niche Tag (e.g. Tech, Fitness, Gaming)") },
                    placeholder = { Text("Keywords for targeted interest") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Color(platform.hexColor),
                        unfocusedBorderColor = DarkSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Account Notes") },
                    placeholder = { Text("Creation date, SIM card, IP notes...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Color(platform.hexColor),
                        unfocusedBorderColor = DarkSurfaceVariant
                    )
                )

                // Warmup Duration Target Selection (3, 5, 7 days; default 5)
                Column {
                    Text(
                        text = "Warmup Cycle Duration",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 7).forEach { days ->
                            val isSelected = targetDays == days
                            Button(
                                onClick = { targetDays = days },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(platform.hexColor) else DarkSurfaceVariant,
                                    contentColor = if (isSelected) Color.White else TextSecondary
                                )
                            ) {
                                Text(
                                    text = "$days Days" + if (days == 5) " ★" else "",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text(
                        text = "5 days recommended to establish consistent organic user signals.",
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Daily Minutes Target
                Column {
                    Text(
                        text = "Daily Target: $targetMinutes min/day",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45).forEach { mins ->
                            val isSelected = targetMinutes == mins
                            Button(
                                onClick = { targetMinutes = mins },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(platform.hexColor) else DarkSurfaceVariant,
                                    contentColor = if (isSelected) Color.White else TextSecondary
                                )
                            ) {
                                Text(
                                    text = "$mins min",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanUsername = if (!username.startsWith("@") && username.isNotBlank()) "@$username" else username
                    val updated = (existingAccount ?: AccountEntity(
                        username = cleanUsername,
                        platform = platform
                    )).copy(
                        username = cleanUsername,
                        platform = platform,
                        nicheTag = nicheTag,
                        notes = notes,
                        targetDailyMinutes = targetMinutes,
                        targetDays = targetDays
                    )
                    onConfirm(updated)
                },
                enabled = username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(platform.hexColor))
            ) {
                Text(if (existingAccount == null) "Add Account" else "Save")
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

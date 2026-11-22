package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerdantCard
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CurrencyFormatter
import com.example.viewmodel.FinanceUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    uiState: FinanceUiState,
    onUpdateProfile: (String, String) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onEnableAppLock: (String) -> Unit,
    onDisableAppLock: () -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onTogglePrivacyScreen: (Boolean) -> Unit,
    onSaveAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onCreateCategory: (String, TransactionType, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onLoadSampleData: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val prefs = uiState.preferences
    val currency = uiState.currency

    var showEditProfile by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "profile_header_title") {
                Text(
                    text = "Profile & Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Profile Identity Card
            item(key = "profile_card") {
                VerdantCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_profile_avatar_1795332176663),
                            contentDescription = "User avatar",
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prefs.userName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = prefs.userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                        }
                        IconButton(
                            onClick = { showEditProfile = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Accounts & Wallets Management
            item(key = "accounts_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Accounts & Wallets",
                        actionLabel = "Add",
                        actionTestTag = "add_account_button",
                        onActionClick = {
                            editingAccount = null
                            showAccountDialog = true
                        }
                    )
                    VerdantCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        uiState.accounts.forEachIndexed { idx, acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(iconKey = acc.account.iconKey, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.account.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${AccountType.entries.firstOrNull { it.name == acc.account.type }?.label ?: acc.account.type}  •  ${acc.transactionCount} entries",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.mutedText
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.format(acc.currentBalance, currency),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (acc.currentBalance >= 0) extended.income else extended.expense
                                )
                                if (uiState.accounts.size > 1) {
                                    IconButton(onClick = { onDeleteAccount(acc.account) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete account",
                                            tint = extended.mutedText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < uiState.accounts.lastIndex) {
                                HorizontalDivider(color = extended.border)
                            }
                        }
                    }
                }
            }

            // Custom Categories Management
            item(key = "categories_section") {
                val customCategories = uiState.categories.filter { it.isCustom }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Categories",
                        subtitle = "${uiState.categories.size} active (${customCategories.size} custom)",
                        actionLabel = "New Custom",
                        actionTestTag = "profile_add_category_button",
                        onActionClick = { showCategoryDialog = true }
                    )
                    VerdantCard(contentPadding = PaddingValues(16.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.categories.forEach { cat ->
                                Surface(
                                    color = if (cat.isCustom) extended.sageTint else MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, extended.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (cat.isCustom) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Remove custom category",
                                                tint = extended.expense,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable { onDeleteCategory(cat) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Appearance & Currency
            item(key = "preferences_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Preferences")
                    VerdantCard {
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { onSelectTheme(mode) },
                                    label = { Text(mode.label) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = extended.border)
                        Spacer(Modifier.height(16.dp))

                        Text("Display Currency", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyOption.entries.forEach { opt ->
                                FilterChip(
                                    selected = currency == opt,
                                    onClick = { onSelectCurrency(opt) },
                                    label = { Text("${opt.symbol} ${opt.code}") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = extended.border)
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Budget Threshold Alerts", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Show alerts when nearing or exceeding limits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.mutedText
                                )
                            }
                            Switch(
                                checked = prefs.budgetAlertsEnabled,
                                onCheckedChange = onToggleBudgetAlerts
                            )
                        }
                    }
                }
            }

            // Security & Privacy Controls
            item(key = "security_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Security & Privacy")
                    VerdantCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Biometric & PIN App Lock", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Require biometric or 4-digit PIN on launch",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.mutedText
                                    )
                                }
                            }
                            Switch(
                                checked = prefs.appLockEnabled,
                                onCheckedChange = { wantEnable ->
                                    if (wantEnable) showSetPinDialog = true else onDisableAppLock()
                                },
                                modifier = Modifier.testTag("toggle_app_lock")
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = extended.border)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Screen Privacy Protection", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Hide balances in app switcher & block screenshots",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.mutedText
                                    )
                                }
                            }
                            Switch(
                                checked = prefs.privacyScreenEnabled,
                                onCheckedChange = onTogglePrivacyScreen,
                                modifier = Modifier.testTag("toggle_privacy_screen")
                            )
                        }
                    }
                }
            }

            // Data Backup & Management
            item(key = "backup_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Data & Backup",
                        subtitle = "Local SQLite database with portable JSON backups"
                    )
                    VerdantCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExportBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("backup_export_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, extended.border)
                            ) {
                                Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Backup")
                            }
                            OutlinedButton(
                                onClick = onRestoreBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("backup_restore_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, extended.border)
                            ) {
                                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLoadSampleData,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, extended.border)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sample Data")
                            }
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, extended.expense.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = extended.expense,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Reset All", color = extended.expense)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        var name by remember { mutableStateOf(prefs.userName) }
        var email by remember { mutableStateOf(prefs.userEmail) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(name, email)
                        showEditProfile = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditProfile = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAccountDialog) {
        AccountEditorDialog(
            existing = editingAccount,
            currencySymbol = currency.symbol,
            onSave = { acc ->
                onSaveAccount(acc)
                showAccountDialog = false
            },
            onDismiss = { showAccountDialog = false }
        )
    }

    if (showCategoryDialog) {
        CreateCustomCategoryDialog(
            initialType = TransactionType.EXPENSE,
            onConfirm = { name, type, icon ->
                onCreateCategory(name, type, icon)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showSetPinDialog) {
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Set 4-Digit Security PIN", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        text = "Used alongside device biometrics to unlock Verdant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.mutedText
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                pin = it
                                error = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = error != null,
                        supportingText = error?.let { msg -> { Text(msg) } },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin.length != 4) {
                            error = "Enter exactly 4 digits"
                        } else {
                            onEnableAppLock(pin)
                            showSetPinDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Enable Lock") }
            },
            dismissButton = { TextButton(onClick = { showSetPinDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showResetConfirm) {
        DeleteConfirmDialog(
            title = "Reset All Financial Data?",
            message = "This will permanently delete all transactions, budgets, savings goals, and bill reminders on this device.",
            onConfirm = {
                onResetAllData()
                showResetConfirm = false
            },
            onDismiss = { showResetConfirm = false }
        )
    }
}

@Composable
private fun AccountEditorDialog(
    existing: AccountEntity?,
    currencySymbol: String,
    onSave: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember {
        mutableStateOf(
            AccountType.entries.firstOrNull { it.name == existing?.type } ?: AccountType.BANK
        )
    }
    var balanceText by remember { mutableStateOf(existing?.initialBalance?.toLong()?.toString() ?: "0") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Account / Wallet" else "Edit Account", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g., ICICI Bank, PhonePe UPI") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Opening Balance ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccountType.entries.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { accType ->
                                FilterChip(
                                    selected = type == accType,
                                    onClick = { type = accType },
                                    label = { Text(accType.label) }
                                )
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = VerdantThemeTokens.colors.expense)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "Enter an account name"
                    } else {
                        val icon = when (type) {
                            AccountType.BANK -> "account_balance"
                            AccountType.UPI -> "qr_code_2"
                            AccountType.CARD -> "credit_card"
                            AccountType.CASH -> "wallet"
                        }
                        onSave(
                            AccountEntity(
                                id = existing?.id ?: 0L,
                                name = name.trim(),
                                type = type.name,
                                initialBalance = balanceText.toDoubleOrNull() ?: 0.0,
                                iconKey = icon
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp)
    )
}

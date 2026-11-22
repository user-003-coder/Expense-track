package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionWithDetails
import com.example.ui.components.AlertsBottomSheet
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionDetailBottomSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.VerdantTheme
import com.example.ui.theme.VerdantThemeTokens
import com.example.viewmodel.FinanceViewModel

enum class VerdantDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    TRANSACTIONS("transactions", R.string.nav_transactions, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    ANALYTICS("analytics", R.string.nav_analytics, Icons.Filled.Insights, Icons.Outlined.Insights),
    BUDGETS("budgets", R.string.nav_budgets, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

            // Dynamically apply FLAG_SECURE privacy control
            LaunchedEffect(uiState.preferences.privacyScreenEnabled) {
                if (uiState.preferences.privacyScreenEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val darkTheme = when (uiState.preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            VerdantTheme(darkTheme = darkTheme) {
                if (uiState.preferences.appLockEnabled && !isUnlocked) {
                    AppLockScreen(
                        onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                        onBiometricSuccess = { viewModel.unlockApp() }
                    )
                } else {
                    VerdantAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun VerdantAppScaffold(viewModel: FinanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = VerdantThemeTokens.colors
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by remember { mutableStateOf(VerdantDestination.HOME) }
    var isAddEditOpen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var homeDetailTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var pendingHomeDeleteId by remember { mutableStateOf<Long?>(null) }

    // Back press on secondary tabs returns cleanly to Home
    BackHandler(enabled = !isAddEditOpen && currentDestination != VerdantDestination.HOME) {
        currentDestination = VerdantDestination.HOME
    }

    // Collect toast/snackbar feedback messages
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Zero-permission Storage Access Framework launchers for CSV, PDF, and JSON Backup/Restore
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsvToUri(it) } }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { viewModel.exportPdfToUri(it) } }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackupToUri(it) } }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.restoreBackupFromUri(it) } }

    if (isAddEditOpen) {
        AddEditTransactionScreen(
            existingTransaction = editingTransaction,
            categories = uiState.categories,
            accounts = uiState.accounts,
            currency = uiState.currency,
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                isAddEditOpen = false
                editingTransaction = null
            },
            onCreateCustomCategory = { name, type, icon ->
                viewModel.addCustomCategory(name, type, icon)
            },
            onBack = {
                isAddEditOpen = false
                editingTransaction = null
            }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavRail = maxWidth >= 680.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Spacer(Modifier.height(8.dp))
                        FloatingActionButton(
                            onClick = {
                                editingTransaction = null
                                isAddEditOpen = true
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                            modifier = Modifier.testTag("fab_add_transaction")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_add_transaction)
                            )
                        }
                    }
                ) {
                    Spacer(Modifier.height(12.dp))
                    VerdantDestination.entries.forEach { dest ->
                        val selected = currentDestination == dest
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentDestination = dest },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = extended.sageTint,
                                unselectedIconColor = extended.mutedText,
                                unselectedTextColor = extended.mutedText
                            ),
                            modifier = Modifier.testTag("nav_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    if (!useNavRail && currentDestination != VerdantDestination.PROFILE) {
                        FloatingActionButton(
                            onClick = {
                                editingTransaction = null
                                isAddEditOpen = true
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                            modifier = Modifier.testTag("fab_add_transaction")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_add_transaction)
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            VerdantDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = extended.sageTint,
                                        unselectedIconColor = extended.mutedText,
                                        unselectedTextColor = extended.mutedText
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val screenModifier = Modifier.padding(innerPadding)
                when (currentDestination) {
                    VerdantDestination.HOME -> HomeScreen(
                        uiState = uiState,
                        onToggleBalanceVisibility = viewModel::toggleHideBalance,
                        onOpenNotifications = { showAlertsSheet = true },
                        onOpenProfile = { currentDestination = VerdantDestination.PROFILE },
                        onViewAllTransactions = { currentDestination = VerdantDestination.TRANSACTIONS },
                        onOpenTransactionDetail = { detail -> homeDetailTransaction = detail },
                        onSelectAnalyticsPeriod = viewModel::setAnalyticsPeriod,
                        onOpenAnalytics = { currentDestination = VerdantDestination.ANALYTICS },
                        onOpenBudgets = { currentDestination = VerdantDestination.BUDGETS },
                        onAddMoneyToGoal = viewModel::addMoneyToGoal,
                        onAddTransactionClick = {
                            editingTransaction = null
                            isAddEditOpen = true
                        },
                        modifier = screenModifier
                    )

                    VerdantDestination.TRANSACTIONS -> TransactionsScreen(
                        uiState = uiState,
                        onSearchChange = viewModel::updateSearchQuery,
                        onTypeFilterChange = viewModel::updateTypeFilter,
                        onCategoryFilterChange = viewModel::updateCategoryFilter,
                        onPaymentMethodFilterChange = viewModel::updatePaymentMethodFilter,
                        onClearFilters = viewModel::clearFilters,
                        onAddTransactionClick = {
                            editingTransaction = null
                            isAddEditOpen = true
                        },
                        onEditTransaction = { detail ->
                            editingTransaction = detail.transaction
                            isAddEditOpen = true
                        },
                        onDeleteTransaction = viewModel::deleteTransaction,
                        modifier = screenModifier
                    )

                    VerdantDestination.ANALYTICS -> AnalyticsScreen(
                        uiState = uiState,
                        onSelectPeriod = viewModel::setAnalyticsPeriod,
                        onExportCsv = { csvExportLauncher.launch("verdant_transactions.csv") },
                        onExportPdf = { pdfExportLauncher.launch("verdant_financial_report.pdf") },
                        modifier = screenModifier
                    )

                    VerdantDestination.BUDGETS -> BudgetsScreen(
                        uiState = uiState,
                        onSaveBudget = viewModel::saveBudget,
                        onDeleteBudget = viewModel::deleteBudget,
                        onSaveGoal = viewModel::saveGoal,
                        onAddMoneyToGoal = viewModel::addMoneyToGoal,
                        onDeleteGoal = viewModel::deleteGoal,
                        onSaveBill = viewModel::saveBillReminder,
                        onToggleBillPaid = viewModel::markBillPaid,
                        onDeleteBill = viewModel::deleteBillReminder,
                        modifier = screenModifier
                    )

                    VerdantDestination.PROFILE -> ProfileScreen(
                        uiState = uiState,
                        onUpdateProfile = viewModel::updateProfile,
                        onSelectCurrency = viewModel::setCurrency,
                        onSelectTheme = viewModel::setThemeMode,
                        onEnableAppLock = viewModel::enableAppLock,
                        onDisableAppLock = viewModel::disableAppLock,
                        onToggleBudgetAlerts = viewModel::setBudgetAlertsEnabled,
                        onTogglePrivacyScreen = viewModel::setPrivacyScreenEnabled,
                        onSaveAccount = viewModel::saveAccount,
                        onDeleteAccount = viewModel::deleteAccount,
                        onCreateCategory = viewModel::addCustomCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        onExportBackup = { jsonBackupLauncher.launch("verdant_backup.json") },
                        onRestoreBackup = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                        onLoadSampleData = viewModel::loadSampleData,
                        onResetAllData = viewModel::clearAllData,
                        modifier = screenModifier
                    )
                }
            }
        }
    }

    if (showAlertsSheet) {
        AlertsBottomSheet(
            alerts = uiState.alerts,
            onDismiss = { showAlertsSheet = false },
            onOpenBudgets = { currentDestination = VerdantDestination.BUDGETS }
        )
    }

    homeDetailTransaction?.let { detail ->
        TransactionDetailBottomSheet(
            item = detail,
            currency = uiState.currency,
            onDismiss = { homeDetailTransaction = null },
            onEdit = {
                homeDetailTransaction = null
                editingTransaction = detail.transaction
                isAddEditOpen = true
            },
            onDeleteRequest = {
                pendingHomeDeleteId = detail.transaction.id
                homeDetailTransaction = null
            }
        )
    }

    pendingHomeDeleteId?.let { idToDelete ->
        com.example.ui.components.DeleteConfirmDialog(
            onConfirm = {
                viewModel.deleteTransaction(idToDelete)
                pendingHomeDeleteId = null
            },
            onDismiss = { pendingHomeDeleteId = null }
        )
    }
}

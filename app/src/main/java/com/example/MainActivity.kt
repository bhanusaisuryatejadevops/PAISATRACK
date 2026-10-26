package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.BackupManager
import com.example.data.local.entity.TransactionEntity
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DebtsAndBillsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PaisaTrackTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.AppStrings
import com.example.util.DateUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            PaisaTrackTheme(darkTheme = isDark) {
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
                val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
                val language by viewModel.appLanguage.collectAsStateWithLifecycle()

                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        language = language,
                        onComplete = { viewModel.completeOnboarding() }
                    )
                } else if (isLocked) {
                    LockScreen(
                        language = language,
                        onVerifyPin = { pin -> viewModel.verifyPin(pin) }
                    )
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.lockApp()
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Transient UI sheets
    var showAddTxSheet by remember { mutableStateOf(false) }
    var selectedTxForEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    // Backup & Export Storage Access Framework Launchers
    var pendingBackupPassword by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportCsv(stream)
            }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportPdf(stream)
            }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = pendingBackupPassword
        if (uri != null && password != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.createEncryptedBackup(password, stream)
            }
        }
        pendingBackupPassword = null
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestorePasswordDialog = true
        }
    }

    // Observe Event Messages for Toast / Snackbar
    LaunchedEffect(Unit) {
        viewModel.eventMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Back handler: Return to HOME if on sub-tab
    BackHandler(enabled = currentTab != ScreenTab.HOME) {
        viewModel.selectTab(ScreenTab.HOME)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                // 1. Home
                NavigationBarItem(
                    selected = currentTab == ScreenTab.HOME,
                    onClick = { viewModel.selectTab(ScreenTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = AppStrings.get("home", language)) },
                    label = { Text(AppStrings.get("home", language)) },
                    modifier = Modifier.testTag("tab_home")
                )

                // 2. Transactions
                NavigationBarItem(
                    selected = currentTab == ScreenTab.TRANSACTIONS,
                    onClick = { viewModel.selectTab(ScreenTab.TRANSACTIONS) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = AppStrings.get("transactions", language)) },
                    label = { Text(AppStrings.get("transactions", language)) },
                    modifier = Modifier.testTag("tab_transactions")
                )

                // 3. Budgets & Goals
                NavigationBarItem(
                    selected = currentTab == ScreenTab.BUDGETS,
                    onClick = { viewModel.selectTab(ScreenTab.BUDGETS) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = AppStrings.get("budgets", language)) },
                    label = { Text(AppStrings.get("budgets", language)) },
                    modifier = Modifier.testTag("tab_budgets")
                )

                // 4. Reports & Analytics
                NavigationBarItem(
                    selected = currentTab == ScreenTab.REPORTS,
                    onClick = { viewModel.selectTab(ScreenTab.REPORTS) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = AppStrings.get("reports", language)) },
                    label = { Text(AppStrings.get("reports", language)) },
                    modifier = Modifier.testTag("tab_reports")
                )

                // 5. Settings
                NavigationBarItem(
                    selected = currentTab == ScreenTab.SETTINGS,
                    onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = AppStrings.get("settings", language)) },
                    label = { Text(AppStrings.get("settings", language)) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.HOME -> {
                    val income by viewModel.monthIncome.collectAsStateWithLifecycle()
                    val expense by viewModel.monthExpense.collectAsStateWithLifecycle()
                    val todayExpense by viewModel.todayExpense.collectAsStateWithLifecycle()
                    val recentTx by viewModel.recentTransactions.collectAsStateWithLifecycle()
                    val catBreakdown by viewModel.monthCategoryExpenseBreakdown.collectAsStateWithLifecycle()
                    val budget by viewModel.overallBudget.collectAsStateWithLifecycle()
                    val pendingBills by viewModel.pendingReminders.collectAsStateWithLifecycle()

                    HomeScreen(
                        language = language,
                        incomePaisa = income,
                        expensePaisa = expense,
                        todayExpensePaisa = todayExpense,
                        recentTransactions = recentTx,
                        categoryBreakdown = catBreakdown,
                        overallBudget = budget,
                        pendingBills = pendingBills,
                        onAddTransactionClick = {
                            selectedTxForEdit = null
                            showAddTxSheet = true
                        },
                        onTransactionClick = { tx ->
                            selectedTxForEdit = tx
                            showAddTxSheet = true
                        },
                        onSeeAllTransactionsClick = { viewModel.selectTab(ScreenTab.TRANSACTIONS) },
                        onMarkBillPaid = { bill -> viewModel.markBillPaid(bill) },
                        onLockAppClick = { viewModel.lockApp() },
                        isLockEnabled = isAppLockEnabled
                    )
                }

                ScreenTab.TRANSACTIONS -> {
                    val filteredTx by viewModel.filteredTransactions.collectAsStateWithLifecycle()
                    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
                    val datePreset by viewModel.dateFilterPreset.collectAsStateWithLifecycle()
                    val paymentMode by viewModel.selectedPaymentMode.collectAsStateWithLifecycle()

                    TransactionsScreen(
                        language = language,
                        transactions = filteredTx,
                        searchQuery = query,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        typeFilter = typeFilter,
                        onTypeFilterChange = { viewModel.updateTypeFilter(it) },
                        dateRangePreset = datePreset,
                        onDateRangePresetChange = { viewModel.updateDateFilter(it) },
                        selectedPaymentMode = paymentMode,
                        onPaymentModeChange = { viewModel.updatePaymentModeFilter(it) },
                        onAddTransactionClick = {
                            selectedTxForEdit = null
                            showAddTxSheet = true
                        },
                        onTransactionClick = { tx ->
                            selectedTxForEdit = tx
                            showAddTxSheet = true
                        }
                    )
                }

                ScreenTab.BUDGETS -> {
                    val overallBudget by viewModel.overallBudget.collectAsStateWithLifecycle()
                    val monthExpense by viewModel.monthExpense.collectAsStateWithLifecycle()
                    val catBudgets by viewModel.currentMonthBudgets.collectAsStateWithLifecycle()
                    val goals by viewModel.allGoals.collectAsStateWithLifecycle()

                    BudgetsScreen(
                        language = language,
                        overallBudget = overallBudget,
                        monthExpensePaisa = monthExpense,
                        categoryBudgets = catBudgets,
                        savingsGoals = goals,
                        onSaveBudget = { b -> viewModel.saveBudget(b) },
                        onDeleteBudget = { id -> viewModel.deleteBudget(id) },
                        onSaveSavingsGoal = { g -> viewModel.saveSavingsGoal(g) },
                        onAddContribution = { g, amt -> viewModel.addSavingsContribution(g, amt) },
                        onDeleteSavingsGoal = { id -> viewModel.deleteSavingsGoal(id) }
                    )
                }

                ScreenTab.DEBTS -> {
                    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
                    val totalLent by viewModel.totalLent.collectAsStateWithLifecycle()
                    val totalBorrowed by viewModel.totalBorrowed.collectAsStateWithLifecycle()
                    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()

                    DebtsAndBillsScreen(
                        language = language,
                        debts = debts,
                        totalLentPaisa = totalLent,
                        totalBorrowedPaisa = totalBorrowed,
                        billReminders = reminders,
                        onSaveDebt = { d -> viewModel.saveDebt(d) },
                        onRecordDebtRepayment = { id, amt, note -> viewModel.recordDebtRepayment(id, amt, note) },
                        onSettleDebt = { id -> viewModel.settleDebt(id) },
                        onDeleteDebt = { d -> viewModel.deleteDebt(d) },
                        onSaveBillReminder = { r -> viewModel.saveBillReminder(r) },
                        onMarkBillPaid = { r -> viewModel.markBillPaid(r) },
                        onDeleteBillReminder = { id -> viewModel.deleteBillReminder(id) }
                    )
                }

                ScreenTab.REPORTS -> {
                    val income by viewModel.monthIncome.collectAsStateWithLifecycle()
                    val expense by viewModel.monthExpense.collectAsStateWithLifecycle()
                    val catBreakdown by viewModel.monthCategoryExpenseBreakdown.collectAsStateWithLifecycle()
                    val paymentBreakdown by viewModel.getReportsPaymentModeBreakdown(DateUtils.getStartOfMonth(), DateUtils.getEndOfMonth()).collectAsStateWithLifecycle()

                    ReportsScreen(
                        language = language,
                        incomePaisa = income,
                        expensePaisa = expense,
                        categoryBreakdown = catBreakdown,
                        paymentBreakdown = paymentBreakdown,
                        onExportCsvClick = {
                            val defaultName = "PaisaTrack_Report_${DateUtils.formatMonthKey(System.currentTimeMillis())}.csv"
                            exportCsvLauncher.launch(defaultName)
                        },
                        onExportPdfClick = {
                            val defaultName = "PaisaTrack_Statement_${DateUtils.formatMonthKey(System.currentTimeMillis())}.pdf"
                            exportPdfLauncher.launch(defaultName)
                        }
                    )
                }

                ScreenTab.SETTINGS -> {
                    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

                    SettingsScreen(
                        language = language,
                        onLanguageChange = { viewModel.setLanguage(it) },
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.setTheme(it) },
                        isAppLockEnabled = isAppLockEnabled,
                        onSetPin = { viewModel.setPin(it) },
                        onDisablePin = { viewModel.disablePin() },
                        onCreateBackupClick = { password ->
                            pendingBackupPassword = password
                            val defaultName = "PaisaTrack_Backup_${System.currentTimeMillis()}.ptbackup"
                            createBackupLauncher.launch(defaultName)
                        },
                        onRestoreBackupTrigger = {
                            restoreBackupLauncher.launch(arrayOf("*/*"))
                        },
                        onWipeAllData = { viewModel.wipeAllData() }
                    )
                }
            }
        }
    }

    // Add / Edit Transaction Sheet
    if (showAddTxSheet) {
        val categories by viewModel.allCategories.collectAsStateWithLifecycle()

        AddEditTransactionSheet(
            existingTransaction = selectedTxForEdit,
            categories = categories,
            language = language,
            onDismiss = {
                showAddTxSheet = false
                selectedTxForEdit = null
            },
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                showAddTxSheet = false
                selectedTxForEdit = null
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx)
                showAddTxSheet = false
                selectedTxForEdit = null
            }
        )
    }

    // Restore Backup Password & Mode Dialog
    if (showRestorePasswordDialog && pendingRestoreUri != null) {
        var restorePassword by remember { mutableStateOf("") }
        var restoreMode by remember { mutableStateOf(BackupManager.RestoreMode.REPLACE_ALL) }

        AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                pendingRestoreUri = null
            },
            title = { Text(AppStrings.get("restore_confirm_title", language)) },
            text = {
                Column {
                    Text(AppStrings.get("backup_password_prompt", language))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_restore_password")
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Restore Option:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = restoreMode == BackupManager.RestoreMode.REPLACE_ALL,
                            onClick = { restoreMode = BackupManager.RestoreMode.REPLACE_ALL }
                        )
                        Text(AppStrings.get("replace", language), style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = restoreMode == BackupManager.RestoreMode.MERGE,
                            onClick = { restoreMode = BackupManager.RestoreMode.MERGE }
                        )
                        Text(AppStrings.get("merge", language), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri
                        if (uri != null && restorePassword.isNotBlank()) {
                            context.contentResolver.openInputStream(uri)?.let { stream ->
                                viewModel.restoreEncryptedBackup(restorePassword, stream, restoreMode)
                            }
                        }
                        showRestorePasswordDialog = false
                        pendingRestoreUri = null
                    },
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    pendingRestoreUri = null
                }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}

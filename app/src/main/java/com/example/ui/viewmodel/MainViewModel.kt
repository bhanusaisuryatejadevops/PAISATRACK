package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.export.ReportExporter
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CategoryTotal
import com.example.data.local.dao.PaymentModeTotal
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.PaisaRepository
import com.example.data.security.SecurityManager
import com.example.util.AppLanguage
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

enum class ScreenTab {
    HOME,
    TRANSACTIONS,
    BUDGETS,
    DEBTS,
    REPORTS,
    SETTINGS
}

enum class TransactionFilterType {
    ALL,
    EXPENSE,
    INCOME
}

enum class DateRangePreset {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val repository = PaisaRepository(database)
    val preferencesManager = PreferencesManager(application)

    // Language & Theme
    val appLanguage: StateFlow<AppLanguage> = preferencesManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val themeMode: StateFlow<String> = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val isOnboardingCompleted: StateFlow<Boolean> = preferencesManager.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // App Lock
    val isAppLockEnabled: StateFlow<Boolean> = preferencesManager.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var storedPinHash: String? = null
    private var storedPinSalt: String? = null

    // Navigation Tab
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Filters for Transactions screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow(TransactionFilterType.ALL)
    val typeFilter: StateFlow<TransactionFilterType> = _typeFilter.asStateFlow()

    private val _dateFilterPreset = MutableStateFlow(DateRangePreset.THIS_MONTH)
    val dateFilterPreset: StateFlow<DateRangePreset> = _dateFilterPreset.asStateFlow()

    private val _selectedPaymentMode = MutableStateFlow<String?>(null)
    val selectedPaymentMode: StateFlow<String?> = _selectedPaymentMode.asStateFlow()

    // Feedback message events
    private val _eventMessage = MutableSharedFlow<String>()
    val eventMessage: SharedFlow<String> = _eventMessage.asSharedFlow()

    init {
        // Initial check for App Lock
        viewModelScope.launch {
            preferencesManager.pinHash.collect { hash ->
                storedPinHash = hash
            }
        }
        viewModelScope.launch {
            preferencesManager.pinSalt.collect { salt ->
                storedPinSalt = salt
            }
        }
        viewModelScope.launch {
            preferencesManager.isAppLockEnabled.collect { enabled ->
                if (enabled) {
                    _isLocked.value = true
                }
            }
        }
        // Recurring transactions check on startup
        viewModelScope.launch {
            repository.processRecurringTransactions()
        }
    }

    // App Lock Handlers
    fun verifyPin(enteredPin: String): Boolean {
        val hash = storedPinHash
        val salt = storedPinSalt
        if (hash != null && salt != null) {
            val verified = SecurityManager.verifyPin(enteredPin, hash, salt)
            if (verified) {
                _isLocked.value = false
                return true
            }
        }
        return false
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            val salt = SecurityManager.generateSalt()
            val hash = SecurityManager.hashPin(pin, salt)
            preferencesManager.setAppLock(true, hash, salt)
            _isLocked.value = false
            _eventMessage.emit("App Lock PIN activated successfully!")
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            preferencesManager.setAppLock(false)
            _isLocked.value = false
            _eventMessage.emit("App Lock PIN disabled.")
        }
    }

    fun lockApp() {
        if (isAppLockEnabled.value) {
            _isLocked.value = true
        }
    }

    // Tab navigation
    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            preferencesManager.setAppLanguage(lang)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(theme)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    // Transactions filtering & search
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateTypeFilter(filter: TransactionFilterType) {
        _typeFilter.value = filter
    }

    fun updateDateFilter(preset: DateRangePreset) {
        _dateFilterPreset.value = preset
    }

    fun updatePaymentModeFilter(mode: String?) {
        _selectedPaymentMode.value = mode
    }

    // Date range bounds
    private val currentMonthStart = DateUtils.getStartOfMonth()
    private val currentMonthEnd = DateUtils.getEndOfMonth()
    private val todayStart = DateUtils.getStartOfDay()
    private val todayEnd = DateUtils.getEndOfDay()

    // Home Metrics
    val monthIncome: StateFlow<Long> = repository.getIncomeInRange(currentMonthStart, currentMonthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthExpense: StateFlow<Long> = repository.getExpenseInRange(currentMonthStart, currentMonthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val todayExpense: StateFlow<Long> = repository.getExpenseInRange(todayStart, todayEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthCategoryExpenseBreakdown: StateFlow<List<CategoryTotal>> =
        repository.getCategoryExpenseBreakdown(currentMonthStart, currentMonthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All categories
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _searchQuery,
        _typeFilter,
        _dateFilterPreset,
        _selectedPaymentMode
    ) { txList, query, type, datePreset, paymentMode ->
        val (rangeStart, rangeEnd) = when (datePreset) {
            DateRangePreset.TODAY -> DateUtils.getStartOfDay() to DateUtils.getEndOfDay()
            DateRangePreset.THIS_WEEK -> DateUtils.getStartOfWeek() to DateUtils.getEndOfDay()
            DateRangePreset.THIS_MONTH -> DateUtils.getStartOfMonth() to DateUtils.getEndOfMonth()
            DateRangePreset.THIS_YEAR -> DateUtils.getStartOfYear() to DateUtils.getEndOfYear()
            DateRangePreset.ALL -> 0L to Long.MAX_VALUE
        }

        txList.filter { tx ->
            val matchesRange = tx.dateEpochMillis in rangeStart..rangeEnd
            val matchesType = when (type) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.EXPENSE -> tx.type == "EXPENSE"
                TransactionFilterType.INCOME -> tx.type == "INCOME"
            }
            val matchesPayment = paymentMode == null || tx.paymentMode.equals(paymentMode, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    tx.categoryName.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.paymentMode.contains(query, ignoreCase = true)

            matchesRange && matchesType && matchesPayment && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budgets
    private val currentMonthKey = DateUtils.formatMonthKey(System.currentTimeMillis())
    val currentMonthBudgets: StateFlow<List<BudgetEntity>> = repository.getBudgetsForMonth(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallBudget: StateFlow<BudgetEntity?> = repository.overallMonthlyBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Debts
    val allDebts: StateFlow<List<DebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLent: StateFlow<Long> = repository.totalLentActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalBorrowed: StateFlow<Long> = repository.totalBorrowedActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Bills
    val pendingReminders: StateFlow<List<BillReminderEntity>> = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<BillReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Savings Goals
    val allGoals: StateFlow<List<SavingsGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reports Breakdown Flow
    fun getReportsCategoryBreakdown(startDate: Long, endDate: Long): StateFlow<List<CategoryTotal>> {
        return repository.getCategoryExpenseBreakdown(startDate, endDate)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getReportsPaymentModeBreakdown(startDate: Long, endDate: Long): StateFlow<List<PaymentModeTotal>> {
        return repository.getPaymentModeBreakdown(startDate, endDate)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // CRUD Actions
    fun saveTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(tx)
            _eventMessage.emit("Transaction saved successfully!")
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _eventMessage.emit("Transaction deleted.")
        }
    }

    fun saveBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.saveBudget(budget)
            _eventMessage.emit("Budget updated.")
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
            _eventMessage.emit("Budget removed.")
        }
    }

    fun saveDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.saveDebt(debt)
            _eventMessage.emit("Udhaar/Debt recorded.")
        }
    }

    fun recordDebtRepayment(debtId: Long, amountPaisa: Long, note: String) {
        viewModelScope.launch {
            repository.recordRepayment(debtId, amountPaisa, note)
            _eventMessage.emit("Repayment recorded.")
        }
    }

    fun settleDebt(debtId: Long) {
        viewModelScope.launch {
            repository.markDebtSettled(debtId)
            _eventMessage.emit("Debt marked as settled!")
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            _eventMessage.emit("Record removed.")
        }
    }

    fun saveBillReminder(reminder: BillReminderEntity) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
            _eventMessage.emit("Bill reminder saved.")
        }
    }

    fun markBillPaid(reminder: BillReminderEntity) {
        viewModelScope.launch {
            repository.markReminderPaid(reminder)
            _eventMessage.emit("Bill marked paid & expense recorded!")
        }
    }

    fun deleteBillReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
            _eventMessage.emit("Reminder removed.")
        }
    }

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveGoal(goal)
            _eventMessage.emit("Savings goal saved.")
        }
    }

    fun addSavingsContribution(goal: SavingsGoalEntity, contributionPaisa: Long) {
        viewModelScope.launch {
            repository.addContributionToGoal(goal, contributionPaisa)
            _eventMessage.emit("Contribution added to goal!")
        }
    }

    fun deleteSavingsGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoalById(id)
            _eventMessage.emit("Goal deleted.")
        }
    }

    // Export & Backup
    fun exportCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val list = repository.getAllTransactionsSnapshot()
                ReportExporter.exportCsv(list, outputStream)
                _eventMessage.emit("CSV exported successfully!")
            } catch (e: Exception) {
                _eventMessage.emit("CSV export failed: ${e.message}")
            }
        }
    }

    fun exportPdf(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val list = repository.getAllTransactionsSnapshot()
                val income = repository.getIncomeSum(0L, Long.MAX_VALUE)
                val expense = repository.getExpenseSum(0L, Long.MAX_VALUE)
                ReportExporter.exportPdf(list, income, expense, outputStream)
                _eventMessage.emit("PDF report generated successfully!")
            } catch (e: Exception) {
                _eventMessage.emit("PDF export failed: ${e.message}")
            }
        }
    }

    fun createEncryptedBackup(password: String, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                BackupManager.createEncryptedBackup(database, password, outputStream)
                _eventMessage.emit("Encrypted backup saved successfully!")
            } catch (e: Exception) {
                _eventMessage.emit("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreEncryptedBackup(
        password: String,
        inputStream: InputStream,
        mode: BackupManager.RestoreMode
    ) {
        viewModelScope.launch {
            try {
                BackupManager.restoreEncryptedBackup(database, password, inputStream, mode)
                _eventMessage.emit("Backup restored successfully!")
            } catch (e: Exception) {
                _eventMessage.emit("Restore failed: ${e.message}")
            }
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            preferencesManager.clearAllPreferences()
            AppDatabase.populateDefaultCategories(database.categoryDao())
            _eventMessage.emit("All application data erased cleanly.")
        }
    }
}

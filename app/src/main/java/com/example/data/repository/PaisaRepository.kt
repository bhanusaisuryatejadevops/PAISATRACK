package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.dao.CategoryTotal
import com.example.data.local.dao.PaymentModeTotal
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtRepaymentEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PaisaRepository(private val database: AppDatabase) {

    private val txDao = database.transactionDao()
    private val catDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val debtDao = database.debtDao()
    private val billDao = database.billReminderDao()
    private val goalDao = database.savingsGoalDao()

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = txDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = txDao.getRecentTransactions(8)

    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        txDao.getTransactionsInRange(startDate, endDate)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        txDao.getTransactionById(id)

    fun getIncomeInRange(startDate: Long, endDate: Long): Flow<Long> =
        txDao.getTotalIncomeInRange(startDate, endDate)

    fun getExpenseInRange(startDate: Long, endDate: Long): Flow<Long> =
        txDao.getTotalExpenseInRange(startDate, endDate)

    suspend fun getExpenseSum(startDate: Long, endDate: Long): Long =
        txDao.getExpenseSumInRange(startDate, endDate)

    suspend fun getIncomeSum(startDate: Long, endDate: Long): Long =
        txDao.getIncomeSumInRange(startDate, endDate)

    fun getCategoryExpenseBreakdown(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        txDao.getCategoryExpenseBreakdown(startDate, endDate)

    fun getPaymentModeBreakdown(startDate: Long, endDate: Long): Flow<List<PaymentModeTotal>> =
        txDao.getPaymentModeBreakdown(startDate, endDate)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return if (transaction.id == 0L) {
            txDao.insertTransaction(transaction)
        } else {
            txDao.updateTransaction(transaction)
            transaction.id
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        txDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        txDao.deleteById(id)
    }

    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> =
        txDao.getAllTransactionsSnapshot()

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = catDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        catDao.getCategoriesByType(type)

    suspend fun addCategory(category: CategoryEntity): Long =
        catDao.insertCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) =
        catDao.deleteCategory(category)

    // Budgets
    fun getBudgetsForMonth(monthKey: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(monthKey)

    val overallMonthlyBudget: Flow<BudgetEntity?> = budgetDao.getOverallMonthlyBudget()

    suspend fun saveBudget(budget: BudgetEntity): Long {
        return if (budget.id == 0L) {
            budgetDao.insertBudget(budget)
        } else {
            budgetDao.updateBudget(budget)
            budget.id
        }
    }

    suspend fun deleteBudgetById(id: Long) = budgetDao.deleteById(id)

    // Debts & Khata
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebts()
    val totalLentActive: Flow<Long> = debtDao.getTotalLentActive()
    val totalBorrowedActive: Flow<Long> = debtDao.getTotalBorrowedActive()

    fun getDebtsByDirection(direction: String): Flow<List<DebtEntity>> =
        debtDao.getDebtsByDirection(direction)

    fun getRepaymentsForDebt(debtId: Long): Flow<List<DebtRepaymentEntity>> =
        debtDao.getRepaymentsForDebt(debtId)

    suspend fun saveDebt(debt: DebtEntity): Long {
        return if (debt.id == 0L) {
            debtDao.insertDebt(debt)
        } else {
            debtDao.updateDebt(debt)
            debt.id
        }
    }

    suspend fun recordRepayment(debtId: Long, amountPaisa: Long, note: String = "") {
        val repayment = DebtRepaymentEntity(
            debtId = debtId,
            amountPaisa = amountPaisa,
            dateEpochMillis = System.currentTimeMillis(),
            note = note
        )
        debtDao.recordRepaymentAndUpdateDebt(repayment)
    }

    suspend fun markDebtSettled(debtId: Long) {
        val debt = debtDao.getDebtById(debtId) ?: return
        if (debt.remainingAmountPaisa > 0) {
            recordRepayment(debtId, debt.remainingAmountPaisa, "Settled in full")
        } else {
            debtDao.updateDebt(debt.copy(status = "SETTLED"))
        }
    }

    suspend fun deleteDebt(debt: DebtEntity) = debtDao.deleteDebt(debt)

    // Bill Reminders
    val allReminders: Flow<List<BillReminderEntity>> = billDao.getAllReminders()
    val pendingReminders: Flow<List<BillReminderEntity>> = billDao.getPendingReminders()

    suspend fun saveReminder(reminder: BillReminderEntity): Long {
        return if (reminder.id == 0L) {
            billDao.insertReminder(reminder)
        } else {
            billDao.updateReminder(reminder)
            reminder.id
        }
    }

    suspend fun markReminderPaid(reminder: BillReminderEntity) {
        val updated = reminder.copy(
            isPaidThisCycle = true,
            lastPaidDateEpochMillis = System.currentTimeMillis()
        )
        billDao.updateReminder(updated)

        // Also optionally record as transaction expense automatically
        val expenseTx = TransactionEntity(
            amountPaisa = reminder.amountPaisa,
            type = "EXPENSE",
            categoryId = 4L, // Utilities default
            categoryName = reminder.categoryName,
            categoryIcon = "bolt",
            categoryColor = 0xFFFBC02D,
            dateEpochMillis = System.currentTimeMillis(),
            paymentMode = "UPI",
            note = "Bill: ${reminder.title}"
        )
        txDao.insertTransaction(expenseTx)
    }

    suspend fun deleteReminderById(id: Long) = billDao.deleteById(id)

    // Savings Goals
    val allGoals: Flow<List<SavingsGoalEntity>> = goalDao.getAllGoals()

    suspend fun saveGoal(goal: SavingsGoalEntity): Long {
        return if (goal.id == 0L) {
            goalDao.insertGoal(goal)
        } else {
            goalDao.updateGoal(goal)
            goal.id
        }
    }

    suspend fun addContributionToGoal(goal: SavingsGoalEntity, contributionPaisa: Long) {
        val updated = goal.copy(
            savedAmountPaisa = goal.savedAmountPaisa + contributionPaisa
        )
        goalDao.updateGoal(updated)
    }

    suspend fun deleteGoalById(id: Long) = goalDao.deleteById(id)

    // Recurring Transactions Processor
    suspend fun processRecurringTransactions() {
        val recurring = txDao.getRecurringTransactions()
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        for (tx in recurring) {
            val freq = tx.recurringFrequency ?: continue
            val lastCreated = tx.dateEpochMillis
            calendar.timeInMillis = lastCreated

            val isDue = when (freq) {
                "DAILY" -> {
                    now - lastCreated >= 24 * 60 * 60 * 1000L
                }
                "WEEKLY" -> {
                    now - lastCreated >= 7 * 24 * 60 * 60 * 1000L
                }
                "MONTHLY" -> {
                    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                    val txMonth = calendar.get(Calendar.MONTH)
                    currentMonth != txMonth
                }
                else -> false
            }

            if (isDue) {
                val newTx = tx.copy(
                    id = 0,
                    dateEpochMillis = now,
                    createdAt = now,
                    isRecurring = false // Generated child is not a template
                )
                txDao.insertTransaction(newTx)
                // Update parent timestamp to prevent repeated generation
                txDao.updateTransaction(tx.copy(dateEpochMillis = now))
            }
        }
    }

    suspend fun clearAllData() {
        txDao.clearAll()
        budgetDao.clearAll()
        debtDao.clearAllRepayments()
        debtDao.clearAllDebts()
        billDao.clearAll()
        goalDao.clearAll()
    }
}

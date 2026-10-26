package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateEpochMillis"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"]),
        Index(value = ["paymentMode"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountPaisa: Long, // Accurate integer minor units (1 INR = 100 paisa)
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val dateEpochMillis: Long,
    val paymentMode: String, // "UPI", "CASH", "BANK_ACCOUNT", "CREDIT_CARD", "DEBIT_CARD"
    val note: String = "",
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null, // "DAILY", "WEEKLY", "MONTHLY"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "EXPENSE" or "INCOME"
    val iconName: String, // e.g. "shopping_cart", "restaurant", "home", etc.
    val colorHex: Long,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["monthYear", "categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthYear: String, // e.g. "2026-10" or "ALL"
    val categoryId: Long? = null, // null = overall monthly budget limit
    val categoryName: String? = null,
    val monthlyLimitPaisa: Long,
    val warningThresholdPercent: Int = 80 // e.g. 80%
)

@Entity(
    tableName = "debts",
    indices = [Index(value = ["status"]), Index(value = ["direction"])]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val phoneNumber: String? = null,
    val direction: String, // "LENT" (You will receive) or "BORROWED" (You owe)
    val initialAmountPaisa: Long,
    val remainingAmountPaisa: Long,
    val dueDateEpochMillis: Long? = null,
    val notes: String = "",
    val status: String = "ACTIVE", // "ACTIVE" or "SETTLED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "debt_repayments",
    indices = [Index(value = ["debtId"])],
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DebtRepaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val amountPaisa: Long,
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "bill_reminders",
    indices = [Index(value = ["isPaidThisCycle"])]
)
data class BillReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amountPaisa: Long,
    val dueDayOfMonth: Int, // 1..31
    val categoryName: String = "Utilities",
    val isPaidThisCycle: Boolean = false,
    val lastPaidDateEpochMillis: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmountPaisa: Long,
    val savedAmountPaisa: Long = 0L,
    val targetDateEpochMillis: Long? = null,
    val colorHex: Long = 0xFF0D6B42
)

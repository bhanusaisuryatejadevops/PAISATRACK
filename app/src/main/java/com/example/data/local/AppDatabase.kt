package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BillReminderDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.DebtDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtRepaymentEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        DebtEntity::class,
        DebtRepaymentEntity::class,
        BillReminderEntity::class,
        SavingsGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun billReminderDao(): BillReminderDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paisatrack.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                // Expenses (Indian context)
                CategoryEntity(name = "Food & Groceries", type = "EXPENSE", iconName = "restaurant", colorHex = 0xFFE65100, isDefault = true),
                CategoryEntity(name = "Rent & Housing", type = "EXPENSE", iconName = "home", colorHex = 0xFF5D4037, isDefault = true),
                CategoryEntity(name = "Travel & Petrol", type = "EXPENSE", iconName = "directions_car", colorHex = 0xFF0277BD, isDefault = true),
                CategoryEntity(name = "Bills & Utilities", type = "EXPENSE", iconName = "bolt", colorHex = 0xFFFBC02D, isDefault = true),
                CategoryEntity(name = "Shopping & Clothes", type = "EXPENSE", iconName = "shopping_bag", colorHex = 0xFFC2185B, isDefault = true),
                CategoryEntity(name = "Health & Medical", type = "EXPENSE", iconName = "local_hospital", colorHex = 0xFFD32F2F, isDefault = true),
                CategoryEntity(name = "Education & Fees", type = "EXPENSE", iconName = "school", colorHex = 0xFF512DA8, isDefault = true),
                CategoryEntity(name = "Entertainment & OTT", type = "EXPENSE", iconName = "movie", colorHex = 0xFF7B1FA2, isDefault = true),
                CategoryEntity(name = "EMI & Loans", type = "EXPENSE", iconName = "account_balance", colorHex = 0xFF455A64, isDefault = true),
                CategoryEntity(name = "Personal & Household", type = "EXPENSE", iconName = "cleaning_services", colorHex = 0xFF388E3C, isDefault = true),
                CategoryEntity(name = "Gifts & Donations", type = "EXPENSE", iconName = "card_giftcard", colorHex = 0xFFE91E63, isDefault = true),
                CategoryEntity(name = "Other Expenses", type = "EXPENSE", iconName = "category", colorHex = 0xFF607D8B, isDefault = true),

                // Incomes
                CategoryEntity(name = "Salary", type = "INCOME", iconName = "payments", colorHex = 0xFF198754, isDefault = true),
                CategoryEntity(name = "Business & Freelance", type = "INCOME", iconName = "storefront", colorHex = 0xFF00796B, isDefault = true),
                CategoryEntity(name = "Investments & Returns", type = "INCOME", iconName = "trending_up", colorHex = 0xFF2E7D32, isDefault = true),
                CategoryEntity(name = "Udhaar Return / Loans", type = "INCOME", iconName = "currency_exchange", colorHex = 0xFF0288D1, isDefault = true),
                CategoryEntity(name = "Bonus & Gifts", type = "INCOME", iconName = "redeem", colorHex = 0xFFFF8F00, isDefault = true),
                CategoryEntity(name = "Other Income", type = "INCOME", iconName = "add_circle", colorHex = 0xFF4CAF50, isDefault = true)
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}

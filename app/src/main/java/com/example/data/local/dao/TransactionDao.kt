package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val totalPaisa: Long
)

data class PaymentModeTotal(
    val paymentMode: String,
    val totalPaisa: Long
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startDate AND :endDate ORDER BY dateEpochMillis DESC, id DESC")
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis BETWEEN :startDate AND :endDate")
    fun getTotalIncomeInRange(startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :startDate AND :endDate")
    fun getTotalExpenseInRange(startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :startDate AND :endDate")
    suspend fun getExpenseSumInRange(startDate: Long, endDate: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis BETWEEN :startDate AND :endDate")
    suspend fun getIncomeSumInRange(startDate: Long, endDate: Long): Long

    @Query("""
        SELECT categoryName, categoryIcon, categoryColor, SUM(amountPaisa) as totalPaisa 
        FROM transactions 
        WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :startDate AND :endDate 
        GROUP BY categoryName 
        ORDER BY totalPaisa DESC
    """)
    fun getCategoryExpenseBreakdown(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT paymentMode, SUM(amountPaisa) as totalPaisa 
        FROM transactions 
        WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :startDate AND :endDate 
        GROUP BY paymentMode 
        ORDER BY totalPaisa DESC
    """)
    fun getPaymentModeBreakdown(startDate: Long, endDate: Long): Flow<List<PaymentModeTotal>>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1")
    suspend fun getRecurringTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis ASC")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

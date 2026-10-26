package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtRepaymentEntity
import kotlinx.coroutines.flow.Flow

data class DebtWithRepayments(
    val debt: DebtEntity,
    val repayments: List<DebtRepaymentEntity>
)

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY status ASC, createdAt DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE direction = :direction ORDER BY status ASC, createdAt DESC")
    fun getDebtsByDirection(direction: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Query("SELECT COALESCE(SUM(remainingAmountPaisa), 0) FROM debts WHERE direction = 'LENT' AND status = 'ACTIVE'")
    fun getTotalLentActive(): Flow<Long>

    @Query("SELECT COALESCE(SUM(remainingAmountPaisa), 0) FROM debts WHERE direction = 'BORROWED' AND status = 'ACTIVE'")
    fun getTotalBorrowedActive(): Flow<Long>

    @Query("SELECT * FROM debt_repayments WHERE debtId = :debtId ORDER BY dateEpochMillis DESC")
    fun getRepaymentsForDebt(debtId: Long): Flow<List<DebtRepaymentEntity>>

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsSnapshot(): List<DebtEntity>

    @Query("SELECT * FROM debt_repayments")
    suspend fun getAllRepaymentsSnapshot(): List<DebtRepaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: DebtRepaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayments(repayments: List<DebtRepaymentEntity>)

    @Transaction
    suspend fun recordRepaymentAndUpdateDebt(repayment: DebtRepaymentEntity) {
        val debt = getDebtById(repayment.debtId) ?: return
        val newRemaining = (debt.remainingAmountPaisa - repayment.amountPaisa).coerceAtLeast(0L)
        val newStatus = if (newRemaining == 0L) "SETTLED" else "ACTIVE"
        insertRepayment(repayment)
        updateDebt(debt.copy(remainingAmountPaisa = newRemaining, status = newStatus))
    }

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("DELETE FROM debts")
    suspend fun clearAllDebts()

    @Query("DELETE FROM debt_repayments")
    suspend fun clearAllRepayments()
}

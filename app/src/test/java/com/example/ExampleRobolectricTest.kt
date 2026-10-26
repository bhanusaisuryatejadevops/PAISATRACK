package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtRepaymentEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppNameStringResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("PaisaTrack", appName)
    }

    @Test
    fun testTransactionDaoInsertionAndSum() {
        runBlocking {
            val tx1 = TransactionEntity(
                amountPaisa = 150000L, // ₹1,500.00
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Food & Groceries",
                categoryIcon = "restaurant",
                categoryColor = 0xFFE65100,
                dateEpochMillis = 1000L,
                paymentMode = "UPI"
            )
            val tx2 = TransactionEntity(
                amountPaisa = 250000L, // ₹2,500.00
                type = "EXPENSE",
                categoryId = 2,
                categoryName = "Travel",
                categoryIcon = "directions_car",
                categoryColor = 0xFF0277BD,
                dateEpochMillis = 2000L,
                paymentMode = "CASH"
            )

            db.transactionDao().insertTransaction(tx1)
            db.transactionDao().insertTransaction(tx2)

            val sum = db.transactionDao().getExpenseSumInRange(0L, 5000L)
            assertEquals(400000L, sum)
        }
    }

    @Test
    fun testDebtRepaymentAndSettlement() {
        runBlocking {
            val debt = DebtEntity(
                personName = "Rahul Sharma",
                direction = "LENT",
                initialAmountPaisa = 500000L, // ₹5,000
                remainingAmountPaisa = 500000L
            )
            val debtId = db.debtDao().insertDebt(debt)

            // Record partial repayment of ₹2,000
            val repayment = DebtRepaymentEntity(
                debtId = debtId,
                amountPaisa = 200000L,
                dateEpochMillis = System.currentTimeMillis(),
                note = "UPI repayment"
            )
            db.debtDao().recordRepaymentAndUpdateDebt(repayment)

            val updatedDebt = db.debtDao().getDebtById(debtId)
            assertNotNull(updatedDebt)
            assertEquals(300000L, updatedDebt!!.remainingAmountPaisa)
            assertEquals("ACTIVE", updatedDebt.status)

            // Record remaining repayment of ₹3,000
            val finalRepayment = DebtRepaymentEntity(
                debtId = debtId,
                amountPaisa = 300000L,
                dateEpochMillis = System.currentTimeMillis(),
                note = "Final settlement"
            )
            db.debtDao().recordRepaymentAndUpdateDebt(finalRepayment)

            val settledDebt = db.debtDao().getDebtById(debtId)
            assertNotNull(settledDebt)
            assertEquals(0L, settledDebt!!.remainingAmountPaisa)
            assertEquals("SETTLED", settledDebt.status)
        }
    }

    @Test
    fun testEncryptedBackupAndRestore() {
        runBlocking {
            // Insert sample transaction
            val tx = TransactionEntity(
                amountPaisa = 99900L,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Shopping",
                categoryIcon = "shopping_bag",
                categoryColor = 0xFFC2185B,
                dateEpochMillis = 1000L,
                paymentMode = "UPI",
                note = "Test item"
            )
            db.transactionDao().insertTransaction(tx)

            val password = "SuperSecretPassword123"
            val outStream = ByteArrayOutputStream()
            BackupManager.createEncryptedBackup(db, password, outStream)

            val backupBytes = outStream.toByteArray()
            assertTrue(backupBytes.size > 60) // Magic header + salt + IV + ciphertext

            // Clear db
            db.transactionDao().clearAll()
            assertEquals(0, db.transactionDao().getAllTransactionsSnapshot().size)

            // Restore with correct password
            val inStream = ByteArrayInputStream(backupBytes)
            BackupManager.restoreEncryptedBackup(db, password, inStream, BackupManager.RestoreMode.REPLACE_ALL)

            val restoredList = db.transactionDao().getAllTransactionsSnapshot()
            assertEquals(1, restoredList.size)
            assertEquals(99900L, restoredList[0].amountPaisa)
            assertEquals("Shopping", restoredList[0].categoryName)

            // Verify restore with wrong password fails
            val wrongInStream = ByteArrayInputStream(backupBytes)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    BackupManager.restoreEncryptedBackup(db, "WrongPassword!", wrongInStream, BackupManager.RestoreMode.REPLACE_ALL)
                }
            }
        }
    }
}

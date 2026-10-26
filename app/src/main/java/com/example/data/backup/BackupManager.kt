package com.example.data.backup

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtRepaymentEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private val MAGIC_HEADER = "PAISATRACK_ENC01".toByteArray(StandardCharsets.US_ASCII)
    private const val SALT_LENGTH = 32
    private const val IV_LENGTH = 12 // Standard AES-GCM IV length
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_BIT_LENGTH = 256
    private const val GCM_TAG_BIT_LENGTH = 128

    enum class RestoreMode {
        REPLACE_ALL,
        MERGE
    }

    suspend fun createEncryptedBackup(
        database: AppDatabase,
        password: String,
        outputStream: OutputStream
    ) {
        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("timestamp", System.currentTimeMillis())
        rootJson.put("app", "PaisaTrack")

        // 1. Transactions
        val transactions = database.transactionDao().getAllTransactionsSnapshot()
        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("amountPaisa", tx.amountPaisa)
                put("type", tx.type)
                put("categoryId", tx.categoryId)
                put("categoryName", tx.categoryName)
                put("categoryIcon", tx.categoryIcon)
                put("categoryColor", tx.categoryColor)
                put("dateEpochMillis", tx.dateEpochMillis)
                put("paymentMode", tx.paymentMode)
                put("note", tx.note)
                put("isRecurring", tx.isRecurring)
                put("recurringFrequency", tx.recurringFrequency ?: "")
                put("createdAt", tx.createdAt)
            }
            txArray.put(obj)
        }
        rootJson.put("transactions", txArray)

        // 2. Categories
        val categories = database.categoryDao().getAllCategoriesSnapshot()
        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("type", cat.type)
                put("iconName", cat.iconName)
                put("colorHex", cat.colorHex)
                put("isDefault", cat.isDefault)
            }
            catArray.put(obj)
        }
        rootJson.put("categories", catArray)

        // 3. Budgets
        val budgets = database.budgetDao().getAllBudgetsSnapshot()
        val budgetArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("monthYear", b.monthYear)
                put("categoryId", b.categoryId ?: -1L)
                put("categoryName", b.categoryName ?: "")
                put("monthlyLimitPaisa", b.monthlyLimitPaisa)
                put("warningThresholdPercent", b.warningThresholdPercent)
            }
            budgetArray.put(obj)
        }
        rootJson.put("budgets", budgetArray)

        // 4. Debts
        val debts = database.debtDao().getAllDebtsSnapshot()
        val debtArray = JSONArray()
        for (d in debts) {
            val obj = JSONObject().apply {
                put("id", d.id)
                put("personName", d.personName)
                put("phoneNumber", d.phoneNumber ?: "")
                put("direction", d.direction)
                put("initialAmountPaisa", d.initialAmountPaisa)
                put("remainingAmountPaisa", d.remainingAmountPaisa)
                put("dueDateEpochMillis", d.dueDateEpochMillis ?: -1L)
                put("notes", d.notes)
                put("status", d.status)
                put("createdAt", d.createdAt)
            }
            debtArray.put(obj)
        }
        rootJson.put("debts", debtArray)

        // 5. Debt Repayments
        val repayments = database.debtDao().getAllRepaymentsSnapshot()
        val repayArray = JSONArray()
        for (r in repayments) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("debtId", r.debtId)
                put("amountPaisa", r.amountPaisa)
                put("dateEpochMillis", r.dateEpochMillis)
                put("note", r.note)
            }
            repayArray.put(obj)
        }
        rootJson.put("debt_repayments", repayArray)

        // 6. Bill Reminders
        val reminders = database.billReminderDao().getAllRemindersSnapshot()
        val remArray = JSONArray()
        for (rem in reminders) {
            val obj = JSONObject().apply {
                put("id", rem.id)
                put("title", rem.title)
                put("amountPaisa", rem.amountPaisa)
                put("dueDayOfMonth", rem.dueDayOfMonth)
                put("categoryName", rem.categoryName)
                put("isPaidThisCycle", rem.isPaidThisCycle)
                put("lastPaidDateEpochMillis", rem.lastPaidDateEpochMillis ?: -1L)
                put("notes", rem.notes)
            }
            remArray.put(obj)
        }
        rootJson.put("bill_reminders", remArray)

        // 7. Savings Goals
        val goals = database.savingsGoalDao().getAllGoalsSnapshot()
        val goalArray = JSONArray()
        for (g in goals) {
            val obj = JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("targetAmountPaisa", g.targetAmountPaisa)
                put("savedAmountPaisa", g.savedAmountPaisa)
                put("targetDateEpochMillis", g.targetDateEpochMillis ?: -1L)
                put("colorHex", g.colorHex)
            }
            goalArray.put(obj)
        }
        rootJson.put("savings_goals", goalArray)

        val jsonBytes = rootJson.toString().toByteArray(StandardCharsets.UTF_8)

        // Cryptography: PBKDF2 + AES-GCM
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BIT_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(jsonBytes)

        outputStream.use { out ->
            out.write(MAGIC_HEADER)
            out.write(salt)
            out.write(iv)
            out.write(cipherText)
            out.flush()
        }
    }

    suspend fun restoreEncryptedBackup(
        database: AppDatabase,
        password: String,
        inputStream: InputStream,
        restoreMode: RestoreMode
    ) {
        val allBytes = inputStream.use { it.readBytes() }
        val headerLen = MAGIC_HEADER.size
        val minLen = headerLen + SALT_LENGTH + IV_LENGTH + 16 // At least 16 bytes auth tag

        if (allBytes.size < minLen) {
            throw IllegalArgumentException("Invalid backup file: File size is too small or truncated.")
        }

        // Validate Magic Header
        for (i in 0 until headerLen) {
            if (allBytes[i] != MAGIC_HEADER[i]) {
                throw IllegalArgumentException("Invalid backup file format. Magic header mismatch.")
            }
        }

        val salt = allBytes.copyOfRange(headerLen, headerLen + SALT_LENGTH)
        val iv = allBytes.copyOfRange(headerLen + SALT_LENGTH, headerLen + SALT_LENGTH + IV_LENGTH)
        val cipherText = allBytes.copyOfRange(headerLen + SALT_LENGTH + IV_LENGTH, allBytes.size)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BIT_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = try {
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            throw IllegalArgumentException("Incorrect password or corrupted backup file: ${e.message}", e)
        }

        val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)
        val rootJson = JSONObject(jsonString)

        if (restoreMode == RestoreMode.REPLACE_ALL) {
            database.transactionDao().clearAll()
            database.budgetDao().clearAll()
            database.debtDao().clearAllRepayments()
            database.debtDao().clearAllDebts()
            database.billReminderDao().clearAll()
            database.savingsGoalDao().clearAll()
        }

        // Parse & insert Categories
        val catArray = rootJson.optJSONArray("categories")
        if (catArray != null) {
            val categories = mutableListOf<CategoryEntity>()
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = if (restoreMode == RestoreMode.REPLACE_ALL) obj.optLong("id", 0) else 0,
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        iconName = obj.optString("iconName", "category"),
                        colorHex = obj.optLong("colorHex", 0xFF607D8B),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
            database.categoryDao().insertCategories(categories)
        }

        // Parse & insert Transactions
        val txArray = rootJson.optJSONArray("transactions")
        if (txArray != null) {
            val transactions = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = if (restoreMode == RestoreMode.REPLACE_ALL) obj.optLong("id", 0) else 0,
                        amountPaisa = obj.getLong("amountPaisa"),
                        type = obj.getString("type"),
                        categoryId = obj.optLong("categoryId", 1),
                        categoryName = obj.getString("categoryName"),
                        categoryIcon = obj.optString("categoryIcon", "payments"),
                        categoryColor = obj.optLong("categoryColor", 0xFF198754),
                        dateEpochMillis = obj.getLong("dateEpochMillis"),
                        paymentMode = obj.optString("paymentMode", "UPI"),
                        note = obj.optString("note", ""),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        recurringFrequency = obj.optString("recurringFrequency").takeIf { it.isNotBlank() },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            database.transactionDao().insertTransactions(transactions)
        }

        // Parse & insert Budgets
        val budgetArray = rootJson.optJSONArray("budgets")
        if (budgetArray != null) {
            val budgets = mutableListOf<BudgetEntity>()
            for (i in 0 until budgetArray.length()) {
                val obj = budgetArray.getJSONObject(i)
                val catId = obj.optLong("categoryId", -1L).takeIf { it != -1L }
                val catName = obj.optString("categoryName").takeIf { it.isNotBlank() }
                budgets.add(
                    BudgetEntity(
                        id = if (restoreMode == RestoreMode.REPLACE_ALL) obj.optLong("id", 0) else 0,
                        monthYear = obj.getString("monthYear"),
                        categoryId = catId,
                        categoryName = catName,
                        monthlyLimitPaisa = obj.getLong("monthlyLimitPaisa"),
                        warningThresholdPercent = obj.optInt("warningThresholdPercent", 80)
                    )
                )
            }
            database.budgetDao().insertBudgets(budgets)
        }

        // Parse & insert Debts
        val debtArray = rootJson.optJSONArray("debts")
        if (debtArray != null) {
            val debts = mutableListOf<DebtEntity>()
            for (i in 0 until debtArray.length()) {
                val obj = debtArray.getJSONObject(i)
                debts.add(
                    DebtEntity(
                        id = if (restoreMode == RestoreMode.REPLACE_ALL) obj.optLong("id", 0) else 0,
                        personName = obj.getString("personName"),
                        phoneNumber = obj.optString("phoneNumber").takeIf { it.isNotBlank() },
                        direction = obj.getString("direction"),
                        initialAmountPaisa = obj.getLong("initialAmountPaisa"),
                        remainingAmountPaisa = obj.getLong("remainingAmountPaisa"),
                        dueDateEpochMillis = obj.optLong("dueDateEpochMillis", -1L).takeIf { it != -1L },
                        notes = obj.optString("notes", ""),
                        status = obj.optString("status", "ACTIVE"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            database.debtDao().insertDebts(debts)
        }

        // Parse & insert Debt Repayments
        val repayArray = rootJson.optJSONArray("debt_repayments")
        if (repayArray != null && restoreMode == RestoreMode.REPLACE_ALL) {
            val repayments = mutableListOf<DebtRepaymentEntity>()
            for (i in 0 until repayArray.length()) {
                val obj = repayArray.getJSONObject(i)
                repayments.add(
                    DebtRepaymentEntity(
                        id = obj.optLong("id", 0),
                        debtId = obj.getLong("debtId"),
                        amountPaisa = obj.getLong("amountPaisa"),
                        dateEpochMillis = obj.optLong("dateEpochMillis", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }
            database.debtDao().insertRepayments(repayments)
        }

        // Parse & insert Bill Reminders
        val remArray = rootJson.optJSONArray("bill_reminders")
        if (remArray != null) {
            val reminders = mutableListOf<BillReminderEntity>()
            for (i in 0 until remArray.length()) {
                val obj = remArray.getJSONObject(i)
                reminders.add(
                    BillReminderEntity(
                        id = if (restoreMode == RestoreMode.REPLACE_ALL) obj.optLong("id", 0) else 0,
                        title = obj.getString("title"),
                        amountPaisa = obj.getLong("amountPaisa"),
                        dueDayOfMonth = obj.getInt("dueDayOfMonth"),
                        categoryName = obj.optString("categoryName", "Utilities"),
                        isPaidThisCycle = obj.optBoolean("isPaidThisCycle", false),
                        lastPaidDateEpochMillis = obj.optLong("lastPaidDateEpochMillis", -1L).takeIf { it != -1L },
                        notes = obj.optString("notes", "")
                    )
                )
            }
            database.billReminderDao().insertReminders(reminders)
        }

        // Parse & insert Savings Goals
        val goalArray = rootJson.optJSONArray("savings_goals")
        if (goalArray != null) {
            val goals = mutableListOf<SavingsGoalEntity>()
            for (i in 0 until goalArray.length()) {
                val obj = goalArray.getJSONObject(i)
                goals.add(
                    SavingsGoalEntity(
                        id = if (restoreMode == RestoreMode.REPLACE_ALL) obj.optLong("id", 0) else 0,
                        title = obj.getString("title"),
                        targetAmountPaisa = obj.getLong("targetAmountPaisa"),
                        savedAmountPaisa = obj.optLong("savedAmountPaisa", 0L),
                        targetDateEpochMillis = obj.optLong("targetDateEpochMillis", -1L).takeIf { it != -1L },
                        colorHex = obj.optLong("colorHex", 0xFF0D6B42)
                    )
                )
            }
            database.savingsGoalDao().insertGoals(goals)
        }
    }
}

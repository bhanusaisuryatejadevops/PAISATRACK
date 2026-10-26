package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.common.getCategoryIconVector
import com.example.ui.theme.PaisaExpenseRed
import com.example.ui.theme.PaisaIncomeGreen
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    existingTransaction: TransactionEntity? = null,
    categories: List<CategoryEntity>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(existingTransaction?.type ?: "EXPENSE") }
    var amountInput by remember {
        mutableStateOf(
            if (existingTransaction != null) {
                val rupees = existingTransaction.amountPaisa / 100.0
                if (rupees % 1.0 == 0.0) rupees.toLong().toString() else rupees.toString()
            } else ""
        )
    }

    val availableCategories = categories.filter { it.type == type }
    var selectedCategory by remember {
        mutableStateOf(
            availableCategories.firstOrNull { it.id == existingTransaction?.categoryId }
                ?: availableCategories.firstOrNull()
        )
    }

    var paymentMode by remember { mutableStateOf(existingTransaction?.paymentMode ?: "UPI") }
    var note by remember { mutableStateOf(existingTransaction?.note ?: "") }
    var isRecurring by remember { mutableStateOf(existingTransaction?.isRecurring ?: false) }
    var recurringFrequency by remember { mutableStateOf(existingTransaction?.recurringFrequency ?: "MONTHLY") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val paymentModes = listOf("UPI", "CASH", "BANK_ACCOUNT", "CREDIT_CARD", "DEBIT_CARD")
    val quickAddAmounts = listOf(50, 100, 200, 500, 1000, 2000)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTransaction == null) {
                        if (type == "EXPENSE") AppStrings.get("add_expense", language) else AppStrings.get("add_income", language)
                    } else {
                        AppStrings.get("edit", language) + " " + AppStrings.get("transactions", language)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row {
                    if (existingTransaction != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(existingTransaction)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = AppStrings.get("delete", language),
                                tint = PaisaExpenseRed
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = AppStrings.get("cancel", language))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Type Toggle: Expense vs Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                // Expense Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (type == "EXPENSE") PaisaExpenseRed else Color.Transparent)
                        .clickable {
                            type = "EXPENSE"
                            selectedCategory = categories.firstOrNull { it.type == "EXPENSE" }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("type_expense_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("filter_expense", language),
                        fontWeight = FontWeight.Bold,
                        color = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Income Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (type == "INCOME") PaisaIncomeGreen else Color.Transparent)
                        .clickable {
                            type = "INCOME"
                            selectedCategory = categories.firstOrNull { it.type == "INCOME" }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("type_income_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("filter_income", language),
                        fontWeight = FontWeight.Bold,
                        color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        amountInput = input
                        errorMessage = null
                    }
                },
                label = { Text(AppStrings.get("amount", language)) },
                prefix = {
                    Text(
                        text = "₹ ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = PaisaExpenseRed) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input")
            )

            // Quick Add Rupee Chips
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickAddAmounts) { addVal ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            val current = amountInput.toDoubleOrNull() ?: 0.0
                            val updated = (current + addVal).toLong()
                            amountInput = updated.toString()
                        }
                    ) {
                        Text(
                            text = "+₹$addVal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Picker
            Text(
                text = AppStrings.get("category", language),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(availableCategories) { cat ->
                    val isSelected = selectedCategory?.id == cat.id
                    val catColor = Color(cat.colorHex)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat }
                            .padding(6.dp)
                            .testTag("category_chip_${cat.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) catColor else catColor.copy(alpha = 0.15f))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIconVector(cat.iconName),
                                contentDescription = cat.name,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Mode Chips
            Text(
                text = AppStrings.get("payment_method", language),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(paymentModes) { mode ->
                    val isSelected = paymentMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { paymentMode = mode },
                        label = { Text(mode) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("payment_chip_$mode")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(AppStrings.get("notes", language)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_notes_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recurring Transaction Switch
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recurring Schedule",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Auto-creates record at each interval",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            modifier = Modifier.testTag("recurring_switch")
                        )
                    }

                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DAILY", "WEEKLY", "MONTHLY").forEach { freq ->
                                FilterChip(
                                    selected = recurringFrequency == freq,
                                    onClick = { recurringFrequency = freq },
                                    label = { Text(freq) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val paisa = CurrencyFormatter.parseInrToPaisa(amountInput)
                    if (paisa <= 0L) {
                        errorMessage = "Please enter a valid amount greater than ₹0"
                        return@Button
                    }
                    val cat = selectedCategory
                    if (cat == null) {
                        errorMessage = "Please select a category"
                        return@Button
                    }

                    val tx = TransactionEntity(
                        id = existingTransaction?.id ?: 0L,
                        amountPaisa = paisa,
                        type = type,
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryIcon = cat.iconName,
                        categoryColor = cat.colorHex,
                        dateEpochMillis = existingTransaction?.dateEpochMillis ?: System.currentTimeMillis(),
                        paymentMode = paymentMode,
                        note = note.trim(),
                        isRecurring = isRecurring,
                        recurringFrequency = if (isRecurring) recurringFrequency else null,
                        createdAt = existingTransaction?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(tx)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_transaction_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "EXPENSE") MaterialTheme.colorScheme.primary else PaisaIncomeGreen
                )
            ) {
                Text(
                    text = AppStrings.get("save", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.DebtEntity
import com.example.ui.theme.PaisaExpenseRed
import com.example.ui.theme.PaisaIncomeGreen
import com.example.ui.theme.PaisaWarningAmber
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter

enum class SectionTab {
    KHATA,
    BILLS
}

@Composable
fun DebtsAndBillsScreen(
    language: AppLanguage,
    debts: List<DebtEntity>,
    totalLentPaisa: Long,
    totalBorrowedPaisa: Long,
    billReminders: List<BillReminderEntity>,
    onSaveDebt: (DebtEntity) -> Unit,
    onRecordDebtRepayment: (Long, Long, String) -> Unit,
    onSettleDebt: (Long) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    onSaveBillReminder: (BillReminderEntity) -> Unit,
    onMarkBillPaid: (BillReminderEntity) -> Unit,
    onDeleteBillReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf(SectionTab.KHATA) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var selectedDebtForRepayment by remember { mutableStateOf<DebtEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentSection == SectionTab.KHATA) showAddDebtDialog = true
                    else showAddBillDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_khata_bill")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (currentSection == SectionTab.KHATA) "Add Debt" else "Add Bill"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Section Segmented Bar: Khata / Udhaar vs Bills & EMI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentSection == SectionTab.KHATA) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { currentSection = SectionTab.KHATA }
                        .padding(vertical = 10.dp)
                        .testTag("tab_section_khata"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("debts_khata", language),
                        fontWeight = FontWeight.Bold,
                        color = if (currentSection == SectionTab.KHATA) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentSection == SectionTab.BILLS) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { currentSection = SectionTab.BILLS }
                        .padding(vertical = 10.dp)
                        .testTag("tab_section_bills"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("bills_emi", language),
                        fontWeight = FontWeight.Bold,
                        color = if (currentSection == SectionTab.BILLS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (currentSection == SectionTab.KHATA) {
                // Khata / Debts View
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Summary Balance Cards: You Lent vs You Borrowed
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Lent (Receive)
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = PaisaIncomeGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "You Lent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = CurrencyFormatter.formatPaisaToInr(totalLentPaisa),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PaisaIncomeGreen
                                        )
                                    )
                                    Text(
                                        text = "To Receive",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Borrowed (Pay)
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = PaisaExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "You Borrowed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = CurrencyFormatter.formatPaisaToInr(totalBorrowedPaisa),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PaisaExpenseRed
                                        )
                                    )
                                    Text(
                                        text = "To Pay",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    if (debts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No active khata or loan records. Tap + to record money given or taken.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(debts, key = { it.id }) { debt ->
                            val isLent = debt.direction == "LENT"
                            val isSettled = debt.status == "SETTLED"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isLent) PaisaIncomeGreen.copy(alpha = 0.15f)
                                                        else PaisaExpenseRed.copy(alpha = 0.15f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (isLent) PaisaIncomeGreen else PaisaExpenseRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = debt.personName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = if (isLent) "Lent • You will get" else "Borrowed • You owe",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = if (isLent) PaisaIncomeGreen else PaisaExpenseRed
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = CurrencyFormatter.formatPaisaToInr(debt.remainingAmountPaisa),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSettled) MaterialTheme.colorScheme.outline
                                                    else if (isLent) PaisaIncomeGreen else PaisaExpenseRed
                                                )
                                            )
                                            if (isSettled) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PaisaIncomeGreen.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "SETTLED",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = PaisaIncomeGreen,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (debt.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = debt.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    if (!isSettled) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { selectedDebtForRepayment = debt },
                                                modifier = Modifier.testTag("btn_repay_${debt.id}")
                                            ) {
                                                Text(AppStrings.get("record_repayment", language), fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { onSettleDebt(debt.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.testTag("btn_settle_${debt.id}")
                                            ) {
                                                Text(AppStrings.get("mark_settled", language), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            } else {
                // Bills & EMI Reminders View
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (billReminders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No bill reminders set. Add Electricity, Rent, Wi-Fi, or EMI reminders!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(billReminders, key = { it.id }) { bill ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (bill.isPaidThisCycle) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (bill.isPaidThisCycle) PaisaIncomeGreen.copy(alpha = 0.15f)
                                                    else PaisaWarningAmber.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (bill.isPaidThisCycle) Icons.Default.CheckCircle else Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = if (bill.isPaidThisCycle) PaisaIncomeGreen else PaisaWarningAmber,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = bill.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Due: Day ${bill.dueDayOfMonth} of month • ${CurrencyFormatter.formatPaisaToInr(bill.amountPaisa)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!bill.isPaidThisCycle) {
                                            Button(
                                                onClick = { onMarkBillPaid(bill) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.testTag("btn_pay_bill_${bill.id}")
                                            ) {
                                                Text(AppStrings.get("mark_paid", language), fontSize = 11.sp)
                                            }
                                        } else {
                                            Text(
                                                text = "PAID",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = PaisaIncomeGreen
                                                )
                                            )
                                        }
                                        IconButton(onClick = { onDeleteBillReminder(bill.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete bill",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Add Debt Dialog
    if (showAddDebtDialog) {
        var personName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var direction by remember { mutableStateOf("LENT") } // LENT or BORROWED
        var amountInput by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text(AppStrings.get("add_debt", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Lent vs Borrowed Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (direction == "LENT") PaisaIncomeGreen else Color.Transparent)
                                .clickable { direction = "LENT" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "I Lent (+)",
                                fontWeight = FontWeight.Bold,
                                color = if (direction == "LENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (direction == "BORROWED") PaisaExpenseRed else Color.Transparent)
                                .clickable { direction = "BORROWED" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "I Borrowed (-)",
                                fontWeight = FontWeight.Bold,
                                color = if (direction == "BORROWED") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text(AppStrings.get("person_name", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_debt_person")
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(AppStrings.get("amount", language)) },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_debt_amount")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(AppStrings.get("phone_number", language)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(AppStrings.get("notes", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paisa = CurrencyFormatter.parseInrToPaisa(amountInput)
                        if (personName.isNotBlank() && paisa > 0) {
                            val debt = DebtEntity(
                                personName = personName.trim(),
                                phoneNumber = phone.trim().takeIf { it.isNotBlank() },
                                direction = direction,
                                initialAmountPaisa = paisa,
                                remainingAmountPaisa = paisa,
                                notes = notes.trim()
                            )
                            onSaveDebt(debt)
                        }
                        showAddDebtDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_add_debt")
                ) {
                    Text(AppStrings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Add Bill Dialog
    if (showAddBillDialog) {
        var billTitle by remember { mutableStateOf("") }
        var billAmount by remember { mutableStateOf("") }
        var dueDayInput by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text(AppStrings.get("add_bill", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billTitle,
                        onValueChange = { billTitle = it },
                        label = { Text(AppStrings.get("bill_name", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = billAmount,
                        onValueChange = { billAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(AppStrings.get("amount", language)) },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDayInput,
                        onValueChange = { dueDayInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Due Day of Month (1 - 31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paisa = CurrencyFormatter.parseInrToPaisa(billAmount)
                        val day = dueDayInput.toIntOrNull()?.coerceIn(1, 31) ?: 1
                        if (billTitle.isNotBlank() && paisa > 0) {
                            val bill = BillReminderEntity(
                                title = billTitle.trim(),
                                amountPaisa = paisa,
                                dueDayOfMonth = day
                            )
                            onSaveBillReminder(bill)
                        }
                        showAddBillDialog = false
                    }
                ) {
                    Text(AppStrings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Record Repayment Dialog
    selectedDebtForRepayment?.let { debt ->
        var repayAmountInput by remember { mutableStateOf("") }
        var repayNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedDebtForRepayment = null },
            title = { Text("Record Repayment for ${debt.personName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remaining Balance: ${CurrencyFormatter.formatPaisaToInr(debt.remainingAmountPaisa)}")
                    OutlinedTextField(
                        value = repayAmountInput,
                        onValueChange = { repayAmountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Repayment Amount") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repayNote,
                        onValueChange = { repayNote = it },
                        label = { Text("Note / Mode (e.g. GPay, Cash)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paisa = CurrencyFormatter.parseInrToPaisa(repayAmountInput)
                        if (paisa > 0) {
                            onRecordDebtRepayment(debt.id, paisa, repayNote.trim())
                        }
                        selectedDebtForRepayment = null
                    }
                ) {
                    Text("Confirm Repayment")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtForRepayment = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.common.TransactionItemRow
import com.example.ui.theme.PaisaExpenseRed
import com.example.ui.theme.PaisaIncomeGreen
import com.example.ui.viewmodel.DateRangePreset
import com.example.ui.viewmodel.TransactionFilterType
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter

@Composable
fun TransactionsScreen(
    language: AppLanguage,
    transactions: List<TransactionEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    typeFilter: TransactionFilterType,
    onTypeFilterChange: (TransactionFilterType) -> Unit,
    dateRangePreset: DateRangePreset,
    onDateRangePresetChange: (DateRangePreset) -> Unit,
    selectedPaymentMode: String?,
    onPaymentModeChange: (String?) -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalExpensePaisa = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amountPaisa }
    val totalIncomePaisa = transactions.filter { it.type == "INCOME" }.sumOf { it.amountPaisa }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("tx_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStrings.get("add_transaction", language),
                    modifier = Modifier.size(28.dp)
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

            // Title
            Text(
                text = AppStrings.get("transactions", language),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(AppStrings.get("search_hint", language), fontSize = 14.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Type Segmented Control (All, Expense, Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp)
            ) {
                TransactionFilterType.entries.forEach { fType ->
                    val isSelected = typeFilter == fType
                    val label = when (fType) {
                        TransactionFilterType.ALL -> AppStrings.get("filter_all", language)
                        TransactionFilterType.EXPENSE -> AppStrings.get("filter_expense", language)
                        TransactionFilterType.INCOME -> AppStrings.get("filter_income", language)
                    }
                    val activeColor = when (fType) {
                        TransactionFilterType.ALL -> MaterialTheme.colorScheme.primary
                        TransactionFilterType.EXPENSE -> PaisaExpenseRed
                        TransactionFilterType.INCOME -> PaisaIncomeGreen
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeColor else Color.Transparent)
                            .clickable { onTypeFilterChange(fType) }
                            .padding(vertical = 8.dp)
                            .testTag("filter_type_${fType.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date Range Presets Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DateRangePreset.entries) { preset ->
                    val label = when (preset) {
                        DateRangePreset.TODAY -> "Today"
                        DateRangePreset.THIS_WEEK -> "This Week"
                        DateRangePreset.THIS_MONTH -> "This Month"
                        DateRangePreset.THIS_YEAR -> "This Year"
                        DateRangePreset.ALL -> "All Time"
                    }
                    FilterChip(
                        selected = dateRangePreset == preset,
                        onClick = { onDateRangePresetChange(preset) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Payment Mode Filters Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedPaymentMode == null,
                        onClick = { onPaymentModeChange(null) },
                        label = { Text("All Modes", fontSize = 11.sp) }
                    )
                }
                items(listOf("UPI", "CASH", "BANK_ACCOUNT", "CREDIT_CARD")) { mode ->
                    FilterChip(
                        selected = selectedPaymentMode == mode,
                        onClick = { onPaymentModeChange(if (selectedPaymentMode == mode) null else mode) },
                        label = { Text(mode, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Summary Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${transactions.size} records found",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        if (totalIncomePaisa > 0) {
                            Text(
                                text = "+${CurrencyFormatter.formatPaisaToInr(totalIncomePaisa)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PaisaIncomeGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (totalExpensePaisa > 0) {
                            Text(
                                text = "-${CurrencyFormatter.formatPaisaToInr(totalExpensePaisa)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PaisaExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transactions LazyColumn
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("no_transactions", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItemRow(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.common.BudgetProgressBar
import com.example.ui.theme.PaisaIncomeGreen
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun BudgetsScreen(
    language: AppLanguage,
    overallBudget: BudgetEntity?,
    monthExpensePaisa: Long,
    categoryBudgets: List<BudgetEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    onSaveBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onSaveSavingsGoal: (SavingsGoalEntity) -> Unit,
    onAddContribution: (SavingsGoalEntity, Long) -> Unit,
    onDeleteSavingsGoal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalForContribution by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGoalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("budget_fab_add_goal")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStrings.get("add_goal", language)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.get("budgets", language),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            // Overall Monthly Budget Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overall_budget_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Overall Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = DateUtils.formatMonthYear(System.currentTimeMillis()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showSetBudgetDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Set Budget",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (overallBudget != null && overallBudget.monthlyLimitPaisa > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = AppStrings.get("budget_spent", language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPaisaToInr(monthExpensePaisa),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = AppStrings.get("budget_limit", language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPaisaToInr(overallBudget.monthlyLimitPaisa),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            BudgetProgressBar(
                                spentPaisa = monthExpensePaisa,
                                budgetLimitPaisa = overallBudget.monthlyLimitPaisa,
                                warningThresholdPercent = overallBudget.warningThresholdPercent
                            )
                        } else {
                            Text(
                                text = "No overall budget set for this month. Set a budget to track spending limits and get warnings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showSetBudgetDialog = true },
                                modifier = Modifier.testTag("btn_set_monthly_budget")
                            ) {
                                Text(AppStrings.get("set_budget", language))
                            }
                        }
                    }
                }
            }

            // Savings Goals Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("savings_goals", language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    TextButton(onClick = { showAddGoalDialog = true }) {
                        Text("+ " + AppStrings.get("add_goal", language))
                    }
                }
            }

            // Savings Goals Items
            if (savingsGoals.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No savings goals created yet. Set goals for emergency funds, vacations, or major purchases!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(savingsGoals, key = { it.id }) { goal ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = Color(goal.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                IconButton(onClick = { onDeleteSavingsGoal(goal.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Goal",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val progress = if (goal.targetAmountPaisa > 0) {
                                (goal.savedAmountPaisa.toFloat() / goal.targetAmountPaisa.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val percent = (progress * 100).toInt()

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PaisaIncomeGreen
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${CurrencyFormatter.formatPaisaToInr(goal.savedAmountPaisa)} of ${CurrencyFormatter.formatPaisaToInr(goal.targetAmountPaisa)} ($percent%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = { selectedGoalForContribution = goal },
                                    modifier = Modifier.testTag("btn_contribute_${goal.id}")
                                ) {
                                    Text("+ Add Money", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Set Monthly Budget Dialog
    if (showSetBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf(
                if (overallBudget != null && overallBudget.monthlyLimitPaisa > 0) {
                    (overallBudget.monthlyLimitPaisa / 100).toString()
                } else ""
            )
        }
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            title = { Text(AppStrings.get("set_budget", language)) },
            text = {
                Column {
                    Text("Enter overall monthly expense limit:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_budget_limit")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paisa = CurrencyFormatter.parseInrToPaisa(budgetInput)
                        if (paisa > 0) {
                            val newBudget = BudgetEntity(
                                id = overallBudget?.id ?: 0L,
                                monthYear = DateUtils.formatMonthKey(System.currentTimeMillis()),
                                categoryId = null,
                                categoryName = null,
                                monthlyLimitPaisa = paisa,
                                warningThresholdPercent = 80
                            )
                            onSaveBudget(newBudget)
                        }
                        showSetBudgetDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_budget")
                ) {
                    Text(AppStrings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var titleInput by remember { mutableStateOf("") }
        var targetInput by remember { mutableStateOf("") }
        var initialSavedInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text(AppStrings.get("add_goal", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text(AppStrings.get("goal_title", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(AppStrings.get("target_amount", language)) },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialSavedInput,
                        onValueChange = { initialSavedInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(AppStrings.get("saved_so_far", language)) },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = CurrencyFormatter.parseInrToPaisa(targetInput)
                        val saved = CurrencyFormatter.parseInrToPaisa(initialSavedInput)
                        if (titleInput.isNotBlank() && target > 0) {
                            val goal = SavingsGoalEntity(
                                title = titleInput.trim(),
                                targetAmountPaisa = target,
                                savedAmountPaisa = saved
                            )
                            onSaveSavingsGoal(goal)
                        }
                        showAddGoalDialog = false
                    }
                ) {
                    Text(AppStrings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Add Contribution Dialog
    selectedGoalForContribution?.let { goal ->
        var addAmountInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedGoalForContribution = null },
            title = { Text("Add Money to ${goal.title}") },
            text = {
                Column {
                    Text("How much money would you like to add to this goal?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addAmountInput,
                        onValueChange = { addAmountInput = it.filter { ch -> ch.isDigit() } },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paisa = CurrencyFormatter.parseInrToPaisa(addAmountInput)
                        if (paisa > 0) {
                            onAddContribution(goal, paisa)
                        }
                        selectedGoalForContribution = null
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedGoalForContribution = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}

package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.CategoryTotal
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.PaisaCashGreen
import com.example.ui.theme.PaisaExpenseRed
import com.example.ui.theme.PaisaIncomeGreen
import com.example.ui.theme.PaisaUpiBlue
import com.example.ui.theme.PaisaWarningAmber
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun getCategoryIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "home" -> Icons.Default.Home
        "directions_car" -> Icons.Default.DirectionsCar
        "bolt" -> Icons.Default.Bolt
        "shopping_bag" -> Icons.Default.ShoppingBag
        "local_hospital" -> Icons.Default.LocalHospital
        "school" -> Icons.Default.School
        "movie" -> Icons.Default.Movie
        "account_balance" -> Icons.Default.AccountBalance
        "cleaning_services" -> Icons.Default.CleaningServices
        "card_giftcard" -> Icons.Default.CardGiftcard
        "payments" -> Icons.Default.Payments
        "storefront" -> Icons.Default.Storefront
        "trending_up" -> Icons.Default.TrendingUp
        "currency_exchange" -> Icons.Default.CurrencyExchange
        else -> Icons.Default.Category
    }
}

@Composable
fun getPaymentModeIconVector(mode: String): ImageVector {
    return when (mode.uppercase()) {
        "UPI" -> Icons.Default.QrCode
        "CASH" -> Icons.Default.Wallet
        "BANK_ACCOUNT" -> Icons.Default.AccountBalance
        "CREDIT_CARD", "DEBIT_CARD" -> Icons.Default.CreditCard
        else -> Icons.Default.Payments
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == "INCOME"
    val iconColor = Color(transaction.categoryColor)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_row_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with tinted circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIconVector(transaction.categoryIcon),
                    contentDescription = transaction.categoryName,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Notes & Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Payment Mode Tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (transaction.paymentMode) {
                            "UPI" -> PaisaUpiBlue.copy(alpha = 0.12f)
                            "CASH" -> PaisaCashGreen.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = transaction.paymentMode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = when (transaction.paymentMode) {
                                "UPI" -> PaisaUpiBlue
                                "CASH" -> Color(0xFF0F6E46)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = DateUtils.formatDate(transaction.dateEpochMillis),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            val amountColor = if (isIncome) PaisaIncomeGreen else PaisaExpenseRed
            val prefix = if (isIncome) "+ " else "- "
            Text(
                text = prefix + CurrencyFormatter.formatPaisaToInr(
                    transaction.amountPaisa,
                    showDecimals = false,
                    showSymbol = true
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = amountColor
            )
        }
    }
}

@Composable
fun DonutChartCanvas(
    categories: List<CategoryTotal>,
    totalSpentPaisa: Long,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp
) {
    val total = if (totalSpentPaisa > 0) totalSpentPaisa else categories.sumOf { it.totalPaisa }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (categories.isEmpty() || total <= 0) {
                // Empty state gray circle
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = size.minDimension / 2 - strokeWidth.toPx(),
                    style = Stroke(width = strokeWidth.toPx())
                )
                return@Canvas
            }

            var startAngle = -90f
            val canvasSize = size.minDimension
            val radius = canvasSize / 2 - strokeWidth.toPx()
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)

            categories.forEach { item ->
                val sweepAngle = (item.totalPaisa.toFloat() / total.toFloat()) * 360f
                if (sweepAngle > 0f) {
                    drawArc(
                        color = Color(item.categoryColor),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f, // Subtle gap
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // Center Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.formatPaisaToInr(total, showDecimals = false),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BudgetProgressBar(
    spentPaisa: Long,
    budgetLimitPaisa: Long,
    warningThresholdPercent: Int = 80,
    modifier: Modifier = Modifier
) {
    if (budgetLimitPaisa <= 0) return

    val progress = (spentPaisa.toFloat() / budgetLimitPaisa.toFloat()).coerceIn(0f, 1.5f)
    val percentage = ((spentPaisa.toDouble() / budgetLimitPaisa.toDouble()) * 100).toInt()

    val barColor = when {
        percentage >= 100 -> PaisaExpenseRed
        percentage >= warningThresholdPercent -> PaisaWarningAmber
        else -> PaisaIncomeGreen
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 600),
        label = "budget_progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$percentage% used",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = barColor
            )
            val remaining = budgetLimitPaisa - spentPaisa
            val remainingText = if (remaining >= 0) {
                "${CurrencyFormatter.formatPaisaToInr(remaining)} left"
            } else {
                "Exceeded by ${CurrencyFormatter.formatPaisaToInr(-remaining)}"
            }
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelMedium,
                color = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else PaisaExpenseRed
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.2f),
        )
    }
}

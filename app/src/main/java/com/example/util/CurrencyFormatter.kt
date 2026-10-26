package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Currency formatter tailored for the Indian Rupee (INR - ₹) using the Indian Numbering System
 * (thousands, lakhs, crores) and integer minor units (paisa).
 *
 * 1 Rupee = 100 Paisa.
 * Avoids any floating point precision issues by operating on Long paisa.
 */
object CurrencyFormatter {

    const val CURRENCY_SYMBOL = "₹"

    /**
     * Formats paisa (Long) into a standard Indian Rupee string, e.g., ₹1,23,456.75 or ₹1,23,456
     */
    fun formatPaisaToInr(paisa: Long, showDecimals: Boolean = false, showSymbol: Boolean = true): String {
        val isNegative = paisa < 0
        val positivePaisa = abs(paisa)
        val rupees = positivePaisa / 100
        val remainderPaisa = positivePaisa % 100

        val formattedRupees = formatIndianNumber(rupees)
        val prefix = if (isNegative) "- " else ""
        val symbolPrefix = if (showSymbol) "$CURRENCY_SYMBOL " else ""

        return if (showDecimals && remainderPaisa > 0) {
            val decimalPart = String.format(Locale.ROOT, "%02d", remainderPaisa)
            "$prefix$symbolPrefix$formattedRupees.$decimalPart"
        } else if (showDecimals) {
            "$prefix$symbolPrefix$formattedRupees.00"
        } else {
            "$prefix$symbolPrefix$formattedRupees"
        }
    }

    /**
     * Compact Indian format for charts and tight badges:
     * - Under 1 Lakh: ₹45,000
     * - 1 Lakh to 99 Lakhs: ₹1.5 L
     * - 1 Crore and above: ₹2.4 Cr
     */
    fun formatPaisaCompact(paisa: Long): String {
        val isNegative = paisa < 0
        val positiveRupees = abs(paisa) / 100.0
        val prefix = if (isNegative) "-" else ""

        val formatted = when {
            positiveRupees >= 1_00_00_000.0 -> {
                val cr = positiveRupees / 1_00_00_000.0
                String.format(Locale.ROOT, "%.2f Cr", cr)
            }
            positiveRupees >= 1_00_000.0 -> {
                val lakh = positiveRupees / 1_00_000.0
                String.format(Locale.ROOT, "%.2f L", lakh)
            }
            positiveRupees >= 1_000.0 -> {
                val k = positiveRupees / 1_000.0
                String.format(Locale.ROOT, "%.1f K", k)
            }
            else -> {
                String.format(Locale.ROOT, "%.0f", positiveRupees)
            }
        }
        return "$prefix$CURRENCY_SYMBOL$formatted"
    }

    /**
     * Converts a user entered decimal text (e.g. "1250", "1250.50", "12,500") into exact paisa (Long).
     */
    fun parseInrToPaisa(input: String): Long {
        if (input.isBlank()) return 0L
        val clean = input.replace(",", "").replace("₹", "").trim()
        val parts = clean.split(".")
        val rupeePart = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val decimalPartStr = parts.getOrNull(1) ?: "0"

        val paisaPart = when {
            decimalPartStr.isEmpty() -> 0L
            decimalPartStr.length == 1 -> (decimalPartStr + "0").toLongOrNull() ?: 0L
            else -> decimalPartStr.substring(0, 2).toLongOrNull() ?: 0L
        }

        val total = (rupeePart * 100L) + paisaPart
        return total
    }

    /**
     * Indian numbering system comma placement:
     * Last 3 digits grouped, then grouped by 2s: 1,23,45,678
     */
    fun formatIndianNumber(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str

        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        val formattedRemaining = sb.reverse().toString()
        return "$formattedRemaining,$lastThree"
    }
}

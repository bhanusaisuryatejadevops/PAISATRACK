package com.example

import com.example.data.security.SecurityManager
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testPaisaToInrFormatting() {
        // ₹1,23,456
        val formatted1 = CurrencyFormatter.formatPaisaToInr(12345600L, showDecimals = false, showSymbol = true)
        assertEquals("₹ 1,23,456", formatted1)

        // With decimals: ₹1,500.50
        val formatted2 = CurrencyFormatter.formatPaisaToInr(150050L, showDecimals = true, showSymbol = true)
        assertEquals("₹ 1,500.50", formatted2)

        // Lakh format: 1,00,000
        val formattedLakh = CurrencyFormatter.formatIndianNumber(100000L)
        assertEquals("1,00,000", formattedLakh)

        // Crore format: 1,23,45,678
        val formattedCrore = CurrencyFormatter.formatIndianNumber(12345678L)
        assertEquals("1,23,45,678", formattedCrore)
    }

    @Test
    fun testParseInrToPaisa() {
        assertEquals(150000L, CurrencyFormatter.parseInrToPaisa("1500"))
        assertEquals(150050L, CurrencyFormatter.parseInrToPaisa("1500.50"))
        assertEquals(150050L, CurrencyFormatter.parseInrToPaisa("₹ 1,500.50"))
        assertEquals(2500000L, CurrencyFormatter.parseInrToPaisa("25,000"))
        assertEquals(0L, CurrencyFormatter.parseInrToPaisa(""))
    }

    @Test
    fun testCompactIndianCurrency() {
        assertEquals("₹45.0 K", CurrencyFormatter.formatPaisaCompact(4500000L))
        assertEquals("₹1.50 L", CurrencyFormatter.formatPaisaCompact(15000000L))
        assertEquals("₹2.50 Cr", CurrencyFormatter.formatPaisaCompact(2500000000L))
    }

    @Test
    fun testSecurityPinHashingAndVerification() {
        val pin = "4321"
        val salt = SecurityManager.generateSalt()
        val hash = SecurityManager.hashPin(pin, salt)

        // Verify correct pin
        assertTrue(SecurityManager.verifyPin("4321", hash, salt))

        // Verify incorrect pin
        assertFalse(SecurityManager.verifyPin("1234", hash, salt))
        assertFalse(SecurityManager.verifyPin("0000", hash, salt))
    }

    @Test
    fun testDateMonthBoundaries() {
        val now = System.currentTimeMillis()
        val startOfMonth = DateUtils.getStartOfMonth(now)
        val endOfMonth = DateUtils.getEndOfMonth(now)

        assertTrue(startOfMonth < endOfMonth)

        val calStart = Calendar.getInstance().apply { timeInMillis = startOfMonth }
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))

        val calEnd = Calendar.getInstance().apply { timeInMillis = endOfMonth }
        assertEquals(calEnd.getActualMaximum(Calendar.DAY_OF_MONTH), calEnd.get(Calendar.DAY_OF_MONTH))
    }
}

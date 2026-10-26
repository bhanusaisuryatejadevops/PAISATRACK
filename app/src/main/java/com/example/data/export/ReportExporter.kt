package com.example.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.TransactionEntity
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object ReportExporter {

    fun exportCsv(transactions: List<TransactionEntity>, outputStream: OutputStream) {
        val sb = StringBuilder()
        // CSV Header
        sb.append("ID,Date,Time,Type,Category,Amount (INR),Payment Mode,Notes,Recurring\n")

        for (tx in transactions) {
            val dateStr = DateUtils.formatFullDate(tx.dateEpochMillis)
            val timeStr = DateUtils.formatTime(tx.dateEpochMillis)
            val amountInr = String.format("%.2f", tx.amountPaisa / 100.0)
            val safeNote = "\"" + tx.note.replace("\"", "\"\"") + "\""
            val safeCat = "\"" + tx.categoryName.replace("\"", "\"\"") + "\""

            sb.append("${tx.id},")
                .append("$dateStr,")
                .append("$timeStr,")
                .append("${tx.type},")
                .append("$safeCat,")
                .append("$amountInr,")
                .append("${tx.paymentMode},")
                .append("$safeNote,")
                .append("${if (tx.isRecurring) "Yes" else "No"}\n")
        }

        outputStream.use { out ->
            out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            out.flush()
        }
    }

    fun exportPdf(
        transactions: List<TransactionEntity>,
        incomePaisa: Long,
        expensePaisa: Long,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width in PostScript points
        val pageHeight = 842 // Standard A4 height in PostScript points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Banner Background (Deep Emerald)
        paint.color = Color.rgb(13, 107, 66)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PaisaTrack — Financial Report", 24f, 42f, paint)

        // Subtitle
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val reportDate = DateUtils.formatFullDate(System.currentTimeMillis())
        canvas.drawText("Generated on: $reportDate  •  Currency: Indian Rupee (INR)", 24f, 65f, paint)

        // Summary Card Area
        val cardTop = 110f
        paint.color = Color.rgb(240, 246, 242)
        canvas.drawRoundRect(24f, cardTop, (pageWidth - 24).toFloat(), cardTop + 75f, 8f, 8f, paint)

        paint.color = Color.rgb(25, 28, 26)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL INCOME", 40f, cardTop + 24f, paint)
        canvas.drawText("TOTAL EXPENSES", 220f, cardTop + 24f, paint)
        canvas.drawText("NET SAVINGS / BALANCE", 400f, cardTop + 24f, paint)

        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // Income Value
        paint.color = Color.rgb(25, 135, 84) // Green
        canvas.drawText(CurrencyFormatter.formatPaisaToInr(incomePaisa, showDecimals = true), 40f, cardTop + 50f, paint)

        // Expense Value
        paint.color = Color.rgb(220, 53, 69) // Red
        canvas.drawText(CurrencyFormatter.formatPaisaToInr(expensePaisa, showDecimals = true), 220f, cardTop + 50f, paint)

        // Net Balance
        val netBalance = incomePaisa - expensePaisa
        paint.color = if (netBalance >= 0) Color.rgb(13, 107, 66) else Color.rgb(220, 53, 69)
        canvas.drawText(CurrencyFormatter.formatPaisaToInr(netBalance, showDecimals = true), 400f, cardTop + 50f, paint)

        // Table Header
        val tableTop = 215f
        paint.color = Color.rgb(228, 236, 230)
        canvas.drawRect(24f, tableTop, (pageWidth - 24).toFloat(), tableTop + 24f, paint)

        paint.color = Color.rgb(65, 73, 66)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", 32f, tableTop + 16f, paint)
        canvas.drawText("CATEGORY", 110f, tableTop + 16f, paint)
        canvas.drawText("MODE", 230f, tableTop + 16f, paint)
        canvas.drawText("NOTE", 310f, tableTop + 16f, paint)
        canvas.drawText("AMOUNT (INR)", 470f, tableTop + 16f, paint)

        // Table Rows
        var currentY = tableTop + 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f

        val displayList = transactions.take(28) // Fit gracefully on page 1
        for (tx in displayList) {
            paint.color = Color.rgb(80, 80, 80)
            canvas.drawText(DateUtils.formatDate(tx.dateEpochMillis), 32f, currentY, paint)

            val catName = if (tx.categoryName.length > 18) tx.categoryName.take(16) + ".." else tx.categoryName
            canvas.drawText(catName, 110f, currentY, paint)
            canvas.drawText(tx.paymentMode, 230f, currentY, paint)

            val note = if (tx.note.length > 24) tx.note.take(22) + ".." else tx.note
            canvas.drawText(if (note.isBlank()) "-" else note, 310f, currentY, paint)

            val isIncome = tx.type == "INCOME"
            paint.color = if (isIncome) Color.rgb(25, 135, 84) else Color.rgb(220, 53, 69)
            val prefix = if (isIncome) "+ " else "- "
            val formattedAmt = prefix + CurrencyFormatter.formatPaisaToInr(tx.amountPaisa, showDecimals = true, showSymbol = true)
            canvas.drawText(formattedAmt, 470f, currentY, paint)

            // Divider line
            paint.color = Color.rgb(235, 235, 235)
            canvas.drawLine(24f, currentY + 6f, (pageWidth - 24).toFloat(), currentY + 6f, paint)

            currentY += 20f
        }

        if (transactions.size > 28) {
            paint.color = Color.GRAY
            paint.textSize = 9f
            canvas.drawText("+ ${transactions.size - 28} more records available in full CSV export.", 32f, currentY + 12f, paint)
        }

        // Footer
        paint.color = Color.rgb(150, 150, 150)
        paint.textSize = 8.5f
        canvas.drawText("Confidential • PaisaTrack Secure Offline Financial Statement", 24f, (pageHeight - 20).toFloat(), paint)

        document.finishPage(page)

        outputStream.use { out ->
            document.writeTo(out)
            out.flush()
        }
        document.close()
    }
}

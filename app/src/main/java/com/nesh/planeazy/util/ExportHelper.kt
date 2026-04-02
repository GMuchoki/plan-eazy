package com.nesh.planeazy.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nesh.planeazy.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun exportToCSV(context: Context, transactions: List<Transaction>) {
        val fileName = "transactions_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        val header = "Date,Title,Type,Category,Sub-Category,Amount,Payment Method,Provider,Units,Note\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val csvData = transactions.joinToString("\n") { t ->
            val date = dateFormat.format(Date(t.date))
            "${date},\"${t.title}\",${t.type},\"${t.category}\",\"${t.subCategory ?: ""}\",${t.amount},\"${t.paymentMethodType}\",\"${t.paymentMethodProvider}\",${t.units ?: ""},\"${t.note}\""
        }

        file.writeText(header + csvData)
        shareFile(context, file, "text/csv")
    }

    fun exportToExcelFriendly(context: Context, transactions: List<Transaction>) {
        exportToCSV(context, transactions)
    }

    fun exportToPDF(context: Context, transactions: List<Transaction>, currency: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var y = 40f
        val xMargin = 40f
        
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.parseColor("#101D3D") 
        canvas.drawText("Plan Eazy - Financial Report", xMargin, y, titlePaint)
        
        y += 30f
        paint.textSize = 12f
        paint.color = Color.GRAY
        val dfFull = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        canvas.drawText("Generated on: ${dfFull.format(Date())}", xMargin, y, paint)
        
        y += 40f
        
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.BLACK
        canvas.drawText("Summary:", xMargin, y, paint)
        y += 20f
        
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Total Income: $currency ${String.format(Locale.getDefault(), "%,.2f", totalIncome)}", xMargin + 10, y, paint)
        y += 15f
        canvas.drawText("Total Expense: $currency ${String.format(Locale.getDefault(), "%,.2f", totalExpense)}", xMargin + 10, y, paint)
        y += 15f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = if (netBalance >= 0) Color.parseColor("#2E7D32") else Color.RED
        canvas.drawText("Net Balance: $currency ${String.format(Locale.getDefault(), "%,.2f", netBalance)}", xMargin + 10, y, paint)
        
        y += 40f
        
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("Date", xMargin, y, paint)
        canvas.drawText("Title", xMargin + 70, y, paint)
        canvas.drawText("Category", xMargin + 200, y, paint)
        canvas.drawText("Amount ($currency)", xMargin + 400, y, paint)
        
        y += 10f
        canvas.drawLine(xMargin, y, 555f, y, paint)
        y += 20f
        
        paint.typeface = Typeface.DEFAULT
        val dfShort = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        
        transactions.forEach { t -> 
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            
            canvas.drawText(dfShort.format(Date(t.date)), xMargin, y, paint)
            val displayTitle = if (t.title.length > 20) t.title.take(17) + "..." else t.title
            canvas.drawText(displayTitle, xMargin + 70, y, paint)
            canvas.drawText(t.category, xMargin + 200, y, paint)
            
            val oldColor = paint.color
            paint.color = when(t.type) {
                TransactionType.INCOME -> Color.parseColor("#2E7D32")
                TransactionType.EXPENSE -> Color.RED
                else -> Color.BLUE
            }
            val amountStr = "${if (t.type == TransactionType.EXPENSE) "-" else "+"} ${String.format(Locale.getDefault(), "%,.2f", t.amount)}"
            canvas.drawText(amountStr, xMargin + 400, y, paint)
            paint.color = oldColor
            
            y += 20f
        }
        
        pdfDocument.finishPage(page)
        
        val fileName = "PlanEazy_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            Log.e("PDFExport", "Error writing PDF", e)
            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportDebtsPDF(context: Context, debts: List<Debt>, currency: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var y = 40f
        val xMargin = 40f
        
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.parseColor("#101D3D") 
        canvas.drawText("Plan Eazy - Debts & Loans Report", xMargin, y, titlePaint)
        
        y += 30f
        paint.textSize = 12f
        paint.color = Color.GRAY
        val dfFull = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        canvas.drawText("Generated on: ${dfFull.format(Date())}", xMargin, y, paint)
        
        y += 40f
        
        val totalIOwe = debts.filter { it.type == DebtType.OWED_BY_ME }.sumOf { it.totalAmount - it.paidAmount }
        val totalOwedToMe = debts.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.totalAmount - it.paidAmount }
        
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.BLACK
        canvas.drawText("Debt Summary:", xMargin, y, paint)
        y += 20f
        
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Total I Owe: $currency ${String.format(Locale.getDefault(), "%,.2f", totalIOwe)}", xMargin + 10, y, paint)
        y += 15f
        canvas.drawText("Total Owed to Me: $currency ${String.format(Locale.getDefault(), "%,.2f", totalOwedToMe)}", xMargin + 10, y, paint)
        
        y += 40f
        
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("Person/Title", xMargin, y, paint)
        canvas.drawText("Type", xMargin + 150, y, paint)
        canvas.drawText("Remaining", xMargin + 250, y, paint)
        canvas.drawText("Total", xMargin + 400, y, paint)
        
        y += 10f
        canvas.drawLine(xMargin, y, 555f, y, paint)
        y += 20f
        
        debts.forEach { d ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            
            canvas.drawText("${d.personName} (${d.title})", xMargin, y, paint)
            canvas.drawText(if (d.type == DebtType.OWED_BY_ME) "I Owe" else "Lent", xMargin + 150, y, paint)
            
            val remaining = d.totalAmount - d.paidAmount
            val oldColor = paint.color
            paint.color = if (d.type == DebtType.OWED_BY_ME) Color.RED else Color.parseColor("#2E7D32")
            canvas.drawText(String.format(Locale.getDefault(), "%,.2f", remaining), xMargin + 250, y, paint)
            paint.color = oldColor
            
            canvas.drawText(String.format(Locale.getDefault(), "%,.2f", d.totalAmount), xMargin + 400, y, paint)
            
            y += 20f
        }
        
        pdfDocument.finishPage(page)
        
        val fileName = "PlanEazy_Debts_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            Log.e("PDFExport", "Error writing Debts PDF", e)
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Export Report"))
    }
}

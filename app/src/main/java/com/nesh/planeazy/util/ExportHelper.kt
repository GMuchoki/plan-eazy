package com.nesh.planeazy.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nesh.planeazy.data.model.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // PDF export often requires a library like iText or PdfBox. 
    // For now, we'll implement a simple text-based "Report" version or use basic canvas.
    // For high-end apps, EXCEL is preferred via Apache POI.
    // Since we want to keep it clean and standard, CSV is excellent for Excel.
    
    fun exportToExcelFriendly(context: Context, transactions: List<Transaction>) {
        // Excel can open CSV directly, but we can name it .xls if we used a library.
        // Let's stick to professional CSV format as it is universal for accounting.
        exportToCSV(context, transactions)
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

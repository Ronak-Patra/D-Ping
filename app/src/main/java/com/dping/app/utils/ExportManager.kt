package com.dping.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dping.app.data.model.DiagnosticRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareText(context: Context, title: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun exportHistoryToCsv(records: List<DiagnosticRecord>): String {
        val sb = StringBuilder()
        // Header
        sb.append("ID,Test Type,Target,Timestamp,Date Time,Duration (ms),Status,Summary,Country\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        for (rec in records) {
            val dateStr = dateFormat.format(Date(rec.timestamp))
            val statusStr = if (rec.isSuccess) "Success" else "Failed"

            sb.append(rec.id).append(",")
            sb.append(escapeCsv(rec.testType)).append(",")
            sb.append(escapeCsv(rec.target)).append(",")
            sb.append(rec.timestamp).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(rec.durationMs).append(",")
            sb.append(escapeCsv(statusStr)).append(",")
            sb.append(escapeCsv(rec.summary)).append(",")
            sb.append(escapeCsv(rec.countryName ?: "")).append("\n")
        }

        return sb.toString()
    }

    fun shareCsvFile(context: Context, csvContent: String, fileNamePrefix: String = "dping_history") {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.csv"
            val file = File(exportDir, fileName)
            file.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Diagnostic CSV"))
        } catch (_: Exception) {
            // Fallback to text share
            shareText(context, "D Ping Diagnostic Export", csvContent)
        }
    }

    fun exportHistoryToJson(records: List<DiagnosticRecord>): String {
        val jsonArray = org.json.JSONArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (rec in records) {
            val obj = org.json.JSONObject().apply {
                put("id", rec.id)
                put("testType", rec.testType)
                put("target", rec.target)
                put("timestamp", rec.timestamp)
                put("dateTime", dateFormat.format(Date(rec.timestamp)))
                put("durationMs", rec.durationMs)
                put("isSuccess", rec.isSuccess)
                put("summary", rec.summary)
                put("details", rec.detailsJson)
                put("countryName", rec.countryName ?: "")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun shareJsonFile(context: Context, jsonContent: String, fileNamePrefix: String = "dping_history") {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.json"
            val file = File(exportDir, fileName)
            file.writeText(jsonContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Diagnostic JSON"))
        } catch (_: Exception) {
            shareText(context, "D Ping Diagnostic Export JSON", jsonContent)
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}

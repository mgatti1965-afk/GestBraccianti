package com.example.gestbraccianti.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.model.WorkerYearStats
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun generatePdfReport(
    context: Context,
    logs: List<WorkLog>,
    yearStats: List<WorkerYearStats>,
    filterTitle: String,
    referenceDate: Long
): File? {
    val pdfDocument = PdfDocument()
    val paint = Paint()
    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
    }
    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
    }
    val bodyPaint = Paint().apply {
        textSize = 12f
    }
    val boldPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
    }

    // A4 size in points (72 points per inch)
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var myPage = pdfDocument.startPage(myPageInfo)
    var canvas: Canvas = myPage.canvas

    val margin = 40f
    var y = 60f

    // Header info
    val prefs = context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE)
    val ownerName = prefs.getString("owner_name", "") ?: ""
    val ownerSurname = prefs.getString("owner_surname", "") ?: ""
    
    val sdf = when (filterTitle) {
        "Mese" -> SimpleDateFormat("MMMM yyyy", Locale.ITALY)
        "Settimana" -> SimpleDateFormat("'Settimana' w, yyyy", Locale.ITALY)
        "Giorno" -> SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY)
        else -> SimpleDateFormat("yyyy", Locale.ITALY)
    }
    val period = sdf.format(Date(referenceDate)).replaceFirstChar { it.uppercase() }

    canvas.drawText("RIEPILOGO PRESENZE E COMPENSI", margin, y, titlePaint)
    y += 25f
    if (ownerSurname.isNotBlank() || ownerName.isNotBlank()) {
        canvas.drawText("Azienda: $ownerSurname $ownerName", margin, y, bodyPaint)
        y += 20f
    }
    canvas.drawText("Periodo: $period", margin, y, bodyPaint)
    y += 30f
    canvas.drawLine(margin, y, pageWidth - margin, y, paint)
    y += 30f

    val workerMap = yearStats.associateBy { it.workerId }
    val calendar = Calendar.getInstance(Locale.ITALY)
    val daySdf = SimpleDateFormat("dd/MM", Locale.ITALY)

    fun checkNewPage() {
        if (y > pageHeight - 60f) {
            pdfDocument.finishPage(myPage)
            pageNumber++
            myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            myPage = pdfDocument.startPage(myPageInfo)
            canvas = myPage.canvas
            y = 60f
        }
    }

    var totalOverallHours = 0.0
    var totalOverallEarnings = 0.0

    when (filterTitle) {
        "Giorno" -> {
            logs.sortedBy { it.workerId }.forEach { log ->
                val worker = workerMap[log.workerId]
                val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                val earnStr = String.format(Locale.ITALY, "%.2f €", log.totalHours * log.hourlyRate)
                val line = "• $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr"
                
                checkNewPage()
                canvas.drawText(line, margin + 10f, y, bodyPaint)
                y += 18f
                
                totalOverallHours += log.totalHours
                totalOverallEarnings += (log.totalHours * log.hourlyRate)
            }
        }
        "Settimana" -> {
            val groupedByWeek = logs.groupBy { log ->
                calendar.timeInMillis = log.date
                calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.WEEK_OF_YEAR)
            }.toSortedMap()

            groupedByWeek.forEach { (weekKey, weekLogs) ->
                val weekNum = weekKey % 100
                val weekYear = weekKey / 100
                calendar.set(Calendar.YEAR, weekYear)
                calendar.set(Calendar.WEEK_OF_YEAR, weekNum)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startWeek = Date(calendar.timeInMillis)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val endWeek = Date(calendar.timeInMillis)
                
                val weekHeader = "SETTIMANA $weekNum (${daySdf.format(startWeek)} - ${daySdf.format(endWeek)})"
                
                checkNewPage()
                canvas.drawText(weekHeader, margin, y, headerPaint)
                y += 20f
                
                var totalWeekHours = 0.0
                var totalWeekEarnings = 0.0
                weekLogs.sortedBy { it.date }.forEach { log ->
                    val worker = workerMap[log.workerId]
                    val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                    val earnStr = String.format(Locale.ITALY, "%.2f €", log.totalHours * log.hourlyRate)
                    val line = "• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr"
                    
                    checkNewPage()
                    canvas.drawText(line, margin + 10f, y, bodyPaint)
                    y += 18f
                    
                    totalWeekHours += log.totalHours
                    totalWeekEarnings += (log.totalHours * log.hourlyRate)
                }
                val totWeekEarnStr = String.format(Locale.ITALY, "%.2f €", totalWeekEarnings)
                checkNewPage()
                canvas.drawText("Totale settimana: ${formatDecimalHours(totalWeekHours)}h | $totWeekEarnStr", margin + 10f, y, boldPaint)
                y += 30f
                
                totalOverallHours += totalWeekHours
                totalOverallEarnings += totalWeekEarnings
            }
        }
        else -> { // Anno o Mese
            val groupedByMonth = logs.groupBy { log ->
                calendar.timeInMillis = log.date
                calendar.get(Calendar.MONTH)
            }.toSortedMap()

            groupedByMonth.forEach { (monthIdx, monthLogs) ->
                calendar.set(Calendar.MONTH, monthIdx)
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(calendar.time).uppercase()
                
                checkNewPage()
                canvas.drawText(monthName, margin, y, headerPaint)
                y += 20f
                
                var totalMonthHours = 0.0
                var totalMonthEarnings = 0.0
                
                monthLogs.sortedBy { it.date }.forEach { log ->
                    val worker = workerMap[log.workerId]
                    val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                    val earnStr = String.format(Locale.ITALY, "%.2f €", log.totalHours * log.hourlyRate)
                    val line = "• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr"
                    
                    checkNewPage()
                    canvas.drawText(line, margin + 10f, y, bodyPaint)
                    y += 18f
                    
                    totalMonthHours += log.totalHours
                    totalMonthEarnings += (log.totalHours * log.hourlyRate)
                }
                
                val totMonthEarnStr = String.format(Locale.ITALY, "%.2f €", totalMonthEarnings)
                checkNewPage()
                canvas.drawText("Totale mese: ${formatDecimalHours(totalMonthHours)}h | $totMonthEarnStr", margin + 10f, y, boldPaint)
                y += 30f
                
                totalOverallHours += totalMonthHours
                totalOverallEarnings += totalMonthEarnings
            }
        }
    }

    // Grand Totals
    y += 10f
    checkNewPage()
    canvas.drawLine(margin, y, pageWidth - margin, y, paint)
    y += 25f
    val footerLabel = when(filterTitle) {
        "Giorno" -> "TOTALE GIORNALIERO"
        "Settimana" -> "TOTALE COMPLESSIVO"
        else -> "TOTALE COMPLESSIVO"
    }
    canvas.drawText(footerLabel, margin, y, headerPaint)
    y += 20f
    canvas.drawText("Ore totali: ${formatDecimalHours(totalOverallHours)} h", margin, y, bodyPaint)
    y += 18f
    val totalEarnStr = String.format(Locale.ITALY, "%.2f €", totalOverallEarnings)
    canvas.drawText("Importo totale: $totalEarnStr", margin, y, boldPaint)

    pdfDocument.finishPage(myPage)

    val directory = File(context.getExternalFilesDir(null), "reports")
    if (!directory.exists()) directory.mkdirs()
    
    val fileName = "Riepilogo_${System.currentTimeMillis()}.pdf"
    val file = File(directory, fileName)

    try {
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    } catch (e: Exception) {
        e.printStackTrace()
        pdfDocument.close()
        return null
    }
}

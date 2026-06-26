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
    referenceDate: Long,
    groupingType: com.example.gestbraccianti.ui.screens.GroupingType = com.example.gestbraccianti.ui.screens.GroupingType.BY_WORKER,
    viewMode: com.example.gestbraccianti.ui.screens.ViewMode = com.example.gestbraccianti.ui.screens.ViewMode.DETAIL,
    groups: List<com.example.gestbraccianti.data.entity.WorkerGroup> = emptyList(),
    groupToWorkers: Map<Long, List<Long>> = emptyMap()
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
    y += 15f
    val modeText = if (groupingType == com.example.gestbraccianti.ui.screens.GroupingType.BY_GROUP) "Raggruppamento: Gruppi" else "Raggruppamento: Braccianti"
    val viewText = if (viewMode == com.example.gestbraccianti.ui.screens.ViewMode.TOTALS) "Vista: Solo Totali" else "Vista: Dettaglio"
    canvas.drawText("$modeText | $viewText", margin, y, bodyPaint.apply { textSize = 10f })
    bodyPaint.textSize = 12f
    y += 20f
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

    // Group logs by month for consistency with UI
    val monthlyLogs = logs.groupBy { log ->
        calendar.timeInMillis = log.date
        calendar.get(Calendar.MONTH)
    }.toSortedMap()

    monthlyLogs.forEach { (monthIdx, mLogs) ->
        calendar.set(Calendar.MONTH, monthIdx)
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(calendar.time).uppercase()
        
        checkNewPage()
        canvas.drawText(monthName, margin, y, headerPaint)
        y += 25f

        if (groupingType == com.example.gestbraccianti.ui.screens.GroupingType.BY_GROUP) {
            // Logic for groups
            groups.forEach { group ->
                val workersInGroup = groupToWorkers[group.id] ?: emptyList()
                val gLogs = mLogs.filter { it.workerId in workersInGroup }
                if (gLogs.isNotEmpty()) {
                    val hours = gLogs.sumOf { it.totalHours }
                    val earnings = gLogs.sumOf { it.totalHours * it.hourlyRate }
                    
                    checkNewPage()
                    canvas.drawText("• ${group.name}", margin + 10f, y, boldPaint)
                    y += 18f
                    canvas.drawText("  Totale: ${formatDecimalHours(hours)}h | ${String.format(Locale.ITALY, "%.2f €", earnings)}", margin + 10f, y, bodyPaint)
                    y += 22f
                }
            }
            val allGroupWorkers = groupToWorkers.values.flatten().toSet()
            val noGroupLogs = mLogs.filter { it.workerId !in allGroupWorkers }
            if (noGroupLogs.isNotEmpty()) {
                val hours = noGroupLogs.sumOf { it.totalHours }
                val earnings = noGroupLogs.sumOf { it.totalHours * it.hourlyRate }
                checkNewPage()
                canvas.drawText("• Senza Gruppo", margin + 10f, y, boldPaint)
                y += 18f
                canvas.drawText("  Totale: ${formatDecimalHours(hours)}h | ${String.format(Locale.ITALY, "%.2f €", earnings)}", margin + 10f, y, bodyPaint)
                y += 22f
            }
        } else if (viewMode == com.example.gestbraccianti.ui.screens.ViewMode.TOTALS) {
            // Logic for worker totals
            mLogs.groupBy { it.workerId }.forEach { (wId, wLogs) ->
                val worker = workerMap[wId]
                val workerName = "${worker?.surname ?: ""} ${worker?.name ?: "Bracc. $wId"}"
                val hours = wLogs.sumOf { it.totalHours }
                val earnings = wLogs.sumOf { it.totalHours * it.hourlyRate }
                
                checkNewPage()
                canvas.drawText("• $workerName", margin + 10f, y, boldPaint)
                y += 18f
                canvas.drawText("  Totale: ${formatDecimalHours(hours)}h | ${String.format(Locale.ITALY, "%.2f €", earnings)}", margin + 10f, y, bodyPaint)
                y += 22f
            }
        } else {
            // Logic for detail
            mLogs.sortedBy { it.date }.forEach { log ->
                val worker = workerMap[log.workerId]
                val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                val earnStr = String.format(Locale.ITALY, "%.2f €", log.totalHours * log.hourlyRate)
                val line = "• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr"
                
                checkNewPage()
                canvas.drawText(line, margin + 10f, y, bodyPaint)
                y += 18f
            }
        }

        val totalMonthHours = mLogs.sumOf { it.totalHours }
        val totalMonthEarnings = mLogs.sumOf { it.totalHours * it.hourlyRate }
        val totMonthEarnStr = String.format(Locale.ITALY, "%.2f €", totalMonthEarnings)
        
        checkNewPage()
        y += 5f
        canvas.drawText("TOTALE PERIODO: ${formatDecimalHours(totalMonthHours)}h | $totMonthEarnStr", margin + 10f, y, boldPaint)
        y += 35f
        
        totalOverallHours += totalMonthHours
        totalOverallEarnings += totalMonthEarnings
    }

    // Grand Totals
    y += 10f
    checkNewPage()
    canvas.drawLine(margin, y, pageWidth - margin, y, paint)
    y += 25f
    canvas.drawText("RIEPILOGO COMPLESSIVO", margin, y, headerPaint)
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

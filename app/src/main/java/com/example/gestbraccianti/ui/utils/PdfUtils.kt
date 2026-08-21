package com.example.gestbraccianti.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.model.WorkerYearStats
import com.example.gestbraccianti.ui.utils.formatCurrency
import com.example.gestbraccianti.ui.utils.formatDecimal
import com.example.gestbraccianti.ui.utils.formatHours
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
        "Mese" -> TimeUtils.monthYearFormatter
        "Settimana" -> TimeUtils.weekYearFormatter
        "Giorno" -> TimeUtils.fullDateFormatter
        else -> TimeUtils.yearFormatter
    }
    val period = TimeUtils.format(referenceDate, sdf).replaceFirstChar { it.uppercase() }

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
    var totalOrdHours = 0.0
    var totalExtHours = 0.0
    var totalHolHours = 0.0
    var totalOrdAmt = 0.0
    var totalExtAmt = 0.0
    var totalHolAmt = 0.0

    val threshold = prefs.getFloat("extra_hours_threshold", 8.0f).toDouble()
    val festiveType = prefs.getInt("festive_days_type", 0)

    // Helper to accumulate totals
    fun accumulateTotals(log: WorkLog) {
        val cal = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = log.date }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isFestive = log.isManualHoliday || when (festiveType) {
            1 -> dayOfWeek == Calendar.SATURDAY
            2 -> dayOfWeek == Calendar.SUNDAY
            3 -> dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            else -> false
        }

        if (isFestive) {
            totalHolHours += log.totalHours
            totalHolAmt += log.totalAmount
        } else {
            if (log.totalHours > threshold) {
                totalOrdHours += threshold
                totalExtHours += (log.totalHours - threshold)
                totalOrdAmt += (threshold * log.hourlyRate)
                totalExtAmt += ((log.totalHours - threshold) * log.extraHourlyRate)
            } else {
                totalOrdHours += log.totalHours
                totalOrdAmt += log.totalAmount
            }
        }
        totalOverallHours += log.totalHours
        totalOverallEarnings += log.totalAmount
    }

    // Group logs by month for consistency with UI
    val monthlyLogs = logs.groupBy { log ->
        calendar.timeInMillis = log.date
        calendar.get(Calendar.MONTH)
    }.toSortedMap()

    monthlyLogs.forEach { (monthIdx, mLogs) ->
        mLogs.forEach { accumulateTotals(it) }
        
        calendar.set(Calendar.MONTH, monthIdx)
        val monthName = TimeUtils.formatMonth(calendar.timeInMillis).uppercase()
        
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
                    val earnings = gLogs.sumOf { it.totalAmount }
                    
                    checkNewPage()
                    canvas.drawText("• ${group.name}", margin + 10f, y, boldPaint)
                    y += 18f
                    canvas.drawText("  Totale: ${formatHours(hours)}h | ${formatCurrency(earnings)}", margin + 10f, y, bodyPaint)
                    y += 22f
                }
            }
            val allGroupWorkers = groupToWorkers.values.flatten().toSet()
            val noGroupLogs = mLogs.filter { it.workerId !in allGroupWorkers }
            if (noGroupLogs.isNotEmpty()) {
                val hours = noGroupLogs.sumOf { it.totalHours }
                val earnings = noGroupLogs.sumOf { it.totalAmount }
                checkNewPage()
                canvas.drawText("• Senza Gruppo", margin + 10f, y, boldPaint)
                y += 18f
                canvas.drawText("  Totale: ${formatHours(hours)}h | ${formatCurrency(earnings)}", margin + 10f, y, bodyPaint)
                y += 22f
            }
        } else if (viewMode == com.example.gestbraccianti.ui.screens.ViewMode.TOTALS) {
            // Logic for worker totals
            mLogs.groupBy { it.workerId }.forEach { (wId, wLogs) ->
                val worker = workerMap[wId]
                val workerName = "${worker?.surname ?: ""} ${worker?.name ?: "Bracc. $wId"}"
                val hours = wLogs.sumOf { it.totalHours }
                val earnings = wLogs.sumOf { it.totalAmount }
                
                checkNewPage()
                canvas.drawText("• $workerName", margin + 10f, y, boldPaint)
                y += 18f
                canvas.drawText("  Totale: ${formatHours(hours)}h | ${formatCurrency(earnings)}", margin + 10f, y, bodyPaint)
                y += 22f
            }
        } else {
            // Logic for detail
            mLogs.sortedBy { it.date }.forEach { log ->
                val worker = workerMap[log.workerId]
                val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                val earnStr = formatCurrency(log.totalAmount)
                val calTemp = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = log.date }
                val isFestive = log.isManualHoliday || when (festiveType) {
                    1 -> calTemp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    2 -> calTemp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    3 -> calTemp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calTemp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    else -> false
                }
                val festStr = if (isFestive) " [F]" else ""
                val line = "• ${TimeUtils.format(log.date, TimeUtils.dayMonthFormatter)}$festStr $workerName: ${formatHours(log.totalHours)}h | $earnStr"
                
                checkNewPage()
                canvas.drawText(line, margin + 10f, y, bodyPaint)
                y += 18f
            }
        }

        val totalMonthHours = mLogs.sumOf { it.totalHours }
        val totalMonthEarnings = mLogs.sumOf { it.totalAmount }
        val totMonthEarnStr = formatCurrency(totalMonthEarnings)
        
        checkNewPage()
        y += 5f
        canvas.drawText("TOTALE PERIODO: ${formatHours(totalMonthHours)}h | $totMonthEarnStr", margin + 10f, y, boldPaint)
        y += 35f
    }

    // Grand Totals
    y += 10f
    checkNewPage()
    canvas.drawLine(margin, y, pageWidth - margin, y, paint)
    y += 25f
    canvas.drawText("RIEPILOGO COMPLESSIVO", margin, y, headerPaint)
    y += 20f
    
    // Details split
    canvas.drawText("Ore ordinarie: ${formatHours(totalOrdHours)}h (${formatCurrency(totalOrdAmt)})", margin, y, bodyPaint)
    y += 18f
    canvas.drawText("Ore straordinarie: ${formatHours(totalExtHours)}h (${formatCurrency(totalExtAmt)})", margin, y, bodyPaint)
    y += 18f
    canvas.drawText("Ore festive: ${formatHours(totalHolHours)}h (${formatCurrency(totalHolAmt)})", margin, y, bodyPaint)
    y += 25f

    canvas.drawText("Ore totali: ${formatHours(totalOverallHours)} h", margin, y, bodyPaint)
    y += 18f
    val totalEarnStr = formatCurrency(totalOverallEarnings)
    canvas.drawText("Importo totale: $totalEarnStr", margin, y, boldPaint)

    pdfDocument.finishPage(myPage)


    val directory = File(context.getExternalFilesDir(null), "reports")
    if (!directory.exists()) directory.mkdirs()
    
    val timestamp = TimeUtils.format(System.currentTimeMillis(), TimeUtils.fileTimestampFormatter)
    val fileName = "GestBraccianti_Rep_$timestamp.pdf"
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

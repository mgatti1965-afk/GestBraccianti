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

    val festiveType = prefs.getInt("festive_days_type", 3)
    val globalFestiveDates = prefs.getStringSet("global_festive_dates", emptySet())
        ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    // Helper to accumulate totals
    fun accumulateTotals(log: WorkLog) {
        totalOrdHours += log.ordinaryHours
        totalExtHours += log.extraHours
        totalHolHours += log.holidayHours
        totalOrdAmt += log.ordinaryAmount
        totalExtAmt += log.extraAmount
        totalHolAmt += log.holidayAmount
        
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
                    var gOrdH = 0.0; var gExtH = 0.0; var gHolH = 0.0
                    var gOrdA = 0.0; var gExtA = 0.0; var gHolA = 0.0
                    
                    gLogs.forEach { log ->
                        gOrdH += log.ordinaryHours
                        gExtH += log.extraHours
                        gHolH += log.holidayHours
                        gOrdA += log.ordinaryAmount
                        gExtA += log.extraAmount
                        gHolA += log.holidayAmount
                    }

                    val hours = gLogs.sumOf { it.totalHours }
                    val earnings = gLogs.sumOf { it.totalAmount }
                    
                    checkNewPage()
                    canvas.drawText(group.name, margin + 10f, y, boldPaint)
                    y += 18f
                    
                    val originalSize = bodyPaint.textSize
                    bodyPaint.textSize = 10f
                    canvas.drawText("Ord: ${formatHours(gOrdH)} (${formatCurrency(gOrdA)})", margin + 10f, y, bodyPaint)
                    canvas.drawText("Str: ${formatHours(gExtH)} (${formatCurrency(gExtA)})", 215f, y, bodyPaint)
                    canvas.drawText("Fes: ${formatHours(gHolH)} (${formatCurrency(gHolA)})", 390f, y, bodyPaint)
                    y += 18f
                    
                    canvas.drawText("TOTALE GRUPPO: ${formatHours(hours)} | ${formatCurrency(earnings)}", margin + 10f, y, boldPaint)
                    bodyPaint.textSize = originalSize
                    y += 28f
                }
            }
            val allGroupWorkers = groupToWorkers.values.flatten().toSet()
            val noGroupLogs = mLogs.filter { it.workerId !in allGroupWorkers }
            if (noGroupLogs.isNotEmpty()) {
                var gOrdH = 0.0; var gExtH = 0.0; var gHolH = 0.0
                var gOrdA = 0.0; var gExtA = 0.0; var gHolA = 0.0
                
                noGroupLogs.forEach { log ->
                    gOrdH += log.ordinaryHours
                    gExtH += log.extraHours
                    gHolH += log.holidayHours
                    gOrdA += log.ordinaryAmount
                    gExtA += log.extraAmount
                    gHolA += log.holidayAmount
                }
                val hours = noGroupLogs.sumOf { it.totalHours }
                val earnings = noGroupLogs.sumOf { it.totalAmount }

                checkNewPage()
                canvas.drawText("Senza Gruppo", margin + 10f, y, boldPaint)
                y += 18f
                
                val originalSize = bodyPaint.textSize
                bodyPaint.textSize = 10f
                canvas.drawText("Ord: ${formatHours(gOrdH)} (${formatCurrency(gOrdA)})", margin + 10f, y, bodyPaint)
                canvas.drawText("Str: ${formatHours(gExtH)} (${formatCurrency(gExtA)})", 215f, y, bodyPaint)
                canvas.drawText("Fes: ${formatHours(gHolH)} (${formatCurrency(gHolA)})", 390f, y, bodyPaint)
                y += 18f
                
                canvas.drawText("TOTALE: ${formatHours(hours)} | ${formatCurrency(earnings)}", margin + 10f, y, boldPaint)
                bodyPaint.textSize = originalSize
                y += 28f
            }
        } else if (viewMode == com.example.gestbraccianti.ui.screens.ViewMode.TOTALS) {
            // Logic for worker totals
            mLogs.groupBy { it.workerId }.forEach { (wId, wLogs) ->
                val worker = workerMap[wId]
                val workerName = "${worker?.surname ?: ""} ${worker?.name ?: "Bracc. $wId"}"
                
                var wOrdH = 0.0; var wExtH = 0.0; var wHolH = 0.0
                var wOrdA = 0.0; var wExtA = 0.0; var wHolA = 0.0
                
                wLogs.forEach { log ->
                    wOrdH += log.ordinaryHours
                    wExtH += log.extraHours
                    wHolH += log.holidayHours
                    wOrdA += log.ordinaryAmount
                    wExtA += log.extraAmount
                    wHolA += log.holidayAmount
                }
                
                val hours = wLogs.sumOf { it.totalHours }
                val earnings = wLogs.sumOf { it.totalAmount }
                
                checkNewPage()
                canvas.drawText(workerName, margin + 10f, y, boldPaint)
                y += 18f
                
                val originalSize = bodyPaint.textSize
                bodyPaint.textSize = 10f
                canvas.drawText("Ord: ${formatHours(wOrdH)} (${formatCurrency(wOrdA)})", margin + 10f, y, bodyPaint)
                canvas.drawText("Str: ${formatHours(wExtH)} (${formatCurrency(wExtA)})", 215f, y, bodyPaint)
                canvas.drawText("Fes: ${formatHours(wHolH)} (${formatCurrency(wHolA)})", 390f, y, bodyPaint)
                y += 18f
                
                canvas.drawText("TOTALE: ${formatHours(hours)} | ${formatCurrency(earnings)}", margin + 10f, y, boldPaint)
                bodyPaint.textSize = originalSize
                y += 28f
            }
        } else {
            // Logic for detail
            mLogs.sortedBy { it.date }.forEach { log ->
                val worker = workerMap[log.workerId]
                val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                
                val isFestive = TimeUtils.isFestive(log.date, log.isManualHoliday, festiveType, globalFestiveDates)
                
                val ordStr = formatHours(log.ordinaryHours)
                val extStr = formatHours(log.extraHours)
                val holStr = formatHours(log.holidayHours)
                val earnStr = formatCurrency(log.totalAmount)
                
                val dateStr = TimeUtils.format(log.date, TimeUtils.dayMonthFormatter)
                val festMark = if (isFestive) "*" else ""
                
                checkNewPage()
                
                // Troncamento nome se troppo lungo per evitare sovrapposizioni
                val fullText = "$dateStr$festMark $workerName"
                val namePaint = Paint(bodyPaint)
                var displayName = fullText
                val maxNameWidth = 180f
                if (namePaint.measureText(fullText) > maxNameWidth) {
                    var truncated = fullText
                    while (truncated.isNotEmpty() && namePaint.measureText("$truncated...") > maxNameWidth) {
                        truncated = truncated.dropLast(1)
                    }
                    displayName = "$truncated..."
                }
                
                canvas.drawText(displayName, margin + 10f, y, bodyPaint)
                
                val originalTextSize = bodyPaint.textSize
                bodyPaint.textSize = 9f // Leggermente più piccolo per i dettagli
                
                // Colonne per le ore spostate a destra per dare spazio al nome
                canvas.drawText("Ord: $ordStr", 240f, y, bodyPaint)
                canvas.drawText("Str: $extStr", 340f, y, bodyPaint)
                canvas.drawText("Fes: $holStr", 440f, y, bodyPaint)
                
                // Importo allineato a destra e in grassetto
                bodyPaint.textAlign = android.graphics.Paint.Align.RIGHT
                bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(earnStr, pageWidth - margin - 10f, y, bodyPaint)
                
                // Reset paint
                bodyPaint.textSize = originalTextSize
                bodyPaint.textAlign = android.graphics.Paint.Align.LEFT
                bodyPaint.typeface = Typeface.DEFAULT
                y += 18f
            }
        }

        val totalMonthHours = mLogs.sumOf { it.totalHours }
        val totalMonthEarnings = mLogs.sumOf { it.totalAmount }
        val totMonthEarnStr = formatCurrency(totalMonthEarnings)
        
        checkNewPage()
        y += 5f
        canvas.drawText("TOTALE PERIODO: ${formatHours(totalMonthHours)} | $totMonthEarnStr", margin + 10f, y, boldPaint)
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
    canvas.drawText("Ore ordinarie: ${formatHours(totalOrdHours)} (${formatCurrency(totalOrdAmt)})", margin, y, bodyPaint)
    y += 18f
    canvas.drawText("Ore straordinarie: ${formatHours(totalExtHours)} (${formatCurrency(totalExtAmt)})", margin, y, bodyPaint)
    y += 18f
    canvas.drawText("Ore festive: ${formatHours(totalHolHours)} (${formatCurrency(totalHolAmt)})", margin, y, bodyPaint)
    y += 25f

    canvas.drawText("Ore totali: ${formatHours(totalOverallHours)}", margin, y, bodyPaint)
    y += 18f
    val totalEarnStr = formatCurrency(totalOverallEarnings)
    canvas.drawText("Importo totale: $totalEarnStr", margin, y, boldPaint)

    pdfDocument.finishPage(myPage)


    val directory = File(context.getExternalFilesDir(null), "reports")
    if (!directory.exists()) directory.mkdirs()
    
    val timestamp = TimeUtils.format(System.currentTimeMillis(), TimeUtils.fileTimestampFormatter)
    val fileName = "GestBraccianti_$timestamp.pdf"
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

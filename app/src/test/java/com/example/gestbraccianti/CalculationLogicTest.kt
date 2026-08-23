package com.example.gestbraccianti

import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.ui.utils.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalculationLogicTest {

    private val threshold = 8.0
    private val hourlyRate = 10.0
    private val extraRate = 15.0
    private val holidayRate = 20.0

    @Test
    fun testIsFestive() {
        // Saturday 2024-05-18
        val saturday = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 18, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Sunday 2024-05-19
        val sunday = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 19, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Monday 2024-05-20
        val monday = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 20, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // FestiveType 0: None
        assertEquals(false, TimeUtils.isFestive(saturday, false, 0))
        assertEquals(false, TimeUtils.isFestive(sunday, false, 0))
        assertEquals(false, TimeUtils.isFestive(monday, false, 0))

        // FestiveType 1: Saturday
        assertEquals(true, TimeUtils.isFestive(saturday, false, 1))
        assertEquals(false, TimeUtils.isFestive(sunday, false, 1))

        // FestiveType 2: Sunday
        assertEquals(false, TimeUtils.isFestive(saturday, false, 2))
        assertEquals(true, TimeUtils.isFestive(sunday, false, 2))

        // FestiveType 3: Sat & Sun
        assertEquals(true, TimeUtils.isFestive(saturday, false, 3))
        assertEquals(true, TimeUtils.isFestive(sunday, false, 3))
        assertEquals(false, TimeUtils.isFestive(monday, false, 3))

        // Manual override
        assertEquals(true, TimeUtils.isFestive(monday, true, 0))
    }

    @Test
    fun testCalculateAmounts() {
        // Case 1: Ordinary day, hours <= threshold
        val log1 = createLog(hours = 6.0, isManual = false)
        val amount1 = calculateAmount(log1, isFestive = false)
        assertEquals(60.0, amount1, 0.01)

        // Case 2: Ordinary day, hours > threshold
        val log2 = createLog(hours = 10.0, isManual = false)
        val amount2 = calculateAmount(log2, isFestive = false)
        // 8 * 10 + 2 * 15 = 80 + 30 = 110
        assertEquals(110.0, amount2, 0.01)

        // Case 3: Holiday day (Sunday, festiveType 2)
        val log3 = createLog(hours = 5.0, isManual = false)
        val amount3 = calculateAmount(log3, isFestive = true)
        // 5 * 20 = 100
        assertEquals(100.0, amount3, 0.01)

        // Case 4: Manual Holiday
        val log4 = createLog(hours = 8.0, isManual = true)
        val amount4 = calculateAmount(log4, isFestive = true)
        // 8 * 20 = 160
        assertEquals(160.0, amount4, 0.01)
    }

    private fun createLog(hours: Double, isManual: Boolean): WorkLog {
        return WorkLog(
            workerId = 1,
            harvestYearId = 2024,
            date = 0L,
            totalHours = hours,
            hourlyRate = hourlyRate,
            extraHourlyRate = extraRate,
            holidayHourlyRate = holidayRate,
            isManualHoliday = isManual
        )
    }

    private fun calculateAmount(log: WorkLog, isFestive: Boolean): Double {
        return if (isFestive) {
            log.totalHours * log.holidayHourlyRate
        } else {
            if (log.totalHours > threshold) {
                (threshold * log.hourlyRate) + ((log.totalHours - threshold) * log.extraHourlyRate)
            } else {
                log.totalHours * log.hourlyRate
            }
        }
    }
}

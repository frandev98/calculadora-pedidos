package com.francisco.calculadorapedidos.logic

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

data class WeeklySlotDef(val slotId: String, val fixedIndex: Int, val fallbackName: String, val targetPoints: Int)

object FuxionCalendarLogic {


    fun getAbsoluteWeek(periodId: Int, weekIndex: Int): Int {
        return ((periodId - 1) * 4) + weekIndex
    }

    fun getCycleWeek(absoluteWeek: Int): Int {
        val cycle = absoluteWeek % 13
        return if (cycle == 0) 13 else cycle
    }

    fun getSlotsForWeek(periodId: Int, weekId: Int, userStartPeriod: Int): List<WeeklySlotDef> {
        val deltaPeriods = (periodId - userStartPeriod + 13) % 13
        val relativeAbsWeek = (deltaPeriods * 4) + weekId
        val cycleWeek = ((relativeAbsWeek - 1) % 13) + 1

        return when (cycleWeek) {
            1 -> listOf(WeeklySlotDef("C1", 1, "Cliente 1", 60), WeeklySlotDef("C2", 2, "Cliente 2", 60))
            2 -> listOf(WeeklySlotDef("C3", 3, "Cliente 3", 60), WeeklySlotDef("C4", 4, "Cliente 4", 60))
            3 -> listOf(WeeklySlotDef("C5", 5, "Cliente 5", 60), WeeklySlotDef("C6", 6, "Cliente 6", 60))
            4 -> listOf(WeeklySlotDef("C7", 7, "Cliente 7", 60), WeeklySlotDef("C8", 8, "Cliente 8", 60))
            5 -> listOf(WeeklySlotDef("C1", 1, "Cliente 1", 60), WeeklySlotDef("C2", 2, "Cliente 2", 60))
            6 -> listOf(WeeklySlotDef("C3", 3, "Cliente 3", 60), WeeklySlotDef("C4", 4, "Cliente 4", 60))
            7 -> listOf(WeeklySlotDef("C5", 5, "Cliente 5", 60), WeeklySlotDef("C6", 6, "Cliente 6", 60))
            8 -> listOf(WeeklySlotDef("C7", 7, "Cliente 7", 60), WeeklySlotDef("C8", 8, "Cliente 8", 60))
            9 -> listOf(WeeklySlotDef("C1", 1, "Cliente 1", 60), WeeklySlotDef("C2", 2, "Cliente 2", 60))
            10 -> listOf(WeeklySlotDef("C3", 3, "Cliente 3", 60), WeeklySlotDef("C4", 4, "Cliente 4", 60))
            11 -> listOf(WeeklySlotDef("C5", 5, "Cliente 5", 60), WeeklySlotDef("C6", 6, "Cliente 6", 60))
            12 -> listOf(WeeklySlotDef("C7", 7, "Cliente 7", 60), WeeklySlotDef("C8", 8, "Cliente 8", 60))
            13 -> listOf(WeeklySlotDef("C9", 9, "Cliente 9", 125))
            else -> emptyList()
        }
    }

    // ARQUITECTURA TEMPORAL ESTÁTICA (Implementada sobre java.util para compatibilidad)

    fun getPeriodDates(year: Int, period: Int): Pair<Date, Date> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (period in 1..12) {
            cal.add(Calendar.DAY_OF_YEAR, (period - 1) * 28)
            val start = cal.time
            cal.add(Calendar.DAY_OF_YEAR, 27)
            val end = cal.time
            return Pair(start, end)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, 12 * 28)
            val start = cal.time
            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            val end = cal.time
            return Pair(start, end)
        }
    }

    fun getWeekDates(year: Int, period: Int, week: Int): Pair<Date, Date> {
        val (periodStart, periodEnd) = getPeriodDates(year, period)
        val cal = Calendar.getInstance().apply { time = periodStart }

        if (period == 13 && week == 4) {
            cal.add(Calendar.DAY_OF_YEAR, 21)
            return Pair(cal.time, periodEnd)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, (week - 1) * 7)
            val start = cal.time
            cal.add(Calendar.DAY_OF_YEAR, 6)
            return Pair(start, cal.time)
        }
    }

    data class FuxionStatus(
        val period: Int,
        val week: Int,
        val currentDayOfPeriod: Int,
        val daysRemainingInWeek: Int,
        val periodStartDate: Date,
        val weekStartDate: Date,
        val weekEndDate: Date
    )

    fun calculateStatus(currentDate: Date = Date()): FuxionStatus {
        val currentCa = Calendar.getInstance().apply {
            time = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val year = currentCa.get(Calendar.YEAR)
        val normalizedCurrent = currentCa.time

        for (p in 1..13) {
            val (pStart, pEnd) = getPeriodDates(year, p)
            if (!normalizedCurrent.before(pStart) && !normalizedCurrent.after(pEnd)) {
                for (w in 1..4) {
                    val (wStart, wEnd) = getWeekDates(year, p, w)
                    if (!normalizedCurrent.before(wStart) && !normalizedCurrent.after(wEnd)) {
                        val diffMillisWeek = wEnd.time - normalizedCurrent.time
                        val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillisWeek).toInt()

                        val diffMillisPeriod = normalizedCurrent.time - pStart.time
                        val currentDayOfPeriod = TimeUnit.MILLISECONDS.toDays(diffMillisPeriod).toInt() + 1

                        return FuxionStatus(p, w, currentDayOfPeriod, daysRemaining, pStart, wStart, wEnd)
                    }
                }
            }
        }

        val (fallbackStart, fallbackEnd) = getWeekDates(year, 13, 4)
        return FuxionStatus(13, 4, 1, 0, fallbackStart, fallbackStart, fallbackEnd)
    }

    fun getDaysInPeriod(year: Int, period: Int): Int {
        val (start, end) = getPeriodDates(year, period)
        val diff = end.time - start.time
        return TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
    }

    fun getDaysInWeek(year: Int, period: Int, week: Int): Int {
        val (start, end) = getWeekDates(year, period, week)
        val diff = end.time - start.time
        return TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
    }

    fun addDays(date: Date, days: Int): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.time
    }


}
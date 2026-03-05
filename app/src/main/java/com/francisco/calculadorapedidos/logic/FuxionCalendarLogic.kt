package com.francisco.calculadorapedidos.logic

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

// Definición estructural de la ranura de cliente
data class WeeklySlotDef(val slotId: String, val fixedIndex: Int, val fallbackName: String, val targetPoints: Int)

object FuxionCalendarLogic {

    fun getAbsoluteWeek(periodId: Int, weekIndex: Int): Int {
        return ((periodId - 1) * 4) + weekIndex
    }

    fun getCycleWeek(absoluteWeek: Int): Int {
        val cycle = absoluteWeek % 13
        return if (cycle == 0) 13 else cycle
    }

    // ALGORITMO CORREGIDO: Bucle iterativo 3x4 + Nodo aislado
    fun getSlotsForWeek(periodId: Int, weekId: Int, userStartPeriod: Int): List<WeeklySlotDef> {

        // Desfase modular para sincronizar con la fecha de afiliación del usuario
        val deltaPeriods = (periodId - userStartPeriod + 13) % 13
        val relativeAbsWeek = (deltaPeriods * 4) + weekId
        val cycleWeek = ((relativeAbsWeek - 1) % 13) + 1

        return when (cycleWeek) {
            // BLOQUE 1
            1 -> listOf(WeeklySlotDef("C1", 1, "Cliente 1", 60), WeeklySlotDef("C2", 2, "Cliente 2", 60))
            2 -> listOf(WeeklySlotDef("C3", 3, "Cliente 3", 60), WeeklySlotDef("C4", 4, "Cliente 4", 60))
            3 -> listOf(WeeklySlotDef("C5", 5, "Cliente 5", 60), WeeklySlotDef("C6", 6, "Cliente 6", 60))
            4 -> listOf(WeeklySlotDef("C7", 7, "Cliente 7", 60), WeeklySlotDef("C8", 8, "Cliente 8", 60))

            // BLOQUE 2
            5 -> listOf(WeeklySlotDef("C1", 1, "Cliente 1", 60), WeeklySlotDef("C2", 2, "Cliente 2", 60))
            6 -> listOf(WeeklySlotDef("C3", 3, "Cliente 3", 60), WeeklySlotDef("C4", 4, "Cliente 4", 60))
            7 -> listOf(WeeklySlotDef("C5", 5, "Cliente 5", 60), WeeklySlotDef("C6", 6, "Cliente 6", 60))
            8 -> listOf(WeeklySlotDef("C7", 7, "Cliente 7", 60), WeeklySlotDef("C8", 8, "Cliente 8", 60))

            // BLOQUE 3
            9 -> listOf(WeeklySlotDef("C1", 1, "Cliente 1", 60), WeeklySlotDef("C2", 2, "Cliente 2", 60))
            10 -> listOf(WeeklySlotDef("C3", 3, "Cliente 3", 60), WeeklySlotDef("C4", 4, "Cliente 4", 60))
            11 -> listOf(WeeklySlotDef("C5", 5, "Cliente 5", 60), WeeklySlotDef("C6", 6, "Cliente 6", 60))
            12 -> listOf(WeeklySlotDef("C7", 7, "Cliente 7", 60), WeeklySlotDef("C8", 8, "Cliente 8", 60))

            // NODO AISLADO (CIERRE DE CICLO PRO 500)
            13 -> listOf(WeeklySlotDef("C9", 9, "Cliente 9", 125))

            else -> emptyList()
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

    fun calculateStatus(anchorDate: Date, currentDate: Date = Date()): FuxionStatus {
        val startCa = Calendar.getInstance().apply { time = anchorDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val currentCa = Calendar.getInstance().apply { time = currentDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        val diffMillis = currentCa.timeInMillis - startCa.timeInMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

        if (diffDays < 0) return FuxionStatus(1, 1, 1, 6, anchorDate, anchorDate, addDays(anchorDate, 6))

        val totalWeeksPassed = diffDays / 7
        val periodIndex = totalWeeksPassed / 4
        val currentPeriod = periodIndex + 1
        val weekIndex = totalWeeksPassed % 4
        val currentWeek = weekIndex + 1
        val currentDayOfPeriod = (diffDays % 28) + 1
        val dayOfWeekIndex = diffDays % 7
        val daysRemainingInWeek = 6 - dayOfWeekIndex
        val currentPeriodStartDate = addDays(anchorDate, periodIndex * 28)
        val currentWeekStartDate = addDays(anchorDate, totalWeeksPassed * 7)
        val currentWeekEndDate = addDays(currentWeekStartDate, 6)

        return FuxionStatus(
            period = currentPeriod,
            week = currentWeek,
            currentDayOfPeriod = currentDayOfPeriod,
            daysRemainingInWeek = daysRemainingInWeek,
            periodStartDate = currentPeriodStartDate,
            weekStartDate = currentWeekStartDate,
            weekEndDate = currentWeekEndDate
        )
    }

    fun getFullYearPlan(anchorDate: Date): List<PeriodInfo> {
        val list = mutableListOf<PeriodInfo>()
        var pointer = Calendar.getInstance().apply { time = anchorDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        for (p in 1..13) {
            val start = pointer.time
            pointer.add(Calendar.DAY_OF_YEAR, 27)
            val end = pointer.time
            pointer.add(Calendar.DAY_OF_YEAR, 1)

            list.add(PeriodInfo(p, start, end))
        }
        return list
    }

    data class PeriodInfo(val number: Int, val start: Date, val end: Date)

    fun addDays(date: Date, days: Int): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.time
    }

    fun getPeriodDates(anchor: Date, periodNumber: Int): Pair<Date, Date> {
        val daysToStart = (periodNumber - 1) * 28
        val start = addDays(anchor, daysToStart)
        val end = addDays(start, 27)
        return Pair(start, end)
    }
}
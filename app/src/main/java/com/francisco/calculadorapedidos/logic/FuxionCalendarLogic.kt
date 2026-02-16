package com.francisco.calculadorapedidos.logic

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Lógica pura para el calendario Fuxion.
 * Se basa en una "Fecha Ancla" (Inicio del Periodo 1) para calcular matemáticamente
 * en qué punto del ciclo estamos.
 *
 * Reglas:
 * - 1 Año Fuxion = 13 Periodos (o más, pero el ciclo base es de 13)
 * - 1 Periodo = 4 Semanas
 * - 1 Semana = 7 Días
 * - Total ciclo base: 13 * 4 * 7 = 364 días.
 *
 * Nota: Fuxion a veces tiene "Semana 53" o ajustes por feriados.
 * Esta clase maneja la lógica ESTÁNDAR. Las excepciones se manejarán
 * permitiendo editar manualmente las fechas en la UI.
 */
object FuxionCalendarLogic {

    data class FuxionStatus(
        val period: Int,        // 1..13
        val week: Int,          // 1..4
        val currentDayOfPeriod: Int, // 1..28
        val daysRemainingInWeek: Int, // 0..6 (0 = hoy es cierre)
        val periodStartDate: Date,
        val weekStartDate: Date,
        val weekEndDate: Date // Fecha de cierre (usualmente Martes prefijado)
    )

    /**
     * Calcula el estado actual dado el inicio del Periodo 1.
     * @param anchorDate Inicio del Periodo 1 del año en curso.
     * @param currentDate Fecha actual (por defecto 'ahora').
     */
    fun calculateStatus(anchorDate: Date, currentDate: Date = Date()): FuxionStatus {
        // Normalizamos fechas a medianoche para evitar problemas de horas
        val startCa = Calendar.getInstance().apply { time = anchorDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val currentCa = Calendar.getInstance().apply { time = currentDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        val diffMillis = currentCa.timeInMillis - startCa.timeInMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

        // Si la fecha actual es anterior al ancla, asumimos dia 1 del P1 (o error, pero mejor fallback)
        if (diffDays < 0) return FuxionStatus(1, 1, 1, 6, anchorDate, anchorDate, addDays(anchorDate, 6))

        // Cálculos matemáticos puros
        // Periodo (0-indexed internamente)
        val totalWeeksPassed = diffDays / 7
        val periodIndex = totalWeeksPassed / 4
        val currentPeriod = periodIndex + 1

        // Semana dentro del periodo (0-3)
        val weekIndex = totalWeeksPassed % 4
        val currentWeek = weekIndex + 1

        // Días dentro del periodo y semana
        val currentDayOfPeriod = (diffDays % 28) + 1
        val dayOfWeekIndex = diffDays % 7 // 0..6 (0 = primer dia de la semana)
        val daysRemainingInWeek = 6 - dayOfWeekIndex

        // Fechas de inicio/fin
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
    
    // Helper para obtener la lista de los 13 periodos del año (Para la UI "Year Overview")
    fun getFullYearPlan(anchorDate: Date): List<PeriodInfo> {
        val list = mutableListOf<PeriodInfo>()
        var pointer = Calendar.getInstance().apply { time = anchorDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        
        for (p in 1..13) {
            val start = pointer.time
            pointer.add(Calendar.DAY_OF_YEAR, 27) // Fin es start + 27 dias
            val end = pointer.time
            pointer.add(Calendar.DAY_OF_YEAR, 1) // Avanzamos al siguiente periodo
            
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
}

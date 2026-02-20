package com.francisco.calculadorapedidos.logic

data class StrategyTask(
    val clientIndex: Int, // El número del cliente fijo (1-27)
    val targetPoints: Int // Cuántos puntos debe hacer
)

object StrategyEngine {

    // DEFINICIÓN EXACTA DE TU EXCEL
    // Pair(Protagonistas, Apoyo)
    // Protagonistas: Quienes compran en Semana 1, 2 y 3
    // Apoyo: Quienes compran en Semana 4 (Grupo de 3)
    private val PERIOD_STRATEGY_MAP = mapOf(
        1 to Pair(listOf(1, 2, 3),      listOf(4, 5, 6)),
        2 to Pair(listOf(7, 8, 9),      listOf(10, 11, 12)),
        3 to Pair(listOf(4, 5, 6),      listOf(1, 2, 3)),       // Inversión de P1
        4 to Pair(listOf(10, 11, 12),   listOf(7, 8, 9)),       // Inversión de P2
        5 to Pair(listOf(1, 2, 3),      listOf(13, 14, 15)),    // Entra Grupo E
        6 to Pair(listOf(4, 5, 6),      listOf(16, 17, 18)),    // Entra Grupo F
        7 to Pair(listOf(7, 8, 9),      listOf(13, 14, 15)),
        8 to Pair(listOf(10, 11, 12),   listOf(16, 17, 18)),
        9 to Pair(listOf(19, 20, 21),   listOf(22, 23, 24)),    // Entran G y H
        10 to Pair(listOf(16, 17, 18),  listOf(19, 20, 21)),    // Grupo F pasa a Protagonista
        11 to Pair(listOf(13, 14, 15),  listOf(16, 17, 18)),    // Grupo E pasa a Protagonista
        12 to Pair(listOf(19, 20, 21),  listOf(25, 26, 27)),    // Entra Grupo I
        13 to Pair(listOf(22, 23, 24),  listOf(25, 26, 27))     // El último bloque del Excel
    )

    fun getRequiredClients(period: Int, week: Int, periodGoal: Int): List<StrategyTask> {
        // 1. OBTENER CONFIGURACIÓN DEL PERIODO
        // Si nos piden un periodo > 13 (ej. año siguiente), usamos el mapa ciclicamente o por defecto el 1
        val safePeriod = if (period > 13) (period - 1) % 13 + 1 else period
        val strategy = PERIOD_STRATEGY_MAP[safePeriod]
            ?: Pair(listOf(1, 2, 3), listOf(4, 5, 6)) // Fallback de seguridad

        val protagonists = strategy.first
        val support = strategy.second

        // 2. DETERMINAR PUNTOS SEGÚN META (Base vs Pro)
        val isProStrategy = periodGoal >= 645

        val heavyPoints = if (isProStrategy) 180 else 120
        val lightPoints = if (isProStrategy) 35 else 60

        // 3. ASIGNAR TAREAS
        return if (week < 4) {
            // SEMANAS 1, 2, 3: Toca a un Protagonista
            // Semana 1 -> índice 0, Semana 2 -> índice 1, Semana 3 -> índice 2
            val clientIndex = protagonists.getOrElse(week - 1) { 1 }
            listOf(StrategyTask(clientIndex, heavyPoints))
        } else {
            // SEMANA 4: Toca a todo el equipo de Apoyo
            support.map { clientIndex ->
                StrategyTask(clientIndex, lightPoints)
            }
        }
    }
}
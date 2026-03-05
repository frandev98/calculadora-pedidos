package com.francisco.calculadorapedidos.logic

object FuxionFinancialLogic {

    data class TimeCoordinate(val year: Int, val period: Int, val week: Int)

    /**
     * Algoritmo de Eje de Tiempo Absoluto.
     * Calcula la coordenada temporal exacta de la semana actual y las 3 semanas anteriores,
     * cruzando umbrales de periodos y años sin desbordamiento de índices.
     */
    fun getPV4Coordinates(currentYear: Int, currentPeriod: Int, currentWeek: Int): List<TimeCoordinate> {
        val coordinates = mutableListOf<TimeCoordinate>()

        // Conversión a escala lineal de 52 semanas
        val currentAbsoluteWeek = ((currentPeriod - 1) * 4) + currentWeek

        for (i in 0..3) {
            var targetAbsWeek = currentAbsoluteWeek - i
            var targetYear = currentYear

            // Condición de Frontera: Retroceso de Año (Year Shifting)
            if (targetAbsWeek <= 0) {
                targetAbsWeek += 52
                targetYear -= 1
            }

            // Descompresión a Coordenadas Fuxion
            val targetPeriod = ((targetAbsWeek - 1) / 4) + 1
            val targetWeek = ((targetAbsWeek - 1) % 4) + 1

            coordinates.add(TimeCoordinate(targetYear, targetPeriod, targetWeek))
        }

        return coordinates
    }

    /**
     * Función Escalonada de Descuento (Reglas de Negocio Estrictas).
     */
    fun getDiscountPercentage(pv4Points: Int): Double {
        return when {
            pv4Points >= 500 -> 0.40
            pv4Points >= 300 -> 0.30
            pv4Points >= 100 -> 0.25
            pv4Points >= 60 -> 0.20
            else -> 0.0
        }
    }

    /**
     * Cálculo de Rentabilidad Neta (Extracción de IGV - 1.18).
     */
    fun calculateDirectSalesBonus(totalPrice: Double, discountPercentage: Double): Double {
        if (discountPercentage == 0.0) return 0.0
        return (totalPrice * discountPercentage) / 1.18
    }

    /**
     * Constante Condicional Corporativa.
     */
    fun calculatePro1Bonus(periodTotalPoints: Int): Double {
        return if (periodTotalPoints >= 500) 200.0 else 0.0
    }
}
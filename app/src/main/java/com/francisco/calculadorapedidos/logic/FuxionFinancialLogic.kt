package com.francisco.calculadorapedidos.logic

object FuxionFinancialLogic {

    data class TimeCoordinate(val year: Int, val period: Int, val week: Int)

    fun getPV4Coordinates(currentYear: Int, currentPeriod: Int, currentWeek: Int): List<TimeCoordinate> {
        val coordinates = mutableListOf<TimeCoordinate>()
        val currentAbsoluteWeek = ((currentPeriod - 1) * 4) + currentWeek

        for (i in 0..3) {
            var targetAbsWeek = currentAbsoluteWeek - i
            var targetYear = currentYear

            if (targetAbsWeek <= 0) {
                targetAbsWeek += 52
                targetYear -= 1
            }

            val targetPeriod = ((targetAbsWeek - 1) / 4) + 1
            val targetWeek = ((targetAbsWeek - 1) % 4) + 1

            coordinates.add(TimeCoordinate(targetYear, targetPeriod, targetWeek))
        }
        return coordinates
    }

    // --- NUEVO: ESCALÓN DE DESCUENTO DIRECTO (PAQUETE DE INICIO) ---
    fun getAffiliationDiscount(affiliationPv: Int): Double {
        return when {
            affiliationPv >= 300 -> 0.30
            affiliationPv >= 100 -> 0.25
            affiliationPv >= 40 -> 0.20
            else -> 0.0 // Aún no alcanza el mínimo de activación
        }
    }

    // ESCALÓN DE DESCUENTO REGULAR (CASHBACK PV4)
    fun getDiscountPercentage(pv4Points: Int): Double {
        return when {
            pv4Points >= 500 -> 0.40
            pv4Points >= 300 -> 0.30
            pv4Points >= 100 -> 0.25
            pv4Points >= 60 -> 0.20
            else -> 0.0
        }
    }

    fun calculateDirectSalesBonus(totalPrice: Double, discountPercentage: Double): Double {
        if (discountPercentage == 0.0) return 0.0
        return (totalPrice * discountPercentage) / 1.18
    }

    fun calculatePro1Bonus(periodTotalPoints: Int): Double {
        return if (periodTotalPoints >= 500) 200.0 else 0.0
    }
}
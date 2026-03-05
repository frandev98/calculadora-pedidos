package com.francisco.calculadorapedidos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.OrderMetrics
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.logic.FuxionFinancialLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// CONTRATO INMUTABLE DE ESTADO UI
data class PeriodUiState(
    val totalPoints: Int = 0,
    val directSalesBonus: Double = 0.0,
    val pro1Bonus: Double = 0.0,
    val weeklyMetrics: Map<Int, OrderMetrics> = emptyMap(),
    val isLoading: Boolean = true
)

class PeriodViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PeriodUiState())
    val uiState: StateFlow<PeriodUiState> = _uiState.asStateFlow()

    fun loadPeriodData(year: Int, periodId: Int, orderRepository: OrderRepository) {
        // EJECUCIÓN ESTRICTA EN HILO SECUNDARIO (I/O)
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            var tempPoints = 0
            var tempDirectSalesBonus = 0.0
            val metricsMap = mutableMapOf<Int, OrderMetrics>()

            for (w in 1..4) {
                val metrics = orderRepository.getWeekMetrics(year, periodId, w)
                metricsMap[w] = metrics
                tempPoints += metrics.points

                if (metrics.money > 0) {
                    val coords = FuxionFinancialLogic.getPV4Coordinates(year, periodId, w)
                    var weekHistoricalPV4 = 0
                    coords.forEach { coord ->
                        weekHistoricalPV4 += orderRepository.getWeekMetrics(coord.year, coord.period, coord.week).points
                    }
                    val discount = FuxionFinancialLogic.getDiscountPercentage(weekHistoricalPV4)
                    tempDirectSalesBonus += FuxionFinancialLogic.calculateDirectSalesBonus(metrics.money, discount)
                }
            }

            val computedPro1 = FuxionFinancialLogic.calculatePro1Bonus(tempPoints)

            // MUTACIÓN DE ESTADO ATÓMICA
            _uiState.value = PeriodUiState(
                totalPoints = tempPoints,
                directSalesBonus = tempDirectSalesBonus,
                pro1Bonus = computedPro1,
                weeklyMetrics = metricsMap,
                isLoading = false
            )
        }
    }
}
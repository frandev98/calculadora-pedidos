package com.francisco.calculadorapedidos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.OrderMetrics
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.logic.FuxionFinancialLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// CONTRATO INMUTABLE DE ESTADO
data class WeekUiState(
    val clientProgress: Map<String, OrderMetrics> = emptyMap(),
    val currentWeekPoints: Int = 0,
    val currentWeekMoney: Double = 0.0,
    val currentPV4Points: Int = 0,
    val currentDiscount: Double = 0.0,
    val activeWildcards: List<Client> = emptyList(),
    val isLoading: Boolean = true
)

class WeekViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeekUiState())
    val uiState: StateFlow<WeekUiState> = _uiState.asStateFlow()

    fun loadWeekData(
        year: Int,
        periodId: Int,
        weekId: Int,
        allClients: List<Client>,
        orderRepository: OrderRepository
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val progressMap = mutableMapOf<String, OrderMetrics>()
            var totalPts = 0
            var totalMoney = 0.0
            val wildcardsInThisWeek = mutableListOf<Client>()

            // ITERACIÓN DE MATRIZ DE CLIENTES
            allClients.forEach { client ->
                val order = orderRepository.getOrder(year, periodId, weekId, client.id)
                val pts = order.sumOf { it.first.points * it.second }.toInt()
                val money = order.sumOf { it.first.price * it.second }.toDouble()

                if (pts > 0) {
                    progressMap[client.id] = OrderMetrics(pts, money)
                    totalPts += pts
                    totalMoney += money
                    if (client.type == com.francisco.calculadorapedidos.data.ClientType.WILDCARD) {
                        wildcardsInThisWeek.add(client)
                    }
                } else if (client.type == com.francisco.calculadorapedidos.data.ClientType.FIXED) {
                    progressMap[client.id] = OrderMetrics(0, 0.0)
                }
            }

            // EXTRACCIÓN HISTÓRICA DE PV4
            val coordinates = FuxionFinancialLogic.getPV4Coordinates(year, periodId, weekId)
            var historicalPV4 = 0
            coordinates.forEach { coord ->
                historicalPV4 += orderRepository.getWeekMetrics(coord.year, coord.period, coord.week).points
            }
            val discount = FuxionFinancialLogic.getDiscountPercentage(historicalPV4)

            // EMISIÓN ATÓMICA DE ESTADO
            _uiState.value = WeekUiState(
                clientProgress = progressMap,
                currentWeekPoints = totalPts,
                currentWeekMoney = totalMoney,
                currentPV4Points = historicalPV4,
                currentDiscount = discount,
                activeWildcards = wildcardsInThisWeek,
                isLoading = false
            )
        }
    }
}
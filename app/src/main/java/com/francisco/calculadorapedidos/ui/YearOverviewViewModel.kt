package com.francisco.calculadorapedidos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.OrderMetrics
import com.francisco.calculadorapedidos.data.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YearOverviewViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    // CACHÉ DE ESTADO LÁBIL: Mapeo O(1) -> Año a sus 13 periodos
    private val _yearlyMetricsCache = MutableStateFlow<Map<Int, Map<Int, OrderMetrics>>>(emptyMap())
    val yearlyMetricsCache: StateFlow<Map<Int, Map<Int, OrderMetrics>>> = _yearlyMetricsCache.asStateFlow()

    fun loadYear(year: Int) {
        // Inhibición de recargas iterativas si la matriz de memoria ya contiene el año
        if (_yearlyMetricsCache.value.containsKey(year)) return

        viewModelScope.launch(Dispatchers.IO) {
            val yearMetrics = mutableMapOf<Int, OrderMetrics>()
            for (period in 1..13) {
                yearMetrics[period] = orderRepository.getPeriodMetrics(year, period)
            }

            // Mutación atómica de estado
            val currentCache = _yearlyMetricsCache.value.toMutableMap()
            currentCache[year] = yearMetrics
            _yearlyMetricsCache.value = currentCache
        }
    }
}
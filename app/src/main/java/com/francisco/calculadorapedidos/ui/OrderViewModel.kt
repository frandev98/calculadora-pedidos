package com.francisco.calculadorapedidos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.DistributionConfig
import com.francisco.calculadorapedidos.data.DistributionResult
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.logic.DistributionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderViewModel : ViewModel() {

    // --- ESTADO ---
    private val _selectedProducts = mutableStateListOf<Pair<Product, Int>>()
    val selectedProducts: List<Pair<Product, Int>> get() = _selectedProducts

    var distributionResult by mutableStateOf<DistributionResult?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var targetGoal by mutableStateOf(540)
        private set

    // NUEVO: Saber si es modo semanal
    var isWeeklyMode by mutableStateOf(false)
        private set

    private val distributionCalculator = DistributionCalculator()

    // --- CONFIGURACIÓN ---
    fun setupMode(goal: Int, isWeekly: Boolean) {
        targetGoal = goal
        isWeeklyMode = isWeekly
        // Limpiamos resultados previos si cambiamos de modo
        distributionResult = null
    }

    // --- GESTIÓN DE PRODUCTOS (Reutilizada) ---
    fun addProduct(product: Product) {
        val existingIndex = _selectedProducts.indexOfFirst { it.first.id == product.id }
        if (existingIndex != -1) {
            val (prod, qty) = _selectedProducts[existingIndex]
            _selectedProducts[existingIndex] = prod to (qty + 1)
        } else {
            _selectedProducts.add(product to 1)
        }
    }

    fun incrementQuantity(product: Product) {
        val index = _selectedProducts.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val (prod, qty) = _selectedProducts[index]
            _selectedProducts[index] = prod to (qty + 1)
        }
    }

    fun decrementQuantity(product: Product) {
        val index = _selectedProducts.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val (prod, qty) = _selectedProducts[index]
            if (qty > 1) {
                _selectedProducts[index] = prod to (qty - 1)
            }
        }
    }

    fun removeProduct(product: Product) {
        _selectedProducts.removeAll { it.first.id == product.id }
    }

    // --- NUEVA FUNCIÓN: LIMPIAR CARRITO ---
    fun clearCart() {
        _selectedProducts.clear()
    }

    fun clearResult() {
        distributionResult = null
    }

    // --- LÓGICA DE ACCIÓN PRINCIPAL ---
    fun onPrincipalActionButtonClick() {
        if (isWeeklyMode) {
            // LÓGICA SEMANAL: Aquí podrías guardar en base de datos o simplemente mostrar confirmación
            // Por ahora, no hacemos cálculo complejo, quizás solo un mensaje de éxito.
            // (Para este MVP, no haremos nada complejo aquí, la UI manejará la visualización)
        } else {
            // LÓGICA PERIODO: Ejecuta el algoritmo de distribución
            calculateDistribution()
        }
    }

    private fun calculateDistribution() {
        isLoading = true
        val config = DistributionConfig(targetPoints = targetGoal.toDouble())

        viewModelScope.launch(Dispatchers.Default) {
            delay(1000)
            val result = distributionCalculator.calculate(_selectedProducts, config)
            withContext(Dispatchers.Main) {
                distributionResult = result
                isLoading = false
            }
        }
    }

    fun loadProducts(products: List<Pair<Product, Int>>) {
        _selectedProducts.clear()
        _selectedProducts.addAll(products)
    }
}
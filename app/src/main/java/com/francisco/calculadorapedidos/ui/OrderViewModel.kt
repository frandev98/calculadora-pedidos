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

    // --- 1. ESTADO DE LA LISTA DE PRODUCTOS ---
    private val _selectedProducts = mutableStateListOf<Pair<Product, Int>>()
    val selectedProducts: List<Pair<Product, Int>> get() = _selectedProducts

    // --- 2. ESTADO DEL RESULTADO ---
    var distributionResult by mutableStateOf<DistributionResult?>(null)
        private set

    // --- 3. ESTADO DE CARGA (Loading) ---
    // CORRECCIÓN: Eliminado el duplicado. Solo debe aparecer una vez.
    var isLoading by mutableStateOf(false)
        private set

    // --- 4. META ACTUAL (Por defecto 540) ---
    var targetGoal by mutableStateOf(540)
        private set

    // Función para actualizar la meta desde la UI (ej: al recibirla del Dashboard)
    fun setGoal(goal: Int) {
        targetGoal = goal
    }

    // --- 5. LA LÓGICA (Instanciamos la calculadora) ---
    private val distributionCalculator = DistributionCalculator()

    // --- FUNCIONES DE GESTIÓN DEL CARRITO ---

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

    fun clearResult() {
        distributionResult = null
    }

    fun getTotalPoints(): Double {
        return _selectedProducts.sumOf { it.first.points * it.second }
    }

    // --- 6. LA FUNCIÓN MAESTRA: CALCULAR ---
    fun calculateDistribution() {
        isLoading = true

        // CORRECCIÓN: Usamos 'targetGoal' para configurar la calculadora.
        // Así le pasamos la intención del usuario (540 o 645).
        // (Asegúrate de que tu clase DistributionConfig acepte este parámetro,
        // si no, déjalo vacío como DistributionConfig()).
        val config = DistributionConfig(targetPoints = targetGoal.toDouble())

        viewModelScope.launch(Dispatchers.Default) {

            delay(1000) // Animación UX

            val result = distributionCalculator.calculate(_selectedProducts, config)

            withContext(Dispatchers.Main) {
                distributionResult = result
                isLoading = false
            }
        }
    }
}
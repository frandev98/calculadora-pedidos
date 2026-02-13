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
import kotlinx.coroutines.withContext // <--- ESTE IMPORT FALTABA

class OrderViewModel : ViewModel() {

    // --- 1. ESTADO DE LA LISTA DE PRODUCTOS ---
    // Usamos mutableStateListOf para que la UI se actualice al cambiar la lista
    private val _selectedProducts = mutableStateListOf<Pair<Product, Int>>()
    val selectedProducts: List<Pair<Product, Int>> get() = _selectedProducts

    // --- 2. ESTADO DEL RESULTADO ---
    var distributionResult by mutableStateOf<DistributionResult?>(null)
        private set

    // --- 3. ESTADO DE CARGA (Loading) ---
    var isLoading by mutableStateOf(false)
        private set

    // --- 4. LA LÓGICA (Instanciamos la calculadora) ---
    // Le ponemos el nombre correcto para que coincida con la función de abajo
    private val distributionCalculator = DistributionCalculator()

    // --- FUNCIONES DE GESTIÓN DEL CARRITO ---

    // Agregar producto nuevo (o sumar si ya existe)
    // Esta función la usa el Catálogo
    fun addProduct(product: Product) {
        val existingIndex = _selectedProducts.indexOfFirst { it.first.id == product.id }
        if (existingIndex != -1) {
            val (prod, qty) = _selectedProducts[existingIndex]
            _selectedProducts[existingIndex] = prod to (qty + 1)
        } else {
            _selectedProducts.add(product to 1)
        }
    }

    // Incrementar cantidad (+)
    fun incrementQuantity(product: Product) {
        val index = _selectedProducts.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val (prod, qty) = _selectedProducts[index]
            _selectedProducts[index] = prod to (qty + 1)
        }
    }

    // Decrementar cantidad (-)
    // NOTA: Aquí aplicamos el "Freno de Mano". Solo resta si es mayor a 1.
    // Para eliminar, el usuario debe usar el botón de eliminar explícito.
    fun decrementQuantity(product: Product) {
        val index = _selectedProducts.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val (prod, qty) = _selectedProducts[index]
            if (qty > 1) {
                _selectedProducts[index] = prod to (qty - 1)
            }
            // Si es 1, no hace nada (por seguridad UX)
        }
    }

    // Eliminar producto completamente (Papelera)
    fun removeProduct(product: Product) {
        _selectedProducts.removeAll { it.first.id == product.id }
    }

    // Limpiar resultados (Botón volver)
    fun clearResult() {
        distributionResult = null
    }

    // Obtener total de puntos para la barra de progreso
    fun getTotalPoints(): Double {
        return _selectedProducts.sumOf { it.first.points * it.second }
    }

    // --- 5. LA FUNCIÓN MAESTRA: CALCULAR ---
    fun calculateDistribution() {
        // A. Encendemos el indicador de carga
        isLoading = true

        // B. Preparamos la configuración (Esto faltaba en tu código anterior)
        val config = DistributionConfig()

        // C. Lanzamos el proceso en segundo plano (Default Dispatcher)
        viewModelScope.launch(Dispatchers.Default) {

            // Pequeña pausa para que se vea la animación (UX)
            delay(1000)

            // D. Ejecutamos las 20,000 iteraciones (Trabajo pesado)
            val result = distributionCalculator.calculate(_selectedProducts, config)

            // E. Volvemos al hilo principal para mostrar el resultado
            withContext(Dispatchers.Main) {
                distributionResult = result
                isLoading = false // Apagamos el indicador
            }
        }
    }
}
package com.francisco.calculadorapedidos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.ClientType
import com.francisco.calculadorapedidos.data.DistributionResult
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.logic.DistributionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class OrderViewModel : ViewModel() {

    private val _selectedProducts = mutableStateListOf<Pair<Product, Int>>()
    val selectedProducts: List<Pair<Product, Int>> get() = _selectedProducts

    var distributionResult by mutableStateOf<DistributionResult?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var targetGoal by mutableStateOf(540)
        private set

    var isWeeklyMode by mutableStateOf(false)
        private set

    var currentPeriodId by mutableStateOf(1)
        private set

    var userStartPeriod by mutableStateOf(1)
        private set

    private val distributionCalculator = DistributionCalculator()

    fun setupMode(goal: Int, isWeekly: Boolean, periodId: Int, startPeriod: Int) {
        targetGoal = goal
        isWeeklyMode = isWeekly
        currentPeriodId = periodId
        userStartPeriod = startPeriod
        distributionResult = null
    }

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

    fun clearCart() {
        _selectedProducts.clear()
    }

    fun clearResult() {
        distributionResult = null
    }

    fun onPrincipalActionButtonClick() {
        if (!isWeeklyMode) {
            calculateDistribution()
        }
    }

    private fun calculateDistribution() {
        isLoading = true

        viewModelScope.launch(Dispatchers.Default) {
            delay(1000)
            val result = distributionCalculator.calculate(_selectedProducts, currentPeriodId, userStartPeriod)
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

    // RESOLUCIÓN DE RUPTURA: Inyección de Corrutina para I/O Relacional
    fun saveFullDistribution(year: Int, result: DistributionResult, orderRepository: OrderRepository, clientRepository: ClientRepository) {
        viewModelScope.launch {
            val weeks = listOf(result.week1, result.week2, result.week3, result.week4)

            for (week in weeks) {
                for (slot in week.slots) {
                    var clientEntity = clientRepository.getFixedClient(slot.fixedIndex)

                    if (clientEntity == null) {
                        val newClient = Client(
                            id = UUID.randomUUID().toString(),
                            name = slot.clientId,
                            type = ClientType.FIXED,
                            fixedIndex = slot.fixedIndex
                        )
                        clientRepository.saveClient(newClient)
                        clientEntity = newClient
                    }

                    val realClientId = clientEntity.id
                    val productsToSave = slot.items.map { Pair(it.product, it.quantity) }

                    // Delegación segura: el repositorio ya maneja su propio Dispatchers.IO internamente
                    orderRepository.saveOrder(year, currentPeriodId, week.weekIndex, realClientId, productsToSave)
                }
            }
        }
    }
}
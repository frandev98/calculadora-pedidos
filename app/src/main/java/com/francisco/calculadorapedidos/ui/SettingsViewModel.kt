package com.francisco.calculadorapedidos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.data.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository,
    private val dataStore: FuxionDataStore
) : ViewModel() {

    val userStartTimestampFlow: StateFlow<Long?> = dataStore.userStartTimestampFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveUserStartTimestamp(timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) { dataStore.saveUserStartTimestamp(timestamp) }
    }

    fun factoryReset(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            orderRepository.wipeAllRelationalData()
            clientRepository.wipeAllClients()
            dataStore.clearData()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }
}
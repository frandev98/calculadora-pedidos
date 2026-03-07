package com.francisco.calculadorapedidos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.ClientType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor(
    private val repository: ClientRepository
) : ViewModel() {

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    init {
        loadClients()
    }

    fun loadClients() {
        viewModelScope.launch {
            var currentList = repository.getAllClients()

            // AUDITORÍA DE INTEGRIDAD: Garantizar ranuras fijas 1 al 9
            val fixedClients = currentList.filter { it.type == ClientType.FIXED }
            var dbMutated = false

            for (i in 1..9) {
                if (fixedClients.none { it.fixedIndex == i }) {
                    val stubClient = Client(
                        id = UUID.randomUUID().toString(),
                        name = "Cliente $i",
                        type = ClientType.FIXED,
                        fixedIndex = i
                    )
                    repository.saveClient(stubClient)
                    dbMutated = true
                }
            }

            if (dbMutated) {
                currentList = repository.getAllClients()
            }

            _clients.value = currentList
        }
    }

    fun saveFixedClient(index: Int, name: String, code: String, email: String, pass: String) {
        viewModelScope.launch {
            val existingClient = _clients.value.find { it.fixedIndex == index }
            val client = Client(
                id = existingClient?.id ?: UUID.randomUUID().toString(),
                name = name,
                fuxionId = code,
                email = email,
                storePassword = pass,
                type = ClientType.FIXED,
                fixedIndex = index
            )
            repository.saveClient(client)
            loadClients()
        }
    }

    fun saveWildcardClient(name: String, code: String, email: String, pass: String, existingId: String? = null) {
        viewModelScope.launch {
            val client = Client(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                fuxionId = code,
                email = email,
                storePassword = pass,
                type = ClientType.WILDCARD,
                fixedIndex = null
            )
            repository.saveClient(client)
            loadClients()
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            repository.deleteClient(clientId)
            loadClients()
        }
    }
}
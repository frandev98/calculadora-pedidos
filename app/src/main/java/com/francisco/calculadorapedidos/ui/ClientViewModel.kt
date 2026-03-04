package com.francisco.calculadorapedidos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.ClientType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ClientViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClientRepository(application)

    // ESTA ERA LA VARIABLE QUE FALTABA Y CAUSABA EL ERROR "_clients"
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    init {
        loadClients()
    }

    fun loadClients() {
        viewModelScope.launch {
            var currentList = repository.getAllClients()

            // AUDITORÍA DE INTEGRIDAD: Garantizar ranuras fijas 1 al 9
            val fixedClients = currentList.filter { it.type == com.francisco.calculadorapedidos.data.ClientType.FIXED }
            var dbMutated = false

            for (i in 1..9) {
                if (fixedClients.none { it.fixedIndex == i }) {
                    val stubClient = com.francisco.calculadorapedidos.data.Client(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "Cliente $i",
                        type = com.francisco.calculadorapedidos.data.ClientType.FIXED,
                        fixedIndex = i
                    )
                    repository.saveClient(stubClient)
                    dbMutated = true
                }
            }

            // Recarga estricta si hubo mutación para mantener coherencia en memoria
            if (dbMutated) {
                currentList = repository.getAllClients()
            }

            _clients.value = currentList
        }
    }

    fun saveFixedClient(
        index: Int,
        name: String,
        code: String,
        email: String,       // <--- CAMBIO: Solo email
        pass: String
    ) {
        viewModelScope.launch {
            val existingClient = _clients.value.find { it.fixedIndex == index }

            val client = Client(
                id = existingClient?.id ?: UUID.randomUUID().toString(),
                name = name,
                fuxionId = code,
                email = email,
                storePassword = pass, // Guardamos en el nuevo campo
                type = ClientType.FIXED,
                fixedIndex = index
            )
            repository.saveClient(client)
            loadClients()
        }
    }

    fun saveWildcardClient(
        name: String,
        code: String,
        email: String,       // <--- CAMBIO: Solo email
        pass: String,
        existingId: String? = null
    ) {
        viewModelScope.launch {
            val client = Client(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                fuxionId = code,
                email = email,
                storePassword = pass, // Guardamos en el nuevo campo
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
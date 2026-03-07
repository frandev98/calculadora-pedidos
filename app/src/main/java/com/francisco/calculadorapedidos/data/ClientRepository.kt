package com.francisco.calculadorapedidos.data

import com.francisco.calculadorapedidos.data.db.ClientDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientRepository(private val clientDao: ClientDao) {

    suspend fun saveClient(client: Client) {
        withContext(Dispatchers.IO) { clientDao.insertClient(client) }
    }

    suspend fun deleteClient(clientId: String) {
        withContext(Dispatchers.IO) { clientDao.deleteClient(clientId) }
    }

    suspend fun getAllClients(): List<Client> {
        return withContext(Dispatchers.IO) { clientDao.getAllClients() }
    }

    suspend fun getFixedClient(index: Int): Client? {
        return withContext(Dispatchers.IO) { clientDao.getFixedClient(index) }
    }

    suspend fun wipeAllClients() {
        withContext(Dispatchers.IO) { clientDao.deleteAllClients() }
    }
}
package com.francisco.calculadorapedidos.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ClientRepository(context: Context) {
    private val prefs = context.getSharedPreferences("fuxion_clients_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val CLIENTS_KEY = "saved_clients_list"

    fun saveClient(client: Client) {
        val currentList = getAllClients().toMutableList()

        // Si ya existe (mismo ID o mismo índice fijo), lo reemplazamos
        val existingIndex = currentList.indexOfFirst {
            it.id == client.id || (client.type == ClientType.FIXED && it.fixedIndex == client.fixedIndex)
        }

        if (existingIndex != -1) {
            currentList[existingIndex] = client
        } else {
            currentList.add(client)
        }

        saveListToPrefs(currentList)
    }

    fun deleteClient(clientId: String) {
        val currentList = getAllClients().toMutableList()
        currentList.removeAll { it.id == clientId }
        saveListToPrefs(currentList)
    }

    fun getAllClients(): List<Client> {
        val json = prefs.getString(CLIENTS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Client>>() {}.type
            val rawList: List<Client> = gson.fromJson(json, type)

            // --- BLOQUE DE SANEAMIENTO ACTUALIZADO ---
            // Sanea los campos NUEVOS (email y storePassword)
            rawList.map { client ->
                client.copy(
                    email = client.email ?: "",
                    storePassword = client.storePassword ?: "",
                    fuxionId = client.fuxionId ?: "",
                    name = client.name ?: "Sin Nombre"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Obtener cliente fijo por su posición (1-27)
    fun getFixedClient(index: Int): Client? {
        return getAllClients().find { it.type == ClientType.FIXED && it.fixedIndex == index }
    }

    private fun saveListToPrefs(list: List<Client>) {
        val json = gson.toJson(list)
        prefs.edit().putString(CLIENTS_KEY, json).apply()
    }
}
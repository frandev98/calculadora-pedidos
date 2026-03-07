package com.francisco.calculadorapedidos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.francisco.calculadorapedidos.data.Client

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients")
    suspend fun getAllClients(): List<Client>

    @Query("SELECT * FROM clients WHERE type = 'FIXED' AND fixedIndex = :index LIMIT 1")
    suspend fun getFixedClient(index: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: String)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()
}
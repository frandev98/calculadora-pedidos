package com.francisco.calculadorapedidos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DraftDao {
    @Insert
    suspend fun insertDrafts(drafts: List<DraftEntity>)

    // CONSULTAS MUTADAS A 4 DIMENSIONES
    @Query("SELECT * FROM period_drafts WHERE year = :year AND period = :period AND week = :week AND clientId = :clientId")
    suspend fun getDrafts(year: Int, period: Int, week: Int, clientId: String): List<DraftEntity>

    @Query("DELETE FROM period_drafts WHERE year = :year AND period = :period AND week = :week AND clientId = :clientId")
    suspend fun clearDrafts(year: Int, period: Int, week: Int, clientId: String)

    @Query("DELETE FROM period_drafts")
    suspend fun wipeDrafts()
}
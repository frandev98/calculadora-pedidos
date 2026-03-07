package com.francisco.calculadorapedidos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DraftDao {
    @Insert
    suspend fun insertDrafts(drafts: List<DraftEntity>)

    @Query("SELECT * FROM period_drafts WHERE year = :year AND period = :period")
    suspend fun getDrafts(year: Int, period: Int): List<DraftEntity>

    @Query("DELETE FROM period_drafts WHERE year = :year AND period = :period")
    suspend fun clearDrafts(year: Int, period: Int)

    @Query("DELETE FROM period_drafts")
    suspend fun wipeDrafts()
}
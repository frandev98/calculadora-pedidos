package com.francisco.calculadorapedidos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OrderDao {

    @Insert
    fun insertItems(items: List<OrderRecordEntity>): List<Long>

    @Query("DELETE FROM order_records WHERE year = :year AND period = :period AND week = :week AND clientId = :clientId")
    fun deleteOrder(year: Int, period: Int, week: Int, clientId: String): Int

    @Query("SELECT * FROM order_records WHERE year = :year AND period = :period AND week = :week AND clientId = :clientId")
    fun getOrderItems(year: Int, period: Int, week: Int, clientId: String): List<OrderRecordEntity>

    // NUEVO: Extracción matricial para cálculo segregado en memoria
    @Query("SELECT * FROM order_records WHERE year = :year AND period = :period")
    fun getPeriodOrders(year: Int, period: Int): List<OrderRecordEntity>

    @Query("SELECT * FROM order_records WHERE year = :year AND period = :period AND week = :week")
    fun getWeekOrders(year: Int, period: Int, week: Int): List<OrderRecordEntity>

    @Query("DELETE FROM order_records")
    fun wipeDatabase(): Int
}
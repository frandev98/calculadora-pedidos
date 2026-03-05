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

    @Query("""
        SELECT COALESCE(SUM(points * quantity), 0.0) 
        FROM order_records 
        WHERE year = :year AND period = :period
    """)
    fun getPeriodTotalPoints(year: Int, period: Int): Double

    @Query("""
        SELECT COALESCE(SUM(price * quantity * CASE WHEN isAffiliation THEN 0.8 ELSE 1.0 END), 0.0) 
        FROM order_records 
        WHERE year = :year AND period = :period
    """)
    fun getPeriodTotalMoney(year: Int, period: Int): Double

    @Query("""
        SELECT COALESCE(SUM(points * quantity), 0.0) 
        FROM order_records 
        WHERE year = :year AND period = :period AND week = :week
    """)
    fun getWeekTotalPoints(year: Int, period: Int, week: Int): Double

    @Query("""
        SELECT COALESCE(SUM(price * quantity * CASE WHEN isAffiliation THEN 0.8 ELSE 1.0 END), 0.0) 
        FROM order_records 
        WHERE year = :year AND period = :period AND week = :week
    """)
    fun getWeekTotalMoney(year: Int, period: Int, week: Int): Double

    @Query("DELETE FROM order_records")
    fun wipeDatabase(): Int
}
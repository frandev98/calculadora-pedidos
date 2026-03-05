package com.francisco.calculadorapedidos.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

// DTO para guardar de forma segura
data class SavedCartItem(
    val product: Product?,
    val quantity: Int
)

// NUEVA ENTIDAD: Consolidación de lectura I/O
data class OrderMetrics(
    val points: Int,
    val money: Double
)

class OrderRepository(context: Context) {
    private val prefs = context.getSharedPreferences("fuxion_orders_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        migrateLegacyKeys()
    }

    private fun migrateLegacyKeys() {
        val allEntries = prefs.all
        val editor = prefs.edit()
        var mutated = false
        val fallbackYear = Calendar.getInstance().get(Calendar.YEAR)

        val orderRegex = Regex("^order_p(\\d+)_w(\\d+)_c(.*)$")
        val goalRegex = Regex("^goal_p(\\d+)$")
        val draftRegex = Regex("^draft_p(\\d+)$")

        for ((key, value) in allEntries) {
            val orderMatch = orderRegex.matchEntire(key)
            if (orderMatch != null && value is String) {
                val period = orderMatch.groupValues[1].toInt()
                val week = orderMatch.groupValues[2].toInt()
                val clientId = orderMatch.groupValues[3]

                editor.putString(getOrderKey(fallbackYear, period, week, clientId), value)
                editor.remove(key)
                mutated = true
                continue
            }

            val goalMatch = goalRegex.matchEntire(key)
            if (goalMatch != null && value is Int) {
                val period = goalMatch.groupValues[1].toInt()
                editor.putInt(getGoalKey(fallbackYear, period), value)
                editor.remove(key)
                mutated = true
                continue
            }

            val draftMatch = draftRegex.matchEntire(key)
            if (draftMatch != null && value is String) {
                val period = draftMatch.groupValues[1].toInt()
                editor.putString(getDraftKey(fallbackYear, period), value)
                editor.remove(key)
                mutated = true
                continue
            }
        }
        if (mutated) editor.apply()
    }

    private fun getOrderKey(year: Int, period: Int, week: Int, clientId: String) = "order_y${year}_p${period}_w${week}_c${clientId}"
    private fun getGoalKey(year: Int, period: Int) = "goal_y${year}_p$period"
    private fun getDraftKey(year: Int, period: Int) = "draft_y${year}_p$period"

    fun saveOrder(year: Int, period: Int, week: Int, clientId: String, products: List<Pair<Product, Int>>) {
        val safeList = products.map { SavedCartItem(it.first, it.second) }
        val json = gson.toJson(safeList)
        prefs.edit().putString(getOrderKey(year, period, week, clientId), json).apply()
    }

    fun getOrder(year: Int, period: Int, week: Int, clientId: String): List<Pair<Product, Int>> {
        val json = prefs.getString(getOrderKey(year, period, week, clientId), null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedCartItem>>() {}.type
            val safeList: List<SavedCartItem> = gson.fromJson(json, type)
            safeList.mapNotNull { item ->
                if (item.product != null) Pair(item.product, item.quantity) else null
            }
        } catch (e: Exception) { emptyList() }
    }

    fun clearOrder(year: Int, period: Int, week: Int, clientId: String) {
        prefs.edit().remove(getOrderKey(year, period, week, clientId)).apply()
    }

    fun savePeriodGoal(year: Int, period: Int, goal: Int) {
        prefs.edit().putInt(getGoalKey(year, period), goal).apply()
    }

    fun getPeriodGoal(year: Int, period: Int): Int {
        val key = getGoalKey(year, period)
        if (prefs.contains(key)) return prefs.getInt(key, 540)
        return when (period) {
            in 1..4 -> 540
            in 5..8 -> 645
            in 9..11 -> 540
            in 12..13 -> 645
            else -> 540
        }
    }

    // LECTURA CONSOLIDADA: Retorna Puntos y Dinero en un solo escaneo
    fun getPeriodMetrics(year: Int, period: Int): OrderMetrics {
        var totalPoints = 0.0
        var totalMoney = 0.0
        val allKeys = prefs.all.keys
        val prefix = "order_y${year}_p${period}_"
        val periodKeys = allKeys.filter { it.startsWith(prefix) }

        for (key in periodKeys) {
            val json = prefs.getString(key, null) ?: continue
            try {
                val type = object : TypeToken<List<SavedCartItem>>() {}.type
                val items: List<SavedCartItem> = gson.fromJson(json, type)
                totalPoints += items.sumOf { (it.product?.points ?: 0.0) * it.quantity }
                totalMoney += items.sumOf { (it.product?.price ?: 0.0) * it.quantity }
            } catch (e: Exception) { continue }
        }
        return OrderMetrics(totalPoints.toInt(), totalMoney)
    }

    fun getWeekMetrics(year: Int, period: Int, week: Int): OrderMetrics {
        var totalPoints = 0.0
        var totalMoney = 0.0
        val allKeys = prefs.all.keys
        val weekPrefix = "order_y${year}_p${period}_w${week}_"
        val weekKeys = allKeys.filter { it.startsWith(weekPrefix) }

        for (key in weekKeys) {
            val json = prefs.getString(key, null) ?: continue
            try {
                val type = object : TypeToken<List<SavedCartItem>>() {}.type
                val items: List<SavedCartItem> = gson.fromJson(json, type)
                totalPoints += items.sumOf { (it.product?.points ?: 0.0) * it.quantity }
                totalMoney += items.sumOf { (it.product?.price ?: 0.0) * it.quantity }
            } catch (e: Exception) { continue }
        }
        return OrderMetrics(totalPoints.toInt(), totalMoney)
    }

    fun savePeriodDraft(year: Int, period: Int, products: List<Pair<Product, Int>>) {
        val safeList = products.map { SavedCartItem(it.first, it.second) }
        val json = gson.toJson(safeList)
        prefs.edit().putString(getDraftKey(year, period), json).apply()
    }

    fun getPeriodDraft(year: Int, period: Int): List<Pair<Product, Int>> {
        val json = prefs.getString(getDraftKey(year, period), null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedCartItem>>() {}.type
            val safeList: List<SavedCartItem> = gson.fromJson(json, type)
            safeList.mapNotNull { item ->
                if (item.product != null) Pair(item.product, item.quantity) else null
            }
        } catch (e: Exception) { emptyList() }
    }

    fun clearPeriodDraft(year: Int, period: Int) {
        prefs.edit().remove(getDraftKey(year, period)).apply()
    }
}
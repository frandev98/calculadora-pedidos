package com.francisco.calculadorapedidos.data

import android.content.Context
import com.francisco.calculadorapedidos.data.db.FuxionDatabase
import com.francisco.calculadorapedidos.data.db.OrderRecordEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SavedCartItem(val product: Product?, val quantity: Int)
data class OrderMetrics(val points: Int, val money: Double)

class OrderRepository(context: Context) {

    private val dao = FuxionDatabase.getDatabase(context).orderDao()
    private val prefs = context.getSharedPreferences("fuxion_orders_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val AFFILIATION_CLIENT_ID = "AFFILIATION_GHOST"
    }

    // --- TRANSACCIONES SQL (Room) ---

    suspend fun saveOrder(year: Int, period: Int, week: Int, clientId: String, products: List<Pair<Product, Int>>) {
        withContext(Dispatchers.IO) {
            dao.deleteOrder(year, period, week, clientId)
            val isAffil = clientId == AFFILIATION_CLIENT_ID
            val entities = products.map { (prod, qty) ->
                OrderRecordEntity(
                    year = year,
                    period = period,
                    week = week,
                    clientId = clientId,
                    isAffiliation = isAffil,
                    productId = prod.id,
                    productCode = prod.code,         // HOMOLOGACIÓN DE FIRMA
                    productName = prod.name,
                    productCategory = prod.category, // HOMOLOGACIÓN DE FIRMA
                    productPresentation = prod.presentation,
                    productImageRes = prod.imageRes,
                    points = prod.points,
                    price = prod.price,
                    quantity = qty
                )
            }
            if (entities.isNotEmpty()) dao.insertItems(entities)
        }
    }

    suspend fun getOrder(year: Int, period: Int, week: Int, clientId: String): List<Pair<Product, Int>> {
        return withContext(Dispatchers.IO) {
            val entities = dao.getOrderItems(year, period, week, clientId)
            entities.map { entity ->
                val prod = Product(
                    id = entity.productId,
                    code = entity.productCode,             // HOMOLOGACIÓN DE FIRMA
                    name = entity.productName,
                    category = entity.productCategory,     // HOMOLOGACIÓN DE FIRMA
                    points = entity.points,
                    price = entity.price,
                    presentation = entity.productPresentation,
                    imageRes = entity.productImageRes
                )
                Pair(prod, entity.quantity)
            }
        }
    }

    suspend fun clearOrder(year: Int, period: Int, week: Int, clientId: String) {
        withContext(Dispatchers.IO) { dao.deleteOrder(year, period, week, clientId) }
    }

    suspend fun getAffiliationOrder(year: Int, startPeriod: Int): List<Pair<Product, Int>> {
        return getOrder(year, startPeriod, 1, AFFILIATION_CLIENT_ID)
    }

    // --- LECTURA AGREGADA (O(log N) mediante SQLite) ---

    suspend fun getPeriodMetrics(year: Int, period: Int): OrderMetrics {
        return withContext(Dispatchers.IO) {
            val pts = dao.getPeriodTotalPoints(year, period)
            val money = dao.getPeriodTotalMoney(year, period)
            OrderMetrics(pts.toInt(), money)
        }
    }

    suspend fun getWeekMetrics(year: Int, period: Int, week: Int): OrderMetrics {
        return withContext(Dispatchers.IO) {
            val pts = dao.getWeekTotalPoints(year, period, week)
            val money = dao.getWeekTotalMoney(year, period, week)
            OrderMetrics(pts.toInt(), money)
        }
    }

    suspend fun wipeAllRelationalData() {
        withContext(Dispatchers.IO) { dao.wipeDatabase() }
    }

    // --- LLAVES PLANAS (Mantenidas en SharedPreferences) ---

    private fun getGoalKey(year: Int, period: Int) = "goal_y${year}_p$period"
    private fun getDraftKey(year: Int, period: Int) = "draft_y${year}_p$period"

    fun savePeriodGoal(year: Int, period: Int, goal: Int) {
        prefs.edit().putInt(getGoalKey(year, period), goal).apply()
    }

    fun getPeriodGoal(year: Int, period: Int): Int {
        val key = getGoalKey(year, period)
        if (prefs.contains(key)) return prefs.getInt(key, 540)
        return when (period) { in 1..4 -> 540; in 5..8 -> 645; in 9..11 -> 540; in 12..13 -> 645; else -> 540 }
    }

    fun savePeriodDraft(year: Int, period: Int, products: List<Pair<Product, Int>>) {
        val safeList = products.map { SavedCartItem(it.first, it.second) }
        prefs.edit().putString(getDraftKey(year, period), gson.toJson(safeList)).apply()
    }

    fun getPeriodDraft(year: Int, period: Int): List<Pair<Product, Int>> {
        val json = prefs.getString(getDraftKey(year, period), null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedCartItem>>() {}.type
            val safeList: List<SavedCartItem> = gson.fromJson(json, type)
            safeList.mapNotNull { if (it.product != null) Pair(it.product, it.quantity) else null }
        } catch (e: Exception) { emptyList() }
    }

    fun clearPeriodDraft(year: Int, period: Int) {
        prefs.edit().remove(getDraftKey(year, period)).apply()
    }
}
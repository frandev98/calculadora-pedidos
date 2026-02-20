package com.francisco.calculadorapedidos.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// DTO para guardar de forma segura
data class SavedCartItem(
    val product: Product?,
    val quantity: Int
)

class OrderRepository(context: Context) {
    private val prefs = context.getSharedPreferences("fuxion_orders_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- GENERACIÓN DE LLAVES ---
    // La llave ahora es única por Periodo + Semana + Cliente
    private fun getKey(period: Int, week: Int, clientId: String) = "order_p${period}_w${week}_c${clientId}"
    private fun getGoalKey(period: Int) = "goal_p$period"

    // --- GUARDAR PEDIDO (Con Cliente) ---
    fun saveOrder(period: Int, week: Int, clientId: String, products: List<Pair<Product, Int>>) {
        val safeList = products.map { SavedCartItem(it.first, it.second) }
        val json = gson.toJson(safeList)
        prefs.edit().putString(getKey(period, week, clientId), json).apply()
    }

    // --- OBTENER PEDIDO (Con Cliente) ---
    fun getOrder(period: Int, week: Int, clientId: String): List<Pair<Product, Int>> {
        val json = prefs.getString(getKey(period, week, clientId), null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<SavedCartItem>>() {}.type
            val safeList: List<SavedCartItem> = gson.fromJson(json, type)

            // Filtramos productos nulos por seguridad
            safeList.mapNotNull { item ->
                if (item.product != null) {
                    Pair(item.product, item.quantity)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- BORRAR PEDIDO (Con Cliente) ---
    fun clearOrder(period: Int, week: Int, clientId: String) {
        prefs.edit().remove(getKey(period, week, clientId)).apply()
    }

    // --- MÉTODOS DE OBJETIVOS DEL PERIODO ---
    fun savePeriodGoal(period: Int, goal: Int) {
        prefs.edit().putInt(getGoalKey(period), goal).apply()
    }

    fun getPeriodGoal(period: Int): Int {
        // 1. Si el usuario ya guardó una preferencia manualmente, respetamos eso.
        if (prefs.contains(getGoalKey(period))) {
            return prefs.getInt(getGoalKey(period), 540)
        }

        // 2. Si es la primera vez, aplicamos la lógica de tu Excel:
        // Periodos 1-4: BASE (540)
        // Periodos 5-8: PRO (645)
        // Periodos 9-11: BASE (540)
        // Periodos 12-13: PRO (645)
        return when (period) {
            in 1..4 -> 540
            in 5..8 -> 645
            in 9..11 -> 540
            in 12..13 -> 645
            else -> 540
        }
    }

    // --- CÁLCULO DE TOTALES (Lógica de Escaneo) ---

    // 1. Obtener total de PUNTOS DEL PERIODO (Suma de todas las semanas y todos los clientes)
    fun getPeriodTotalPoints(period: Int): Int {
        var total = 0.0
        // Obtenemos todas las claves guardadas en la app
        val allKeys = prefs.all.keys

        // Filtramos solo las que pertenecen a este periodo (prefijo "order_p{period}_")
        val prefix = "order_p${period}_"
        val periodKeys = allKeys.filter { it.startsWith(prefix) }

        for (key in periodKeys) {
            val json = prefs.getString(key, null) ?: continue
            try {
                val type = object : TypeToken<List<SavedCartItem>>() {}.type
                val items: List<SavedCartItem> = gson.fromJson(json, type)
                // Sumamos los puntos de esta orden
                total += items.sumOf { (it.product?.points ?: 0.0) * it.quantity }
            } catch (e: Exception) {
                continue
            }
        }
        return total.toInt()
    }

    // 2. Obtener total de PUNTOS DE LA SEMANA (Suma de todos los clientes en esa semana)
    fun getWeekTotalPoints(period: Int, week: Int): Int {
        var total = 0.0
        val allKeys = prefs.all.keys
        // Filtramos por periodo Y semana exacta (prefijo "order_p{period}_w{week}_")
        val weekPrefix = "order_p${period}_w${week}_"

        val weekKeys = allKeys.filter { it.startsWith(weekPrefix) }

        for (key in weekKeys) {
            val json = prefs.getString(key, null) ?: continue
            try {
                val type = object : TypeToken<List<SavedCartItem>>() {}.type
                val items: List<SavedCartItem> = gson.fromJson(json, type)
                total += items.sumOf { (it.product?.points ?: 0.0) * it.quantity }
            } catch (e: Exception) { continue }
        }
        return total.toInt()
    }

    // --- ESTRUCTURAS AUXILIARES PARA LA UI ---
    data class PeriodSummary(
        val totalPoints: Int,
        val goal: Int,
        val isCurrent: Boolean
    )

    fun getPeriodSummary(period: Int): PeriodSummary {
        val total = getPeriodTotalPoints(period)
        val goal = getPeriodGoal(period)
        return PeriodSummary(total, goal, false)
    }
}
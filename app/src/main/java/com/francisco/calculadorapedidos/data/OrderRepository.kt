package com.francisco.calculadorapedidos.data

import com.francisco.calculadorapedidos.data.db.DraftDao
import com.francisco.calculadorapedidos.data.db.DraftEntity
import com.francisco.calculadorapedidos.data.db.OrderDao
import com.francisco.calculadorapedidos.data.db.OrderRecordEntity
import com.francisco.calculadorapedidos.logic.FuxionFinancialLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// MUTACIÓN ESTRUCTURAL: Segregación de flujos monetarios con Valores por Defecto
data class OrderMetrics(
    val points: Int = 0,
    val regularMoney: Double = 0.0,     // Dinero que GENERA comisiones (Clientes)
    val affiliationMoney: Double = 0.0  // Costo hundido directo (Afiliación)
) {
    val money: Double get() = regularMoney + affiliationMoney // Setter de retrocompatibilidad
}

class OrderRepository(
    private val orderDao: OrderDao,
    private val draftDao: DraftDao
) {

    companion object {
        const val AFFILIATION_CLIENT_ID = "AFFILIATION_GHOST"
    }

    suspend fun saveOrder(year: Int, period: Int, week: Int, clientId: String, products: List<Pair<Product, Int>>) {
        withContext(Dispatchers.IO) {
            orderDao.deleteOrder(year, period, week, clientId)
            val isAffil = clientId == AFFILIATION_CLIENT_ID
            val entities = products.map { (prod, qty) ->
                OrderRecordEntity(
                    year = year, period = period, week = week, clientId = clientId,
                    isAffiliation = isAffil, productId = prod.id, productCode = prod.code,
                    productName = prod.name, productCategory = prod.category,
                    productPresentation = prod.presentation, productImageRes = prod.imageRes,
                    points = prod.points, price = prod.price, quantity = qty
                )
            }
            if (entities.isNotEmpty()) orderDao.insertItems(entities)
        }
    }

    suspend fun getOrder(year: Int, period: Int, week: Int, clientId: String): List<Pair<Product, Int>> {
        return withContext(Dispatchers.IO) {
            val entities = orderDao.getOrderItems(year, period, week, clientId)
            entities.map { entity ->
                val prod = Product(
                    id = entity.productId, code = entity.productCode, name = entity.productName,
                    category = entity.productCategory, points = entity.points, price = entity.price,
                    presentation = entity.productPresentation, imageRes = entity.productImageRes
                )
                Pair(prod, entity.quantity)
            }
        }
    }

    suspend fun clearOrder(year: Int, period: Int, week: Int, clientId: String) {
        withContext(Dispatchers.IO) { orderDao.deleteOrder(year, period, week, clientId) }
    }

    suspend fun getAffiliationOrder(year: Int, startPeriod: Int): List<Pair<Product, Int>> {
        return getOrder(year, startPeriod, 1, AFFILIATION_CLIENT_ID)
    }

    // --- MOTOR DE PROCESAMIENTO SEGREGADO ---

    private fun calculateMetrics(orders: List<OrderRecordEntity>): OrderMetrics {
        var totalPts = 0.0
        var regMoney = 0.0
        var affPts = 0.0
        var affBaseMoney = 0.0

        orders.forEach {
            val pts = it.points * it.quantity
            val price = it.price * it.quantity
            totalPts += pts

            if (it.isAffiliation) {
                affPts += pts
                affBaseMoney += price
            } else {
                regMoney += price
            }
        }

        // El descuento de afiliación exige evaluar todos los puntos combinados de ese paquete
        val affDiscount = FuxionFinancialLogic.getAffiliationDiscount(affPts.toInt())
        val affNetMoney = affBaseMoney * (1.0 - affDiscount)

        return OrderMetrics(totalPts.toInt(), regMoney, affNetMoney)
    }

    suspend fun getPeriodMetrics(year: Int, period: Int): OrderMetrics {
        return withContext(Dispatchers.IO) {
            val orders = orderDao.getPeriodOrders(year, period)
            calculateMetrics(orders)
        }
    }

    suspend fun getWeekMetrics(year: Int, period: Int, week: Int): OrderMetrics {
        return withContext(Dispatchers.IO) {
            val orders = orderDao.getWeekOrders(year, period, week)
            calculateMetrics(orders)
        }
    }

    suspend fun wipeAllRelationalData() {
        withContext(Dispatchers.IO) {
            orderDao.wipeDatabase()
            draftDao.wipeDrafts()
        }
    }

    // --- TRANSACCIONES SQL (Room) PARA BORRADORES (UNIVERSAL SANDBOX) ---

    suspend fun savePeriodDraft(year: Int, period: Int, week: Int, clientId: String, products: List<Pair<Product, Int>>) {
        withContext(Dispatchers.IO) {
            draftDao.clearDrafts(year, period, week, clientId)
            val entities = products.map { (prod, qty) ->
                DraftEntity(year = year, period = period, week = week, clientId = clientId, productId = prod.id, quantity = qty)
            }
            if (entities.isNotEmpty()) draftDao.insertDrafts(entities)
        }
    }

    suspend fun getPeriodDraft(year: Int, period: Int, week: Int, clientId: String): List<Pair<Product, Int>> {
        return withContext(Dispatchers.IO) {
            val entities = draftDao.getDrafts(year, period, week, clientId)
            entities.mapNotNull { entity ->
                val product = ProductCatalog.masterList.find { it.id == entity.productId }
                if (product != null) Pair(product, entity.quantity) else null
            }
        }
    }

    suspend fun clearPeriodDraft(year: Int, period: Int, week: Int, clientId: String) {
        withContext(Dispatchers.IO) { draftDao.clearDrafts(year, period, week, clientId) }
    }
}
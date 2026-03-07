package com.francisco.calculadorapedidos.data

import com.francisco.calculadorapedidos.data.db.DraftDao
import com.francisco.calculadorapedidos.data.db.DraftEntity
import com.francisco.calculadorapedidos.data.db.OrderDao
import com.francisco.calculadorapedidos.data.db.OrderRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OrderMetrics(val points: Int, val money: Double)

class OrderRepository(
    private val orderDao: OrderDao,
    private val draftDao: DraftDao
) {

    companion object {
        const val AFFILIATION_CLIENT_ID = "AFFILIATION_GHOST"
    }

    // --- TRANSACCIONES SQL (Room) PARA PEDIDOS CONSOLIDADOS ---

    suspend fun saveOrder(year: Int, period: Int, week: Int, clientId: String, products: List<Pair<Product, Int>>) {
        withContext(Dispatchers.IO) {
            orderDao.deleteOrder(year, period, week, clientId)
            val isAffil = clientId == AFFILIATION_CLIENT_ID
            val entities = products.map { (prod, qty) ->
                OrderRecordEntity(
                    year = year,
                    period = period,
                    week = week,
                    clientId = clientId,
                    isAffiliation = isAffil,
                    productId = prod.id,
                    productCode = prod.code,
                    productName = prod.name,
                    productCategory = prod.category,
                    productPresentation = prod.presentation,
                    productImageRes = prod.imageRes,
                    points = prod.points,
                    price = prod.price,
                    quantity = qty
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
                    id = entity.productId,
                    code = entity.productCode,
                    name = entity.productName,
                    category = entity.productCategory,
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
        withContext(Dispatchers.IO) { orderDao.deleteOrder(year, period, week, clientId) }
    }

    suspend fun getAffiliationOrder(year: Int, startPeriod: Int): List<Pair<Product, Int>> {
        return getOrder(year, startPeriod, 1, AFFILIATION_CLIENT_ID)
    }

    // --- LECTURA AGREGADA (O(log N) mediante SQLite) ---

    suspend fun getPeriodMetrics(year: Int, period: Int): OrderMetrics {
        return withContext(Dispatchers.IO) {
            val pts = orderDao.getPeriodTotalPoints(year, period)
            val money = orderDao.getPeriodTotalMoney(year, period)
            OrderMetrics(pts.toInt(), money)
        }
    }

    suspend fun getWeekMetrics(year: Int, period: Int, week: Int): OrderMetrics {
        return withContext(Dispatchers.IO) {
            val pts = orderDao.getWeekTotalPoints(year, period, week)
            val money = orderDao.getWeekTotalMoney(year, period, week)
            OrderMetrics(pts.toInt(), money)
        }
    }

    suspend fun wipeAllRelationalData() {
        withContext(Dispatchers.IO) {
            orderDao.wipeDatabase()
            draftDao.wipeDrafts()
        }
    }

    // --- TRANSACCIONES SQL (Room) PARA BORRADORES (DRAFTS) ---

    suspend fun savePeriodDraft(year: Int, period: Int, products: List<Pair<Product, Int>>) {
        withContext(Dispatchers.IO) {
            draftDao.clearDrafts(year, period)
            val entities = products.map { (prod, qty) ->
                DraftEntity(year = year, period = period, productId = prod.id, quantity = qty)
            }
            if (entities.isNotEmpty()) draftDao.insertDrafts(entities)
        }
    }

    suspend fun getPeriodDraft(year: Int, period: Int): List<Pair<Product, Int>> {
        return withContext(Dispatchers.IO) {
            val entities = draftDao.getDrafts(year, period)
            // Reconstrucción del objeto Product mediante cruce con el catálogo maestro en memoria
            entities.mapNotNull { entity ->
                val product = ProductCatalog.masterList.find { it.id == entity.productId }
                if (product != null) Pair(product, entity.quantity) else null
            }
        }
    }

    suspend fun clearPeriodDraft(year: Int, period: Int) {
        withContext(Dispatchers.IO) { draftDao.clearDrafts(year, period) }
    }
}
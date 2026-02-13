package com.francisco.calculadorapedidos.logic

import com.francisco.calculadorapedidos.data.*
import kotlin.math.max

class DistributionCalculator {

    private val MAX_ITERATIONS = 20000

    fun calculate(
        selectedProducts: List<Pair<Product, Int>>,
        config: DistributionConfig
    ): DistributionResult {

        val originalFlatList = flattenInventory(selectedProducts)
        if (originalFlatList.isEmpty()) return DistributionResult()

        val totalPoints = originalFlatList.sumOf { it.points }
        val isHighMode = totalPoints >= 645.0 // MODO ALTO (645) vs MODO BAJO (540)

        // Definimos los mínimos para validar al final
        val minS = if (isHighMode) 180.0 else 120.0
        val minP = 35.0

        var bestResult: DistributionResult? = null
        var bestScore = -Double.MAX_VALUE

        for (i in 0 until MAX_ITERATIONS) {
            // AQUÍ ESTÁ EL CAMBIO CLAVE:
            // Usamos dos estrategias diferentes según el modo
            val candidateResult = if (isHighMode) {
                distributeForHighMode(originalFlatList)
            } else {
                distributeForLowMode(originalFlatList)
            }

            if (isValid(candidateResult, minS, minP)) {
                val score = calculateScore(candidateResult, isHighMode)
                if (score > bestScore) {
                    bestResult = candidateResult
                    bestScore = score
                    // Si es perfecto, salimos antes
                    if (score >= 950.0) break
                }
            }
        }

        return bestResult ?: if (isHighMode) distributeForHighMode(originalFlatList) else distributeForLowMode(originalFlatList)
    }

    // --- ESTRATEGIA 1: MODO BAJO (540 pts) ---
    // Prioridad: Pedidos P a 60 pts (Francotirador)
    private fun distributeForLowMode(originalList: List<Product>): DistributionResult {
        val inventory = originalList.toMutableList()
        inventory.shuffle()

        val weeksNormal = Array(3) { i -> Bucket(weekIndex = i + 1, isWeek4 = false) }
        val subPedidosS4 = Array(3) { i -> Bucket(weekIndex = 4, isWeek4 = true, subIndex = i + 1) }

        // 1. FRANCOTIRADOR DE P (Buscar 60 pts)
        val pBuckets = listOf(subPedidosS4[0], subPedidosS4[1], subPedidosS4[2])
        for (bucket in pBuckets) {
            fillSmartTarget(bucket, inventory, target = 60.0, maxOverflow = 8.0, tag = "[P${bucket.subIndex}] ")
        }

        // 2. LLENAR SEMANAS AL MÍNIMO (120 pts)
        val sBuckets = listOf(weeksNormal[0], weeksNormal[1], weeksNormal[2])
        for (bucket in sBuckets) {
            fillSmartTarget(bucket, inventory, target = 120.0, maxOverflow = 100.0, tag = "")
        }

        // 3. REPARTIR SOBRAS BALANCEADAS
        distributeLeftoversBalanced(inventory, weeksNormal.toList())

        return buildResult(originalList, weeksNormal, subPedidosS4)
    }

    // --- ESTRATEGIA 2: MODO ALTO (645 pts) ---
    // Prioridad: Semanas S a 180 pts (Constructor de Semanas)
    private fun distributeForHighMode(originalList: List<Product>): DistributionResult {
        val inventory = originalList.toMutableList()
        inventory.shuffle()

        val weeksNormal = Array(3) { i -> Bucket(weekIndex = i + 1, isWeek4 = false) }
        val subPedidosS4 = Array(3) { i -> Bucket(weekIndex = 4, isWeek4 = true, subIndex = i + 1) }

        // 1. SUELOS DE SEGURIDAD PARA P (Solo 35 pts)
        // Primero aseguramos que P1, P2, P3 tengan al menos algo, para no quedarnos en cero.
        val pBuckets = listOf(subPedidosS4[0], subPedidosS4[1], subPedidosS4[2])
        for (bucket in pBuckets) {
            fillSmartTarget(bucket, inventory, target = 35.0, maxOverflow = 10.0, tag = "[P${bucket.subIndex}] ")
        }

        // 2. PRIORIDAD MASIVA A SEMANAS (Buscar 180 pts)
        // Aquí es donde "robamos" los puntos grandes para S1, S2, S3
        val sBuckets = listOf(weeksNormal[0], weeksNormal[1], weeksNormal[2])
        for (bucket in sBuckets) {
            // Buscamos llegar a 180 exactos o pasarnos poco
            fillSmartTarget(bucket, inventory, target = 180.0, maxOverflow = 20.0, tag = "")
        }

        // 3. REPARTIR SOBRAS BALANCEADAS (Preferiblemente a las semanas si faltan, o a P si sobran)
        // En modo alto, si sobra algo, intentamos reforzar las semanas primero si no llegaron a 180,
        // o reforzar P si ya todos están bien.

        // Unimos todos los buckets elegibles para recibir sobras
        // (Pero priorizamos semanas si están bajas)
        while (inventory.isNotEmpty()) {
            // Buscamos quién necesita más amor.
            // Si alguna semana tiene menos de 180, le damos prioridad.
            val weakWeek = weeksNormal.firstOrNull { it.currentPoints < 180.0 }

            if (weakWeek != null) {
                weakWeek.addItem(inventory.removeAt(0), "(Extra) ")
            } else {
                // Si todas las semanas ya tienen 180+, repartimos al P más pobre o a la S más pobre
                // para mantener balance.
                val allBuckets = weeksNormal.toList() + subPedidosS4.toList()
                val target = allBuckets.minByOrNull { it.currentPoints }!!
                val tag = if(target.isWeek4) "[P${target.subIndex}] (Extra) " else "(Extra) "
                target.addItem(inventory.removeAt(0), tag)
            }
        }

        return buildResult(originalList, weeksNormal, subPedidosS4)
    }

    // --- FUNCIONES AUXILIARES COMUNES ---

    private fun distributeLeftoversBalanced(inventory: MutableList<Product>, targets: List<Bucket>) {
        while (inventory.isNotEmpty()) {
            val targetBucket = targets.minByOrNull { it.currentPoints }!!
            val item = inventory.removeAt(0)
            val tag = if(targetBucket.isWeek4) "[P${targetBucket.subIndex}] (Extra) " else "(Extra) "
            targetBucket.addItem(item, tag)
        }
    }

    private fun fillSmartTarget(
        bucket: Bucket,
        inventory: MutableList<Product>,
        target: Double,
        maxOverflow: Double,
        tag: String
    ) {
        val iterator = inventory.iterator()
        while (iterator.hasNext()) {
            if (bucket.currentPoints >= target) break
            val item = iterator.next()
            val newSum = bucket.currentPoints + item.points

            if (newSum <= target + 0.1) {
                bucket.addItem(item, tag)
                iterator.remove()
            } else if (newSum <= target + maxOverflow) {
                bucket.addItem(item, tag)
                iterator.remove()
                break
            }
        }
    }

    private fun buildResult(originalList: List<Product>, weeks: Array<Bucket>, subOrders: Array<Bucket>): DistributionResult {
        return DistributionResult(
            week1 = groupItems(weeks[0].items),
            week2 = groupItems(weeks[1].items),
            week3 = groupItems(weeks[2].items),
            week4 = Week4Result(
                subOrder1 = groupItems(subOrders[0].items, "[P1] "),
                subOrder2 = groupItems(subOrders[1].items, "[P2] "),
                subOrder3 = groupItems(subOrders[2].items, "[P3] "),
                extras = emptyList()
            ),
            globalPoints = originalList.sumOf { it.points },
            globalMoney = originalList.sumOf { it.price }
        )
    }

    private fun calculateScore(res: DistributionResult, isHighMode: Boolean): Double {
        var score = 0.0
        val s1 = res.week1.sumOf { it.totalPoints }
        val s2 = res.week2.sumOf { it.totalPoints }
        val s3 = res.week3.sumOf { it.totalPoints }
        val p1 = res.week4.subOrder1.sumOf { it.totalPoints }
        val p2 = res.week4.subOrder2.sumOf { it.totalPoints }
        val p3 = res.week4.subOrder3.sumOf { it.totalPoints }

        if (isHighMode) {
            // --- SCORE MODO ALTO (645) ---
            // Meta: S >= 180. P >= 35.

            // 1. Premiar S cerca de 180
            fun scoreS(valS: Double) {
                if (valS >= 180.0) score += 100.0
                else score += (valS / 180.0) * 50.0
            }
            scoreS(s1); scoreS(s2); scoreS(s3)

            // 2. Castigar S disparejas
            val diffS = maxOf(s1, s2, s3) - minOf(s1, s2, s3)
            score -= diffS * 2.0

            // 3. Puntos extra si P supera el mínimo dignamente (pero sin obsesión)
            if (p1 >= 35) score += 20; if (p2 >= 35) score += 20; if (p3 >= 35) score += 20

        } else {
            // --- SCORE MODO BAJO (540) ---
            // Meta: P == 60. S >= 120.

            // 1. Premiar P == 60 (Francotirador)
            fun scoreP(valP: Double) {
                if (valP >= 60.0) {
                    score += 200.0
                    val excess = valP - 60.0
                    score -= excess * 10.0 // Castigo fuerte por pasarse
                } else {
                    score += (valP / 60.0) * 50.0
                }
            }
            scoreP(p1); scoreP(p2); scoreP(p3)

            // 2. Premiar Balance en S
            val diffS = maxOf(s1, s2, s3) - minOf(s1, s2, s3)
            score += max(0.0, 150.0 - (diffS * 2.0))
        }

        return score
    }

    private fun isValid(res: DistributionResult, minS: Double, minP: Double): Boolean {
        if (res.week1.sumOf { it.totalPoints } < minS - 0.1) return false
        if (res.week2.sumOf { it.totalPoints } < minS - 0.1) return false
        if (res.week3.sumOf { it.totalPoints } < minS - 0.1) return false
        if (res.week4.subOrder1.sumOf { it.totalPoints } < minP - 0.1) return false
        if (res.week4.subOrder2.sumOf { it.totalPoints } < minP - 0.1) return false
        if (res.week4.subOrder3.sumOf { it.totalPoints } < minP - 0.1) return false
        return true
    }

    // --- CLASES AUXILIARES ---
    class Bucket(val weekIndex: Int, val isWeek4: Boolean, val subIndex: Int = 0) {
        val items = mutableListOf<DistributedItem>()
        var currentPoints = 0.0
        fun addItem(prod: Product, tag: String) {
            items.add(DistributedItem(prod, 1, prod.points, prod.price, tag))
            currentPoints += prod.points
        }
    }
    private fun flattenInventory(items: List<Pair<Product, Int>>): List<Product> {
        val list = mutableListOf<Product>()
        var idCounter = 1
        items.forEach { (prod, qty) -> repeat(qty) { list.add(prod.copy(id = idCounter++)) } }
        return list
    }
    private fun groupItems(items: List<DistributedItem>, defaultTag: String = ""): List<DistributedItem> {
        return items.groupBy { it.tag + it.product.name }
            .map { (_, list) ->
                val rep = list.first()
                val count = list.size
                DistributedItem(rep.product, count, rep.product.points * count, rep.product.price * count, rep.tag.ifEmpty { defaultTag })
            }
            .sortedBy { it.tag }
    }
}
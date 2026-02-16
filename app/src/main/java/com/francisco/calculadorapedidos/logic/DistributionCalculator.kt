package com.francisco.calculadorapedidos.logic

import com.francisco.calculadorapedidos.data.*
import kotlin.math.max
import kotlin.math.pow // Necesario para la varianza

class DistributionCalculator {

    fun calculate(
        selectedProducts: List<Pair<Product, Int>>,
        config: DistributionConfig
    ): DistributionResult {

        val originalFlatList = flattenInventory(selectedProducts)
        if (originalFlatList.isEmpty()) return DistributionResult()

        val totalPoints = originalFlatList.sumOf { it.points }
        val isHighMode = totalPoints >= 645.0 // MODO ALTO (645) vs MODO BAJO (540)

        // Deterministic execution: Sort once, process once per mode
        // Sort by points descending to prioritize larger items first, or consistent order
        val sortedList = originalFlatList.sortedByDescending { it.points }

        val finalResult = if (isHighMode) {
            distributeForHighMode(sortedList)
        } else {
            distributeForLowMode(sortedList)
        }

        // Apply refinement to the deterministic result
        return refineResult(finalResult, originalFlatList, isHighMode)
    }

    // --- FASE 3: REFINAMIENTO (POST-PROCESO) ---
    private fun refineResult(
        baseResult: DistributionResult,
        originalList: List<Product>, // <--- AHORA RECIBIMOS LA LISTA REAL
        isHighMode: Boolean
    ): DistributionResult {

        var currentResult = baseResult
        var currentScore = calculateScore(currentResult, isHighMode)
        var improved = true
        val MAX_REFINEMENT_ROUNDS = 50

        var rounds = 0
        while (improved && rounds < MAX_REFINEMENT_ROUNDS) {
            improved = false
            rounds++

            // Reconstruimos Buckets temporales desglosando items (cantidad 1)
            val s1 = Bucket(1, false).apply { baseResult.week1.forEach { item -> repeat(item.quantity) { addItem(item.product, item.tag) } } }
            val s2 = Bucket(2, false).apply { baseResult.week2.forEach { item -> repeat(item.quantity) { addItem(item.product, item.tag) } } }
            val s3 = Bucket(3, false).apply { baseResult.week3.forEach { item -> repeat(item.quantity) { addItem(item.product, item.tag) } } }
            val p1 = Bucket(4, true, 1).apply { baseResult.week4.subOrder1.forEach { item -> repeat(item.quantity) { addItem(item.product, item.tag) } } }
            val p2 = Bucket(4, true, 2).apply { baseResult.week4.subOrder2.forEach { item -> repeat(item.quantity) { addItem(item.product, item.tag) } } }
            val p3 = Bucket(4, true, 3).apply { baseResult.week4.subOrder3.forEach { item -> repeat(item.quantity) { addItem(item.product, item.tag) } } }

            val buckets = listOf(s1, s2, s3, p1, p2, p3)

            // ESTRATEGIA A: MOVER (Move)
            for (source in buckets) {
                if (source.items.isEmpty()) continue
                for (dest in buckets) {
                    if (source == dest) continue

                    for (i in source.items.indices) {
                        val item = source.items[i]

                        // CORRECCIÓN AQUÍ: Usamos item.totalPoints en lugar de item.points
                        source.currentPoints -= item.totalPoints
                        dest.currentPoints += item.totalPoints

                        // Pasamos 'originalList' real para que el Score y Totales sean correctos
                        val tempResult = buildResultFromBuckets(originalList, buckets.toTypedArray())
                        val newScore = calculateScore(tempResult, isHighMode)

                        if (newScore > currentScore + 0.01) {
                            source.items.removeAt(i)
                            dest.addItem(item.product, item.tag)

                            currentResult = tempResult
                            currentScore = newScore
                            improved = true
                            break
                        } else {
                            // Revertimos cambio (CORRECCIÓN AQUÍ TAMBIÉN)
                            source.currentPoints += item.totalPoints
                            dest.currentPoints -= item.totalPoints
                        }
                    }
                    if (improved) break
                }
                if (improved) break
            }

            if (improved) continue

            // ESTRATEGIA B: INTERCAMBIAR (Swap)
            for (bucketA in buckets) {
                for (bucketB in buckets) {
                    if (bucketA == bucketB) continue

                    for (i in bucketA.items.indices) {
                        for (j in bucketB.items.indices) {
                            val itemA = bucketA.items[i]
                            val itemB = bucketB.items[j]

                            // CORRECCIÓN: item.totalPoints
                            if (itemA.totalPoints == itemB.totalPoints) continue

                            bucketA.currentPoints = bucketA.currentPoints - itemA.totalPoints + itemB.totalPoints
                            bucketB.currentPoints = bucketB.currentPoints - itemB.totalPoints + itemA.totalPoints

                            val tempResult = buildResultFromBuckets(originalList, buckets.toTypedArray())
                            val newScore = calculateScore(tempResult, isHighMode)

                            if (newScore > currentScore + 0.01) {
                                bucketA.items.removeAt(i)
                                bucketB.items.removeAt(j)
                                bucketA.addItem(itemB.product, itemB.tag)
                                bucketB.addItem(itemA.product, itemA.tag)

                                currentResult = tempResult
                                currentScore = newScore
                                improved = true
                                break
                            } else {
                                // Revertimos (CORRECCIÓN)
                                bucketA.currentPoints = bucketA.currentPoints + itemA.totalPoints - itemB.totalPoints
                                bucketB.currentPoints = bucketB.currentPoints + itemB.totalPoints - itemA.totalPoints
                            }
                        }
                        if (improved) break
                    }
                    if (improved) break
                }
                if (improved) break
            }
        }

        return currentResult
    }

    private fun buildResultFromBuckets(originalList: List<Product>, buckets: Array<Bucket>): DistributionResult {
        val sWeeks = arrayOf(buckets[0], buckets[1], buckets[2])
        val pWeeks = arrayOf(buckets[3], buckets[4], buckets[5])
        return buildResult(originalList, sWeeks, pWeeks)
    }

    // --- ESTRATEGIA 1: MODO BAJO (540 pts) ---
    private fun distributeForLowMode(originalList: List<Product>): DistributionResult {
        val inventory = originalList.toMutableList()
        // inventory.shuffle() // REMOVED: Non-deterministic

        val weeksNormal = Array(3) { i -> Bucket(weekIndex = i + 1, isWeek4 = false) }
        val subPedidosS4 = Array(3) { i -> Bucket(weekIndex = 4, isWeek4 = true, subIndex = i + 1) }

        // 1. FRANCOTIRADOR DE P (Prioridad 1: P=60)
        val pBuckets = listOf(subPedidosS4[0], subPedidosS4[1], subPedidosS4[2])
        for (bucket in pBuckets) {
            fillSmartTarget(bucket, inventory, target = 60.0, maxOverflow = 8.0, tag = "[P${bucket.subIndex}] ")
        }

        // 2. LLENAR SEMANAS AL MÍNIMO (Prioridad 2: S=120)
        val sBuckets = listOf(weeksNormal[0], weeksNormal[1], weeksNormal[2])
        for (bucket in sBuckets) {
            fillSmartTarget(bucket, inventory, target = 120.0, maxOverflow = 50.0, tag = "")
        }

        // 3. REPARTIR SOBRAS A SEMANAS
        distributeLeftoversBalanced(inventory, weeksNormal.toList())

        return buildResult(originalList, weeksNormal, subPedidosS4)
    }

    // --- ESTRATEGIA 2: MODO ALTO (>= 645 pts) ---
    private fun distributeForHighMode(originalList: List<Product>): DistributionResult {
        val inventory = originalList.toMutableList()
        // inventory.shuffle() // REMOVED: Non-deterministic

        val weeksNormal = Array(3) { i -> Bucket(weekIndex = i + 1, isWeek4 = false) }
        val subPedidosS4 = Array(3) { i -> Bucket(weekIndex = 4, isWeek4 = true, subIndex = i + 1) }

        // 1. SUELOS DE SEGURIDAD PARA P (P>=35) - ESTRATEGIA COMBINATORIA "SMART FIT"
        // En lugar de llenar a lo bruto, buscamos combinaciones EXACTAS (o casi exactas) de 2 o 3 productos
        // que sumen entre 35 y 42. Esto prioriza usar "16+20=36" sobre "26+16=42".
        val pBuckets = listOf(subPedidosS4[0], subPedidosS4[1], subPedidosS4[2])
        for (bucket in pBuckets) {
            // Intentamos encontrar el mejor subconjunto posible
            val bestSubset = findBestSubset(inventory, minTarget = 35.0, maxTarget = 42.0)
            
            if (bestSubset.isNotEmpty()) {
                // Si encontramos una buena combinación, la usamos
                bestSubset.forEach { 
                    bucket.addItem(it, "[P${bucket.subIndex}] ") 
                    inventory.remove(it) // Ojo: remove elimina la primera instancia, que es lo que queremos
                }
            } else {
                // FALLBACK: Si no hay combinación perfecta, usamos la lógica "Force Fill" antigua
                // pero con tolerancia mínima para evitar desastres
                fillSmartTarget(bucket, inventory, target = 35.0, maxOverflow = 8.0, tag = "[P${bucket.subIndex}] ")
            }
        }

        // 2. PRIORIDAD MASIVA A SEMANAS (S=180)
        // Ahora llenamos S con el grueso de los puntos (y lo que sobró de los combinados)
        val sBuckets = listOf(weeksNormal[0], weeksNormal[1], weeksNormal[2])
        for (bucket in sBuckets) {
            fillSmartTarget(bucket, inventory, target = 180.0, maxOverflow = 10.0, tag = "")
        }

        // 3. REPARTIR SOBRAS A PEDIDOS (P->60)
        // Todo lo que sobre va a P para bonos extra
        distributeLeftoversBalanced(inventory, subPedidosS4.toList())

        return buildResult(originalList, weeksNormal, subPedidosS4)
    }

    // --- NUEVO: BUSCADOR DE COMBINACIONES ÓPTIMAS (Recursivo con Pruning) ---
    private fun findBestSubset(inventory: List<Product>, minTarget: Double, maxTarget: Double): List<Product> {
        if (inventory.isEmpty()) return emptyList()

        // Variables para almacenar la mejor solución encontrada
        var bestSolution: List<Product> = emptyList()
        var bestSum = Double.MAX_VALUE

        // Helper recursivo
        fun backtrack(startIndex: Int, currentSubset: MutableList<Product>, currentSum: Double) {
            // PODA 1: Si ya nos pasamos del máximo, abortar rama
            if (currentSum > maxTarget) return

            // PODA 2: Si ya encontramos una solución "perfecta" (muy cerca del mínimo), no necesitamos seguir buscando exhaustivamente
            // pero si queremos la MEJOR (mínima suma dentro del rango), seguimos.
            // Para optimizar velocidad, si encontramos algo dentro del rango [min, min+1], lo tomamos como "suficientemente bueno" y cortamos.
            if (bestSum <= minTarget + 1.0) return

            // CHECK: ¿Es una solución válida?
            if (currentSum >= minTarget) {
                // Es válida. ¿Es mejor que la que tenemos?
                if (currentSum < bestSum) {
                    bestSum = currentSum
                    bestSolution = ArrayList(currentSubset)
                }
                // No seguimos añadiendo a esta rama porque buscamos la suma MÍNIMA válida
                // y cualquier items extra solo aumentaría la suma.
                return
            }

            // PODA 3: Límite de profundidad (heuristic de la original: pares o tríos)
            // Si tenemos muchos items pequeños, podríamos tener recursión profunda.
            // Para propósitos de este problema ("Combos"), raramente queremos más de 3-4 items para sumar 35.
            if (currentSubset.size >= 4) return

            // ITERACIÓN
            for (i in startIndex until inventory.size) {
                val item = inventory[i]
                
                // Advance
                currentSubset.add(item)
                backtrack(i + 1, currentSubset, currentSum + item.points)
                // Backtrack
                currentSubset.removeAt(currentSubset.size - 1)
                
                // Si encontramos 'perfect match' en la recursión, salir del loop
                if (bestSum <= minTarget + 1.0) return
            }
        }

        // Iniciar búsqueda
        // inventory is already sorted by caller usually, but to be safe for this method's logic:
        // We might want smaller items or larger items?
        // Original logic: "Priorizamos los que sumen MENOS (más cerca de minTarget)".
        // Recursion works well.
        backtrack(0, mutableListOf(), 0.0)

        return bestSolution
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
        
        // CORRECCIÓN CRÍTICA: "Force Fill" (Llenado Forzoso)
        // Si después de la pasada "bonita" seguimos por debajo del objetivo (ej. P < 35),
        // OBLIGAMOS a meter el producto más pequeño disponible para cumplir la regla,
        // aunque nos pasemos del 'maxOverflow'.
        // Preferimos un P=42 (válido) que un P=32 (inválido -500 pts).
        if (bucket.currentPoints < target && inventory.isNotEmpty()) {
            // Buscamos el item más pequeño para minimizar el daño del overflow
            val bestItemIndex = inventory.indices.minByOrNull { inventory[it].points } ?: 0
            val item = inventory.removeAt(bestItemIndex)
            bucket.addItem(item, tag)
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

    // --- SCORE ---
    private fun calculateScore(res: DistributionResult, isHighMode: Boolean): Double {
        var score = 0.0
        val s = listOf(res.week1, res.week2, res.week3).map { it.sumOf { item -> item.totalPoints } }
        val p = listOf(res.week4.subOrder1, res.week4.subOrder2, res.week4.subOrder3).map { it.sumOf { item -> item.totalPoints } }

        fun variance(values: List<Double>): Double {
            val mean = values.average()
            if (mean == 0.0) return 0.0
            return values.sumOf { (it - mean).pow(2) }
        }

        if (isHighMode) {
            // MODO ALTO
            // 1. SEMANAS (S) - El núcleo de la estrategia
            val sCompleted = s.count { it >= 180.0 }
            s.forEach { if (it >= 180.0) score += 100.0 else score -= (180.0 - it) * 10.0 }

            // 2. PEDIDOS (P)
            p.forEach {
                if (it >= 60.0) score += 60.0
                else if (it >= 35.0) {
                    // ZONA CRÍTICA: De 35 a 60.
                    // Si S no está completo, cada punto extra aquí es un 'robo' a S.
                    if (sCompleted < 3) {
                        score += 30.0 // Base segura
                        // Penalizamos el exceso si S lo necesita
                        score -= (it - 35.0) * 2.0 
                    } else {
                        // Si S está feliz, entonces sí premiamos subir hasta 60
                        score += 30.0 + (it - 35.0)
                    }
                }
                else score -= 500.0
            }
            s.forEach { if (it > 180.0) score -= (it - 180.0) * 5.0 }
            score -= variance(p) * 0.5

        } else {
            // MODO BAJO
            p.forEach {
                if (it >= 60.0) {
                    score += 200.0
                    score -= (it - 60.0) * 10.0
                } else {
                    score += (it / 60.0) * 50.0
                }
            }
            s.forEach { if (it >= 120.0) score += 50.0 else score -= (120.0 - it) * 5.0 }
            score -= variance(s) * 0.5
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
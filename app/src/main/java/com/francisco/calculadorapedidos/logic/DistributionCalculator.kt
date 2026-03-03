package com.francisco.calculadorapedidos.logic

import com.francisco.calculadorapedidos.data.*

class DistributionCalculator {

    data class SlotRequest(
        val weekIndex: Int,
        val def: WeeklySlotDef,
        val minPts: Double,
        val maxPts: Double
    )

    fun calculate(
        selectedProducts: List<Pair<Product, Int>>,
        periodId: Int
    ): DistributionResult {
        val inventory = flattenInventory(selectedProducts)
        if (inventory.isEmpty()) return DistributionResult()

        // 1. Generar la matriz de solicitudes (Slots) para las 4 semanas del periodo
        val allRequests = mutableListOf<SlotRequest>()
        val weeklyTargets = (1..4).associateWith { weekIndex ->
            val slots = FuxionCalendarLogic.getSlotsForWeek(periodId, weekIndex)
            slots.forEach { slot ->
                val min = slot.targetPoints.toDouble()
                val max = min + 2.0 // Tolerancia estricta paramétrica de +2
                allRequests.add(SlotRequest(weekIndex, slot, min, max))
            }
            slots
        }

        // 2. Ordenar las solicitudes de mayor a menor para optimizar la poda recursiva
        val sortedRequests = allRequests.sortedByDescending { it.minPts }

        // 3. Ejecutar algoritmo de partición exacta
        val (assignment, isPerfect) = findBestAssignment(inventory, sortedRequests)

        // 4. Construcción de la entidad de salida
        return buildResult(assignment, inventory, weeklyTargets, isPerfect)
    }

    private fun findBestAssignment(
        inventory: List<Product>,
        requests: List<SlotRequest>
    ): Pair<Map<SlotRequest, List<DistributedItem>>, Boolean> {

        val totalInventoryPoints = inventory.sumOf { it.points }

        // --- PARÁMETROS DINÁMICOS DEL USUARIO ---
        // Exceso total = Lo que sobra después de los 500 puntos base (ej. 503 - 500 = 3)
        val excess = maxOf(0.0, totalInventoryPoints - 500.0)
        // Cuánto se le permite "pasarse" a cada semana (ej. 3 / 4 = 0.75 -> redondeado a 1.0)
        val allowedWeekExtra = Math.ceil(excess / 4.0)

        // ==========================================
        // LA CALCULADORA DE CASTIGOS (JERARQUÍA ESTRICTA)
        // ==========================================
        fun calculatePenalty(state: Map<Product, SlotRequest>): Double {
            var penalty = 0.0
            val slotSums = requests.associateWith { 0.0 }.toMutableMap()

            for ((product, slot) in state) {
                slotSums[slot] = slotSums[slot]!! + product.points
            }

            // --- CAPA 1: REGLA DE ORO POR CLIENTE ---
            for (req in requests) {
                val sum = slotSums[req]!!
                val target = req.minPts // 60 pts

                if (sum < target) {
                    // CASTIGO MASIVO: 100,000 por cada punto que falte
                    penalty += (target - sum) * 100_000.0
                }
            }

            // --- REVISIÓN POR SEMANA (CAPAS 1, 2 y 3) ---
            val weeks = requests.map { it.weekIndex }.distinct()
            for (week in weeks) {
                val weekReqs = requests.filter { it.weekIndex == week }
                val weekSum = weekReqs.sumOf { slotSums[it]!! }
                val isSingleClient = weekReqs.size == 1

                val weekMinTarget = if (isSingleClient) weekReqs.first().minPts else 125.0

                // CAPA 1: REGLA DE ORO POR SEMANA
                if (weekSum < weekMinTarget) {
                    penalty += (weekMinTarget - weekSum) * 100_000.0 // CASTIGO MASIVO
                } else {
                    // CAPA 2: LÍMITE DE EXCESO SEMANAL
                    val weekMaxTarget = weekMinTarget + allowedWeekExtra
                    if (weekSum > weekMaxTarget) {
                        // Castigo fuerte, pero 100 veces menor que violar el mínimo vital
                        penalty += (weekSum - weekMaxTarget) * 1_000.0
                    }
                }

                // CAPA 3: BALANCE ESTÉTICO INTERNO (Solo para semanas con 2 clientes)
                if (weekReqs.size == 2) {
                    val diff = Math.abs(slotSums[weekReqs[0]]!! - slotSums[weekReqs[1]]!!)
                    penalty += diff * 10.0 // Castigo leve para buscar equidad (ej. 65-65 en vez de 60-70)
                }
            }

            return penalty
        }

        // ==========================================
        // MOTOR MONTECARLO EVOLUCIONADO
        // ==========================================

        var bestState = inventory.associateWith { requests.random() }.toMutableMap()
        var bestPenalty = calculatePenalty(bestState)

        // Aumentamos a 100,000 iteraciones. En Kotlin toma < 50ms.
        val MAX_ITERATIONS = 100_000
        var noImprovementCounter = 0

        for (i in 0 until MAX_ITERATIONS) {
            if (bestPenalty == 0.0) break

            val newState = bestState.toMutableMap()

            // ========================================================
            // NUEVA MUTACIÓN DINÁMICA: 50% Mover / 50% Intercambiar
            // ========================================================
            if (Math.random() < 0.5) {
                // TÁCTICA 1: Mover 1 producto (Ideal para ajustes grandes)
                val productToMove = inventory.random()
                val currentSlot = newState[productToMove]
                val newSlot = requests.filter { it != currentSlot }.random()
                newState[productToMove] = newSlot
            } else {
                // TÁCTICA 2: Intercambiar 2 productos (El secreto para márgenes estrechos)
                val p1 = inventory.random()
                val p2 = inventory.random()
                val slot1 = newState[p1]
                val slot2 = newState[p2]

                // Solo intercambiamos si pertenecen a clientes distintos
                if (slot1 != slot2) {
                    newState[p1] = slot2!!
                    newState[p2] = slot1!!
                }
            }
            // ========================================================

            val newPenalty = calculatePenalty(newState)

            if (newPenalty < bestPenalty) {
                bestState = newState
                bestPenalty = newPenalty
                noImprovementCounter = 0
            } else {
                noImprovementCounter++
            }

            if (noImprovementCounter > 3000) {
                val tempState = inventory.associateWith { requests.random() }.toMutableMap()
                val tempPenalty = calculatePenalty(tempState)

                if (tempPenalty < bestPenalty + 500_000.0) {
                    bestState = tempState
                    bestPenalty = tempPenalty
                }
                noImprovementCounter = 0
            }
        }

        // ==========================================
        // RESULTADO Y FORMATEO
        // ==========================================
        val finalAssignments = requests.associateWith { mutableListOf<Product>() }
        for ((product, slot) in bestState) {
            finalAssignments[slot]!!.add(product)
        }

        // El plan es válido si no violó las reglas de ORO (castigo < 100,000)
        val isPerfect = bestPenalty < 100_000.0

        val formatted = finalAssignments.mapValues {
            val tag = if (isPerfect) "[${it.key.def.slotId}] " else "[${it.key.def.slotId}] (Error) "
            groupItems(it.value, tag)
        }

        return Pair(formatted, isPerfect)
    }

    private fun greedyAssignment(
        inventory: List<Product>,
        requests: List<SlotRequest>
    ): Map<SlotRequest, List<DistributedItem>> {
        // 1. Ordenar de mayor a menor puntaje
        val sortedInventory = inventory.sortedByDescending { it.points }

        val assignments = requests.associateWith { mutableListOf<Product>() }.toMutableMap()
        val currentSums = requests.associateWith { 0.0 }.toMutableMap()

        // Funciones auxiliares para calcular déficits
        fun getWeekSum(weekIndex: Int) = requests.filter { it.weekIndex == weekIndex }.sumOf { currentSums[it]!! }

        fun getWeekDeficit(weekIndex: Int): Double {
            val weekReqs = requests.filter { it.weekIndex == weekIndex }
            val weekTarget = if (weekReqs.size == 1) weekReqs.first().minPts else 125.0
            return maxOf(0.0, weekTarget - getWeekSum(weekIndex))
        }

        fun getSlotDeficit(req: SlotRequest) = maxOf(0.0, req.minPts - currentSums[req]!!)

        // 2. Llenado inteligente
        for (product in sortedInventory) {
            // PRIORIDAD 1: Clientes (Slots) que aún no llegan a su meta mínima (60 pts)
            val slotsNeedingIndividual = requests.filter { getSlotDeficit(it) > 0 }

            val chosenSlot = if (slotsNeedingIndividual.isNotEmpty()) {
                // Le damos el producto al cliente que esté más lejos de sus 60 puntos
                slotsNeedingIndividual.maxByOrNull { getSlotDeficit(it) }!!
            } else {
                // PRIORIDAD 2: Todos tienen 60, pero la SEMANA aún no llega a 125 pts
                val weeksNeedingTotal = (1..4).filter { getWeekDeficit(it) > 0 }

                if (weeksNeedingTotal.isNotEmpty()) {
                    // Elegimos la semana que esté más lejos de 125 pts
                    val worstWeek = weeksNeedingTotal.maxByOrNull { getWeekDeficit(it) }!!
                    // Dentro de esa semana, le damos el producto al cliente que tenga MENOS puntos para mantener todo parejo
                    requests.filter { it.weekIndex == worstWeek }.minByOrNull { currentSums[it]!! }!!
                } else {
                    // PRIORIDAD 3: Todas las metas cumplidas.
                    // Repartimos lo que sobra al cliente con menos puntos globales para mantener el equilibrio.
                    requests.minByOrNull { currentSums[it]!! }!!
                }
            }

            assignments[chosenSlot]!!.add(product)
            currentSums[chosenSlot] = currentSums[chosenSlot]!! + product.points
        }

        return assignments.mapValues {
            groupItems(it.value, "[${it.key.def.slotId}] (Aprox) ")
        }
    }

    private fun buildResult(
        assignment: Map<SlotRequest, List<DistributedItem>>,
        inventory: List<Product>,
        weeklyTargets: Map<Int, List<WeeklySlotDef>>,
        isPerfect: Boolean
    ): DistributionResult {
        val getSlotsForWeek = { weekIndex: Int ->
            val defs = weeklyTargets[weekIndex] ?: emptyList()
            defs.map { def ->
                val req = assignment.keys.find { it.weekIndex == weekIndex && it.def.slotId == def.slotId }
                val items = if (req != null) assignment[req] ?: emptyList() else emptyList()

                // CORRECCIÓN APLICADA: Se utiliza fallbackName en lugar del obsoleto clientId
                SlotAllocation(def.slotId, def.fallbackName, def.targetPoints, items)
            }
        }

        return DistributionResult(
            week1 = WeeklyAllocation(1, getSlotsForWeek(1)),
            week2 = WeeklyAllocation(2, getSlotsForWeek(2)),
            week3 = WeeklyAllocation(3, getSlotsForWeek(3)),
            week4 = WeeklyAllocation(4, getSlotsForWeek(4)),
            globalPoints = inventory.sumOf { it.points },
            globalMoney = inventory.sumOf { it.price },
            isPerfectFit = isPerfect
        )
    }

    private fun flattenInventory(items: List<Pair<Product, Int>>): List<Product> {
        val list = mutableListOf<Product>()
        var idCounter = 1
        items.forEach { (prod, qty) -> repeat(qty) { list.add(prod.copy(id = idCounter++)) } }
        return list
    }

    private fun groupItems(items: List<Product>, defaultTag: String): List<DistributedItem> {
        return items.groupBy { it.name }
            .map { (_, list) ->
                val rep = list.first()
                DistributedItem(rep, list.size, rep.points * list.size, rep.price * list.size, defaultTag)
            }.sortedByDescending { it.totalPoints }
    }
}
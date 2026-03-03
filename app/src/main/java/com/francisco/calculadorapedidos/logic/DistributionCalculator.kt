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
        val sortedInventory = inventory.sortedByDescending { it.points }
        val used = BooleanArray(sortedInventory.size)
        val result = mutableMapOf<SlotRequest, List<Product>>()

        var iterations = 0
        val MAX_ITERATIONS = 200_000 // Limite de seguridad contra explosión combinatoria

        fun backtrack(reqIdx: Int): Boolean {
            if (reqIdx == requests.size) return true
            val req = requests[reqIdx]

            fun searchSubset(startIdx: Int, currentSum: Double, subset: MutableList<Int>): Boolean {
                iterations++
                if (iterations > MAX_ITERATIONS) return false

                // Poda: Desborde
                if (currentSum > req.maxPts) return false

                // Evaluación de éxito temporal
                if (currentSum >= req.minPts && currentSum <= req.maxPts) {
                    subset.forEach { used[it] = true }
                    result[req] = subset.map { sortedInventory[it] }

                    if (backtrack(reqIdx + 1)) return true

                    // Backtrack (Revertir)
                    subset.forEach { used[it] = false }
                    result.remove(req)
                }

                // Iteración de ramas
                for (i in startIdx until sortedInventory.size) {
                    if (!used[i]) {
                        // Anti-duplicidad: Saltamos ramas idénticas si el producto anterior (igual) falló
                        if (i > 0 && sortedInventory[i].id == sortedInventory[i - 1].id && !used[i - 1]) continue

                        subset.add(i)
                        val found = searchSubset(i + 1, currentSum + sortedInventory[i].points, subset)
                        subset.removeAt(subset.size - 1)
                        if (found) return true
                    }
                }
                return false
            }

            return searchSubset(0, 0.0, mutableListOf())
        }

        val success = backtrack(0)

        // Formateo de productos crudos a DistributedItems
        if (success) {
            val formatted = result.mapValues { groupItems(it.value, "[${it.key.def.slotId}] ") }
            return Pair(formatted, true)
        } else {
            // Fallback: Si el usuario inserta productos imposibles (ej. todo de 10 pts, no da 65 exactos),
            // aplicamos un llenado codicioso (Greedy) para que la UI muestre el error lógico.
            return Pair(greedyAssignment(sortedInventory, requests), false)
        }
    }

    private fun greedyAssignment(
        inventory: List<Product>,
        requests: List<SlotRequest>
    ): Map<SlotRequest, List<DistributedItem>> {
        val result = mutableMapOf<SlotRequest, List<DistributedItem>>()
        val available = inventory.toMutableList()

        for (req in requests) {
            val slotItems = mutableListOf<Product>()
            var sum = 0.0
            val iterator = available.iterator()
            while (iterator.hasNext() && sum < req.minPts) {
                val item = iterator.next()
                if (sum + item.points <= req.maxPts + 15.0) { // Tolerancia laxa solo para el Fallback
                    slotItems.add(item)
                    sum += item.points
                    iterator.remove()
                }
            }
            result[req] = groupItems(slotItems, "[${req.def.slotId}] (Inexacto) ")
        }
        return result
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
                SlotAllocation(def.slotId, def.clientId, def.targetPoints, items)
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
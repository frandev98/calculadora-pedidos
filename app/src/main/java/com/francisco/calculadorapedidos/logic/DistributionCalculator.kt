package com.francisco.calculadorapedidos.logic

import com.francisco.calculadorapedidos.data.*
import java.util.Calendar

class DistributionCalculator {

    data class SlotRequest(
        val weekIndex: Int,
        val def: WeeklySlotDef,
        val minPts: Double,
        val maxPts: Double
    )

    fun calculate(
        selectedProducts: List<Pair<Product, Int>>,
        periodId: Int,
        userStartPeriod: Int,
        targetYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): DistributionResult {
        val inventory = flattenInventory(selectedProducts)
        if (inventory.isEmpty()) return DistributionResult()

        // MATRIZ ASIMÉTRICA DE PONDERACIÓN
        val totalDaysInPeriod = FuxionCalendarLogic.getDaysInPeriod(targetYear, periodId)
        val weekWeights = DoubleArray(4)
        for (week in 1..4) {
            val daysInWeek = FuxionCalendarLogic.getDaysInWeek(targetYear, periodId, week)
            weekWeights[week - 1] = daysInWeek.toDouble() / totalDaysInPeriod.toDouble()
        }

        val allRequests = mutableListOf<SlotRequest>()
        val weeklyTargets = (1..4).associateWith { weekIndex ->
            val slots = FuxionCalendarLogic.getSlotsForWeek(periodId, weekIndex, userStartPeriod)
            slots.forEach { slot ->
                val min = slot.targetPoints.toDouble()
                val max = min + 2.0
                allRequests.add(SlotRequest(weekIndex, slot, min, max))
            }
            slots
        }

        val sortedRequests = allRequests.sortedByDescending { it.minPts }
        val (assignment, isPerfect) = findBestAssignment(inventory, sortedRequests, weekWeights)

        return buildResult(assignment, inventory, weeklyTargets, isPerfect)
    }

    private fun findBestAssignment(
        inventory: List<Product>,
        requests: List<SlotRequest>,
        weekWeights: DoubleArray
    ): Pair<Map<SlotRequest, List<DistributedItem>>, Boolean> {

        val totalInventoryPoints = inventory.sumOf { it.points }
        val excess = maxOf(0.0, totalInventoryPoints - 500.0)

        fun calculatePenalty(state: Map<Product, SlotRequest>): Double {
            var penalty = 0.0
            val slotSums = requests.associateWith { 0.0 }.toMutableMap()
            for ((product, slot) in state) { slotSums[slot] = slotSums[slot]!! + product.points }

            for (req in requests) {
                val sum = slotSums[req]!!
                if (sum < req.minPts) {
                    penalty += (req.minPts - sum) * 1_000_000.0
                } else {
                    penalty += (sum - req.minPts) * 10.0
                }
            }

            val weeks = requests.map { it.weekIndex }.distinct()
            for (week in weeks) {
                val weekReqs = requests.filter { it.weekIndex == week }
                val weekSum = weekReqs.sumOf { slotSums[it]!! }
                val isSingleClient = weekReqs.size == 1

                val expectedPointsForWeek = 500.0 * weekWeights[week - 1]
                val weekMinTarget = if (isSingleClient) maxOf(weekReqs.first().minPts, expectedPointsForWeek) else expectedPointsForWeek

                if (weekSum < weekMinTarget) {
                    penalty += (weekMinTarget - weekSum) * 1_000_000.0
                } else {
                    val weightedExcess = Math.ceil(excess * weekWeights[week - 1])
                    val weekMaxTarget = weekMinTarget + weightedExcess

                    if (weekSum > weekMaxTarget) {
                        penalty += (weekSum - weekMaxTarget) * 10_000.0
                    }
                }

                if (weekReqs.size == 2) {
                    val diff = Math.abs(slotSums[weekReqs[0]]!! - slotSums[weekReqs[1]]!!)
                    penalty += diff * 100.0
                }
            }
            return penalty
        }

        var currentBestState = inventory.associateWith { requests.random() }.toMutableMap()
        var currentBestPenalty = calculatePenalty(currentBestState)

        var globalBestState = currentBestState.toMap()
        var globalBestPenalty = currentBestPenalty

        val MAX_ITERATIONS = 150_000
        var noImprovementCounter = 0

        for (i in 0 until MAX_ITERATIONS) {
            if (globalBestPenalty == 0.0) break

            val newState = currentBestState.toMutableMap()
            val mutationSelector = Math.random()

            if (mutationSelector < 0.35) {
                val productToMove = inventory.random()
                newState[productToMove] = requests.random()
            } else if (mutationSelector < 0.70) {
                val p1 = inventory.random()
                val p2 = inventory.random()
                val s1 = newState[p1]!!
                val s2 = newState[p2]!!
                if (s1 != s2 && p1.points != p2.points) {
                    newState[p1] = s2
                    newState[p2] = s1
                }
            } else {
                val currentSums = requests.associateWith { 0.0 }.toMutableMap()
                for ((p, s) in newState) { currentSums[s] = currentSums[s]!! + p.points }

                val deficitSlots = requests.filter { currentSums[it]!! < it.minPts }
                val excessSlots = requests.filter { currentSums[it]!! > it.minPts }

                if (deficitSlots.isNotEmpty() && excessSlots.isNotEmpty()) {
                    val receiver = deficitSlots.random()
                    val donor = excessSlots.random()
                    val productToGive = newState.filter { it.value == donor }.keys.randomOrNull()
                    if (productToGive != null) {
                        newState[productToGive] = receiver
                    }
                } else if (deficitSlots.isNotEmpty()) {
                    val p = inventory.random()
                    newState[p] = deficitSlots.random()
                }
            }

            val newPenalty = calculatePenalty(newState)

            if (newPenalty <= currentBestPenalty) {
                currentBestState = newState
                currentBestPenalty = newPenalty

                if (newPenalty < globalBestPenalty) {
                    globalBestState = newState.toMap()
                    globalBestPenalty = newPenalty
                    noImprovementCounter = 0
                } else {
                    noImprovementCounter++
                }
            } else {
                noImprovementCounter++
                val temp = (MAX_ITERATIONS - i).toDouble() / MAX_ITERATIONS
                if (Math.random() < temp * 0.02) {
                    currentBestState = newState
                    currentBestPenalty = newPenalty
                }
            }

            if (noImprovementCounter > 4000) {
                val itemsToScramble = (inventory.size * 0.15).toInt().coerceAtLeast(1)
                for (j in 0 until itemsToScramble) {
                    currentBestState[inventory.random()] = requests.random()
                }
                currentBestPenalty = calculatePenalty(currentBestState)
                noImprovementCounter = 0
            }
        }

        val finalAssignments = requests.associateWith { mutableListOf<Product>() }
        for ((product, slot) in globalBestState) {
            finalAssignments[slot]!!.add(product)
        }

        val isPerfect = globalBestPenalty < 1_000_000.0

        val formatted = finalAssignments.mapValues {
            val tag = if (isPerfect) "[${it.key.def.slotId}] " else "[${it.key.def.slotId}] (Error) "
            groupItems(it.value, tag)
        }

        return Pair(formatted, isPerfect)
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

                // INYECCIÓN POSICIONAL ESTRICTA CORREGIDA
                SlotAllocation(def.slotId, def.fallbackName, def.fixedIndex, def.targetPoints, items)
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
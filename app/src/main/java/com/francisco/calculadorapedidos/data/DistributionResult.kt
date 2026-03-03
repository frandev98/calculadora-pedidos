package com.francisco.calculadorapedidos.data

data class DistributionResult(
    val week1: WeeklyAllocation = WeeklyAllocation(1),
    val week2: WeeklyAllocation = WeeklyAllocation(2),
    val week3: WeeklyAllocation = WeeklyAllocation(3),
    val week4: WeeklyAllocation = WeeklyAllocation(4),
    val globalPoints: Double = 0.0,
    val globalMoney: Double = 0.0,
    val isPerfectFit: Boolean = false
)

data class WeeklyAllocation(
    val weekIndex: Int,
    val slots: List<SlotAllocation> = emptyList()
) {
    val totalPoints: Double get() = slots.sumOf { it.achievedPoints }
}

data class SlotAllocation(
    val slotId: String,
    val clientId: String,
    val targetPoints: Int,
    val items: List<DistributedItem>
) {
    val achievedPoints: Double get() = items.sumOf { it.totalPoints }
}

// --- CLASE RESTAURADA ---
// Es imperativo que esta entidad exista para que la vista y las asignaciones funcionen.
data class DistributedItem(
    val product: Product,
    val quantity: Int,
    val totalPoints: Double,
    val totalPrice: Double,
    val tag: String = ""
)
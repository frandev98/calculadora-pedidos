package com.francisco.calculadorapedidos.data

/**
 * Representa UN item ya asignado a una semana.
 * Ejemplo: "3 unidades de Prunex1 para la Semana 1"
 */
data class DistributedItem(
    val product: Product,
    val quantity: Int,
    val totalPoints: Double,
    val totalMoney: Double,
    val tag: String = "" // Para etiquetas como "[P1] (Extra)"
)

/**
 * El resultado final completo.
 * Contiene la lista de cosas para cada semana.
 */
data class DistributionResult(
    val week1: List<DistributedItem> = emptyList(),
    val week2: List<DistributedItem> = emptyList(),
    val week3: List<DistributedItem> = emptyList(),

    // La semana 4 es especial, tiene sub-pedidos (P1, P2, P3)
    val week4: Week4Result = Week4Result(),

    val globalPoints: Double = 0.0,
    val globalMoney: Double = 0.0
)

data class Week4Result(
    val subOrder1: List<DistributedItem> = emptyList(), // P1
    val subOrder2: List<DistributedItem> = emptyList(), // P2
    val subOrder3: List<DistributedItem> = emptyList(), // P3
    val extras: List<DistributedItem> = emptyList()     // Sobrantes
)
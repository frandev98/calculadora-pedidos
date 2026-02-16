package com.francisco.calculadorapedidos.data

data class Product(
    val id: Int,
    val code: String,          // Nuevo: Código (ej. 145079)
    val name: String,          // Nombre principal (ej. Alpha Balance)
    val category: String,      // Nuevo: Categoría (ej. LÍNEA ANTI-EDAD)
    val points: Double,        // QV
    val price: Double,         // Precio Lista
    val presentation: String,  // Detalle (ej. Dp 28 x 5g)
    val isVariableStock: Boolean = false,
    val imageRes: String
)
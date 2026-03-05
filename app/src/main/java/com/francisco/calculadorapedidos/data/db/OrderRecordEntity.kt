package com.francisco.calculadorapedidos.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_records")
data class OrderRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val period: Int,
    val week: Int,
    val clientId: String,
    val isAffiliation: Boolean,

    // Propiedades extraídas del catálogo
    val productId: Int,
    val productCode: String,     // NUEVO: Código inyectado
    val productName: String,
    val productCategory: String, // NUEVO: Categoría inyectada
    val productPresentation: String,
    val productImageRes: String,
    val points: Double,
    val price: Double,
    val quantity: Int
)
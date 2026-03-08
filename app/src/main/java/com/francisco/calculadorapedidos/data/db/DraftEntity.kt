package com.francisco.calculadorapedidos.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val period: Int,
    // EXPANSIÓN ESTRUCTURAL PARA MODO SEMANAL
    val week: Int = 0,
    val clientId: String = "PERIOD",
    val productId: Int,
    val quantity: Int
)
package com.francisco.calculadorapedidos.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val period: Int,
    val productId: Int,
    val quantity: Int
)
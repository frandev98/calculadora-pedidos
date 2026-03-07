package com.francisco.calculadorapedidos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ClientType { FIXED, WILDCARD }

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val fuxionId: String = "",
    val email: String = "",
    val storePassword: String = "",
    val type: ClientType = ClientType.WILDCARD,
    val fixedIndex: Int? = null
)
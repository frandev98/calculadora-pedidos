package com.francisco.calculadorapedidos.data

import java.util.UUID

enum class ClientType {
    FIXED,      // Los 27 Estratégicos
    WILDCARD    // Los Comodines
}

data class Client(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val fuxionId: String = "",      // Código (Se mantiene por si acaso)
    val email: String = "",         // <--- NUEVO PROTAGONISTA (Login)
    val storePassword: String = "", // <--- Renombrado (antes offixPassword)
    val type: ClientType = ClientType.WILDCARD,
    val fixedIndex: Int? = null
)
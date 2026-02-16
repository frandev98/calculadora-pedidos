package com.francisco.calculadorapedidos.data

/**
 * Guarda las reglas del juego.
 * Esto nos permitirá cambiar las metas (35, 60, 120) sin romper la app.
 */
data class DistributionConfig(
    // --- NUEVO CAMPO AGREGADO ---
    // Este es el que el ViewModel está buscando desesperadamente.
    // Lo ponemos con valor por defecto 540.0 para no romper nada.
    val targetPoints: Double = 540.0,

    // --- TUS CAMPOS ORIGINALES (SE MANTIENEN) ---
    // Metas de la Semana 4 (Escalones)
    val week4Base: Double = 35.0,
    val week4Tier1: Double = 60.0,
    val week4Tier2: Double = 120.0,
    val week4Max: Double = 180.0,

    // Metas normales (Semanas 1, 2, 3)
    val weekNormalGoal: Double = 120.0,

    // Umbral para activar el "Modo Alto" (645 puntos)
    val highModeThreshold: Double = 645.0
)
package com.francisco.calculadorapedidos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- ESQUEMA OSCURO (Por si el usuario tiene el móvil en modo noche) ---
private val DarkColorScheme = darkColorScheme(
    primary = FuxionGreen,       // Mantenemos el Verde de éxito
    secondary = FuxionBlue,      // Azul de acción
    tertiary = ProgressOrange,   // Naranja de progreso
    background = Color(0xFF121212), // Negro suave estándar
    surface = Color(0xFF1E1E1E),    // Tarjetas gris oscuro
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0), // Texto claro
    onSurface = Color(0xFFE0E0E0),
    error = ErrorRed
)

// --- ESQUEMA CLARO (Nuestra prioridad estratégica "Zen") ---
private val LightColorScheme = lightColorScheme(
    primary = FuxionGreen,       // El color principal es el Verde (Meta)
    secondary = FuxionBlue,      // El color secundario es el Azul (Acción)
    tertiary = ProgressOrange,   // Alertas y progreso

    // Aquí aplicamos la "Calma Visual":
    background = BackgroundWhite, // Hueso/Gris muy suave
    surface = SurfaceWhite,       // Tarjetas Blancas puras

    // Textos legibles:
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,   // Gris oscuro sobre el fondo
    onSurface = TextPrimary,      // Gris oscuro sobre tarjetas
    error = ErrorRed
)

@Composable
fun CalculadoraPedidosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Cambiamos esto a 'false' para que NO use los colores del wallpaper del usuario,
    // sino NUESTRA psicología de color diseñada.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Código extra para pintar la barra de estado (donde va la hora y batería) del color correcto
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Pintamos la barra de estado del color primario (Verde) o background según prefieras.
            // Aquí la pondré Verde para dar identidad fuerte:
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
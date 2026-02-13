package com.francisco.calculadorapedidos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.francisco.calculadorapedidos.ui.theme.CalculadoraPedidosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculadoraPedidosTheme {
                // Cambiamos la pantalla vieja por la nueva
                OrderScreen()
            }
        }
    }
}
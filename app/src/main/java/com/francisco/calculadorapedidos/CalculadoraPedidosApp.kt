package com.francisco.calculadorapedidos

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CalculadoraPedidosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("HiltApp", "CalculadoraPedidosApp inicializada correctamente")
    }
}
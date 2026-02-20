package com.francisco.calculadorapedidos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.services.FloatingWidgetService

fun launchStoreAssistant(
    context: Context,
    email: String,
    pass: String,
    products: List<Pair<Product, Int>>
) {
    // 1. FORMATEAR EL TEXTO AGRUPADO
    // --- CORRECCIÓN AQUÍ: Usamos 'category' en lugar de 'usage' ---
    val groupedProducts = products.groupBy { it.first.category }

    val stringBuilder = StringBuilder()
    groupedProducts.forEach { (category, items) ->
        // Cabecera de Categoría
        stringBuilder.append("\n--- ${category.uppercase()} ---\n")

        // Productos de esa categoría
        items.forEach { (product, quantity) ->
            stringBuilder.append("• $quantity x ${product.name}\n")
        }
    }
    val finalSummary = stringBuilder.toString()

    // 2. PREPARAR Y LANZAR EL SERVICIO
    if (!Settings.canDrawOverlays(context)) {
        Toast.makeText(context, "Permite mostrar sobre otras apps", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return
    }

    val widgetIntent = Intent(context, FloatingWidgetService::class.java).apply {
        putExtra("PRODUCTS_INFO", finalSummary)
        putExtra("USER_EMAIL", email)
        putExtra("USER_PASS", pass)
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(widgetIntent)
    } else {
        context.startService(widgetIntent)
    }

    // 3. ABRIR CHROME
    val builder = CustomTabsIntent.Builder()
    builder.setShowTitle(true)
    val customTabsIntent = builder.build()
    customTabsIntent.launchUrl(context, Uri.parse("https://ifuxion.com/orphan/enrollment/products"))
}
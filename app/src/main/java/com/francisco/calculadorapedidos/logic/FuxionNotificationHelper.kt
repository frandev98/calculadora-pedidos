package com.francisco.calculadorapedidos.logic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.francisco.calculadorapedidos.R

object FuxionNotificationHelper {

    private const val CHANNEL_ID = "fuxion_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Avisos Fuxion"
            val descriptionText = "Recordatorios de Cierre y Nuevas Semanas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Poner icono propio
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            // Check permission is handled by caller in Activity usually
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Lógica simple para mostrar alerta si estamos cerca del cierre.
     * Esta función se llamaría al abrir la app (en MainActivity).
     */
    fun checkAndNotify(context: Context, status: FuxionCalendarLogic.FuxionStatus) {
        // Ejemplo: Si faltan <= 2 días
        if (status.daysRemainingInWeek <= 2) {
            // Podríamos guardar en DataStore la última vez que notificamos para no ser spam
            // Por ahora, lógica simple.
            showNotification(
                context, 
                "¡Cierre de Semana en ${status.daysRemainingInWeek} días!", 
                "Revisa tu meta para el Periodo ${status.period}."
            )
        }
        
        // Ejemplo: Inicio de nueva semana (Día 1)
        if (status.currentDayOfPeriod % 7 == 1) {
             showNotification(
                context, 
                "¡Inicia la Semana ${status.week}!", 
                "Es momento de planificar tus puntos."
            )
        }
    }
}

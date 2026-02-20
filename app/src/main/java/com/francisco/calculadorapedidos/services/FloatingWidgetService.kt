package com.francisco.calculadorapedidos.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.francisco.calculadorapedidos.R
import kotlin.math.abs

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var collapsedView: View
    private lateinit var expandedView: View
    private lateinit var params: WindowManager.LayoutParams

    // Variables para datos
    private var productsText: String = ""
    private var email: String = ""
    private var pass: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Recibimos los datos nuevos cada vez que se llama
        productsText = intent?.getStringExtra("PRODUCTS_INFO") ?: "Sin datos"
        email = intent?.getStringExtra("USER_EMAIL") ?: ""
        pass = intent?.getStringExtra("USER_PASS") ?: ""

        // Si la vista ya existe, actualizamos el texto al vuelo
        if (::expandedView.isInitialized) {
            expandedView.findViewById<TextView>(R.id.products_tv).text = productsText
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // --- INFLAR VISTAS ---
        collapsedView = LayoutInflater.from(this).inflate(R.layout.widget_collapsed, null)
        expandedView = LayoutInflater.from(this).inflate(R.layout.widget_expanded, null)

        // --- CONFIGURAR PARAMETROS DE VENTANA ---
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        // Iniciar Colapsado
        windowManager.addView(collapsedView, params)

        // --- CONFIGURAR EVENTOS ---
        setupCollapsedTouchListener()
        setupExpandedButtons()
    }

    private fun setupExpandedButtons() {
        // 1. Botón Superior (Minimizar / Flecha abajo)
        // Usamos R.id.btn_minimize_top como definimos en el XML nuevo
        val btnMinimizeTop = expandedView.findViewById<ImageView>(R.id.btn_minimize_top)

        // Pintamos la flecha de blanco para que se vea en el fondo azul
        btnMinimizeTop.setColorFilter(android.graphics.Color.WHITE)

        btnMinimizeTop.setOnClickListener {
            // ACCIÓN: Volver a la burbuja pequeña
            windowManager.removeView(expandedView)
            windowManager.addView(collapsedView, params)
        }

        // 2. Botón Inferior ROJO (Cerrar App)
        val btnCloseApp = expandedView.findViewById<Button>(R.id.btn_close_app)
        btnCloseApp.setOnClickListener {
            // ACCIÓN: Matar el servicio completamente
            stopSelf()
        }

        // 3. Botón Copiar Usuario
        expandedView.findViewById<Button>(R.id.btn_copy_user).setOnClickListener {
            copyToClipboard("Usuario", email)
        }

        // 4. Botón Copiar Password
        expandedView.findViewById<Button>(R.id.btn_copy_pass).setOnClickListener {
            copyToClipboard("Contraseña", pass)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copiado", Toast.LENGTH_SHORT).show()
    }

    private fun setupCollapsedTouchListener() {
        val collapsedIv = collapsedView.findViewById<ImageView>(R.id.collapsed_iv)
        collapsedIv.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val Xdiff = (event.rawX - initialTouchX).toInt()
                        val Ydiff = (event.rawY - initialTouchY).toInt()
                        // Si no se movió mucho, es un CLICK -> Expandir
                        if (abs(Xdiff) < 10 && abs(Ydiff) < 10) {
                            expandWidget()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(collapsedView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun expandWidget() {
        // Intercambiar vistas
        windowManager.removeView(collapsedView)
        // Actualizar datos antes de mostrar
        expandedView.findViewById<TextView>(R.id.products_tv).text = productsText
        windowManager.addView(expandedView, params)
    }

    private fun startForegroundService() {
        val channelId = "floating_widget_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Asistente Fuxion", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Asistente Fuxion Activo")
            .setContentText("Toca para ver tu pedido")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        // NOTA: Android 14 requiere especificar el tipo si usas dataSync
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::collapsedView.isInitialized && collapsedView.isAttachedToWindow) windowManager.removeView(collapsedView)
        if (::expandedView.isInitialized && expandedView.isAttachedToWindow) windowManager.removeView(expandedView)
    }
}
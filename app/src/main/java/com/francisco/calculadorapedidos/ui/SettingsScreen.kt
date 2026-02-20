package com.francisco.calculadorapedidos.ui

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.ui.theme.BackgroundWhite
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.ProgressOrange
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataStore: FuxionDataStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Estado de la fecha ancla
    var anchorDateMillis by remember { mutableStateOf<Long?>(null) }

    // Estado para el diálogo de confirmación de borrado
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Cargar dato inicial
    LaunchedEffect(Unit) {
        anchorDateMillis = dataStore.anchorDateFlow.first()
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FuxionGreen,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Permite scroll si la pantalla es pequeña
        ) {
            // --- SECCIÓN 1: GENERAL (Tu código original) ---
            Text(
                "GENERAL",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // TARJETA DE CONFIGURACIÓN DE FECHA
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = FuxionBlue)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Inicio del Año Fuxion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Fecha base del Periodo 1", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Mostrar fecha actual
                    if (anchorDateMillis != null) {
                        val date = Date(anchorDateMillis!!)
                        val dateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
                        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))

                        val dateStr = dateFormat.format(date)
                        val dayStr = dayOfWeekFormat.format(date).replaceFirstChar { it.uppercase() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Configuración actual:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(dateStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "Los cortes semanales serán los días: $dayStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if(dayStr.lowercase() == "viernes") FuxionGreen else ProgressOrange
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            if (anchorDateMillis != null) {
                                calendar.timeInMillis = anchorDateMillis!!
                            }

                            DatePickerDialog(
                                context,
                                { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, dayOfMonth)
                                    val newDate = newCal.timeInMillis

                                    anchorDateMillis = newDate
                                    scope.launch {
                                        dataStore.saveAnchorDate(newDate)
                                    }
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FuxionBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CAMBIAR FECHA DE INICIO")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // NOTA INFORMATIVA
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp)) {
                    Text(
                        "Nota: Al cambiar la fecha de inicio, todos los periodos y semanas del año se recalcularán automáticamente basándose en ciclos de 7 días exactos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Divider()
            Spacer(Modifier.height(32.dp))

            // --- SECCIÓN 2: ZONA DE PELIGRO (NUEVO) ---
            Text(
                "ZONA DE PELIGRO",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD32F2F), // Rojo oscuro
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Fondo rojo muy suave
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Restablecer Fábrica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            Text("Borra clientes, pedidos y configuraciones.", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("BORRAR TODOS LOS DATOS")
                    }
                }
            }

            // Espacio extra al final para scrolling cómodo
            Spacer(Modifier.height(50.dp))
        }
    }

    // --- DIÁLOGO DE CONFIRMACIÓN DE BORRADO ---
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
            title = { Text("¿Estás absolutamente seguro?") },
            text = {
                Text("Esta acción eliminará permanentemente:\n\n• Todos tus clientes creados.\n• Todos los pedidos guardados.\n• La fecha de inicio configurada.\n\nLa aplicación quedará vacía como recién instalada.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 1. Borrar Clientes
                        context.getSharedPreferences("fuxion_clients_db", Context.MODE_PRIVATE).edit().clear().apply()
                        // 2. Borrar Pedidos
                        context.getSharedPreferences("fuxion_orders_db", Context.MODE_PRIVATE).edit().clear().apply()
                        // 3. Borrar Fecha Inicio
                        scope.launch { dataStore.clearData() }

                        Toast.makeText(context, "App restablecida. Datos borrados.", Toast.LENGTH_LONG).show()
                        showDeleteConfirm = false

                        // Opcional: Volver atrás para forzar recarga visual
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("SÍ, BORRAR TODO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCELAR")
                }
            },
            containerColor = Color.White
        )
    }
}
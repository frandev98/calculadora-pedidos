package com.francisco.calculadorapedidos.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAffiliation: (Int, Int, Int) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userStartTimestamp by viewModel.userStartTimestampFlow.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) }
    val dateText = if (userStartTimestamp != null && userStartTimestamp != 0L) {
        dateFormat.format(Date(userStartTimestamp!!))
    } else {
        "Fecha no configurada"
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (userStartTimestamp != null && userStartTimestamp != 0L) userStartTimestamp else Calendar.getInstance().timeInMillis
    )

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FuxionGreen, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("GENERAL", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = FuxionBlue)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Fecha de Afiliación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Punto exacto de inicio de tu PV4", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha", tint = FuxionBlue)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FuxionBlue,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    if (userStartTimestamp != null) {
                        Spacer(Modifier.height(8.dp))
                        val coords = remember(userStartTimestamp) { FuxionCalendarLogic.getCoordinatesFromDate(userStartTimestamp!!) }
                        Surface(color = FuxionBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Se te asignará a: Año ${coords.year} | Periodo ${coords.period} | Semana ${coords.week}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FuxionBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("NEGOCIO Y RENTABILIDAD", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = FuxionGreen)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Paquete de Inicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Activa tu descuento base", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ingresa los productos de tu afiliación (Mín. 40 pts). Esta inversión suma a tu PV4 de forma inmediata.",
                        style = MaterialTheme.typography.bodySmall, color = Color.DarkGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (userStartTimestamp != null) {
                                val coords = FuxionCalendarLogic.getCoordinatesFromDate(userStartTimestamp!!)
                                onNavigateToAffiliation(coords.year, coords.period, coords.week)
                            } else {
                                Toast.makeText(context, "Configura tu fecha de afiliación primero", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FuxionGreen),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = userStartTimestamp != null
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CONFIGURAR PAQUETE")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(32.dp))

            Text("ZONA DE PELIGRO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
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
            Spacer(Modifier.height(50.dp))
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            viewModel.saveUserStartTimestamp(timestamp)
                        }
                        showDatePicker = false
                    }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
                title = { Text("¿Estás absolutamente seguro?") },
                text = { Text("Esta acción eliminará permanentemente todos tus clientes creados, pedidos y configuraciones.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.factoryReset {
                                Toast.makeText(context, "App restablecida. Datos destruidos.", Toast.LENGTH_LONG).show()
                                showDeleteConfirm = false
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("SÍ, BORRAR TODO") }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCELAR") } },
                containerColor = Color.White
            )
        }
    }
}
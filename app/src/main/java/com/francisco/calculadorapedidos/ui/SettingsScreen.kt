package com.francisco.calculadorapedidos.ui

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataStore: FuxionDataStore,
    onBack: () -> Unit,
    onNavigateToAffiliation: (Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var anchorDateMillis by remember { mutableStateOf<Long?>(null) }
    val userStartPeriod by dataStore.userStartPeriodFlow.collectAsState(initial = 1)

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expandedPeriod by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        anchorDateMillis = dataStore.anchorDateFlow.first()
    }

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
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = FuxionBlue)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Inicio del Año Fuxion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Fecha base del Periodo 1", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (anchorDateMillis != null) {
                        val date = Date(anchorDateMillis!!)
                        val dateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
                        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))

                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(12.dp)
                        ) {
                            Column {
                                Text("Configuración actual:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(dateFormat.format(date), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Cortes semanales los: ${dayOfWeekFormat.format(date).replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = ProgressOrange)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            if (anchorDateMillis != null) calendar.timeInMillis = anchorDateMillis!!
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, dayOfMonth)
                                    val newDate = newCal.timeInMillis
                                    anchorDateMillis = newDate
                                    scope.launch { dataStore.saveAnchorDate(newDate) }
                                },
                                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
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

            Spacer(Modifier.height(16.dp))

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
                            Text("Periodo de Afiliación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Ciclo de inicio de matriz personal", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedPeriod,
                        onExpandedChange = { expandedPeriod = !expandedPeriod }
                    ) {
                        OutlinedTextField(
                            value = "Periodo ${userStartPeriod ?: 1}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FuxionBlue,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPeriod,
                            onDismissRequest = { expandedPeriod = false }
                        ) {
                            (1..13).forEach { period ->
                                DropdownMenuItem(
                                    text = { Text("Periodo $period") },
                                    onClick = {
                                        scope.launch { dataStore.saveUserStartPeriod(period) }
                                        expandedPeriod = false
                                    }
                                )
                            }
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
                            Text("Activa tu PV4 desde el día cero", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ingresa los productos de tu afiliación (Mín. 40 pts). Esta inversión recibe un 20% de descuento automático y sumará al cálculo histórico de tu PV4.",
                        style = MaterialTheme.typography.bodySmall, color = Color.DarkGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                            onNavigateToAffiliation(currentYear, userStartPeriod ?: 1)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FuxionGreen),
                        modifier = Modifier.fillMaxWidth()
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
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
            title = { Text("¿Estás absolutamente seguro?") },
            text = { Text("Esta acción eliminará permanentemente:\n\n• Todos tus clientes creados.\n• Todos los pedidos guardados.\n• La fecha de inicio configurada.\n\nLa aplicación quedará vacía como recién instalada.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val orderRepo = com.francisco.calculadorapedidos.data.OrderRepository(context)
                        context.getSharedPreferences("fuxion_clients_db", Context.MODE_PRIVATE).edit().clear().apply()
                        context.getSharedPreferences("fuxion_orders_db", Context.MODE_PRIVATE).edit().clear().apply()

                        scope.launch {
                            orderRepo.wipeAllRelationalData()
                            dataStore.clearData()
                        }

                        Toast.makeText(context, "App restablecida. Datos relacionales destruidos.", Toast.LENGTH_LONG).show()
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("SÍ, BORRAR TODO") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCELAR") } },
            containerColor = Color.White
        )
    }
}
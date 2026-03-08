package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// INYECCIÓN DE DEPENDENCIA DE HILT
import androidx.hilt.navigation.compose.hiltViewModel
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.OrderMetrics
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.ui.theme.*
import com.francisco.calculadorapedidos.data.FuxionDataStore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekManagementScreen(
    year: Int,
    periodId: Int,
    weekId: Int,
    dataStore: FuxionDataStore,
    onBack: () -> Unit,
    onNavigateToOrder: (String, Int) -> Unit,
    // CORRECCIÓN CRÍTICA: Delegación a Hilt
    clientViewModel: ClientViewModel = hiltViewModel(),
    weekViewModel: WeekViewModel = hiltViewModel()
) {
    val allClients by clientViewModel.clients.collectAsState()
    val uiState by weekViewModel.uiState.collectAsState()

    val userStartTimestamp by dataStore.userStartTimestampFlow.collectAsState(initial = null)

    if (userStartTimestamp == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

// Extracción matemática del periodo para mantener retrocompatibilidad con las lógicas visuales
    val userStartPeriod = remember(userStartTimestamp) {
        FuxionCalendarLogic.getCoordinatesFromDate(userStartTimestamp!!).period
    }

    val slots = remember(periodId, weekId, userStartPeriod) {
        FuxionCalendarLogic.getSlotsForWeek(periodId, weekId, userStartPeriod!!)
    }

    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showWildcardDialog by remember { mutableStateOf(false) }

    val totalWeekTarget = remember(slots) {
        if (slots.size > 1) 125 else slots.firstOrNull()?.targetPoints ?: 125
    }

    // GATILLO DE ESTADO PURGADO DE REPOSITORIOS MANUALES
    LaunchedEffect(year, periodId, weekId, allClients, showWildcardDialog) {
        weekViewModel.loadWeekData(year, periodId, weekId, allClients)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión: Semana $weekId ($year)") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FuxionBlue)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundWhite).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FuxionBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OBJETIVO SEMANAL (PRO 500)", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("${uiState.currentWeekPoints} / $totalWeekTarget pts", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                    if (uiState.currentWeekMoney > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(String.format(Locale("es", "PE"), "Inversión Total: S/ %,.2f", uiState.currentWeekMoney), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    }

                    Spacer(Modifier.height(8.dp))
                    val progress = (uiState.currentWeekPoints.toFloat() / totalWeekTarget.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(8.dp), color = FuxionGreen, trackColor = Color.White.copy(alpha = 0.3f))

                    Spacer(Modifier.height(12.dp))
                    Surface(color = Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥 PV4 Actual: ${uiState.currentPV4Points} pts", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("|", color = Color.White.copy(alpha = 0.5f))
                            Spacer(Modifier.width(8.dp))
                            Text("Descuento: ${(uiState.currentDiscount * 100).toInt()}%", color = Color(0xFFFFCC80), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Matriz de Clientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn {
                items(slots) { slot ->
                    val clientEntity = allClients.find { it.type == com.francisco.calculadorapedidos.data.ClientType.FIXED && it.fixedIndex == slot.fixedIndex }
                    if (clientEntity == null) return@items

                    val metrics = uiState.clientProgress[clientEntity.id] ?: OrderMetrics(0, 0.0)
                    val isCompleted = metrics.points >= slot.targetPoints

                    val cardContainer = if (isCompleted) Color(0xFFE8F5E9) else Color.White
                    val cardBorder = if (isCompleted) FuxionGreen else Color(0xFFE0E0E0)

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onNavigateToOrder(clientEntity.id, slot.targetPoints) },
                        colors = CardDefaults.cardColors(containerColor = cardContainer),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = if (isCompleted) FuxionGreen else Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Person, null, tint = if (!isCompleted) TextSecondary else Color.White, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(clientEntity.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Meta: ${slot.targetPoints} pts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${metrics.points} pts", fontWeight = FontWeight.Bold, color = if (isCompleted) FuxionGreen else FuxionBlue)
                                        if (isCompleted) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.CheckCircle, null, tint = FuxionGreen, modifier = Modifier.size(16.dp)) }
                                    }
                                    if (metrics.money > 0) {
                                        Text(String.format(Locale("es", "PE"), "S/ %,.2f", metrics.money), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.DarkGray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { clientToEdit = clientEntity; showEditDialog = true }) { Icon(Icons.Default.Edit, "Editar", tint = TextSecondary) }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Comodines (Ventas Extra)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        TextButton(onClick = { showWildcardDialog = true }) { Text("+ AGREGAR") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.activeWildcards) { wildcard ->
                    val metrics = uiState.clientProgress[wildcard.id] ?: OrderMetrics(0, 0.0)

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onNavigateToOrder(wildcard.id, 0) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, FuxionBlue)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = FuxionBlue, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(wildcard.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Sin límite de meta", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${metrics.points} pts", fontWeight = FontWeight.Bold, color = FuxionBlue)
                                if (metrics.money > 0) {
                                    Text(String.format(Locale("es", "PE"), "S/ %,.2f", metrics.money), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        ClientDialog(
            clientToEdit = clientToEdit,
            fixedIndex = clientToEdit?.fixedIndex,
            onDismiss = { showEditDialog = false },
            onSave = { name, code, email, pass ->
                if (clientToEdit?.fixedIndex != null) {
                    clientViewModel.saveFixedClient(clientToEdit!!.fixedIndex!!, name, code, email, pass)
                } else {
                    val isNewWildcard = clientToEdit == null
                    val targetId = clientToEdit?.id ?: java.util.UUID.randomUUID().toString()
                    clientViewModel.saveWildcardClient(name, code, email, pass, targetId)
                    if (isNewWildcard) onNavigateToOrder(targetId, 0)
                }
                showEditDialog = false
            }
        )
    }

    if (showWildcardDialog) {
        val availableWildcards = allClients.filter { it.type == com.francisco.calculadorapedidos.data.ClientType.WILDCARD }
        AlertDialog(
            onDismissRequest = { showWildcardDialog = false },
            title = { Text("Seleccionar o Crear Comodín") },
            text = {
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showWildcardDialog = false; clientToEdit = null; showEditDialog = true }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = FuxionBlue)
                            Spacer(Modifier.width(16.dp))
                            Text("Crear Nuevo Comodín", fontWeight = FontWeight.Bold, color = FuxionBlue)
                        }
                        HorizontalDivider()
                    }
                    if (availableWildcards.isEmpty()) {
                        item { Text("No hay comodines previos disponibles.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                    } else {
                        items(availableWildcards) { wc ->
                            Text(wc.name, modifier = Modifier.fillMaxWidth().clickable { showWildcardDialog = false; onNavigateToOrder(wc.id, 0) }.padding(16.dp), fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWildcardDialog = false }) { Text("Cancelar") } }
        )
    }
}
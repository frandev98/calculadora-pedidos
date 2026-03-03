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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekManagementScreen(
    periodId: Int,
    weekId: Int,
    targetGoal: Int,
    onBack: () -> Unit,
    onNavigateToOrder: (String, Int) -> Unit,
    onNavigateToClientList: () -> Unit
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository(context) }
    val clientRepository = remember { ClientRepository(context) } // INYECCIÓN DE REPOSITORIO

    val slots = remember(periodId, weekId) { FuxionCalendarLogic.getSlotsForWeek(periodId, weekId) }
    val totalWeekTarget = remember(slots) { slots.sumOf { it.targetPoints } }

    var clientProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var currentWeekPoints by remember { mutableIntStateOf(0) }

    LaunchedEffect(periodId, weekId) {
        val progressMap = mutableMapOf<String, Int>()
        var total = 0
        slots.forEach { slot ->
            // Se lee al cliente real para buscar su UUID y sus pedidos
            val fixedClient = clientRepository.getFixedClient(slot.fixedIndex)
            if (fixedClient != null) {
                val order = orderRepository.getOrder(periodId, weekId, fixedClient.id)
                val pts = order.sumOf { it.first.points * it.second }.toInt()
                progressMap[fixedClient.id] = pts
                total += pts
            }
        }
        clientProgress = progressMap
        currentWeekPoints = total
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión: Semana $weekId") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FuxionBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("OBJETIVO SEMANAL (PRO 500)", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("$currentWeekPoints / $totalWeekTarget pts", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val progress = (currentWeekPoints.toFloat() / totalWeekTarget.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = FuxionGreen,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Matriz de Clientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn {
                items(slots) { slot ->
                    // VALIDACIÓN DE INTEGRIDAD REFERENCIAL
                    val clientEntity = clientRepository.getFixedClient(slot.fixedIndex)
                    val isConfigured = clientEntity != null

                    val displayName = clientEntity?.name ?: "${slot.fallbackName} (Requiere Configuración)"
                    val clientIdToSave = clientEntity?.id ?: ""

                    val currentPts = if (isConfigured) clientProgress[clientIdToSave] ?: 0 else 0
                    val isCompleted = isConfigured && currentPts >= slot.targetPoints

                    // ESTILOS DINÁMICOS BASADOS EN ESTADO DE CONFIGURACIÓN
                    val cardContainer = if (!isConfigured) Color(0xFFFFF0F0) else if (isCompleted) Color(0xFFE8F5E9) else Color.White
                    val cardBorder = if (!isConfigured) Color.Red else if (isCompleted) FuxionGreen else Color(0xFFE0E0E0)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = isConfigured) {
                                // Bloqueo estricto: no navega si el UUID es vacío
                                if (isConfigured) onNavigateToOrder(clientIdToSave, slot.targetPoints)
                            },
                        colors = CardDefaults.cardColors(containerColor = cardContainer),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (!isConfigured) Color.Red else if (isCompleted) FuxionGreen else Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (!isConfigured) Icons.Default.Warning else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isConfigured && !isCompleted) TextSecondary else Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isConfigured) Color.Red else TextPrimary
                                )
                                Text("Meta de ranura: ${slot.targetPoints} pts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (isConfigured) {
                                    Text(
                                        "$currentPts pts",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) FuxionGreen else FuxionBlue
                                    )
                                    if (isCompleted) {
                                        Icon(Icons.Default.CheckCircle, null, tint = FuxionGreen, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Text("Bloqueado", fontWeight = FontWeight.Bold, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
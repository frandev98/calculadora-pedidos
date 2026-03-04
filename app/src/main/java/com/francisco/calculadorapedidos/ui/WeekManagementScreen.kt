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
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekManagementScreen(
    year: Int,
    periodId: Int,
    weekId: Int,
    dataStore: FuxionDataStore, // <-- NUEVA DEPENDENCIA ESTRUCTURAL
    onBack: () -> Unit,
    onNavigateToOrder: (String, Int) -> Unit,
    clientViewModel: ClientViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository(context) }

    val allClients by clientViewModel.clients.collectAsState()
    val userStartPeriod by dataStore.userStartPeriodFlow.collectAsState(initial = null)

    if (userStartPeriod == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // PROPAGACIÓN DE DESFASE A LA MEMORIA DE COMPOSEC
    val slots = remember(periodId, weekId, userStartPeriod) {
        FuxionCalendarLogic.getSlotsForWeek(periodId, weekId, userStartPeriod!!)
    }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val totalWeekTarget = remember(slots) {
        if (slots.size > 1) 125 else slots.firstOrNull()?.targetPoints ?: 125
    }

    var clientProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var currentWeekPoints by remember { mutableIntStateOf(0) }

    var activeWildcards by remember { mutableStateOf<List<Client>>(emptyList()) }
    var showWildcardDialog by remember { mutableStateOf(false) }

    // INYECCIÓN DE LLAVE 'year' PARA RECOMPOSICIÓN REACTIVA
    LaunchedEffect(year, periodId, weekId, showWildcardDialog, allClients) {
        val progressMap = mutableMapOf<String, Int>()
        var total = 0
        val wildcardsInThisWeek = mutableListOf<Client>()

        allClients.forEach { client ->
            // RESOLUCIÓN DE RUPTURA: Propagación de parámetro interanual
            val order = orderRepository.getOrder(year, periodId, weekId, client.id)
            val pts = order.sumOf { it.first.points * it.second }.toInt()

            if (pts > 0) {
                progressMap[client.id] = pts
                total += pts
                if (client.type == com.francisco.calculadorapedidos.data.ClientType.WILDCARD) {
                    wildcardsInThisWeek.add(client)
                }
            } else if (client.type == com.francisco.calculadorapedidos.data.ClientType.FIXED) {
                progressMap[client.id] = 0
            }
        }

        clientProgress = progressMap
        currentWeekPoints = total
        activeWildcards = wildcardsInThisWeek
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión: Semana $weekId ($year)") },
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
                    val clientEntity = allClients.find { it.type == com.francisco.calculadorapedidos.data.ClientType.FIXED && it.fixedIndex == slot.fixedIndex }

                    if (clientEntity == null) return@items

                    val currentPts = clientProgress[clientEntity.id] ?: 0
                    val isCompleted = currentPts >= slot.targetPoints

                    val cardContainer = if (isCompleted) Color(0xFFE8F5E9) else Color.White
                    val cardBorder = if (isCompleted) FuxionGreen else Color(0xFFE0E0E0)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onNavigateToOrder(clientEntity.id, slot.targetPoints)
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
                                color = if (isCompleted) FuxionGreen else Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (!isCompleted) TextSecondary else Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clientEntity.name,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text("Meta: ${slot.targetPoints} pts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$currentPts pts",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) FuxionGreen else FuxionBlue
                                    )
                                    if (isCompleted) {
                                        Icon(Icons.Default.CheckCircle, null, tint = FuxionGreen, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(onClick = {
                                    clientToEdit = clientEntity
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Comodines (Ventas Extra)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        TextButton(onClick = { showWildcardDialog = true }) {
                            Text("+ AGREGAR")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(activeWildcards) { wildcard ->
                    val currentPts = clientProgress[wildcard.id] ?: 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onNavigateToOrder(wildcard.id, 0) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, FuxionBlue)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = FuxionBlue,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = wildcard.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Sin límite de meta", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Text(
                                "$currentPts pts",
                                fontWeight = FontWeight.Bold,
                                color = FuxionBlue
                            )
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
                    clientViewModel.saveFixedClient(
                        index = clientToEdit!!.fixedIndex!!,
                        name = name,
                        code = code,
                        email = email,
                        pass = pass
                    )
                } else {
                    clientViewModel.saveWildcardClient(
                        name = name,
                        code = code,
                        email = email,
                        pass = pass,
                        existingId = clientToEdit?.id
                    )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showWildcardDialog = false
                                    clientToEdit = null
                                    showEditDialog = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = FuxionBlue)
                            Spacer(Modifier.width(16.dp))
                            Text("Crear Nuevo Comodín", fontWeight = FontWeight.Bold, color = FuxionBlue)
                        }
                        HorizontalDivider()
                    }

                    if (availableWildcards.isEmpty()) {
                        item {
                            Text(
                                "No hay comodines previos disponibles.",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(availableWildcards) { wc ->
                            Text(
                                text = wc.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showWildcardDialog = false
                                        onNavigateToOrder(wc.id, 0)
                                    }
                                    .padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWildcardDialog = false }) { Text("Cancelar") } }
        )
    }
}
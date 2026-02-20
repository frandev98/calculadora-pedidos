package com.francisco.calculadorapedidos.ui

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.ClientType
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.logic.StrategyEngine
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.ProgressOrange
import android.widget.Toast
import androidx.compose.ui.platform.LocalUriHandler

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
    val clientRepo = remember { ClientRepository(context) }
    val orderRepo = remember { OrderRepository(context) }

    // Recalcular estrategia si cambian parámetros
    val strategyTasks = remember(periodId, weekId, targetGoal) {
        StrategyEngine.getRequiredClients(periodId, weekId, targetGoal)
    }

    var refreshTrigger by remember { mutableStateOf(0) }

    // Gestión de Comodines
    var showWildcardDialog by remember { mutableStateOf(false) }
    val allWildcards = remember { clientRepo.getAllClients().filter { it.type == ClientType.WILDCARD } }

    val activeWildcards = remember(refreshTrigger) {
        allWildcards.filter {
            orderRepo.getOrder(periodId, weekId, it.id).isNotEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Semana $weekId - Periodo $periodId") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FuxionBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showWildcardDialog = true },
                containerColor = FuxionBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("AGREGAR COMODÍN")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- ESTRATEGIA PRINCIPAL ---
            item {
                Text("Estrategia Principal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(strategyTasks) { task ->
                val clientObj = remember(key1 = refreshTrigger) { clientRepo.getFixedClient(task.clientIndex) }
                if (clientObj != null) {
                    // Obtenemos orden completa para sacar resumen y puntos
                    val currentOrder = orderRepo.getOrder(periodId, weekId, clientObj.id)
                    val currentPoints = currentOrder.sumOf { it.first.points * it.second }.toInt()
                    val productsSummary = formatOrderSummary(products = currentOrder)

                    ClientCard(
                        client = clientObj, // Ojo: aquí usas el cliente de la tarea
                        targetPoints = task.targetPoints,
                        currentPoints = currentPoints,
                        productsSummary = productsSummary,
                        isStrategy = true,
                        taskIndex = task.clientIndex,
                        onClick = { onNavigateToOrder(clientObj.id, task.targetPoints) },
                        // --- ESTA ES LA LÍNEA QUE FALTABA ---
                        onAutoLogin = {
                            if (clientObj.storePassword.isNotEmpty()) {
                                launchStoreAssistant(
                                    context = context,
                                    email = clientObj.email,
                                    pass = clientObj.storePassword,
                                    products = currentOrder // <--- PASAMOS LA LISTA ORIGINAL DIRECTAMENTE
                                )
                            } else {
                                Toast.makeText(context, "Sin contraseña guardada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    EmptyStrategyCard(index = task.clientIndex, onClick = onNavigateToClientList)
                }
            }

            // --- COMODINES ---
            if (activeWildcards.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Pedidos Adicionales (Comodines)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(activeWildcards) { wildcard ->
                    val currentOrder = orderRepo.getOrder(periodId, weekId, wildcard.id)
                    val currentPoints = currentOrder.sumOf { it.first.points * it.second }.toInt()
                    val productSummary = formatOrderSummary(currentOrder)

                    ClientCard(
                        client = wildcard,
                        targetPoints = 0,
                        currentPoints = currentPoints,
                        productsSummary = productSummary,
                        isStrategy = false,
                        onClick = { onNavigateToOrder(wildcard.id, 0) },

                        // --- ESTA ES LA CORRECCIÓN ---
                        onAutoLogin = {
                            if (wildcard.storePassword.isNotEmpty()) {
                                launchStoreAssistant(
                                    context = context,
                                    email = wildcard.email,
                                    pass = wildcard.storePassword,
                                    products = currentOrder // <--- USA 'productSummary' (SIN LA S) AQUÍ
                                )
                            } else {
                                Toast.makeText(context, "Sin contraseña guardada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // --- TOTAL GLOBAL ---
            item {
                Spacer(Modifier.height(32.dp))
                val totalPoints = orderRepo.getWeekTotalPoints(periodId, weekId)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F))) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL SEMANA:", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("$totalPoints pts", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // --- DIÁLOGO SELECCIÓN COMODÍN ---
    if (showWildcardDialog) {
        AlertDialog(
            onDismissRequest = { showWildcardDialog = false },
            title = { Text("Seleccionar Comodín") },
            text = {
                LazyColumn {
                    items(allWildcards) { wc ->
                        ListItem(
                            headlineContent = { Text(wc.name) },
                            modifier = Modifier.clickable {
                                showWildcardDialog = false
                                onNavigateToOrder(wc.id, 0)
                            }
                        )
                        Divider()
                    }
                    if (allWildcards.isEmpty()) {
                        item { Text("No tienes comodines registrados.") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showWildcardDialog = false
                    onNavigateToClientList()
                }) { Text("CREAR NUEVO") }
            },
            dismissButton = { TextButton(onClick = { showWildcardDialog = false }) { Text("CANCELAR") } }
        )
    }
}

// --- FUNCIÓN HELPER PARA FORMATO DE TEXTO ---
fun formatOrderSummary(products: List<Pair<Product, Int>>): String {
    if (products.isEmpty()) return ""
    // Crea string tipo: "Alpha Balance (2), Vita Xtra (1)"
    return products.joinToString(", ") { "${it.first.name} (${it.second})" }
}

@Composable
fun ClientCard(
    client: Client,
    targetPoints: Int,
    currentPoints: Int,
    productsSummary: String,
    isStrategy: Boolean,
    taskIndex: Int = 0,
    onClick: () -> Unit,
    onAutoLogin: () -> Unit
) {
    val isCompleted = if(targetPoints > 0) currentPoints >= targetPoints else currentPoints > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 1. Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isStrategy) {
                    Surface(color = FuxionBlue, shape = RoundedCornerShape(4.dp)) {
                        Text("#$taskIndex", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (client.email.isNotEmpty()) {
                        Text(client.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                if (isCompleted) Icon(Icons.Default.CheckCircle, null, tint = FuxionGreen)
            }

            Spacer(Modifier.height(12.dp))

            // 2. BOTÓN TIENDA (Autologin)
            // Ya no mostramos chips de copiar, vamos directo a la acción
            Button(
                onClick = onAutoLogin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)) // Azul Tienda
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ENTRAR A TIENDA", fontWeight = FontWeight.Bold)
            }

            // 3. Resumen y Puntos (Se mantiene igual)
            if (productsSummary.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(productsSummary, style = MaterialTheme.typography.bodySmall, color = Color(0xFF616161), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(8.dp))

            // 4. Barra Inferior
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currentPoints / $targetPoints pts",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) FuxionGreen else ProgressOrange,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClick) {
                    Text(if (currentPoints > 0) "EDITAR" else "REGISTRAR PUNTOS")
                }
            }
        }
    }
}

@Composable
fun CopyChip(
    label: String,
    textToCopy: String,
    toastMessage: String // <--- Nuevo parámetro para control total
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable {
            clipboardManager.setText(AnnotatedString(textToCopy))
            // Mensaje corto y directo definido por el padre
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        }
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
        }
    }
}

@Composable
fun EmptyStrategyCard(index: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Falta Cliente #$index", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                Text("Toca para registrarlo ahora", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
            }
        }
    }
}
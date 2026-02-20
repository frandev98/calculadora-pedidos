package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.ClientType
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    viewModel: ClientViewModel = viewModel(),
    onBack: () -> Unit
) {
    // 1. RECOLECTAR EL ESTADO (Soluciona el error de .filter)
    val allClients by viewModel.clients.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }
    var targetFixedIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Socios y Clientes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FuxionBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingClient = null
                        targetFixedIndex = null
                        showDialog = true
                    },
                    containerColor = FuxionGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // TABS
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = FuxionBlue) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Estrategia (1-27)") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Comodines") })
            }

            // CONTENIDO
            if (selectedTab == 0) {
                FixedStrategyGrid(
                    clients = allClients.filter { it.type == ClientType.FIXED },
                    onSlotClick = { index, existingClient ->
                        targetFixedIndex = index
                        editingClient = existingClient
                        showDialog = true
                    }
                )
            } else {
                WildcardList(
                    clients = allClients.filter { it.type == ClientType.WILDCARD },
                    onEdit = {
                        editingClient = it
                        targetFixedIndex = null
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteClient(it.id) }
                )
            }
        }
    }

    // DIÁLOGO
    if (showDialog) {
        ClientDialog(
            clientToEdit = editingClient,
            fixedIndex = targetFixedIndex,
            onDismiss = { showDialog = false },
            onSave = { name, code, email, pass -> // <--- Solo 4 argumentos
                if (targetFixedIndex != null) {
                    viewModel.saveFixedClient(targetFixedIndex!!, name, code, email, pass)
                } else {
                    viewModel.saveWildcardClient(name, code, email, pass, editingClient?.id)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun FixedStrategyGrid(
    clients: List<Client>,
    onSlotClick: (Int, Client?) -> Unit
) {
    // Definimos las fases para agrupar visualmente
    val phases = listOf(
        Triple("Fase 1: Cimientos", 1, 9),
        Triple("Fase 2: Construcción", 10, 18),
        Triple("Fase 3: Expansión", 19, 27)
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp) // Espacio para el FAB
    ) {
        phases.forEach { (title, start, end) ->
            // Cabecera de la Fase
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FuxionBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Grid de esa fase (3 filas de 3)
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    for (row in 0 until 3) { // 3 filas por fase
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (col in 0 until 3) { // 3 columnas
                                val index = start + (row * 3) + col
                                if (index <= end) {
                                    val client = clients.find { it.fixedIndex == index }
                                    Box(Modifier.weight(1f)) {
                                        ClientSlotCard(
                                            position = index,
                                            client = client,
                                            onClick = { onSlotClick(index, client) }
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp)) // Espacio entre filas
                    }
                }
            }
        }
    }
}

@Composable
fun ClientSlotCard(
    position: Int,
    client: Client?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (client != null) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (client != null) FuxionGreen else Color.LightGray)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "#$position",
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (client != null) FuxionGreen else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (client != null) {
                    Icon(Icons.Default.Person, null, tint = FuxionGreen)
                    Text(
                        client.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Icon(Icons.Default.Add, null, tint = Color.Gray)
                    Text("Vacío", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun WildcardList(
    clients: List<Client>,
    onEdit: (Client) -> Unit,
    onDelete: (Client) -> Unit
) {
    if (clients.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No hay clientes comodines aún", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SOLUCIÓN LISTA: Usamos items(clients) directamente
            // NO 'items(count = clients)', eso estaba causando el error de .name
            items(items = clients, key = { it.id }) { client ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(client) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = FuxionBlue.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = client.name.firstOrNull()?.toString() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    color = FuxionBlue
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.name, fontWeight = FontWeight.Bold)
                            // AHORA SÍ RECONOCERÁ .username PORQUE 'client' ES UN OBJETO CLIENT
                            Text(text = "Email: ${client.email}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { onDelete(client) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Gray)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ClientDialog(
    clientToEdit: Client?,
    fixedIndex: Int?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit // Firma simplificada: Name, Code, Email, Pass
) {
    var name by remember { mutableStateOf(clientToEdit?.name ?: "") }
    var code by remember { mutableStateOf(clientToEdit?.fuxionId ?: "") }
    var email by remember { mutableStateOf(clientToEdit?.email ?: "") }
    var pass by remember { mutableStateOf(clientToEdit?.storePassword ?: "") }

    val title = if (fixedIndex != null) "Editar Posición #$fixedIndex" else if (clientToEdit != null) "Editar Comodín" else "Nuevo Cliente"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // 1. Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // 2. Correo (Login)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico (Login)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(12.dp))

                // 3. Contraseña y Código
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Contraseña Tienda") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código (Opc)") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank()) {
                                onSave(name, code, email, pass)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FuxionBlue)
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
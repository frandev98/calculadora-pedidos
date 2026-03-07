package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
// INYECCIÓN CRÍTICA: Delegación a Hilt
import androidx.hilt.navigation.compose.hiltViewModel
import com.francisco.calculadorapedidos.data.Client
import com.francisco.calculadorapedidos.data.ClientType
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    // DELEGACIÓN DEL CICLO DE VIDA A LA FACTORÍA DE HILT
    viewModel: ClientViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    // 1. RECOLECTAR EL ESTADO
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
            onSave = { name, code, email, pass ->
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
    onSlotClick: (Int, Client) -> Unit
) {
    // Ordenamiento estricto por índice matemático para garantizar la correlación con la Matriz PRO 500
    val sortedClients = clients.sortedBy { it.fixedIndex }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Matriz Base PRO 500 (13 Semanas)",
                style = MaterialTheme.typography.titleMedium,
                color = FuxionBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(sortedClients, key = { it.fixedIndex ?: it.id }) { client ->
            val index = client.fixedIndex ?: return@items

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSlotClick(index, client) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AVATAR POSICIONAL: Indicador numérico estricto
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FuxionGreen)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$index",
                                fontWeight = FontWeight.Bold,
                                color = FuxionGreen,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // METADATOS EXPUESTOS
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = client.name,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (client.email.isNotBlank()) {
                            Text(
                                text = "Login: ${client.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Requiere configuración",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                    }

                    // VECTOR DE EDICIÓN
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Cliente",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) } // Espacio para FAB subyacente
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
    onSave: (String, String, String, String) -> Unit
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
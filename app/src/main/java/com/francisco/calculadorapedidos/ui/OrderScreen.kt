package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.francisco.calculadorapedidos.data.DistributedItem
import com.francisco.calculadorapedidos.data.DistributionResult
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.data.ProductCatalog
import com.francisco.calculadorapedidos.ui.theme.BackgroundWhite
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.ProgressOrange
import com.francisco.calculadorapedidos.ui.theme.SurfaceWhite
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    year: Int,
    targetGoal: Int,
    isWeeklyMode: Boolean,
    periodId: Int = 0,
    weekId: Int = 0,
    clientId: String = "",
    viewModel: OrderViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository(context) }

    val dataStore = remember { com.francisco.calculadorapedidos.data.FuxionDataStore(context) }
    val userStartPeriod by dataStore.userStartPeriodFlow.collectAsState(initial = null)

    if (userStartPeriod == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LaunchedEffect(year, targetGoal, isWeeklyMode, periodId, weekId, userStartPeriod) {
        viewModel.setupMode(targetGoal, isWeeklyMode, periodId, userStartPeriod!!)

        if (isWeeklyMode && periodId != 0 && weekId != 0) {
            val savedProducts = orderRepository.getOrder(year, periodId, weekId, clientId)
            if (savedProducts.isNotEmpty()) {
                viewModel.loadProducts(savedProducts)
            }
        } else if (!isWeeklyMode && periodId != 0) {
            val draftProducts = orderRepository.getPeriodDraft(year, periodId)
            if (draftProducts.isNotEmpty()) {
                viewModel.loadProducts(draftProducts)
            }
        }
    }

    var showGlobalSaveSuccess by remember { mutableStateOf(false) }

    if (viewModel.distributionResult != null) {
        ResultView(
            result = viewModel.distributionResult!!,
            onBack = { viewModel.clearResult() },
            onSave = {
                val clientRepo = com.francisco.calculadorapedidos.data.ClientRepository(context)
                viewModel.saveFullDistribution(
                    year = year,
                    result = viewModel.distributionResult!!,
                    orderRepository = orderRepository,
                    clientRepository = clientRepo
                )
                orderRepository.clearPeriodDraft(year, periodId)
                showGlobalSaveSuccess = true
            }
        )
    } else {
        OrderInputView(
            viewModel = viewModel,
            onBack = onBack,
            year = year,
            periodId = periodId,
            weekId = weekId,
            clientId = clientId,
            orderRepository = orderRepository
        )
    }

    if (showGlobalSaveSuccess) {
        SuccessDialog(
            onDismiss = {
                showGlobalSaveSuccess = false
                viewModel.clearResult()
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderInputView(
    viewModel: OrderViewModel,
    onBack: () -> Unit,
    year: Int,
    periodId: Int,
    weekId: Int,
    clientId: String,
    orderRepository: OrderRepository
) {
    var showCatalog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    val title = if (viewModel.isWeeklyMode) "Calculadora Semanal" else "Planificador de Periodo"

    // CÁLCULO DE MÉTRICAS EN TIEMPO REAL (O(N))
    val totalPoints = viewModel.selectedProducts.sumOf { it.first.points * it.second }.toDouble()
    val totalMoney = viewModel.selectedProducts.sumOf { it.first.price * it.second } // Inyección Financiera Total
    val isWeeklyGoalMet = totalPoints >= viewModel.targetGoal

    val canProceed = if (viewModel.isWeeklyMode) true else viewModel.selectedProducts.isNotEmpty()

    LaunchedEffect(viewModel.selectedProducts.toList()) {
        if (!viewModel.isWeeklyMode && periodId != 0) {
            orderRepository.savePeriodDraft(year, periodId, viewModel.selectedProducts.toList())
        }
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (viewModel.selectedProducts.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar todo", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (viewModel.selectedProducts.isNotEmpty() || viewModel.isWeeklyMode) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (canProceed) {
                                if (viewModel.isWeeklyMode) {
                                    if (viewModel.selectedProducts.isEmpty()) {
                                        if (periodId != 0 && weekId != 0) {
                                            orderRepository.clearOrder(year, periodId, weekId, clientId)
                                        }
                                        onBack()
                                    } else {
                                        if (periodId != 0 && weekId != 0) {
                                            orderRepository.saveOrder(year, periodId, weekId, clientId, viewModel.selectedProducts)
                                        }

                                        if (isWeeklyGoalMet) {
                                            showSuccessDialog = true
                                        } else {
                                            showSaveConfirmDialog = true
                                        }
                                    }
                                } else {
                                    viewModel.onPrincipalActionButtonClick()
                                }
                            }
                        },
                        containerColor = if (!canProceed && !viewModel.isWeeklyMode) Color.Gray
                        else if (viewModel.isWeeklyMode && !isWeeklyGoalMet && viewModel.selectedProducts.isNotEmpty()) ProgressOrange
                        else Color(0xFF4CAF50),
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        if (viewModel.isWeeklyMode) {
                            if (viewModel.selectedProducts.isEmpty()) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("CONFIRMAR")
                            } else if (isWeeklyGoalMet) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("LISTO")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("GUARDAR AVANCE")
                            }
                        } else {
                            if (canProceed) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("CALCULAR")
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("VACÍO")
                            }
                        }
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = { showCatalog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("AGREGAR PRODUCTO")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            GamifiedProgressHeader(
                currentPoints = totalPoints,
                currentMoney = totalMoney, // PROPAGACIÓN DE VARIABLE MONETARIA
                targetGoal = viewModel.targetGoal,
                isWeekly = viewModel.isWeeklyMode
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 220.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = viewModel.selectedProducts,
                    key = { (product, _) -> product.id }
                ) { (product, quantity) ->
                    SelectedProductItem(
                        product = product,
                        quantity = quantity,
                        onInc = { viewModel.incrementQuantity(product) },
                        onDec = { if (quantity > 1) viewModel.decrementQuantity(product) },
                        onRemove = { viewModel.removeProduct(product) }
                    )
                }
            }
        }
    }

    if (showCatalog) {
        val currentIds = remember(viewModel.selectedProducts.size) { viewModel.selectedProducts.map { it.first.id } }
        CatalogDialog(
            onDismiss = { showCatalog = false },
            onProductSelected = { viewModel.addProduct(it) },
            excludedIds = currentIds
        )
    }

    if (showSuccessDialog) {
        SuccessDialog(
            onDismiss = {
                showSuccessDialog = false
                onBack()
            }
        )
    }

    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = { Text("Avance Guardado") },
            text = { Text("Tus productos se han guardado. Puedes continuar editando más tarde.") },
            confirmButton = {
                Button(onClick = {
                    showSaveConfirmDialog = false
                    onBack()
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Borrar todo?") },
            text = { Text("Se eliminarán todos los productos seleccionados de esta semana.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCart()
                        if (!viewModel.isWeeklyMode && periodId != 0) {
                            orderRepository.clearPeriodDraft(year, periodId)
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (viewModel.isLoading) {
        CalculatingDialog()
    }
}

@Composable
fun ResultView(
    result: DistributionResult,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundWhite,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onSave,
                containerColor = FuxionGreen,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("GUARDAR DISTRIBUCIÓN", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
            ) {
                item { StrategicResultHeader(totalPoints = result.globalPoints.toDouble(), isPerfect = result.isPerfectFit) }

                val weeks = listOf(result.week1, result.week2, result.week3, result.week4)

                weeks.forEach { week ->
                    item {
                        TimelineWeekItem("Semana ${week.weekIndex}", week.totalPoints.toDouble(), isLast = week.weekIndex == 4) {
                            week.slots.forEach { slot ->
                                SubOrderHeader("${slot.clientId} (Meta: ${slot.targetPoints})", slot.achievedPoints.toDouble())
                                slot.items.forEach { ProductResultRow(it) }
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 16.dp, start = 8.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
        }
    }
}

@Composable
fun SubOrderHeader(title: String, achievedPoints: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            .background(Color(0xFFEEEEEE), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF616161), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
        }
        Text(text = "$achievedPoints pts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
    }
}

@Composable
fun SuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = FuxionGreen,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¡Completado!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Operación finalizada con éxito.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = FuxionGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONTINUAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProductResultRow(item: DistributedItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = Color(0xFFF5F5F5),
                modifier = Modifier.size(45.dp)
            ) {
                val context = LocalContext.current
                val imageResId = remember(item.product.imageRes) {
                    ProductCatalog.getXmlImageId(context, item.product.imageRes)
                }
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    modifier = Modifier.padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = item.product.presentation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val isRedundantTag = item.tag.contains("P1") || item.tag.contains("P2") || item.tag.contains("P3")
                if (item.tag.isNotEmpty() && !isRedundantTag) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (item.tag.contains("Extra")) Color(0xFFFFE0B2) else Color(0xFFC8E6C9),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = item.tag.replace(" ", "").trim(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.tag.contains("Extra")) Color(0xFFE65100) else Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "x${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${item.totalPoints} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SelectedProductItem(product: Product, quantity: Int, onInc: () -> Unit, onDec: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val imageResId = remember(product.imageRes) {
                ProductCatalog.getXmlImageId(context, product.imageRes)
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(0xFFF5F5F5),
                modifier = Modifier.size(60.dp)
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = product.name,
                    modifier = Modifier.padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 2
                )
                Text(
                    text = product.presentation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // RENDERIZADO UX/UI: Subtotales Dinámicos (Puntos y Dinero)
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // Cálculo de puntos totales por fila (Dinámico)
                    val rowPoints = product.points * quantity
                    val formattedPoints = if (rowPoints % 1.0 == 0.0) rowPoints.toInt().toString() else rowPoints.toString()

                    Text(
                        text = "$formattedPoints pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = String.format(Locale("es", "PE"), "S/ %,.2f", product.price * quantity),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.size(24.dp)
                ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), shape = MaterialTheme.shapes.medium)
                        .height(36.dp)
                        .width(100.dp)
                ) {
                    IconButton(onClick = onDec, modifier = Modifier.weight(1f)) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        text = "$quantity",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onInc, modifier = Modifier.weight(1f)) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

data class ProgressState(
    val target: Double,
    val progress: Double,
    val message: String,
    val color: Color
)

@Composable
fun GamifiedProgressHeader(
    currentPoints: Double,
    currentMoney: Double,
    targetGoal: Int,
    isWeekly: Boolean
) {
    val goal = targetGoal.toDouble()

    // IDENTIFICADOR DE ESTADO: Detecta si la pantalla es un nodo de Venta Libre
    val isWildcard = isWeekly && targetGoal == 0

    val state = if (isWeekly) {
        if (isWildcard) {
            // MODO COMODÍN: Progreso infinito y refuerzo positivo azul
            ProgressState(0.0, 1.0, "Venta Libre (Sin Límite) 🌟", FuxionBlue)
        } else if (currentPoints < goal) {
            val p = (currentPoints / goal).coerceIn(0.0, 1.0)
            val left = goal - currentPoints
            ProgressState(goal, p, "Faltan ${String.format("%.1f", left)} pts para tu meta", ProgressOrange)
        } else {
            ProgressState(goal, 1.0, "¡Meta Semanal Cumplida! 🎉", FuxionGreen)
        }
    } else {
        if (currentPoints < goal) {
            val p = (currentPoints / goal).coerceIn(0.0, 1.0)
            val left = goal - currentPoints
            ProgressState(goal, p, "Faltan ${String.format("%.1f", left)} pts para el PRO $targetGoal", ProgressOrange)
        } else {
            ProgressState(goal, 1.0, "¡IMPARABLE! Nivel PRO $targetGoal Alcanzado 🚀", FuxionGreen)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // MUTACIÓN DE TÍTULO SEGÚN ESTADO
            Text(
                text = if (isWildcard) "VENTA COMODÍN" else "TU PROGRESO",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$currentPoints pts",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            if (currentMoney > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(java.util.Locale("es", "PE"), "S/ %,.2f", currentMoney),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BARRA DE PROGRESO (Siempre llena y azul en Modo Comodín)
            LinearProgressIndicator(
                progress = state.progress.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = state.color,
                trackColor = Color(0xFFECEFF1),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (state.progress >= 1.0 && !isWildcard) Icons.Default.Star else Icons.Default.Info,
                    contentDescription = null,
                    tint = state.color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = state.color
                )
            }
        }
    }
}

@Composable
fun StrategicResultHeader(totalPoints: Double, isPerfect: Boolean) {
    val color = if (isPerfect) FuxionGreen else ProgressOrange
    val title = if (isPerfect) "Planificación Exacta" else "Inventario Inexacto"
    val subtitle = if (isPerfect) "Tus productos encajan perfectamente en la matriz PRO 500." else "Se aplicó llenado aproximado. Revisa las cantidades."

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isPerfect) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Total: $totalPoints pts",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineWeekItem(
    weekTitle: String,
    points: Double,
    isLast: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FuxionBlue, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = weekTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = FuxionBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$points pts",
                        color = FuxionBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun CalculatingDialog() {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = FuxionBlue,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analizando Estrategias...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Optimizando tus puntos para la meta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
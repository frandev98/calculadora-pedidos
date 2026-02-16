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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info // Asegúrate de tener este import
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.francisco.calculadorapedidos.data.ProductCatalog
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
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.ui.theme.BackgroundWhite
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.ProgressOrange
import com.francisco.calculadorapedidos.ui.theme.SurfaceWhite
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.francisco.calculadorapedidos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    targetGoal: Int = 540, // Recibimos la meta (540 o 645)
    viewModel: OrderViewModel = viewModel(),
    onBack: () -> Unit
) {
    // Sincronizamos la meta con el ViewModel al entrar
    LaunchedEffect(targetGoal) {
        viewModel.setGoal(targetGoal)
    }

    if (viewModel.distributionResult != null) {
        ResultView(
            result = viewModel.distributionResult!!,
            onBack = { viewModel.clearResult() }
        )
    } else {
        OrderInputView(viewModel, onBack)
    }
}

// --- VISTA 1: INGRESAR PEDIDOS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderInputView(viewModel: OrderViewModel, onBack: () -> Unit) {
    var showCatalog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calculate_period_order_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Botón Calcular
                if (viewModel.selectedProducts.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.calculateDistribution() },
                        containerColor = Color(0xFF4CAF50), // Verde Fuxion
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.calculate_button))
                    }
                }

                // Botón Agregar
                ExtendedFloatingActionButton(
                    onClick = { showCatalog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_product_button))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- HEADER GAMIFICADO ---
            // Calculamos el total actual (Aseguramos Double)
            val totalPoints = viewModel.selectedProducts.sumOf { it.first.points * it.second }.toDouble()

            GamifiedProgressHeader(
                currentPoints = totalPoints,
                targetGoal = viewModel.targetGoal
            )

            // Lista de Productos
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
                        onDec = {
                            if (quantity > 1) {
                                viewModel.decrementQuantity(product)
                            }
                        },
                        onRemove = { viewModel.removeProduct(product) }
                    )
                }
            }
        }
    }

    // Modal Catálogo
    if (showCatalog) {
        val currentIds = remember(viewModel.selectedProducts.size) {
            viewModel.selectedProducts.map { it.first.id }
        }

        CatalogDialog(
            onDismiss = { showCatalog = false },
            onProductSelected = { product ->
                viewModel.addProduct(product)
            },
            excludedIds = currentIds
        )
    }

    // Diálogo de Carga
    if (viewModel.isLoading) {
        CalculatingDialog()
    }
}

// --- VISTA 2: RESULTADOS ---
@Composable
fun ResultView(result: DistributionResult, onBack: () -> Unit) {
    val totalPoints = result.week1.sumOf { it.totalPoints } +
            result.week2.sumOf { it.totalPoints } +
            result.week3.sumOf { it.totalPoints } +
            (result.week4.subOrder1.sumOf { it.totalPoints } +
                    result.week4.subOrder2.sumOf { it.totalPoints } +
                    result.week4.subOrder3.sumOf { it.totalPoints })

    val week4Total = result.week4.subOrder1.sumOf { it.totalPoints } +
            result.week4.subOrder2.sumOf { it.totalPoints } +
            result.week4.subOrder3.sumOf { it.totalPoints }

    Scaffold(
        containerColor = BackgroundWhite
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // CAPA 1: La Lista
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                item { StrategicResultHeader(totalPoints = totalPoints) }

                item {
                    TimelineWeekItem(stringResource(R.string.week_1_title), result.week1.sumOf { it.totalPoints }) {
                        result.week1.forEach { ProductResultRow(it) }
                    }
                }

                item {
                    TimelineWeekItem(stringResource(R.string.week_2_title), result.week2.sumOf { it.totalPoints }) {
                        result.week2.forEach { ProductResultRow(it) }
                    }
                }

                item {
                    TimelineWeekItem(stringResource(R.string.week_3_title), result.week3.sumOf { it.totalPoints }) {
                        result.week3.forEach { ProductResultRow(it) }
                    }
                }

                item {
                    TimelineWeekItem(stringResource(R.string.week_4_title), week4Total, isLast = true) {
                        if (result.week4.subOrder1.isNotEmpty()) {
                            SubOrderHeader(stringResource(R.string.suborder_1_title), result.week4.subOrder1)
                            result.week4.subOrder1.forEach { ProductResultRow(it) }
                        }
                        if (result.week4.subOrder2.isNotEmpty()) {
                            SubOrderHeader(stringResource(R.string.suborder_2_title), result.week4.subOrder2)
                            result.week4.subOrder2.forEach { ProductResultRow(it) }
                        }
                        if (result.week4.subOrder3.isNotEmpty()) {
                            SubOrderHeader(stringResource(R.string.suborder_3_title), result.week4.subOrder3)
                            result.week4.subOrder3.forEach { ProductResultRow(it) }
                        }
                    }
                }
            }

            // CAPA 2: El Botón Flotante de Volver
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back_content_description),
                    tint = Color.White
                )
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun SubOrderHeader(title: String, items: List<DistributedItem>) {
    val total = items.sumOf { it.totalPoints }
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
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color(0xFF616161),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }
        Text(
            text = "$total pts",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242)
        )
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
                Text(
                    text = "${product.points} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
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
                            contentDescription = stringResource(R.string.remove_content_description),
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
    targetGoal: Int
) {
    val goal1 = 540.0
    val goal2 = targetGoal.toDouble()

    // Lógica de cálculo de estado
    val state = when {
        currentPoints < goal1 -> {
            val p = (currentPoints / goal1).coerceIn(0.0, 1.0)
            val left = goal1 - currentPoints
            ProgressState(goal1, p, stringResource(R.string.gamified_progress_missing_fmt, String.format("%.1f", left)), ProgressOrange)
        }
        currentPoints < goal2 -> {
            val p = ((currentPoints - goal1) / (goal2 - goal1)).coerceIn(0.0, 1.0)
            val left = goal2 - currentPoints
            ProgressState(goal2, p, stringResource(R.string.gamified_progress_near_pro_fmt, String.format("%.1f", left)), FuxionBlue)
        }
        else -> {
            ProgressState(goal2, 1.0, stringResource(R.string.gamified_progress_max_reached), FuxionGreen)
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
            Text(
                text = stringResource(R.string.your_progress_label),
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
            Spacer(modifier = Modifier.height(12.dp))

            // CORRECCIÓN: 'progress' es un valor Float, no una lambda
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
                    imageVector = if (currentPoints >= goal2) Icons.Default.Star else Icons.Default.Info,
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
fun StrategicResultHeader(totalPoints: Double) {
    val isPro = totalPoints >= 645
    val color = if (isPro) FuxionGreen else FuxionBlue
    val title = if (isPro) stringResource(R.string.strategy_pro_level) else stringResource(R.string.strategy_base_goal)

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
                imageVector = Icons.Default.CheckCircle,
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
                text = stringResource(R.string.total_accumulated_fmt, totalPoints),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
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
    // CORRECCIÓN: IntrinsicSize.Min asegura que la columna de la línea
    // tenga la misma altura que el contenido.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        // --- COLUMNA IZQUIERDA: La Línea de Tiempo ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FuxionBlue, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            if (!isLast) {
                // Ahora 'weight(1f)' funciona porque el padre tiene altura definida
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- COLUMNA DERECHA: El Contenido ---
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
                    text = stringResource(R.string.analyzing_strategies_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.optimizing_points_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
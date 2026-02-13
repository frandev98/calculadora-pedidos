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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import com.francisco.calculadorapedidos.data.ProductCatalog
import com.francisco.calculadorapedidos.ui.theme.BackgroundWhite
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.ProgressOrange
import com.francisco.calculadorapedidos.ui.theme.SurfaceWhite
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(viewModel: OrderViewModel = viewModel()) {

    if (viewModel.distributionResult != null) {
        ResultView(
            result = viewModel.distributionResult!!,
            onBack = { viewModel.clearResult() }
        )
    } else {
        OrderInputView(viewModel)
    }
}

// --- VISTA 1: INGRESAR PEDIDOS ---
@Composable
fun OrderInputView(viewModel: OrderViewModel) {
    var showCatalog by remember { mutableStateOf(false) }

    Scaffold(
        // ... dentro del Scaffold ...
        containerColor = BackgroundWhite,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Botón Calcular (Se mantiene igual, solo asegúrate de darle espacio abajo)
                if (viewModel.selectedProducts.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.calculateDistribution() },
                        containerColor = Color(0xFF4CAF50), // Verde Fuxion
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("CALCULAR")
                    }
                }

                // NUEVO BOTÓN "AGREGAR" MEJORADO
                ExtendedFloatingActionButton(
                    onClick = { showCatalog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("AGREGAR PRODUCTO") // Texto claro y directo
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- 2. AQUÍ VA EL NUEVO HEADER GAMIFICADO ---
            // Calculamos el total actual
            val totalPoints = viewModel.selectedProducts.sumOf { it.first.points * it.second }

            GamifiedProgressHeader(currentPoints = totalPoints)
            // ---------------------------------------------

            // Lista de Productos (LazyColumn)
            LazyColumn(
                contentPadding = PaddingValues(bottom = 220.dp),
                modifier = Modifier.weight(1f) // Ocupa el resto del espacio
            ) {
                // ... tu lista de items con key ...
                items(
                    items = viewModel.selectedProducts,
                    key = { (product, _) -> product.id }
                ) { (product, quantity) ->
                    SelectedProductItem(
                        product = product,
                        quantity = quantity,
                        onInc = { viewModel.incrementQuantity(product) },
                        onDec = {
                            // Freno de seguridad: Solo restamos si hay más de 1.
                            // Si está en 1 y pulsan menos, no hacemos NADA.
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

    /// Modal Catálogo NUEVO Y MEJORADO
    if (showCatalog) {
        // 1. Calculamos los IDs de los productos que ya tenemos en el carrito
        // Usamos 'remember' para que no lo recalcule a cada rato innecesariamente
        val currentIds = remember(viewModel.selectedProducts) {
            viewModel.selectedProducts.map { it.first.id }
        }

        CatalogDialog(
            onDismiss = { showCatalog = false },
            onProductSelected = { product ->
                viewModel.addProduct(product)
                // showCatalog = false // Opcional: Si quieres que se cierre al elegir
            },
            excludedIds = currentIds // <--- 2. LE PASAMOS LA LISTA NEGRA
        )
    }
    if (viewModel.isLoading) {
        CalculatingDialog()
    }
}

// --- VISTA 2: RESULTADOS (Igual que antes) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultView(result: DistributionResult, onBack: () -> Unit) {
    // Calculamos totales
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
        // 1. ELIMINAMOS 'topBar' DE AQUÍ para que no cree la franja blanca
    ) { padding ->
        // 2. Usamos un BOX para poder apilar cosas (Lista abajo, Botón arriba)
        Box(modifier = Modifier.fillMaxSize()) {

            // CAPA 1: La Lista (El contenido)
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    // IMPORTANTE: Solo aplicamos padding abajo para no tapar con la barra de navegación,
                    // pero ignoramos el de arriba para que el Header Azul toque el techo.
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // Header Azul
                item { StrategicResultHeader(totalPoints = totalPoints) }

                // Semana 1
                item {
                    TimelineWeekItem("SEMANA 1 (Inicio)", result.week1.sumOf { it.totalPoints }) {
                        result.week1.forEach { ProductResultRow(it) }
                    }
                }

                // Semana 2
                item {
                    TimelineWeekItem("SEMANA 2 (Consistencia)", result.week2.sumOf { it.totalPoints }) {
                        result.week2.forEach { ProductResultRow(it) }
                    }
                }

                // Semana 3
                item {
                    TimelineWeekItem("SEMANA 3 (Avance)", result.week3.sumOf { it.totalPoints }) {
                        result.week3.forEach { ProductResultRow(it) }
                    }
                }

                // Semana 4
                item {
                    TimelineWeekItem("SEMANA 4 (Cierre Maestro)", week4Total, isLast = true) {
                        if (result.week4.subOrder1.isNotEmpty()) {
                            SubOrderHeader("Pedido 1", result.week4.subOrder1)
                            result.week4.subOrder1.forEach { ProductResultRow(it) }
                        }
                        if (result.week4.subOrder2.isNotEmpty()) {
                            SubOrderHeader("Pedido 2", result.week4.subOrder2)
                            result.week4.subOrder2.forEach { ProductResultRow(it) }
                        }
                        if (result.week4.subOrder3.isNotEmpty()) {
                            SubOrderHeader("Pedido 3", result.week4.subOrder3)
                            result.week4.subOrder3.forEach { ProductResultRow(it) }
                        }
                    }
                }
            }

            // CAPA 2: El Botón Flotante (Encima de todo)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 8.dp) // Margen para separarlo del borde
                    .align(Alignment.TopStart) // Pegado arriba a la izquierda
            ) {
                // Ahora sí se verá porque estará sobre el Azul
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }
    }
}

// Componentes Auxiliares
@Composable
fun WeekHeader(title: String, items: List<DistributedItem>) {
    val total = items.sumOf { it.totalPoints }
    // Usamos un diseño de "Banner" más sólido
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // Azul muy suave
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp) // Más espacio vertical
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono de calendario pequeño para decorar
                Icon(
                    imageVector = Icons.Default.DateRange, // Asegúrate de importar Icons.Default.DateRange
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }

            // Total destacado
            Surface(
                color = Color(0xFF1565C0),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = "$total pts",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun SubOrderHeader(title: String, items: List<DistributedItem>) {
    val total = items.sumOf { it.totalPoints }

    // Diseño tipo "Etiqueta" o "Banner pequeño"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            .background(Color(0xFFEEEEEE), shape = MaterialTheme.shapes.small) // Fondo gris claro
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Un icono de "Caja" o "Paquete" ayuda visualmente
            Icon(
                imageVector = Icons.Default.ShoppingCart, // O usa ShoppingBag si lo tienes
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

        // El total del sub-pedido resaltado
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
        elevation = CardDefaults.cardElevation(1.dp) // Elevación sutil
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. IMAGEN PEQUEÑA (NUEVO)
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = Color(0xFFF5F5F5),
                modifier = Modifier.size(45.dp) // Tamaño compacto para resultados
            ) {
                Image(
                    painter = painterResource(id = item.product.imageRes),
                    contentDescription = null,
                    modifier = Modifier.padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. DATOS CENTRALES
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )

                Text(
                    text = item.product.presentation,
                    style = MaterialTheme.typography.bodySmall, // Letra pequeña
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Pone "..." si es muy largo
                )

                // 3. EL BADGE/ETIQUETA (LÓGICA MEJORADA)
                // Solo mostramos el badge si NO es P1, P2 o P3 (porque ya tenemos encabezados para eso)
                // CORRECCIÓN: Usamos 'contains' para que detecte "[P1]", "P1", "[ P1 ]", etc.
                val isRedundantTag = item.tag.contains("P1") || item.tag.contains("P2") || item.tag.contains("P3")

                if (item.tag.isNotEmpty() && !isRedundantTag) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        // Si es "Extra" u otra cosa, mantenemos el color
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

            // 4. CANTIDAD Y PUNTOS (A la derecha)
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
                .padding(12.dp), // Padding general equilibrado
            verticalAlignment = Alignment.CenterVertically // Centrado vertical general
        ) {
            // --- SECCIÓN IZQUIERDA: IMAGEN ---
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(0xFFF5F5F5),
                modifier = Modifier.size(60.dp)
            ) {
                Image(
                    painter = painterResource(id = product.imageRes),
                    contentDescription = product.name,
                    modifier = Modifier.padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // --- SECCIÓN CENTRAL: INFORMACIÓN ---
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
                // Recuperamos los puntos aquí para que no queden "huérfanos" abajo
                Text(
                    text = "${product.points} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // --- SECCIÓN DERECHA: ZONA DE CONTROL (X + Contador) ---
            Column(
                horizontalAlignment = Alignment.End, // Todo pegado a la derecha
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. La X (Arriba a la derecha)
                // Usamos un Box para darle un área de toque generosa sin ocupar mucho espacio visual
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.size(24.dp) // Altura reservada para la X
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp) // Botón pequeño
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFBDBDBD), // Gris suave
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp)) // Separación vertical de seguridad

                // 2. El Contador Grande (Abajo a la derecha)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), shape = MaterialTheme.shapes.medium)
                        .height(36.dp) // Altura cómoda
                        .width(100.dp) // Ancho fijo para que no baile
                ) {
                    IconButton(onClick = onDec, modifier = Modifier.weight(1f)) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        text = "$quantity",
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

@Composable
fun GamifiedProgressHeader(currentPoints: Double) {
    val goal1 = 540.0
    val goal2 = 645.0

    // 1. Creamos una estructura temporal para guardar los 4 datos (CORRECCIÓN)
    data class ProgressState(
        val target: Double,
        val progress: Double,
        val message: String,
        val color: Color
    )

    // 2. Calculamos el estado usando nuestra nueva estructura
    val state = when {
        currentPoints < goal1 -> {
            val p = (currentPoints / goal1).coerceIn(0.0, 1.0)
            val left = goal1 - currentPoints
            ProgressState(goal1, p, "Faltan ${String.format("%.1f", left)} pts para el 40%", ProgressOrange)
        }
        currentPoints < goal2 -> {
            val p = ((currentPoints - goal1) / (goal2 - goal1)).coerceIn(0.0, 1.0)
            val left = goal2 - currentPoints
            ProgressState(goal2, p, "¡Bien! A ${String.format("%.1f", left)} pts del Nivel PRO", FuxionBlue)
        }
        else -> {
            ProgressState(goal2, 1.0, "¡IMPARABLE! Meta Máxima Alcanzada 🚀", FuxionGreen)
        }
    }

    // 3. Diseño Visual (Usamos 'state' para acceder a los datos)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TU PROGRESO",
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

            // Barra de Progreso
            LinearProgressIndicator(
                progress = { state.progress.toFloat() }, // Usamos state.progress
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = state.color, // Usamos state.color
                trackColor = Color(0xFFECEFF1),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mensaje Motivacional
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (currentPoints >= goal2) Icons.Default.Star else Icons.Default.Info,
                    contentDescription = null,
                    tint = state.color, // Usamos state.color
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.message, // Usamos state.message
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
    val isPro = totalPoints >= 645 // Meta Grande
    val color = if (isPro) FuxionGreen else FuxionBlue
    val title = if (isPro) "¡ESTRATEGIA NIVEL PRO!" else "¡OBJETIVO 40% LISTO!"

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), // Curva suave abajo
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle, // Importar CheckCircle
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
                text = "Total Acumulado: $totalPoints pts",
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
    isLast: Boolean = false, // Para saber si dibujamos la línea hacia abajo o no
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // --- COLUMNA IZQUIERDA: La Línea de Tiempo ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // El Punto (Hito)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FuxionBlue, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            // La Línea Vertical
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f) // Ocupa todo el alto necesario
                        .background(Color(0xFFE0E0E0)) // Gris suave
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- COLUMNA DERECHA: El Contenido (Tarjeta de la Semana) ---
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Título de la Semana (Hito)
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
                // Badge de Puntos
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

            // Aquí se inyecta la lista de productos
            content()
        }
    }
}

@Composable
fun CalculatingDialog() {
    Dialog(
        onDismissRequest = { /* No permitir cerrar mientras calcula */ },
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
                // Un indicador giratorio con tu color corporativo
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
                    text = "Optimizando tus puntos para el 40%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
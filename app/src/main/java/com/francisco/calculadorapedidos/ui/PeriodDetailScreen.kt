package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.ui.theme.BackgroundWhite
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.ProgressOrange
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    periodId: Int,
    dataStore: FuxionDataStore,
    onBack: () -> Unit,
    onWeekClick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository(context) }

    var refreshTrigger by remember { mutableStateOf(0) }
    var showGoalDialog by remember { mutableStateOf(false) } // Controla el popup de cambio

    val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = null)

    if (anchorDate == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val anchor = Date(anchorDate!!)
    val periodDates = remember(periodId, anchor) { FuxionCalendarLogic.getPeriodDates(anchor, periodId) }
    val currentStatus = remember(anchor) { FuxionCalendarLogic.calculateStatus(anchor) }
    val isCurrentPeriod = currentStatus.period == periodId

    // Cargar la meta (Por defecto vendrá la del Excel gracias al cambio anterior)
    var targetGoal by remember {
        mutableIntStateOf(orderRepository.getPeriodGoal(periodId))
    }

    val dateFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))
    val fullDateFormat = SimpleDateFormat("dd 'de' MMM", Locale("es", "ES"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Periodo $periodId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // FECHAS
            Text(
                text = "${fullDateFormat.format(periodDates.first)} - ${fullDateFormat.format(periodDates.second)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- NUEVO DISEÑO: TARJETA DE OBJETIVO ACTUAL ---
            // En lugar de preguntar, AFIRMAMOS cuál es el objetivo.
            Text("Objetivo Estratégico", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), // Un poco más redondeado
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), // Buen margen interno
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // <--- CLAVE: Empuja extremos
                ) {
                    // SECCIÓN IZQUIERDA: Icono + Textos
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icono circular
                        Surface(
                            color = if(targetGoal == 645) FuxionGreen else Color(0xFF673AB7),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp) // Un pelín más grande para presencia
                        ) {
                            Icon(
                                imageVector = if(targetGoal == 645) Icons.Default.Star else Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        // Textos (Titulo y Subtitulo)
                        Column {
                            Text(
                                // CAMBIO AQUÍ: Nombres actualizados
                                text = if(targetGoal == 645) "PRO 1 Plus" else "PRO 1 Sólido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "$targetGoal puntos", // Mantenemos los puntos visibles
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // SECCIÓN DERECHA: Botón de Acción
                    TextButton(
                        onClick = { showGoalDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            "CAMBIAR",
                            fontWeight = FontWeight.Bold,
                            color = FuxionBlue,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = FuxionBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Planificación Semanal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            // --- LISTA DE SEMANAS ---
            for (weekIndex in 1..4) {
                val weekStartDate = FuxionCalendarLogic.addDays(periodDates.first, (weekIndex - 1) * 7)
                val weekEndDate = FuxionCalendarLogic.addDays(weekStartDate, 6)
                val isThisWeekActive = isCurrentPeriod && (currentStatus.week == weekIndex)

                // Calculamos lo que pide la UI (solo visual)
                val specificTarget = if (targetGoal == 645) { if (weekIndex == 4) 105 else 180 } else { if (weekIndex == 4) 180 else 120 }

                val savedPoints = remember(periodId, weekIndex, targetGoal, refreshTrigger) {
                    orderRepository.getWeekTotalPoints(periodId, weekIndex)
                }

                val hasOrder = savedPoints > 0
                val isGoalMet = savedPoints >= specificTarget

                WeekCard(
                    weekNumber = weekIndex,
                    dateRange = "${dateFormat.format(weekStartDate)} - ${dateFormat.format(weekEndDate)}",
                    target = specificTarget,
                    savedPoints = savedPoints,
                    isCurrent = isThisWeekActive,
                    hasOrder = hasOrder,
                    isGoalMet = isGoalMet,
                    onClick = {
                        onWeekClick(weekIndex, targetGoal)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // --- DIÁLOGO DE SELECCIÓN DE META ---
    if (showGoalDialog) {
        Dialog(onDismissRequest = { showGoalDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Ajustar Objetivo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Cambiar el objetivo recalculará la estrategia de puntos para tus clientes.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                    Spacer(Modifier.height(16.dp))

                    GoalOptionCard(
                        title = "PRO 1 Sólido", // Antes BASE
                        points = "540 pts",
                        icon = Icons.Default.Check,
                        isSelected = targetGoal == 540,
                        primaryColor = Color(0xFF673AB7), selectedBackgroundColor = Color(0xFFEDE7F6),
                        onClick = {
                            targetGoal = 540
                            orderRepository.savePeriodGoal(periodId, 540)
                            showGoalDialog = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    GoalOptionCard(
                        title = "PRO 1 Plus", // Antes PRO 1
                        points = "645 pts",
                        icon = Icons.Default.Star,
                        isSelected = targetGoal == 645,
                        primaryColor = FuxionGreen, selectedBackgroundColor = Color(0xFFE8F5E9),
                        onClick = {
                            targetGoal = 645
                            orderRepository.savePeriodGoal(periodId, 645)
                            showGoalDialog = false
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = { showGoalDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("CANCELAR", color = TextSecondary)
                    }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun WeekCard(
    weekNumber: Int,
    dateRange: String,
    target: Int,
    savedPoints: Int,
    isCurrent: Boolean,
    hasOrder: Boolean,
    isGoalMet: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        hasOrder && isGoalMet -> FuxionGreen
        hasOrder && !isGoalMet -> ProgressOrange
        isCurrent -> FuxionBlue
        else -> Color.Transparent
    }

    val containerColor = when {
        hasOrder && isGoalMet -> Color(0xFFE8F5E9)
        hasOrder && !isGoalMet -> Color(0xFFFFF3E0)
        isCurrent -> Color(0xFFE3F2FD)
        else -> Color(0xFFF5F5F5)
    }

    val borderStroke = if (hasOrder || isCurrent) BorderStroke(2.dp, borderColor) else null

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(if (isCurrent || hasOrder) 2.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Semana $weekNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            hasOrder && !isGoalMet -> Color(0xFFE65100)
                            hasOrder && isGoalMet -> FuxionGreen
                            isCurrent -> FuxionBlue
                            else -> TextPrimary
                        }
                    )

                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = FuxionBlue, shape = RoundedCornerShape(4.dp)) {
                            Text("ACTUAL", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (hasOrder) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (isGoalMet) Icons.Default.CheckCircle else Icons.Default.Info,
                            null,
                            tint = if (isGoalMet) FuxionGreen else ProgressOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(text = dateRange, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Surface(
                color = when {
                    hasOrder && isGoalMet -> FuxionGreen
                    hasOrder && !isGoalMet -> ProgressOrange
                    isCurrent -> Color.White
                    else -> Color(0xFFE0E0E0)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                val text = if (hasOrder) "$savedPoints / $target pts" else "Meta: $target pts"
                val textColor = if (hasOrder) Color.White else if(isCurrent) FuxionBlue else Color.Gray

                Text(text = text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
    }
}

@Composable
fun GoalOptionCard(
    title: String,
    points: String,
    icon: ImageVector,
    isSelected: Boolean,
    primaryColor: Color,
    selectedBackgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) selectedBackgroundColor else Color(0xFFF5F5F5)
    val borderColor = if (isSelected) primaryColor else Color.Transparent
    val contentAlpha = if (isSelected) 1f else 0.6f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isSelected) primaryColor else Color.Gray.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) primaryColor else TextSecondary, modifier = Modifier.alpha(contentAlpha))
                Text(text = points, style = MaterialTheme.typography.bodySmall, color = if (isSelected) primaryColor else TextSecondary, modifier = Modifier.alpha(contentAlpha))
            }
            Spacer(Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = primaryColor)
            }
        }
    }
}
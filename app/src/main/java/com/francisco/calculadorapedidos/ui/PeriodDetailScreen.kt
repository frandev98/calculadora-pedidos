package com.francisco.calculadorapedidos.ui

import com.francisco.calculadorapedidos.data.FuxionDataStore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    year: Int,
    periodId: Int,
    dataStore: FuxionDataStore,
    onBack: () -> Unit,
    onNavigateToWeek: (Int) -> Unit,
    onNavigateToFullPlan: () -> Unit,
    // INYECCIÓN DE DEPENDENCIA DEL MOTOR DE ESTADO
    viewModel: PeriodViewModel = viewModel()
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository(context) }
    val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = null)
    val userStartPeriod by dataStore.userStartPeriodFlow.collectAsState(initial = null)

    // SUSCRIPCIÓN REACTIVA AL FLUJO DE ESTADO MATEMÁTICO
    val uiState by viewModel.uiState.collectAsState()

    if (anchorDate == null || userStartPeriod == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val anchor = Date(anchorDate!!)
    val periodDates = remember(periodId, anchor) { FuxionCalendarLogic.getPeriodDates(anchor, periodId) }
    val currentStatus = remember(anchor) { FuxionCalendarLogic.calculateStatus(anchor) }
    val isCurrentPeriod = currentStatus.period == periodId

    // GATILLO DE EJECUCIÓN UNIDIRECCIONAL (Cero lógica en UI)
    LaunchedEffect(year, periodId) {
        viewModel.loadPeriodData(year, periodId, orderRepository)
    }

    val targetGoal = 500
    val dateFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))
    val fullDateFormat = SimpleDateFormat("dd 'de' MMM", Locale("es", "ES"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Periodo $periodId ($year)") },
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

        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundWhite).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("${fullDateFormat.format(periodDates.first)} - ${fullDateFormat.format(periodDates.second)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Rentabilidad del Periodo", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if(uiState.totalPoints >= targetGoal) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)),
                border = BorderStroke(1.dp, if(uiState.totalPoints >= targetGoal) FuxionGreen else Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = if(uiState.totalPoints >= targetGoal) FuxionGreen else TextSecondary, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Nivel PRO 500", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Progreso: ${uiState.totalPoints} / $targetGoal pts", style = MaterialTheme.typography.bodyMedium, color = if(uiState.totalPoints >= targetGoal) FuxionGreen else TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Venta Directa Estimada:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(String.format(Locale("es", "PE"), "S/ %,.2f", uiState.directSalesBonus), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bono PRO 1:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(String.format(Locale("es", "PE"), "S/ %,.2f", uiState.pro1Bonus), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = if (uiState.pro1Bonus > 0) FuxionGreen else TextSecondary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("GANANCIA NETA:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = FuxionBlue)
                        Text(String.format(Locale("es", "PE"), "S/ %,.2f", uiState.directSalesBonus + uiState.pro1Bonus), style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.ExtraBold, color = FuxionBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Planificación Semanal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            for (weekIndex in 1..4) {
                val weekStartDate = FuxionCalendarLogic.addDays(periodDates.first, (weekIndex - 1) * 7)
                val weekEndDate = FuxionCalendarLogic.addDays(weekStartDate, 6)
                val isThisWeekActive = isCurrentPeriod && (currentStatus.week == weekIndex)

                val weekSlots = FuxionCalendarLogic.getSlotsForWeek(periodId, weekIndex, userStartPeriod!!)
                val specificTarget = if (weekSlots.size > 1) 125 else weekSlots.firstOrNull()?.targetPoints ?: 125

                val metrics = uiState.weeklyMetrics[weekIndex] ?: com.francisco.calculadorapedidos.data.OrderMetrics(0, 0.0)
                val hasOrder = metrics.points > 0
                val isGoalMet = metrics.points >= specificTarget

                WeekCard(
                    weekNumber = weekIndex,
                    dateRange = "${dateFormat.format(weekStartDate)} - ${dateFormat.format(weekEndDate)}",
                    target = specificTarget,
                    savedPoints = metrics.points,
                    savedMoney = metrics.money,
                    isCurrent = isThisWeekActive,
                    hasOrder = hasOrder,
                    isGoalMet = isGoalMet,
                    onClick = { onNavigateToWeek(weekIndex) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onNavigateToFullPlan() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FuxionBlue)
            ) {
                Icon(Icons.Default.AutoGraph, null, tint = FuxionBlue)
                Spacer(Modifier.width(8.dp))
                Text("Planificar Periodo Completo", color = FuxionBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun WeekCard(
    weekNumber: Int,
    dateRange: String,
    target: Int,
    savedPoints: Int,
    savedMoney: Double,
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                }
                Spacer(Modifier.height(4.dp))
                Text(text = dateRange, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
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

                if (hasOrder && savedMoney > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale("es", "PE"), "S/ %,.2f", savedMoney),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
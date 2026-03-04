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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearOverviewScreen(
    dataStore: FuxionDataStore,
    onPeriodClick: (Int, Int) -> Unit, // FIRMA MUTADA: Requiere (Año, Periodo)
    onSettingsClick: () -> Unit,
    onClientsClick: () -> Unit
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository(context) }

    // ESTADO COMPUESTO: Llave es Pair(Año, Periodo)
    var periodPointsMap by remember { mutableStateOf<Map<Pair<Int, Int>, Int>>(emptyMap()) }

    val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = null)

    // TODO: Estos valores deben provenir de FuxionDataStore en el futuro.
    // Por ahora, se asume el año y periodo actual como el punto de inicio (Fallback).
    val fallbackYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var userStartPeriod by remember { mutableIntStateOf(1) }
    var userStartYear by remember { mutableIntStateOf(fallbackYear) }

    if (anchorDate == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val anchor = Date(anchorDate!!)
    val status = remember(anchorDate) { FuxionCalendarLogic.calculateStatus(anchor) }

    // Inyección de estado inicial (Simulación de Onboarding dinámico)
    LaunchedEffect(status) {
        // En producción, esto se lee del DataStore. Si es la primera vez, se graba el status actual.
        userStartPeriod = status.period
        userStartYear = fallbackYear
    }

    // ARITMÉTICA DE VENTANA DESLIZANTE (Rolling Time Window)
    val rollingWindow = remember(userStartPeriod, userStartYear) {
        val window = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until 13) {
            val displayPeriod = ((userStartPeriod - 1 + i) % 13) + 1
            val displayYear = if (displayPeriod < userStartPeriod) userStartYear + 1 else userStartYear
            window.add(Pair(displayYear, displayPeriod))
        }
        window
    }

    // HIDRATACIÓN DE ESTADO 4D
    LaunchedEffect(rollingWindow) {
        val newPointsMap = mutableMapOf<Pair<Int, Int>, Int>()
        for ((year, period) in rollingWindow) {
            newPointsMap[Pair(year, period)] = orderRepository.getPeriodTotalPoints(year, period)
        }
        periodPointsMap = newPointsMap
    }

    val today = Date()
    val todayFormat = SimpleDateFormat("EEEE, d 'DE' MMMM", Locale("es", "ES"))
    val rangeFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Plan Fuxion") },
                actions = {
                    IconButton(onClick = onClientsClick) {
                        Icon(Icons.Default.People, contentDescription = "Mis Socios", tint = Color.White)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FuxionGreen,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundWhite)
        ) {
            item {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = FuxionBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Today, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(todayFormat.format(today).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PERIODO", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("${status.period}", color = Color.White, style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp), fontWeight = FontWeight.Bold, lineHeight = 72.sp)
                            }
                            Spacer(modifier = Modifier.width(32.dp))
                            Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.3f)))
                            Spacer(modifier = Modifier.width(32.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("SEMANA ACTUAL", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("Semana ${status.week}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${rangeFormat.format(status.weekStartDate)} - ${rangeFormat.format(status.weekEndDate)}", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = if (status.daysRemainingInWeek <= 2) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, null, tint = if (status.daysRemainingInWeek <= 2) Color(0xFFE65100) else Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))

                                        val daysText = when (status.daysRemainingInWeek) {
                                            0 -> "¡Cierra hoy!"
                                            1 -> "1 día restante"
                                            else -> "${status.daysRemainingInWeek} días restantes"
                                        }

                                        Text(daysText, style = MaterialTheme.typography.labelSmall, color = if (status.daysRemainingInWeek <= 2) Color(0xFFE65100) else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            item {
                Text("Mi Ciclo PRO 500", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            // ITERACIÓN SOBRE MATRIZ DESLIZANTE
            items(rollingWindow) { (year, periodNum) ->
                // NOTA: Para obtener fechas exactas futuras/pasadas, getPeriodDates debe ser refactorizado
                // para aceptar el año, actualmente solo operará con el anchor original.
                val (start, end) = FuxionCalendarLogic.getPeriodDates(anchor, periodNum)

                // Determinación de estado actual cruzando Año y Periodo
                val isCurrent = (periodNum == status.period && year == fallbackYear)

                val totalPoints = periodPointsMap[Pair(year, periodNum)] ?: 0
                val targetGoal = 500

                PeriodCard(
                    periodNumber = periodNum,
                    yearNumber = year,
                    dateRange = "${rangeFormat.format(start)} - ${rangeFormat.format(end)}",
                    isCurrent = isCurrent,
                    currentPoints = totalPoints,
                    goalPoints = targetGoal,
                    onClick = { onPeriodClick(year, periodNum) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun PeriodCard(
    periodNumber: Int,
    yearNumber: Int,
    dateRange: String,
    isCurrent: Boolean,
    currentPoints: Int,
    goalPoints: Int,
    onClick: () -> Unit
) {
    val isCompleted = currentPoints >= goalPoints && currentPoints > 0
    val progress = (currentPoints.toFloat() / goalPoints.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(if (isCurrent) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFE8F5E9) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrent) BorderStroke(2.dp, FuxionGreen) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = if (isCurrent) FuxionGreen else TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Periodo $periodNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    // INDICADOR DE SALTO INTERANUAL
                    Text(
                        "($yearNumber)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Text("ACTUAL", style = MaterialTheme.typography.labelSmall, color = FuxionGreen, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(dateRange, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                if (currentPoints > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isCompleted) FuxionGreen else FuxionBlue,
                            trackColor = Color(0xFFEEEEEE)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$currentPoints / $goalPoints pts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) FuxionGreen else TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))
            if (isCompleted) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Completado",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}
package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearOverviewScreen(
    dataStore: FuxionDataStore,
    onPeriodClick: (Int) -> Unit // Navigates to PeriodDetail
) {
    val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = null)

    if (anchorDate == null) {
        // Loading or error state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val anchor = Date(anchorDate!!)
        val periods = FuxionCalendarLogic.getFullYearPlan(anchor)
        val currentStatus = FuxionCalendarLogic.calculateStatus(anchor)

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Mi Año Fuxion") })
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Status
                item {
                    StatusCard(currentStatus)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Mis Periodos", style = MaterialTheme.typography.titleMedium)
                }

                items(periods) { period ->
                    PeriodItem(
                        period = period,
                        isCurrent = period.number == currentStatus.period,
                        isPast = period.number < currentStatus.period,
                        onClick = { onPeriodClick(period.number) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(status: FuxionCalendarLogic.FuxionStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Estás en:", style = MaterialTheme.typography.labelMedium)
            Text(
                "Periodo ${status.period} - Semana ${status.week}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Cierre de semana en ${status.daysRemainingInWeek} días",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PeriodItem(
    period: FuxionCalendarLogic.PeriodInfo,
    isCurrent: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.secondaryContainer
        isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = if (isCurrent) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Periodo ${period.number}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${dateFormat.format(period.start)} - ${dateFormat.format(period.end)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isCurrent) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("ACTUAL") }
            } else if (isPast) {
                 Icon(
                     imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                     contentDescription = "Pasado",
                     tint = Color.Gray
                 )
            }
        }
    }
}

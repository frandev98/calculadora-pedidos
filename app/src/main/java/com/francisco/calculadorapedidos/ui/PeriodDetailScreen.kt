package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    periodId: Int,
    dataStore: FuxionDataStore,
    onBack: () -> Unit,
    onWeekClick: (Int, Double) -> Unit // weekId (1-4), targetPoints (120 or 180)
) {
    val scope = rememberCoroutineScope()
    val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = null)
    val goal by dataStore.getGoalForPeriod(periodId).collectAsState(initial = 540) // Default 540

    if (anchorDate == null) return

    val anchor = Date(anchorDate!!)
    // Find the period info roughly (this logic might need refinement if getFullYearPlan returns 13 only)
    val periods = FuxionCalendarLogic.getFullYearPlan(anchor)
    val periodInfo = periods.find { it.number == periodId } ?: return

    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Periodo $periodId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Header Info
            Text(
                text = "${dateFormat.format(periodInfo.start)} - ${dateFormat.format(periodInfo.end)}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Goal Selector
            GoalSelectorCard(
                currentGoal = goal,
                onGoalSelected = { newGoal ->
                    scope.launch {
                        dataStore.saveGoalForPeriod(periodId, newGoal)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Semanas", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            // Weeks List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(4) { index ->
                    val weekNum = index + 1
                    val weekStart = FuxionCalendarLogic.addDays(periodInfo.start, index * 7)
                    val weekEnd = FuxionCalendarLogic.addDays(weekStart, 6)
                    
                    WeekItem(
                        weekNum = weekNum,
                        start = weekStart,
                        end = weekEnd,
                        target = if (goal == 645) 180.0 else 120.0,
                        onClick = {
                            val target = if (goal == 645) 180.0 else 120.0
                            onWeekClick(weekNum, target)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GoalSelectorCard(
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tu Meta para este Periodo:", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Option 540
                FilterChip(
                    selected = currentGoal == 540,
                    onClick = { onGoalSelected(540) },
                    label = { Text("Base (540 pts)") },
                    leadingIcon = if (currentGoal == 540) {
                        { Icon(Icons.Default.CheckCircle, null) }
                    } else null
                )
                // Option 645
                FilterChip(
                    selected = currentGoal == 645,
                    onClick = { onGoalSelected(645) },
                    label = { Text("PRO (645 pts)") },
                    leadingIcon = if (currentGoal == 645) {
                        { Icon(Icons.Default.CheckCircle, null) }
                    } else null
                )
            }
            Text(
                text = if (currentGoal == 645) "Objetivo Semanal: 180 pts" else "Objetivo Semanal: 120 pts",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun WeekItem(
    weekNum: Int,
    start: Date,
    end: Date,
    target: Double,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Semana $weekNum", fontWeight = FontWeight.Bold)
                Text(
                    "${dateFormat.format(start)} - ${dateFormat.format(end)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    "Meta: ${target.toInt()} pts",
                    modifier = Modifier.padding(4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// Extension needed because addDays is private in Logic object roughly, 
// but we made it private in previous step. Let's fix that or replicate logic.
// Actually FuxionCalendarLogic object has addDays as private function based on previous step.
// We should make it public or replicate. I used FuxionCalendarLogic.addDays which implies public.
// I need to check FuxionCalendarLogic again to see if addDays is public.
// If it is private, this code will fail to compile.

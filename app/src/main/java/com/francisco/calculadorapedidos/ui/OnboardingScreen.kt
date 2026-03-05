package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.francisco.calculadorapedidos.data.FuxionDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    dataStore: FuxionDataStore,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var expandedPeriod by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Configuración del Sistema",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "1. Defina la fecha de inicio del Periodo 1 corporativo.",
            style = MaterialTheme.typography.bodyMedium
        )

        DatePicker(state = datePickerState)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "2. Asigne su Periodo de Afiliación (Inicio de Matriz).",
            style = MaterialTheme.typography.bodyMedium
        )

        ExposedDropdownMenuBox(
            expanded = expandedPeriod,
            onExpandedChange = { expandedPeriod = !expandedPeriod }
        ) {
            OutlinedTextField(
                value = "Periodo $selectedPeriod",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedPeriod,
                onDismissRequest = { expandedPeriod = false }
            ) {
                (1..13).forEach { period ->
                    DropdownMenuItem(
                        text = { Text("Periodo $period") },
                        onClick = {
                            selectedPeriod = period
                            expandedPeriod = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    var selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()

                    if (datePickerState.selectedDateMillis != null) {
                        val offset = java.util.TimeZone.getDefault().getOffset(selectedDate)
                        selectedDate += offset
                    }

                    // 1. Persistencia de fecha corporativa global
                    dataStore.saveAnchorDate(selectedDate)

                    // 2. Persistencia escalar y determinista del periodo de inicio manual
                    dataStore.saveUserStartPeriod(selectedPeriod)

                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inicializar Arquitectura")
        }
    }
}
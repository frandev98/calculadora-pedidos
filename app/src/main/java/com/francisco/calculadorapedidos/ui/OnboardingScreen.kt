package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
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
    // Default to Jan 1st of current year if no date selected
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bienvenido a tu Asistente",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Para comenzar, necesitamos saber cuándo inició el Periodo 1 de este año.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        DatePicker(state = datePickerState)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    dataStore.saveAnchorDate(selectedDate)
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Comenzar Mi Negocio")
        }
    }
}

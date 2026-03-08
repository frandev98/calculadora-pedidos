package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(dataStore: FuxionDataStore, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTimestamp by remember { mutableStateOf<Long?>(null) }

    val datePickerState = rememberDatePickerState()
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) }

    Box(modifier = Modifier.fillMaxSize().background(FuxionBlue), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bienvenido a Mi Plan Fuxion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FuxionBlue)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tu Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FuxionBlue)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = if (selectedTimestamp != null) dateFormat.format(Date(selectedTimestamp!!)) else "Selecciona fecha de afiliación",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, null, tint = FuxionBlue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FuxionBlue)
                )

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedTimestamp = datePickerState.selectedDateMillis
                                showDatePicker = false
                            }) { Text("Aceptar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val finalName = name.trim().ifEmpty { "Socio" }
                        val finalTime = selectedTimestamp ?: Calendar.getInstance().timeInMillis
                        scope.launch {
                            dataStore.saveUserName(finalName)
                            dataStore.saveUserStartTimestamp(finalTime)
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FuxionGreen)
                ) {
                    Text("COMENZAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
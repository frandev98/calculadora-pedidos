package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(dataStore: FuxionDataStore, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var period by remember { mutableIntStateOf(1) }
    var expanded by remember { mutableStateOf(false) }

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

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = "Empecé en: Periodo $period",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FuxionBlue)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (1..13).forEach { p ->
                            DropdownMenuItem(text = { Text("Periodo $p") }, onClick = { period = p; expanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val finalName = name.trim().ifEmpty { "Socio" }
                        scope.launch {
                            dataStore.saveUserName(finalName)
                            dataStore.saveUserStartPeriod(period)
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
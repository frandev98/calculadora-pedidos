package com.francisco.calculadorapedidos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.francisco.calculadorapedidos.data.Product
import com.francisco.calculadorapedidos.data.ProductCatalog
import com.francisco.calculadorapedidos.ui.theme.FuxionBlue
import com.francisco.calculadorapedidos.ui.theme.FuxionGreen
import com.francisco.calculadorapedidos.ui.theme.TextPrimary
import com.francisco.calculadorapedidos.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Product) -> Unit,
    excludedIds: List<Int> // <--- 1. NUEVO PARÁMETRO
) {
    // --- ESTADOS LOCALES ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // --- LISTA DE CATEGORÍAS ---
    val categories = remember {
        listOf("TODOS") + ProductCatalog.masterList.map { it.category }.distinct().sorted()
    }

    // --- LÓGICA DE FILTRADO (ACTUALIZADA) ---
    // Ahora depende también de 'excludedIds'
    val filteredList = remember(searchQuery, selectedCategory, excludedIds) {
        ProductCatalog.masterList.filter { product ->
            // A. Verificamos que NO esté en la lista de excluidos
            val isNotExcluded = !excludedIds.contains(product.id)

            // B. Filtros normales (Categoría y Texto)
            val matchCategory = selectedCategory == null || product.category == selectedCategory
            val matchSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    product.code.contains(searchQuery, ignoreCase = true)

            // C. Solo pasa si cumple TODO
            isNotExcluded && matchCategory && matchSearch
        }
    }

    // --- INTERFAZ UI ---
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // ¡ESTO HACE QUE OCUPE TODA LA PANTALLA!
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. ENCABEZADO (Título + Botón Cerrar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 24.dp, end = 8.dp), // Márgenes ajustados
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catálogo de Productos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. BUSCADOR (ESTILO NUEVO "SOFT CAPSULE")
                // Reemplazamos el OutlinedTextField por este TextField moderno
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp) // Margen lateral para que no choque
                        .height(50.dp), // Altura compacta
                    placeholder = {
                        Text("Buscar producto...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = FuxionBlue)
                    },
                    shape = RoundedCornerShape(50), // Bordes totalmente redondos
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent, // Sin línea abajo
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFF0F5F9), // Gris azulado suave
                        unfocusedContainerColor = Color(0xFFF0F5F9)
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. CHIPS DE CATEGORÍAS (Tus filtros originales)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp), // Alineado con el buscador
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = (category == "TODOS" && selectedCategory == null) || category == selectedCategory

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = if (category == "TODOS") null else category
                            },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FuxionBlue, // Usamos el azul corporativo al seleccionar
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) FuxionBlue else Color.LightGray
                            )
                        )
                    }
                }

                // Línea divisoria sutil
                Divider(
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color(0xFFEEEEEE)
                )

                // 4. LISTA DE PRODUCTOS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredList) { product ->
                        ProductCatalogRow(product = product, onSelect = {
                            // 1. Agregamos el producto
                            onProductSelected(product)

                            // 2. ¡IMPORTANTE! Cerramos el diálogo (Quitamos las barras //)
                            onDismiss()
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCatalogRow(product: Product, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() }, // Toda la tarjeta es clickeable
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp) // Bordes redondeados suaves
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. IMAGEN
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5), // Fondo gris suave
                modifier = Modifier.size(50.dp)
            ) {
                Image(
                    painter = painterResource(id = product.imageRes),
                    contentDescription = product.name,
                    modifier = Modifier.padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. TEXTOS
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Cód: ${product.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                // Precio y Puntos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${product.points} pts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FuxionGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "S/. ${product.price}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 3. BOTÓN DE ACCIÓN (+)
            // Visualmente indica "Agregar"
            IconButton(
                onClick = onSelect,
                modifier = Modifier
                    .size(36.dp)
                    .background(FuxionBlue.copy(alpha = 0.1f), CircleShape) // Círculo azul suave de fondo
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar",
                    tint = FuxionBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
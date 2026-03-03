package com.francisco.calculadorapedidos.ui

import com.francisco.calculadorapedidos.R
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    excludedIds: List<Int>
) {
    // --- ESTADOS LOCALES ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // --- LISTA DE CATEGORÍAS ---
    val categories = remember {
        listOf("TODOS") + ProductCatalog.masterList.map { it.category }.distinct().sorted()
    }

    // --- LÓGICA DE FILTRADO ---
    val filteredList = remember(searchQuery, selectedCategory, excludedIds) {
        ProductCatalog.masterList.filter { product ->
            val isNotExcluded = !excludedIds.contains(product.id)
            val matchCategory = selectedCategory == null || product.category == selectedCategory
            val matchSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    product.code.contains(searchQuery, ignoreCase = true)

            isNotExcluded && matchCategory && matchSearch
        }
    }

    // --- INTERFAZ UI ---
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Ocupa toda la pantalla
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Usamos un Box para poder superponer el botón flotante al fondo
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. ENCABEZADO
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 24.dp, end = 8.dp),
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

                    // 2. BUSCADOR
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(50.dp),
                        placeholder = {
                            Text("Buscar producto...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = FuxionBlue)
                        },
                        shape = RoundedCornerShape(50),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF0F5F9),
                            unfocusedContainerColor = Color(0xFFF0F5F9)
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. CHIPS DE CATEGORÍAS
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
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
                                    selectedContainerColor = FuxionBlue,
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

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color(0xFFEEEEEE)
                    )

                    // 4. LISTA DE PRODUCTOS
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Aumentamos el padding inferior a 80.dp para que el último producto no quede tapado por el botón "Listo"
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
                    ) {
                        items(filteredList) { product ->
                            ProductCatalogRow(product = product, onSelect = {
                                // 1. Agregamos el producto al carrito principal
                                onProductSelected(product)

                                // 2. ¡ELIMINADO! Ya no llamamos a onDismiss() aquí para que no se cierre la pantalla.
                            })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // --- NUEVO: BOTÓN FLOTANTE DE "LISTO" ---
                ExtendedFloatingActionButton(
                    onClick = { onDismiss() }, // Este es el que ahora cierra la pantalla
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    containerColor = FuxionGreen,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Listo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TERMINAR Y REVISAR", fontWeight = FontWeight.Bold)
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
                // 1. Obtenemos el ID numérico a partir del nombre (String)
                val imageResId = rememberDrawableId(product.imageRes)
                Image(
                    // 2. Usamos el ID. Si es 0 (no encontrado), usa una imagen por defecto o placeholder
                    painter = if (imageResId != 0) painterResource(id = imageResId) else painterResource(id = R.drawable.ic_launcher_foreground), // Asegúrate de tener un placeholder o usa uno del sistema
                    contentDescription = product.name,
                    modifier = Modifier.padding(all = 4.dp)
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

@Composable
fun rememberDrawableId(imageName: String): Int {
    val context = LocalContext.current
    return remember(imageName) {
        // Busca el ID del recurso en la carpeta 'drawable' usando el nombre
        context.resources.getIdentifier(
            imageName,
            "drawable",
            context.packageName
        )
    }
}
package com.pictofly.ui.screens.config

import android.content.Context
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pictofly.data.model.Category
import com.pictofly.ui.navigation.NavigationRoutes
import com.pictofly.viewmodel.AllCategoriesViewModel
import com.pictofly.viewmodel.LocalContentViewModel
import com.pictofly.viewmodel.OperationState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLocalContentScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val viewModel: AllCategoriesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val localContentViewModel: LocalContentViewModel = hiltViewModel()
    val operationState by localContentViewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ESTADO LOCAL PARA FORZAR RECOMPOSICIÓN
    var refreshTrigger by remember { mutableStateOf(0) }

    // EFECTO PARA RECARGAR CUANDO HAY OPERACIONES EXITOSAS
    LaunchedEffect(operationState, refreshTrigger) {
        val currentState = operationState
        when {
            currentState is OperationState.Success -> {
                val message = currentState.message
                if (message.contains("oculta") || message.contains("restaurada") || message.contains("eliminada")) {
                    refreshTrigger++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agregar Categorías",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(NavigationRoutes.ADD_CATEGORY)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar categoría",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.categories.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.categories,
                            key = { category ->
                                "${category.name}_$refreshTrigger"
                            }
                        ) { category ->
                            CategoryItemCard(
                                category = category,
                                navController = navController,
                                localContentViewModel = localContentViewModel,
                                context = context,
                                onActionComplete = {
                                    refreshTrigger++
                                }
                            )
                        }
                    }
                }
            }

            // SNACKBAR DE OPERACIÓN
            val currentState = operationState
            val snackbarMessage = when {
                currentState is OperationState.Success -> currentState.message
                currentState is OperationState.Error -> currentState.message
                else -> null
            }

            if (snackbarMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(
                            onClick = {
                                localContentViewModel.resetOperationState()
                            }
                        ) {
                            Text(
                                text = "OK",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(text = snackbarMessage)
                }
            }
        }
    }
}

@Composable
fun CategoryItemCard(
    category: Category,
    navController: NavController,
    localContentViewModel: LocalContentViewModel,
    context: Context,
    onActionComplete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var showHideCategoryDialog by remember { mutableStateOf(false) }
    var showRestoreCategoryDialog by remember { mutableStateOf(false) }
    var showErrorSnackbar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // ESTADO LOCAL PARA EL ESTADO OCULTO
    var isHidden by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // CARGAR ESTADO INICIAL Y CUANDO CAMBIA
    LaunchedEffect(key1 = category.name) {
        if (!category.isLocal) {
            isHidden = localContentViewModel.isCategoryHidden(category.name)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!category.isLocal && isHidden)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // IMAGEN
            AsyncImage(
                model = category.imageUrl?.let {
                    ImageRequest.Builder(context).data(it).build()
                } ?: category.localFileUri,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // TEXTO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.Bold,
                    color = if (!category.isLocal && isHidden)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when {
                        category.isLocal -> "Personalizada"
                        isHidden -> "Predeterminada (Oculta)"
                        else -> "Predeterminada"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        category.isLocal -> MaterialTheme.colorScheme.primary
                        isHidden -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // MENÚ
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // ✅ AGREGAR PICTOGRAMAS - VERSIÓN CORREGIDA PARA CATEGORÍAS PREDETERMINADAS
                    if (!isHidden) {

                        // ✅ AGREGAR PICTOGRAMAS - VERSIÓN CORREGIDA PARA CATEGORÍAS PREDETERMINADAS
                        if (!isHidden) {
                            DropdownMenuItem(
                                onClick = {
                                    showMenu = false
                                    isProcessing = true

                                    coroutineScope.launch {
                                        try {
                                            if (category.isLocal) {
                                                // 🟢 CATEGORÍA LOCAL - Buscar ID
                                                Log.d("Navigation", "🔍 Buscando categoría local: ${category.name}")

                                                val categoryId = localContentViewModel.findLocalCategoryIdSync(category)

                                                if (categoryId != null && categoryId.isNotEmpty()) {
                                                    Log.d("Navigation", "✅ Categoría local encontrada con ID: $categoryId")
                                                    navController.navigate("${NavigationRoutes.ADD_PICTOGRAMS}/$categoryId")
                                                } else {
                                                    errorMessage = "Error: No se pudo encontrar la categoría '${category.name}'"
                                                    showErrorSnackbar = true
                                                }
                                            } else {
                                                // 🟡 CATEGORÍA PREDETERMINADA - PASAR EL NOMBRE DIRECTAMENTE
                                                Log.d("Navigation", "🔍 Navegando a AddPictogramsScreen con nombre de categoría: ${category.name}")

                                                // ✅ IMPORTANTE: Pasar el NOMBRE de la categoría, no intentar crear ID
                                                // El ViewModel se encargará de crear/obtener la extensión
                                                navController.navigate("${NavigationRoutes.ADD_PICTOGRAMS}/${category.name}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("Navigation", "❌ Error en navegación: ${e.message}")
                                            errorMessage = "Error: ${e.message}"
                                            showErrorSnackbar = true
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Agregar pictogramas",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )
                        }
                    }

                    // ✅ VER PICTOGRAMAS
                    if (!isHidden) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                navController.navigate("${NavigationRoutes.CATEGORY_PICTOGRAMS}/${category.name}")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Text(
                                    text = "Ver pictogramas",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }

                    // RESTAURAR CATEGORÍA (si está oculta)
                    if (!category.isLocal && isHidden) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                showRestoreCategoryDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Text(
                                    text = "Restaurar categoría",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )

                    // ELIMINAR / OCULTAR
                    DropdownMenuItem(
                        onClick = {
                            showMenu = false
                            if (category.isLocal) {
                                showDeleteCategoryDialog = true
                            } else {
                                showHideCategoryDialog = true
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when {
                                    category.isLocal -> Icons.Default.Delete
                                    else -> Icons.Default.DeleteForever
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        text = {
                            Text(
                                text = when {
                                    category.isLocal -> "Eliminar categoría"
                                    else -> if (isHidden) "Eliminar permanentemente" else "Ocultar categoría"
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
        }
    }

    // Snackbar para errores
    if (showErrorSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showErrorSnackbar = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Text(text = errorMessage)
        }
    }

    // ========== DIÁLOGOS ==========

    // DIÁLOGO ELIMINAR CATEGORÍA LOCAL
    if (showDeleteCategoryDialog && category.isLocal) {
        AlertDialog(
            onDismissRequest = { showDeleteCategoryDialog = false },
            title = {
                Text(
                    text = "¿Estás seguro de eliminar \"${category.name}\"?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteCategoryDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Cancelar")
                    }

                    Button(
                        onClick = {
                            showDeleteCategoryDialog = false
                            localContentViewModel.deleteCategoryPermanently(category)
                            onActionComplete()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Eliminar")
                    }
                }
            },
            dismissButton = {}
        )
    }

    // DIÁLOGO OCULTAR CATEGORÍA PREDETERMINADA
    if (showHideCategoryDialog && !category.isLocal && !isHidden) {
        AlertDialog(
            onDismissRequest = { showHideCategoryDialog = false },
            title = {
                Text(
                    text = "¿Ocultar \"${category.name}\"?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "La categoría desaparecerá de la pantalla principal pero podrás restaurarla después.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHideCategoryDialog = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancelar")
                    }

                    Button(
                        onClick = {
                            showHideCategoryDialog = false
                            coroutineScope.launch {
                                localContentViewModel.hideDefaultCategory(category.name)
                                isHidden = true
                                onActionComplete()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = "Ocultar")
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showRestoreCategoryDialog && !category.isLocal && isHidden) {
        AlertDialog(
            onDismissRequest = { showRestoreCategoryDialog = false },
            title = {
                Text(
                    text = "¿Restaurar \"${category.name}\"?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "La categoría volverá a aparecer en la lista principal con todas tus imágenes personalizadas.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRestoreCategoryDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Cancelar")
                    }

                    Button(
                        onClick = {
                            showRestoreCategoryDialog = false
                            coroutineScope.launch {
                                localContentViewModel.restoreDefaultCategory(category.name)
                                isHidden = false
                                onActionComplete()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = "Restaurar")
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay categorías",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Agrega tu primera categoría para comenzar",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
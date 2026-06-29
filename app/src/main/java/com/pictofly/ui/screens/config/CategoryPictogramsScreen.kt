package com.pictofly.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.pictofly.data.model.Pictogram
import com.pictofly.viewmodel.CategoryPictogramsViewModel
import com.pictofly.viewmodel.OperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPictogramsScreen(
    navController: NavController,
    categoryName: String,
    onBack: () -> Unit
) {
    val viewModel: CategoryPictogramsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(categoryName) {
        viewModel.loadPictogramsForCategory(categoryName)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pictogramToDelete by remember { mutableStateOf<Pictogram?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryName,
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
        }
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

                uiState.pictograms.isEmpty() -> {
                    EmptyPictogramsState(categoryName)
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3), //divide el ancho en 3
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.pictograms,
                            key = { pictogram -> pictogram.name }
                        ) { pictogram ->
                            PictogramCard(
                                pictogram = pictogram,
                                onDeleteClick = {
                                    pictogramToDelete = pictogram
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

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
                                viewModel.resetOperationState()
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

    if (showDeleteDialog && pictogramToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "¿Eliminar pictograma?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val message = if (pictogramToDelete!!.isLocal || pictogramToDelete!!.createdByUser) {
                    "¿Estás seguro de eliminar \"${pictogramToDelete!!.name}\" permanentemente?"
                } else {
                    "¿Estás seguro de ocultar \"${pictogramToDelete!!.name}\"?\nPodrás restaurarlo después."
                }
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
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
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Cancelar")
                    }

                    Button(
                        onClick = {
                            showDeleteDialog = false
                            pictogramToDelete?.let {
                                viewModel.deletePictogram(it, categoryName)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pictogramToDelete!!.isLocal || pictogramToDelete!!.createdByUser)
                                Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (pictogramToDelete!!.isLocal || pictogramToDelete!!.createdByUser)
                                "Eliminar" else "Ocultar"
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@Composable
fun PictogramCard(
    pictogram: Pictogram,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (!pictogram.isPredefined)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(pictogram.getDisplayImageUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = pictogram.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (pictogram.isLocal || pictogram.createdByUser) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "", //2.0 agregar fav
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eliminar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ) {
            Text(
                text = pictogram.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyPictogramsState(categoryName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay pictogramas",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Esta categoría no tiene pictogramas aún.\nAgrega algunos desde el menú de la categoría.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
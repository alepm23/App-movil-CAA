package com.pictofly.ui.screens.config

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.rememberAsyncImagePainter
import com.pictofly.ui.components.CommunicationPictogramItem
import com.pictofly.ui.theme.*
import com.pictofly.viewmodel.LocalContentViewModel
import com.pictofly.viewmodel.OperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationCustomPictogramsScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val viewModel: LocalContentViewModel = hiltViewModel()
    val communicationState by viewModel.communicationModeState.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            viewModel.updatePictogramImageUri(uri)
        }
    )

    Scaffold(
        containerColor = LightGreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agregar imágenes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = IconGreen
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddPictogramDialog() },
                modifier = Modifier.padding(12.dp),
                containerColor = GreenBright,
                contentColor = White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Crear nuevo pictograma",
                    tint = White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightGreenBg)
                .padding(paddingValues)
        ) {
            if (communicationState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GreenBright
                    )
                }
            } else if (communicationState.pictograms.isEmpty()) {
                EmptyState(onAddClick = { viewModel.showAddPictogramDialog() })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(communicationState.pictograms) { pictogram ->
                        CommunicationPictogramItem(
                            pictogram = pictogram,
                            isSelected = false,
                            onDelete = {
                                viewModel.showDeletePictogramDialog(pictogram)
                            }
                        )
                    }
                }
            }

            if (communicationState.showAddDialog) {
                AddPictogramDialog(
                    pictogramName = communicationState.pictogramName,
                    onNameChange = { viewModel.updatePictogramName(it) },
                    selectedImageUri = communicationState.pictogramImageUri,
                    onSelectImage = { imagePicker.launch("image/*") },
                    onClearImage = { viewModel.updatePictogramImageUri(null) },
                    selectedType = communicationState.selectedType,
                    onTypeChange = { type -> viewModel.updatePictogramType(type) },
                    onDismiss = { viewModel.hideAddPictogramDialog() },
                    onSave = {
                        viewModel.saveCommunicationPictogram(context)
                    },
                    isSaveEnabled = communicationState.pictogramName.isNotBlank() &&
                            communicationState.pictogramImageUri != null
                )
            }

            if (communicationState.showDeleteDialog) {
                DeletePictogramDialog(
                    onDismiss = { viewModel.hideDeletePictogramDialog() },
                    onConfirm = { viewModel.deleteCommunicationPictogram() }
                )
            }

            when (operationState) {
                is OperationState.Loading -> {
                    LoadingOverlay((operationState as OperationState.Loading).message)
                }
                is OperationState.Error -> {
                    ErrorSnackbar(
                        message = (operationState as OperationState.Error).message,
                        onDismiss = { viewModel.resetOperationState() }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(LightGreenBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = GreenBright.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Personaliza tu comunicación",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Agrega pictogramas desde tu dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenBright,
                contentColor = White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Añadir",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }
    }
}

@Composable
private fun AddPictogramDialog(
    pictogramName: String,
    onNameChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit,
    onClearImage: () -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isSaveEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            // TÍTULO CENTRADO
            Box(
                modifier = Modifier.wrapContentWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Crear nuevo pictograma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGreen,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = pictogramName,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            text = "Nombre del pictograma",
                            color = TextSecondary
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Ej: Mamá, Papá, Comer...",
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenBright,
                        focusedLabelColor = GreenBright,
                        cursorColor = GreenBright,
                        focusedTextColor = TextPrimary
                    )
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "¿Qué tipo de pictograma es?",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == "subject",
                            onClick = { onTypeChange("subject") },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedType == "subject") White else IconGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sujeto",
                                        color = if (selectedType == "subject") White else TextPrimary
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenBright,
                                selectedLabelColor = White,
                                containerColor = LightGreenBg,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedType == "verb",
                            onClick = { onTypeChange("verb") },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedType == "verb") White else IconGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verbo",
                                        color = if (selectedType == "verb") White else TextPrimary
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenBright,
                                selectedLabelColor = White,
                                containerColor = LightGreenBg,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedImageUri != null)
                                LightGreenBg
                            else
                                LightGreenBg.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = onClearImage,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ErrorRed, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar imagen",
                                    modifier = Modifier.size(18.dp),
                                    tint = White
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = IconGreen.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Selecciona una imagen",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Button(
                    onClick = onSelectImage,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightGreenBg,
                        contentColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = DarkGreen
                        )
                        Text(
                            text = "Seleccionar imagen",
                            style = MaterialTheme.typography.labelMedium,
                            color = DarkGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = IconGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelLarge,
                        color = IconGreen
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaveEnabled) GreenBright else DisabledGray,
                        contentColor = White,
                        disabledContainerColor = DisabledGray,
                        disabledContentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Guardar",
                        style = MaterialTheme.typography.labelLarge,
                        color = White
                    )
                }
            }
        },
        dismissButton = { }
    )
}

@Composable
private fun DeletePictogramDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            // TÍTULO CENTRADO
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Eliminar pictograma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGreen,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = "¿Estás seguro de que deseas eliminar este pictograma? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Cancelar
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = IconGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelLarge,
                        color = IconGreen
                    )
                }

                // Botón Eliminar
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ErrorRed
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Eliminar",
                        style = MaterialTheme.typography.labelLarge,
                        color = ErrorRed
                    )
                }
            }
        },
        dismissButton = { }
    )
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = GreenBright
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = ErrorRed.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
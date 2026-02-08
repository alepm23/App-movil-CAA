// app/src/main/java/com/pictofly/ui/screens/config/CommunicationSelectorScreen.kt
package com.pictofly.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pictofly.data.model.LocalPictogram
import com.pictofly.ui.theme.*
import com.pictofly.viewmodel.CommunicationViewModel
import com.pictofly.viewmodel.LocalContentViewModel
import com.pictofly.viewmodel.OperationState
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape

// 📌 TIPO DE SELECCIÓN
enum class SelectorType {
    SUBJECT,
    VERB
}

// 📌 CONFIGURACIÓN POR TIPO
data class SelectorConfig(
    val type: SelectorType,
    val title: String,
    val emptyTitle: String,
    val emptyDescription: String,
    val addButtonText: String,
    val selectText: String,
    val confirmText: String,
    val defaultIcon: ImageVector,
    val filterType: String  // "subject" o "verb"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationSelectorScreen(
    navController: NavController,
    selectorType: SelectorType,
    onBack: () -> Unit
) {
    // 📌 CONFIGURACIÓN SEGÚN EL TIPO
    val config = when (selectorType) {
        SelectorType.SUBJECT -> SelectorConfig(
            type = SelectorType.SUBJECT,
            title = "Cambiar Sujeto",
            emptyTitle = "No hay sujetos configurados",
            emptyDescription = "Agrega tu primer sujeto personalizado",
            addButtonText = "Agregar sujeto",
            selectText = "Selecciona un sujeto:",
            confirmText = "CONFIRMAR",
            defaultIcon = Icons.Default.Person,
            filterType = "subject"
        )
        SelectorType.VERB -> SelectorConfig(
            type = SelectorType.VERB,
            title = "Cambiar Verbo",
            emptyTitle = "No hay verbos configurados",
            emptyDescription = "Ve a 'Agregar imágenes' y crea pictogramas marcados como 'Verbo'",
            addButtonText = "Agregar verbo",
            selectText = "Selecciona un verbo:",
            confirmText = "CONFIRMAR",
            defaultIcon = Icons.Default.DirectionsRun,
            filterType = "verb"
        )
    }

    val localViewModel: LocalContentViewModel = hiltViewModel()
    val state by localViewModel.communicationModeState.collectAsStateWithLifecycle()
    val communicationViewModel: CommunicationViewModel = hiltViewModel()
    val operationState by localViewModel.operationState.collectAsStateWithLifecycle()

    // 📌 SELECCIÓN ACTUAL SEGÚN EL TIPO
    val selectedItem = when (selectorType) {
        SelectorType.SUBJECT -> communicationViewModel.selectedSubject.collectAsStateWithLifecycle().value
        SelectorType.VERB -> communicationViewModel.selectedVerb.collectAsStateWithLifecycle().value
    }

    var temporarySelectedItem by remember(selectedItem) {
        mutableStateOf<LocalPictogram?>(selectedItem)
    }

    // 📌 FILTRAR PICTOGRAMAS POR TIPO
    val filteredPictograms = state.pictograms.filter { it.type == config.filterType }
    val context = LocalContext.current

    Scaffold(
        containerColor = LightGreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = config.title,
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
                actions = {
                    if (selectedItem != null) {
                        Text(
                            text = "Actual: ${selectedItem!!.name}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = GreenBright
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    scrolledContainerColor = White,
                    titleContentColor = DarkGreen,
                    navigationIconContentColor = IconGreen,
                    actionIconContentColor = GreenBright
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightGreenBg)
                .padding(paddingValues)
        ) {
            // 📌 BANNER SUPERIOR - ÍTEM SELECCIONADO
            SelectedItemBanner(
                selectedItem = temporarySelectedItem ?: selectedItem,
                defaultIcon = config.defaultIcon,
                onConfirm = {
                    temporarySelectedItem?.let { item ->
                        when (selectorType) {
                            SelectorType.SUBJECT -> communicationViewModel.selectSubject(item)
                            SelectorType.VERB -> communicationViewModel.selectVerb(item)
                        }
                        onBack()
                    }
                },
                isEnabled = temporarySelectedItem != null,
                confirmText = if (temporarySelectedItem != null)
                    "${config.confirmText} ${temporarySelectedItem!!.name.uppercase()}"
                else
                    "SELECCIONA UN ${config.filterType.uppercase()}"
            )

            // 📌 LISTA DE ÍTEMS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(White)
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GreenBright)
                        }
                    }
                    filteredPictograms.isEmpty() -> {
                        EmptySelectorState(
                            config = config,
                            onAddClick = {
                                navController.navigate("communication/custom_pictograms")
                            }
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = config.selectText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 120.dp),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredPictograms) { pictogram ->
                                    SelectorItemCard(
                                        pictogram = pictogram,
                                        isTemporarySelected = temporarySelectedItem?.id == pictogram.id,
                                        isActive = selectedItem?.id == pictogram.id && temporarySelectedItem == null,
                                        defaultIcon = config.defaultIcon,
                                        onSelect = { temporarySelectedItem = pictogram },
                                        onDelete = { localViewModel.showDeletePictogramDialog(pictogram) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 📌 DIÁLOGOS Y ESTADOS
            if (state.showDeleteDialog) {
                DeletePictogramDialog(
                    onDismiss = { localViewModel.hideDeletePictogramDialog() },
                    onConfirm = {
                        val deletedId = state.pictogramToDelete?.id
                        localViewModel.deleteCommunicationPictogram()
                        if (temporarySelectedItem?.id == deletedId) {
                            temporarySelectedItem = null
                        }
                    }
                )
            }

            when (operationState) {
                is OperationState.Loading -> {
                    LoadingOverlay((operationState as OperationState.Loading).message)
                }
                is OperationState.Error -> {
                    LaunchedEffect(operationState) {
                        kotlinx.coroutines.delay(3000)
                        localViewModel.resetOperationState()
                    }
                }
                else -> {}
            }
        }
    }
}

// 📌 COMPONENTE: Banner del ítem seleccionado
@Composable
private fun SelectedItemBanner(
    selectedItem: LocalPictogram?,
    defaultIcon: ImageVector,
    onConfirm: () -> Unit,
    isEnabled: Boolean,
    confirmText: String
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGreenBg)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.size(140.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedItem != null) LightGreenBg else White
                ),
                border = if (selectedItem != null)
                    BorderStroke(2.dp, GreenBright)
                else
                    null
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedItem != null) {
                            val imageFile = File(context.filesDir, selectedItem.imagePath)
                            if (imageFile.exists()) {
                                AsyncImage(
                                    model = imageFile,
                                    contentDescription = selectedItem.name,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(LightGreenBg, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        defaultIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = IconGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedItem.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen,
                                maxLines = 1
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(LightGreenBg, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    defaultIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = IconGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (defaultIcon == Icons.Default.Person) "Sujeto" else "Verbo",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = DarkGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenBright,
                    disabledContainerColor = DisabledGray
                )
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }
    }
}

// 📌 COMPONENTE: Tarjeta de ítem en grid
@Composable
private fun SelectorItemCard(
    pictogram: LocalPictogram,
    isTemporarySelected: Boolean,
    isActive: Boolean,
    defaultIcon: ImageVector,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTemporarySelected || isActive) 8.dp else 2.dp
        ),
        border = when {
            isTemporarySelected -> BorderStroke(3.dp, GreenBright)
            isActive -> BorderStroke(2.dp, GreenBright.copy(alpha = 0.5f))
            else -> null
        },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isTemporarySelected -> LightGreenBg
                isActive -> LightGreenBg.copy(alpha = 0.5f)
                else -> White
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val imageFile = File(context.filesDir, pictogram.imagePath)
                if (imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = pictogram.name,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(LightGreenBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            defaultIcon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = IconGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pictogram.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isTemporarySelected || isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isTemporarySelected || isActive) DarkGreen else TextPrimary,
                    maxLines = 1
                )

                if (isTemporarySelected) {
                    Badge(
                        modifier = Modifier.padding(top = 2.dp),
                        containerColor = GreenBright
                    ) {
                        Text(
                            text = "SELECCIONADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = White
                        )
                    }
                }
            }

            if (pictogram.id.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed)
                        .clickable { onDelete() }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Eliminar",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(45f)
                            .align(Alignment.Center),
                        tint = White
                    )
                }
            }
        }
    }
}

// 📌 COMPONENTE: Estado vacío
@Composable
private fun EmptySelectorState(
    config: SelectorConfig,
    onAddClick: () -> Unit
) {
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
                config.defaultIcon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = GreenBright.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = config.emptyTitle,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = config.emptyDescription,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            modifier = Modifier.width(240.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenBright,
                contentColor = White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Text(
                    text = config.addButtonText,
                    style = MaterialTheme.typography.labelLarge,
                    color = White
                )
            }
        }
    }
}

// 📌 COMPONENTE: Diálogo de eliminar
@Composable
private fun DeletePictogramDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
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
                text = "¿Estás seguro de que deseas eliminar este pictograma?",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
            ) {
                Text(
                    text = "Eliminar",
                    style = MaterialTheme.typography.labelLarge,
                    color = ErrorRed
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = IconGreen)
            ) {
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.labelLarge,
                    color = IconGreen
                )
            }
        }
    )
}

// 📌 COMPONENTE: Loading Overlay
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
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = GreenBright)
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
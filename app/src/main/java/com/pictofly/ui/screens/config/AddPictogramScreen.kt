package com.pictofly.ui.screens.config

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pictofly.data.model.LocalPictogram
import com.pictofly.ui.theme.*
import com.pictofly.viewmodel.LocalContentViewModel
import com.pictofly.viewmodel.OperationState
import java.io.File
import kotlinx.coroutines.delay
import android.util.Log

// ✅ SAVER PARA URI - IMPRESCINDIBLE
object UriSaver : Saver<Uri?, String> {
    override fun restore(value: String): Uri? {
        return try {
            Uri.parse(value)
        } catch (e: Exception) {
            null
        }
    }

    override fun SaverScope.save(value: Uri?): String? {
        return value?.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPictogramsScreen(
    navController: NavController,
    categoryId: String,  // Puede ser un ID numérico o un NOMBRE de categoría
    onComplete: () -> Unit
) {
    val viewModel: LocalContentViewModel = hiltViewModel()
    val addPictogramState by viewModel.addPictogramState.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Estado para controlar si la categoría es válida
    var isCategoryValid by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Guardar URI en rememberSaveable
    var currentImageUri by rememberSaveable(stateSaver = UriSaver) {
        mutableStateOf(addPictogramState.pictogramImageUri)
    }

    // ✅ VERIFICAR QUE LA CATEGORÍA EXISTA ANTES DE INICIAR EL FLUJO
    LaunchedEffect(Unit) {
        Log.d("AddPictograms", "🚀 Iniciando con categoryId/Name: $categoryId")

        if (!addPictogramState.isFlowActive) {
            viewModel.ensureCategoryExists(categoryId) { success, finalCategoryId ->
                isCategoryValid = success
                if (success) {
                    Log.d("AddPictograms", "✅ Categoría válida, ID final: $finalCategoryId")
                    viewModel.startAddPictogramsFlow(finalCategoryId)
                } else {
                    Log.e("AddPictograms", "❌ Categoría no válida: $categoryId")
                    errorMessage = "No se pudo acceder a la categoría"
                }
            }
        }
    }

    // ✅ Sincronizar URI local con ViewModel
    LaunchedEffect(currentImageUri) {
        currentImageUri?.let { uri ->
            if (addPictogramState.pictogramImageUri != uri) {
                viewModel.setPictogramImageUri(uri)
            }
        }
    }

    // ✅ Launcher para seleccionar imagen
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                currentImageUri = it
                viewModel.setPictogramImageUri(it)
            }
        }
    )

    // Si hay error, mostrar pantalla de error
    if (errorMessage != null || !isCategoryValid) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = ErrorRed,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = errorMessage ?: "Error: Categoría no encontrada",
                    style = MaterialTheme.typography.titleMedium,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        navController.popBackStack()
                        onComplete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenBright
                    )
                ) {
                    Text("Volver")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agregar Pictogramas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (addPictogramState.addedPictograms.isEmpty()) {
                            viewModel.skipAddPictograms {
                                navController.popBackStack()
                                onComplete()
                            }
                        } else {
                            navController.popBackStack()
                            onComplete()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = IconGreen,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = DarkGreen,
                    navigationIconContentColor = IconGreen
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(LightGreenBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ========== FORMULARIO PARA AGREGAR NUEVO PICTOGRAMA ==========
                Text(
                    text = "Nuevo Pictograma",
                    style = MaterialTheme.typography.titleLarge,
                    color = DarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Nombre del pictograma
                OutlinedTextField(
                    value = addPictogramState.pictogramName,
                    onValueChange = { viewModel.setPictogramName(it) },
                    label = {
                        Text(
                            text = "Nombre del pictograma",
                            color = TextSecondary
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Ej: Perro, Manzana, Feliz...",
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    isError = addPictogramState.nameError != null,
                    supportingText = {
                        if (addPictogramState.nameError != null) {
                            Text(
                                text = addPictogramState.nameError!!,
                                color = ErrorRed
                            )
                        } else {
                            Text(
                                text = "Mínimo 2 caracteres, máximo 30",
                                color = TextSecondary
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenBright,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                        focusedLabelColor = GreenBright,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenBright,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ========== SELECTOR DE TIPO (solo para categorías predeterminadas) ==========
                // Si es una categoría predeterminada (como Sujeto o Verbo), mostrar selector de tipo
                if (addPictogramState.categoryId.matches(Regex("\\D+"))) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tipo de pictograma:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = addPictogramState.selectedType == "subject",
                                onClick = { viewModel.updateAddPictogramType("subject") },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (addPictogramState.selectedType == "subject") White else IconGreen
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sujeto",
                                            color = if (addPictogramState.selectedType == "subject") White else TextPrimary
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
                                selected = addPictogramState.selectedType == "verb",
                                onClick = { viewModel.updateAddPictogramType("verb") },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsRun,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (addPictogramState.selectedType == "verb") White else IconGreen
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Verbo",
                                            color = if (addPictogramState.selectedType == "verb") White else TextPrimary
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

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ========== VISTA DE IMAGEN ==========
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (addPictogramState.pictogramImageUri != null)
                                LightGreenBg
                            else
                                LightGreenBg.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (addPictogramState.pictogramImageUri != null) {
                        val imageUri = addPictogramState.pictogramImageUri
                        key(imageUri) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Imagen seleccionada",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Botón para quitar imagen
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    currentImageUri = null
                                    viewModel.clearPictogramImage()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        ErrorRed.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar imagen",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
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
                                contentDescription = "Seleccionar imagen",
                                modifier = Modifier.size(64.dp),
                                tint = IconGreen.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Selecciona una imagen",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para seleccionar imagen
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightGreenBg,
                        contentColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
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
                            style = MaterialTheme.typography.labelLarge,
                            color = DarkGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ========== BOTONES EN LA MISMA FILA ==========
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ BOTÓN "AÑADIR"
                    Button(
                        onClick = {
                            viewModel.addCurrentPictogram(context) {
                                focusManager.clearFocus()
                            }
                        },
                        enabled = addPictogramState.pictogramName.isNotBlank() &&
                                addPictogramState.pictogramImageUri != null &&
                                addPictogramState.nameError == null,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenBright,
                            contentColor = White,
                            disabledContainerColor = DisabledGray,
                            disabledContentColor = White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
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

                    // ✅ BOTÓN "FINALIZAR" (solo visible si hay pictogramas)
                    if (addPictogramState.addedPictograms.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.completeAddPictogramsFlow {
                                    navController.popBackStack()
                                    onComplete()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Finalizar",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }
                    }
                }

                // Mostrar error de imagen si existe
                addPictogramState.imageError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ========== LISTA DE PICTOGRAMAS AGREGADOS ==========
                if (addPictogramState.addedPictograms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Título con contador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pictogramas agregados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                        Badge(
                            containerColor = GreenBright,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "${addPictogramState.addedPictograms.size}",
                                color = White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lista de pictogramas
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(addPictogramState.addedPictograms) { pictogram ->
                            PictogramItemCard(pictogram = pictogram)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Mostrar estado de operación
            when (val state = operationState) {
                is OperationState.Loading -> {
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
                            shape = RoundedCornerShape(16.dp)
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
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                is OperationState.Success -> {
                    LaunchedEffect(operationState) {
                        delay(2000)
                        viewModel.resetOperationState()
                    }
                }
                is OperationState.Error -> {
                    LaunchedEffect(operationState) {
                        delay(3000)
                        viewModel.resetOperationState()
                    }
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.resetOperationState() }) {
                                Text("OK", color = White)
                            }
                        },
                        containerColor = ErrorRed,
                        contentColor = White
                    ) {
                        Text(text = state.message)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PictogramItemCard(pictogram: LocalPictogram) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MOSTRAR IMAGEN REAL DEL PICTOGRAMA
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightGreenBg),
                contentAlignment = Alignment.Center
            ) {
                if (pictogram.imagePath.isNotEmpty()) {
                    val imageFile = File(context.filesDir, pictogram.imagePath)
                    if (imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = pictogram.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Pictograma",
                            tint = IconGreen.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Pictograma",
                        tint = IconGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Nombre
            Text(
                text = pictogram.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // Indicador de éxito
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Agregado",
                tint = SuccessGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
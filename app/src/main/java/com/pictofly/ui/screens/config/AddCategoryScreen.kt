package com.pictofly.ui.screens.config

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.pictofly.viewmodel.LocalContentViewModel
import com.pictofly.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val viewModel: LocalContentViewModel = hiltViewModel()
    val addCategoryState by viewModel.addCategoryState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (!addCategoryState.isFlowActive) {
            viewModel.startAddCategoryFlow()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.setCategoryImageUri(it) }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (addCategoryState.currentStep) {
                        1 -> Text(
                            text = "Nombre de la categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                        2 -> Text(
                            text = "Imagen de portada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                        3 -> Text(
                            text = "Confirmar categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                        4 -> Text(
                            text = "Categoría creada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                        else -> Text(
                            text = "Agregar Categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (addCategoryState.currentStep > 1) {
                            viewModel.goToPreviousStep()
                        } else {
                            viewModel.cancelAddCategoryFlow()
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = IconGreen
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
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StepIndicator(
                    currentStep = addCategoryState.currentStep,
                    totalSteps = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (addCategoryState.currentStep) {
                    1 -> {
                        Text(
                            text = "¿Cómo se llamará la nueva categoría?",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = DarkGreen,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = addCategoryState.categoryName,
                            onValueChange = { viewModel.setCategoryName(it) },
                            label = {
                                Text(
                                    "Nombre de la categoría",
                                    color = TextSecondary
                                )
                            },
                            placeholder = {
                                Text(
                                    "Ej: Animales, Ropa, Juguetes...",
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                            },
                            isError = addCategoryState.nameError != null,
                            supportingText = {
                                if (addCategoryState.nameError != null) {
                                    Text(
                                        addCategoryState.nameError!!,
                                        color = ErrorRed
                                    )
                                } else {
                                    Text(
                                        "Mínimo 2 caracteres, máximo 50",
                                        color = TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenBright,
                                focusedLabelColor = GreenBright,
                                cursorColor = GreenBright,
                                focusedTextColor = TextPrimary,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                unfocusedLabelColor = TextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.goToNextStep() },
                            enabled = addCategoryState.categoryName.isNotBlank() &&
                                    addCategoryState.nameError == null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenBright,
                                contentColor = White,
                                disabledContainerColor = DisabledGray,
                                disabledContentColor = White
                            )
                        ) {
                            Text(
                                "Continuar",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    2 -> {
                        Text(
                            text = "Selecciona una imagen para la categoría",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = DarkGreen,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Esta imagen aparecerá como portada en la pantalla principal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (addCategoryState.categoryImageUri != null)
                                        LightGreenBg
                                    else
                                        LightGreenBg.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (addCategoryState.categoryImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = addCategoryState.categoryImageUri
                                    ),
                                    contentDescription = "Imagen seleccionada",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.clearCategoryImage() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                ErrorRed.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Quitar imagen",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(20.dp)
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
                                        text = "Sin imagen seleccionada",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightGreenBg,
                                contentColor = DarkGreen
                            )
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
                                    "Seleccionar de la galería",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DarkGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.goToNextStep() },
                            enabled = addCategoryState.categoryImageUri != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenBright,
                                contentColor = White,
                                disabledContainerColor = DisabledGray,
                                disabledContentColor = White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Continuar",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        // Mostrar error de imagen si existe
                        addCategoryState.imageError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // ========== PASO 3: CONFIRMAR ==========
                    3 -> {
                        Text(
                            text = "Revisa los datos de la categoría",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = DarkGreen,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tarjeta de resumen - CENTRADA
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = LightGreenBg
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Nombre - CENTRADO
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Nombre:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = addCategoryState.categoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkGreen,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(1.dp),
                                    color = IconGreen.copy(alpha = 0.3f)
                                )

                                // Imagen (vista previa) - CENTRADA
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Imagen de portada:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    addCategoryState.categoryImageUri?.let { uri ->
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(LightGreenBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(model = uri),
                                                contentDescription = "Vista previa",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botones de acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { viewModel.goToPreviousStep() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightGreenBg,
                                    contentColor = DarkGreen
                                )
                            ) {

                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.completeCategory(context) { categoryId ->
                                            navController.navigate("add_pictograms/$categoryId")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    enabled = addCategoryState.categoryName.isNotBlank() &&
                                            addCategoryState.categoryImageUri != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GreenBright,
                                        contentColor = White,
                                        disabledContainerColor = DisabledGray,
                                        disabledContentColor = White
                                    ),
                                    shape = RoundedCornerShape(12.dp)  // Bordes redondeados
                                ) {
                                    Text(
                                        "Crear Categoría",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    // ========== PASO 4: ÉXITO ==========
                    4 -> {
                        // Este paso se maneja automáticamente navegando a AddPictogramsScreen
                    }
                }

                // Mostrar estado de operación
                when (val state = operationState) {
                    is com.pictofly.viewmodel.OperationState.Loading -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = LightGreenBg
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = GreenBright
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    is com.pictofly.viewmodel.OperationState.Success -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = LightGreenBg
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Éxito",
                                    tint = SuccessGreen
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DarkGreen
                                )
                            }
                        }
                    }
                    is com.pictofly.viewmodel.OperationState.Error -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ErrorRed.copy(alpha = 0.1f)
                            )
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
                                    contentDescription = "Error",
                                    tint = ErrorRed
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Paso $currentStep de $totalSteps",
            style = MaterialTheme.typography.labelMedium,
            color = GreenBright
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalSteps) { index ->
                val stepNumber = index + 1
                val isActive = stepNumber == currentStep
                val isCompleted = stepNumber < currentStep

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isActive -> GreenBright
                                isCompleted -> GreenBright.copy(alpha = 0.7f)
                                else -> LightGray
                            }
                        )
                )
            }
        }
    }
}
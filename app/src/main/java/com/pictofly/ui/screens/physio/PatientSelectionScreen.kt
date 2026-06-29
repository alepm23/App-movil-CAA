package com.pictofly.ui.screens.physio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pictofly.ui.theme.PictoFlyTheme
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSelectionScreen(
    initialIsRightHanded: Boolean = true,
    initialHasFullMovement: Boolean = true,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onContinue: (Boolean, Boolean) -> Unit
) {
    PictoFlyTheme {
        var isRightHanded by remember { mutableStateOf<Boolean?>(null) }
        var hasFullThumbMovement by remember { mutableStateOf<Boolean?>(null) }

        val isContinueEnabled = isRightHanded != null && hasFullThumbMovement != null

        Scaffold(
            topBar = {
                if (showBackButton) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Configurar Calibración",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(if (showBackButton) 16.dp else 32.dp))

                    if (!showBackButton) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Configuración del Paciente",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Selecciona ambas opciones para continuar",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Mano Dominante",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                Button(
                                    onClick = { isRightHanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRightHanded == true)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.width(140.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (isRightHanded == true) 8.dp else 4.dp
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "Diestro",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                Button(
                                    onClick = { isRightHanded = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRightHanded == false)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.width(140.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (isRightHanded == false) 8.dp else 4.dp
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "Zurdo",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            if (isRightHanded != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = if (isRightHanded == true)
                                            "Mano diestra seleccionada"
                                        else "Mano zurda seleccionada",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Movimiento del Dedo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // BOTÓN COMPLETO
                                Button(
                                    onClick = { hasFullThumbMovement = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasFullThumbMovement == true)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.width(140.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (hasFullThumbMovement == true) 8.dp else 4.dp
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "Completo",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                // BOTÓN PARCIAL
                                Button(
                                    onClick = { hasFullThumbMovement = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasFullThumbMovement == false)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.width(140.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (hasFullThumbMovement == false) 8.dp else 4.dp
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "Parcial",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            if (hasFullThumbMovement != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = if (hasFullThumbMovement == true)
                                            "Movimiento completo seleccionado"
                                        else "Movimiento parcial seleccionado",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    // BOTONES DE ACCIÓN
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (showBackButton) {
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.width(150.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),  // Gris claro
                                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),    // Texto gris
                                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp,
                                        disabledElevation = 0.dp
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = null
                                ) {
                                    Text(
                                        text = "CANCELAR",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onContinue(isRightHanded!!, hasFullThumbMovement!!)
                                },
                                modifier = Modifier.width(150.dp),
                                enabled = isContinueEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isContinueEnabled)
                                        MaterialTheme.colorScheme.primary  // Verde
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),  // Gris claro
                                    contentColor = if (isContinueEnabled)
                                        MaterialTheme.colorScheme.onPrimary  // Blanco
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),  // Texto gris
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp,
                                    disabledElevation = 0.dp
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = null
                            ) {
                                Text(
                                    text = "CONTINUAR",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // MENSAJE DE INSTRUCCIÓN
                        if (!isContinueEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ Selecciona ambas opciones para continuar",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }   }   }   }   }   }

@Preview(showBackground = true)
@Composable
fun PatientSelectionScreenPreview() {
    PictoFlyTheme {
        PatientSelectionScreen(
            onContinue = { rightHanded, fullMovement -> }
        )
    }
}
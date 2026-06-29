package com.pictofly.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.compose.foundation.background
import com.pictofly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationModeMenuScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Modo de Comunicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // White
                    scrolledContainerColor = MaterialTheme.colorScheme.surface, // White
                    titleContentColor = MaterialTheme.colorScheme.secondary, // DarkGreen
                    navigationIconContentColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // LightGreenBg
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Opción 1: Agregar imágenes
            CommunicationOptionCard(
                title = "Agregar imágenes",
                description = "Añade imágenes desde tu galeria",
                icon = Icons.Default.AddPhotoAlternate,
                titleStyle = MaterialTheme.typography.titleMedium,
                onClick = {
                    navController.navigate("communication/custom_pictograms")
                }
            )

            // Opción 2: Cambiar sujeto
            CommunicationOptionCard(
                title = "Cambiar sujeto",
                description = "Personaliza quién es el sujeto de las frases",
                icon = Icons.Default.Person,
                titleStyle = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
                onClick = {
                    navController.navigate("communication/change_subject")
                }
            )
            // Opción 3: Cambiar verbo
            CommunicationOptionCard(
                title = "Cambiar verbo",
                description = "Modifica las acciones disponibles en las frases",
                icon = Icons.Default.DirectionsRun,
                titleStyle = MaterialTheme.typography.titleMedium,
                onClick = {
                    navController.navigate("communication/change_verb")
                }
            )

            // Información adicional
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // White
                )
            ) {
            }
        }
    }
}

@Composable
fun CommunicationOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono con fondo verde claro
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background), // LightGreenBg
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary, // DarkGreen
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
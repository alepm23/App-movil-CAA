// app/src/main/java/com/pictofly/ui/screens/config/ConfigMenuScreen.kt
package com.pictofly.ui.screens.config

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import com.pictofly.R
import com.pictofly.ui.navigation.NavigationRoutes
import com.pictofly.ui.theme.*
import com.pictofly.viewmodel.ConfigViewModel
import com.pictofly.viewmodel.LogoutViewModel
import com.pictofly.viewmodel.LogoutState
import android.content.Context

data class ConfigOption(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int? = null,
    val emoji: String? = null,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigMenuScreen(
    navController: NavHostController,
    onCloseSession: () -> Unit,
    viewModel: ConfigViewModel = hiltViewModel(),
    logoutViewModel: LogoutViewModel = hiltViewModel() // ✅ AGREGAR LogoutViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val logoutState by logoutViewModel.logoutState.collectAsState()
    val context = LocalContext.current

    // Cargar datos de configuración al iniciar
    LaunchedEffect(Unit) {
        viewModel.loadCurrentConfiguration()
    }

    // ✅ Efecto para manejar el cierre de sesión exitoso
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            // Navegar al splash después del logout exitoso
            navController.navigate(NavigationRoutes.SPLASH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val configOptions = listOf(
        ConfigOption(
            id = "tutorial",
            title = "YouTube",
            description = "Aprende a usar la app",
            iconResId = R.drawable.youtube_icon_logo,
            route = "tutorial"
        ),
//        ConfigOption(
//            id = "communicationMode",
//            title = "Sujetos y Verbos",
//            description = "Agrega o elimina sujetos y verbos",
//            emoji = "✏\uFE0F",
//            route = NavigationRoutes.COMMUNICATION_MODE_MENU
//        ),
        ConfigOption(
            id = "manageContent",
            title = "Agregar categorias",
            description = "Agregar/eliminar pictogramas",
            emoji = "\uD83D\uDDBC\uFE0F",
            route = "manage_content"
        ),
        ConfigOption(
            id = "pictogramSize",
            title = "Tamaño de Pictogramas",
            description = "Ajustar el tamaño de las categorias",
            emoji = "🔍",
            route = NavigationRoutes.PICTOGRAM_SIZE
        ),
        ConfigOption(
            id = "calibration",
            title = "Configurar Calibración",
            description = "Cambiar zurdo/diestro",
            emoji = "🎮",
            route = NavigationRoutes.CALIBRATION_CONFIG_FROM_MENU
        ),
        ConfigOption(
            id = "calibration",
            title = "Configurar Sonido",
            description = "Cambiar el volumen",
            emoji = "\uD83D\uDD0A",
            route = NavigationRoutes.SOUND_CONFIG_FROM_MENU
        ),
        ConfigOption(
            id = "logout",
            title = "Cerrar Sesión",
            description = "Se borrarán todos tus pictogramas y categorías personalizadas",
            emoji = "🚪",
            route = "logout"
        )
    )

    Scaffold(
        containerColor = LightGreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = IconGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White,
                    scrolledContainerColor = White,
                    titleContentColor = DarkGreen,
                    navigationIconContentColor = IconGreen
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightGreenBg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ========== CARD DE CONFIGURACIÓN ACTUAL ==========
                item {
                    CurrentConfigCard(
                        isLoading = uiState.isLoading,
                        soundDb = uiState.soundDb,
                        isRightHanded = uiState.isRightHanded,
                        calibrationSpeed = uiState.calibrationSpeed
                    )
                }

                // ========== OPCIONES DE CONFIGURACIÓN ==========
                items(configOptions) { option ->
                    ConfigOptionCard(
                        option = option,
                        onOptionClick = { route ->
                            when (route) {
                                "tutorial" -> openYouTubeTutorials(context)
                                "manage_content" -> navController.navigate("manage_content")
                                NavigationRoutes.PICTOGRAM_SIZE -> navController.navigate(NavigationRoutes.PICTOGRAM_SIZE)
                                "logout" -> viewModel.showLogoutDialog()
                                else -> navController.navigate(route)
                            }
                        }
                    )
                }

                // Espacio al final
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Dentro de ConfigMenuScreen
            LogoutDialog(
                showDialog = uiState.showLogoutDialog,
                onDismiss = { viewModel.hideLogoutDialog() },
                onConfirm = {
                    viewModel.hideLogoutDialog()
                    logoutViewModel.performLogout(onCloseSession)
                },
                logoutState = logoutState,
                logoutViewModel = logoutViewModel  // ✅ PASAR EL VIEWMODEL
            )
        }
    }
}

@Composable
fun CurrentConfigCard(
    isLoading: Boolean,
    soundDb: Int,
    isRightHanded: Boolean,
    calibrationSpeed: Float
) {
    if (!isLoading) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(GreenBright, CircleShape)
                    )
                    Text(
                        text = "Configuración actual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ConfigDetailRow(
                    label = "Intensidad",
                    value = "${soundDb}dB",
                    icon = "🔊"
                )

                ConfigDetailRow(
                    label = "Lateralidad",
                    value = if (isRightHanded) "Diestro" else "Zurdo",
                    icon = "✋"
                )

                ConfigDetailRow(
                    label = "Velocidad",
                    value = "${String.format("%.1f", calibrationSpeed)}x",
                    icon = "⚡"
                )
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GreenBright
                )
            }
        }
    }
}

@Composable
fun ConfigDetailRow(
    label: String,
    value: String,
    icon: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = DarkGreen
        )
    }
}

@Composable
fun ConfigOptionCard(
    option: ConfigOption,
    onOptionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOptionClick(option.route) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = White,
            contentColor = TextPrimary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(LightGreenBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (option.iconResId != null) {
                    Image(
                        painter = painterResource(id = option.iconResId),
                        contentDescription = option.title,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (option.emoji != null) {
                    Text(
                        text = option.emoji,
                        fontSize = 24.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * ✅ DIÁLOGO DE CIERRE DE SESIÓN MEJORADO
 * Muestra diferentes estados: confirmación, carga, éxito, error
 */
/**
 * ✅ DIÁLOGO DE CIERRE DE SESIÓN CORREGIDO
 */
@Composable
fun LogoutDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    logoutState: LogoutState,
    logoutViewModel: LogoutViewModel
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (logoutState !is LogoutState.Loading) {
                    onDismiss()
                }
            },
            containerColor = White,
            title = {
                Text(
                    text = when (logoutState) {
                        is LogoutState.Loading -> "Cerrando sesión..."
                        is LogoutState.Success -> "¡Sesión cerrada!"
                        is LogoutState.Error -> "Error"
                        else -> "¿Estás seguro de cerrar sesión?"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (logoutState) {
                        is LogoutState.Error -> ErrorRed
                        else -> DarkGreen
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                when (logoutState) {
                    is LogoutState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = GreenBright,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Eliminando todos tus datos personalizados...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is LogoutState.Success -> {
                        Text(
                            text = logoutState.message,  // ✅ Success TIENE message
                            style = MaterialTheme.typography.bodyLarge,
                            color = SuccessGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is LogoutState.Error -> {
                        Text(
                            text = logoutState.message,  // ✅ Error TIENE message
                            style = MaterialTheme.typography.bodyLarge,
                            color = ErrorRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Text(
                            text =
                                    "Se eliminarán TODOS tus pictogramas personalizados, " +
                                    "categorías creadas y configuraciones. " +
                                    "Las categorías predeterminadas volverán a aparecer.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                when (logoutState) {
                    is LogoutState.Loading -> {
                        // No mostrar botones durante carga
                    }
                    is LogoutState.Success -> {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Aceptar",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    is LogoutState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IconGreen,
                                    contentColor = White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Cerrar",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Button(
                                onClick = {
                                    logoutViewModel.resetState()
                                    onConfirm()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenBright,
                                    contentColor = White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Reintentar",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = GreenBright,
                                    contentColor = White
                                )
                            ) {
                                Text(
                                    text = "Cancelar",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Button(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorRed,
                                    contentColor = White
                                )
                            ) {
                                Text(
                                    text = "Aceptar",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = { }
        )
    }
}

fun openYouTubeTutorials(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.youtube.com/watch?v=408Tayyk18s")
    )
    context.startActivity(intent)
}


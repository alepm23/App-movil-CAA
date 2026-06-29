package com.pictofly.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pictofly.ui.screens.config.*
import com.pictofly.ui.screens.config.AddPictogramsScreen
import com.pictofly.ui.screens.consent.ConsentScreen
import com.pictofly.ui.screens.sound.SoundInputScreen
import com.pictofly.ui.screens.sound.SoundConfigScreen
import com.pictofly.ui.screens.physio.PhysioConfigScreen
import com.pictofly.ui.screens.physio.PatientSelectionScreen
import com.pictofly.ui.screens.joystick.JoystickTestScreen
import com.pictofly.ui.screens.main.MainInterfaceScreen
import com.pictofly.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    val appViewModel: AppViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope() //permitira guardar llamar

    val uiState by appViewModel.uiState.collectAsState() //flow entoces serian datos reactivos

    NavHost(
        navController = navController,
        startDestination = if (!uiState.consentShown) NavigationRoutes.SPLASH
        else if (uiState.isConfigured) NavigationRoutes.MAIN_INTERFACE
        else NavigationRoutes.SOUND_CONFIG
    ) {
        composable(NavigationRoutes.SPLASH) {
            SplashScreenPlaceholder(
                onLoadingComplete = {
                    navController.navigate(NavigationRoutes.CONSENT) {
                        popUpTo(NavigationRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavigationRoutes.CONSENT) {
            ConsentScreen(
                onAccept = {
                    coroutineScope.launch {
                        appViewModel.saveConsentShown()
                        if (uiState.isConfigured) {
                            navController.navigate(NavigationRoutes.MAIN_INTERFACE) {
                                popUpTo(NavigationRoutes.CONSENT) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavigationRoutes.SOUND_CONFIG) {
                                popUpTo(NavigationRoutes.CONSENT) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(NavigationRoutes.SOUND_CONFIG) {
            SoundConfigScreen(
                onContinue = {
                    navController.navigate(NavigationRoutes.SOUND_INPUT)
                }
            )
        }

        composable(NavigationRoutes.SOUND_INPUT) {
            val initialHz = if (uiState.soundHz > 0) uiState.soundHz else null
            val initialDb = if (uiState.soundDb > 0) uiState.soundDb else null

            SoundInputScreen(
                initialHz = initialHz,
                initialDb = initialDb,
                onConfirm = { hz, db ->
                    val finalHz = if (hz == 0) 440 else hz
                    navController.navigate("${NavigationRoutes.PHYSIO_CONFIG}/$finalHz/$db")
                }
            )
        }

        composable("${NavigationRoutes.PHYSIO_CONFIG}/{hz}/{db}") { backStackEntry ->
            val hz = backStackEntry.arguments?.getString("hz")?.toIntOrNull() ?: 440
            val db = backStackEntry.arguments?.getString("db")?.toIntOrNull() ?: 0

            PhysioConfigScreen(
                onContinue = {
                    navController.navigate("${NavigationRoutes.PATIENT_SELECTION}/$hz/$db")
                }
            )
        }

        composable("${NavigationRoutes.PATIENT_SELECTION}/{hz}/{db}") { backStackEntry ->
            val hz = backStackEntry.arguments?.getString("hz")?.toIntOrNull() ?: 440
            val db = backStackEntry.arguments?.getString("db")?.toIntOrNull() ?: 0

            PatientSelectionScreen(
                onContinue = { isRightHandedValue: Boolean, hasFullMovementValue: Boolean ->
                    if (!hasFullMovementValue) {
                        navController.navigate("${NavigationRoutes.JOYSTICK_TEST}/$hz/$db/$isRightHandedValue/$hasFullMovementValue")
                    } else {
                        coroutineScope.launch {
                            appViewModel.saveConfiguration(
                                hz = hz,
                                db = db,
                                isRightHanded = isRightHandedValue,
                                hasFullMovement = hasFullMovementValue,
                                calibrationSpeed = 1.0f
                            )
                            navController.navigate(NavigationRoutes.MAIN_INTERFACE) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable("${NavigationRoutes.JOYSTICK_TEST}/{hz}/{db}/{isRightHanded}/{hasFullMovement}") { backStackEntry ->
            val hz = backStackEntry.arguments?.getString("hz")?.toIntOrNull() ?: 440
            val db = backStackEntry.arguments?.getString("db")?.toIntOrNull() ?: 0
            val isRightHandedValue = backStackEntry.arguments?.getString("isRightHanded")?.toBoolean() ?: true
            val hasFullMovementValue = backStackEntry.arguments?.getString("hasFullMovement")?.toBoolean() ?: false

            JoystickTestScreen(
                onComplete = { calibrationResult ->
                    coroutineScope.launch {
                        appViewModel.saveConfiguration(
                            hz = hz,
                            db = db,
                            isRightHanded = isRightHandedValue,
                            hasFullMovement = hasFullMovementValue,
                            calibrationSpeed = calibrationResult.sensitivity
                        )
                        navController.navigate(NavigationRoutes.MAIN_INTERFACE) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                isLeftHanded = !isRightHandedValue,
                showBackButton = true,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavigationRoutes.MAIN_INTERFACE) {
            MainInterfaceScreen(
                navController = navController,
                soundHz = if (uiState.soundHz > 0) uiState.soundHz else 440,
                soundDb = if (uiState.soundDb > 0) uiState.soundDb else 70,
                isLeftHanded = !uiState.isRightHanded,
                calibrationSpeed = uiState.calibrationSpeed,
                onConfigClick = {
                    navController.navigate(NavigationRoutes.CONFIG_MENU)
                }
            )
        }

        composable(NavigationRoutes.CONFIG_MENU) {
            ConfigMenuScreen(
                navController = navController,
                onCloseSession = {
                    coroutineScope.launch {
                        appViewModel.clearAllData()
                        navController.navigate(NavigationRoutes.SPLASH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavigationRoutes.PICTOGRAM_SIZE) {
            PictogramSizeScreen(
                navController = navController
            )
        }

        composable(NavigationRoutes.SOUND_CONFIG_FROM_MENU) {
            val initialHz = if (uiState.soundHz > 0) uiState.soundHz else null
            val initialDb = if (uiState.soundDb > 0) uiState.soundDb else null

            SoundInputScreen(
                initialHz = initialHz,
                initialDb = initialDb,
                onConfirm = { hz, db ->
                    coroutineScope.launch {
                        if (hz > 0 && db > 0) {
                            val currentSettings = appViewModel.getCurrentSettings()
                            appViewModel.saveConfiguration(
                                hz = hz,
                                db = db,
                                isRightHanded = currentSettings.isRightHanded,
                                hasFullMovement = currentSettings.hasFullMovement,
                                calibrationSpeed = currentSettings.calibrationSpeed
                            )
                        }
                        navController.navigateUp()
                    }
                },
                showBackButton = true,
                onBack = { navController.navigateUp() }
            )
        }

        composable(NavigationRoutes.CALIBRATION_CONFIG_FROM_MENU) {
            var currentHz by remember { mutableIntStateOf(0) }
            var currentDb by remember { mutableIntStateOf(0) }
            var currentIsRightHanded by remember { mutableStateOf(true) }
            var currentHasFullMovement by remember { mutableStateOf(true) }
            var currentSpeed by remember { mutableStateOf(1.0f) }

            LaunchedEffect(Unit) {
                val settings = appViewModel.getCurrentSettings()
                currentHz = settings.soundHz
                currentDb = settings.soundDb
                currentIsRightHanded = settings.isRightHanded
                currentHasFullMovement = settings.hasFullMovement
                currentSpeed = settings.calibrationSpeed
            }

            PatientSelectionScreen(
                initialIsRightHanded = currentIsRightHanded,
                initialHasFullMovement = currentHasFullMovement,
                onContinue = { isRightHandedValue: Boolean, hasFullMovementValue: Boolean ->
                    if (!hasFullMovementValue) {
                        navController.navigate("${NavigationRoutes.JOYSTICK_CONFIG_FROM_MENU}/$currentHz/$currentDb/$isRightHandedValue/$hasFullMovementValue")
                    } else {
                        coroutineScope.launch {
                            appViewModel.saveConfiguration(
                                hz = currentHz,
                                db = currentDb,
                                isRightHanded = isRightHandedValue,
                                hasFullMovement = hasFullMovementValue,
                                calibrationSpeed = currentSpeed
                            )
                            navController.navigateUp()
                        }
                    }
                },
                showBackButton = true,
                onBack = { navController.navigateUp() }
            )
        }

        composable("${NavigationRoutes.JOYSTICK_CONFIG_FROM_MENU}/{hz}/{db}/{isRightHanded}/{hasFullMovement}") { backStackEntry ->
            val hz = backStackEntry.arguments?.getString("hz")?.toIntOrNull() ?: 440
            val db = backStackEntry.arguments?.getString("db")?.toIntOrNull() ?: 0
            val isRightHandedValue = backStackEntry.arguments?.getString("isRightHanded")?.toBoolean() ?: true
            val hasFullMovementValue = backStackEntry.arguments?.getString("hasFullMovement")?.toBoolean() ?: false

            JoystickTestScreen(
                onComplete = { calibrationResult ->
                    coroutineScope.launch {
                        appViewModel.saveConfiguration(
                            hz = hz,
                            db = db,
                            isRightHanded = isRightHandedValue,
                            hasFullMovement = hasFullMovementValue,
                            calibrationSpeed = calibrationResult.sensitivity
                        )
                        navController.navigateUp()
                    }
                },
                isLeftHanded = !isRightHandedValue,
                showBackButton = true,
                onBack = { navController.navigateUp() }
            )
        }

        composable("${NavigationRoutes.CATEGORY_DETAIL}/{categoryName}") { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
        }

        composable(NavigationRoutes.MANAGE_CONTENT) {
            ManageLocalContentScreen(
                navController = navController,
                onBack = { navController.navigateUp() }
            )
        }

        composable(NavigationRoutes.ADD_CATEGORY) {
            AddCategoryScreen(
                navController = navController,
                onBack = { navController.navigateUp() }
            )
        }

        composable("${NavigationRoutes.ADD_PICTOGRAMS}/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            AddPictogramsScreen(
                navController = navController,
                categoryId = categoryId,
                onComplete = {
                    navController.navigate(NavigationRoutes.MAIN_INTERFACE) {
                        popUpTo(NavigationRoutes.MAIN_INTERFACE) { inclusive = false }
                    }
                }
            )
        }

        composable(NavigationRoutes.COMMUNICATION_MODE_MENU) {
            CommunicationModeMenuScreen(
                navController = navController,
                onBack = { navController.navigateUp() }
            )
        }

        composable(NavigationRoutes.COMMUNICATION_CUSTOM_PICTOGRAMS) {
            CommunicationCustomPictogramsScreen(
                navController = navController,
                onBack = { navController.navigateUp() }
            )
        }

        composable(NavigationRoutes.COMMUNICATION_CHANGE_SUBJECT) {
            CommunicationSubjectScreen(
                navController = navController,
                onBack = { navController.navigateUp() }
            )
        }

        composable(NavigationRoutes.COMMUNICATION_CHANGE_VERB) {
            CommunicationVerbScreen(
                navController = navController,
                onBack = { navController.navigateUp() }
            )
        }

        composable("${NavigationRoutes.CATEGORY_PICTOGRAMS}/{categoryName}") { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryPictogramsScreen(
                navController = navController,
                categoryName = categoryName,
                onBack = { navController.navigateUp() }
            )
        }
    }
    }

@Composable
fun SplashScreenPlaceholder(onLoadingComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onLoadingComplete()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationPlaceholderScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = "En construcción",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "⏳ Pantalla en desarrollo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
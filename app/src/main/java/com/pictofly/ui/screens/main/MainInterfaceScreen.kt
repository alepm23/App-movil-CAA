package com.pictofly.ui.screens.main

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pictofly.data.model.Category
import com.pictofly.data.model.LocalPictogram
import com.pictofly.data.model.Pictogram
import com.pictofly.ui.navigation.NavigationRoutes
import com.pictofly.ui.screens.joystick.DraggableJoystickButton
import com.pictofly.ui.screens.joystick.rememberDynamicJoystickState
import com.pictofly.viewmodel.CommunicationViewModel
import com.pictofly.viewmodel.MainViewModel
import com.pictofly.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File
import coil.request.CachePolicy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.pictofly.data.model.PictogramSize
import kotlinx.coroutines.delay
import android.speech.tts.TextToSpeech

@Composable
fun MainInterfaceScreen(
    navController: NavController,
    soundHz: Int = 440,
    soundDb: Int = 70,
    isLeftHanded: Boolean = false,
    calibrationSpeed: Float = 1.0f,
    onConfigClick: () -> Unit = {},
    onCategorySelected: (Category) -> Unit = {}
) {
    // ViewModels
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val carouselSize by settingsViewModel.carouselSize.collectAsState()
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsState()

    // ViewModel para comunicación
    val communicationViewModel: CommunicationViewModel = hiltViewModel()

    val selectedSubject by communicationViewModel.selectedSubject
        .collectAsStateWithLifecycle()

    val selectedVerb by communicationViewModel.selectedVerb
        .collectAsStateWithLifecycle()

    // Estado para el predicado seleccionado
    var selectedPredicate by remember { mutableStateOf<LocalPictogram?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // Perfil de calibración desde el estado
    val calibrationProfile = uiState.calibrationProfile

    // Estado dinámico del joystick
    val dynamicState = rememberDynamicJoystickState(calibrationProfile)

    LaunchedEffect(Unit) {
        mainViewModel.initializeAudio(context)
        communicationViewModel.loadCommunicationPictograms()
    }

    LaunchedEffect(selectedSubject, selectedVerb, selectedPredicate) {
        if (selectedSubject != null && selectedVerb != null && selectedPredicate != null) {
            val phrase = "${selectedSubject!!.name} ${selectedVerb!!.name} ${selectedPredicate!!.name}"
            mainViewModel.speakText(phrase)
            delay(3000)
            selectedPredicate = null
            communicationViewModel.clearSelections()
            mainViewModel.speakText("Selecciona sujeto")
        }
    }

    LaunchedEffect(uiState.selectedCategoryIndex) {
        if (uiState.selectedCategoryIndex in uiState.categories.indices) {
            scope.launch {
                delay(50)
                val layoutInfo = lazyListState.layoutInfo
                val selectedItem = layoutInfo.visibleItemsInfo.find {
                    it.index == uiState.selectedCategoryIndex
                }
                if (selectedItem != null) {
                    val itemWidth = selectedItem.size
                    val viewportWidth = layoutInfo.viewportSize.width
                    val centerOffset = (viewportWidth - itemWidth) / 2
                    lazyListState.animateScrollToItem(
                        index = uiState.selectedCategoryIndex,
                        scrollOffset = -centerOffset
                    )
                } else {
                    lazyListState.animateScrollToItem(uiState.selectedCategoryIndex)
                }
            }
        }
    }

    fun handleJoystickMove(movement: Offset) {
        mainViewModel.handleJoystickMove(
            android.graphics.PointF(movement.x, movement.y),
            calibrationSpeed
        )
    }

    fun handleCenterClick() {
        mainViewModel.handleCenterClick()
    }

    if (uiState.showCategoryDetail && uiState.selectedCategory != null) {
        CategoryDetailScreen(
            category = uiState.selectedCategory!!,
            isLeftHanded = isLeftHanded,
            calibrationSpeed = calibrationSpeed,
            onBackClick = {
                mainViewModel.navigateBackFromCategoryDetail()
            },
            onPictogramSelected = { pictogram ->
                val localPictogram = LocalPictogram(
                    id = "predicate_${System.currentTimeMillis()}",
                    categoryId = "predicate_category",
                    name = pictogram.name,
                    imagePath = if (pictogram.isLocal) pictogram.localImagePath else pictogram.imageUrl,
                    soundPath = null,
                    type = "predicate",
                    createdAt = System.currentTimeMillis()
                )
                selectedPredicate = localPictogram
            }
        )
        return
    }

    val defaultImages = mapOf(
        "Yo" to "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653772/yo_orhjd7.png",
        "Quiero" to "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653773/yoquiero_b6e5x0.png"
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        mainViewModel.stopAudio()
                        onConfigClick()
                    },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = Color(0xFF2C3E50),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Frase Armada",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubjectCard(
                            subject = selectedSubject,
                            defaultImageUrl = defaultImages["Yo"]!!,
                            onClick = {
                                navController.navigate(NavigationRoutes.COMMUNICATION_CHANGE_SUBJECT)
                            }
                        )

                        VerbCard(
                            verb = selectedVerb,
                            defaultImageUrl = defaultImages["Quiero"]!!,
                            onClick = {
                                navController.navigate(NavigationRoutes.COMMUNICATION_CHANGE_VERB)
                            }
                        )

                        ObjectCard(
                            predicate = selectedPredicate,
                            onClick = {
                            }
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Categorias",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(uiState.categories) { index, category ->
                            val isSelected = index == uiState.selectedCategoryIndex
                            val baseSize = if (isSelected) 180.dp else 80.dp
                            val size = baseSize * carouselSize.multiplier
                            val baseImageSize = if (isSelected) 100.dp else 50.dp
                            val imageSize = baseImageSize * carouselSize.multiplier

                            CategoryCarouselItem(
                                category = category,
                                isSelected = isSelected,
                                size = size,
                                imageSize = imageSize,
                                pictogramSize = carouselSize,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                onClick = {
                                    when (category.name) {
                                        "Sujeto" -> {
                                            navController.navigate(NavigationRoutes.COMMUNICATION_CHANGE_SUBJECT)
                                        }
                                        "Verbo" -> {
                                            navController.navigate(NavigationRoutes.COMMUNICATION_CHANGE_VERB)
                                        }
                                        else -> {
                                             mainViewModel.handleCenterClick()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = if (isLeftHanded) Alignment.BottomStart else Alignment.BottomEnd
            ) {
                DraggableJoystickButton(
                    buttonSize = 120.dp,
                    baseDeadZone = calibrationProfile.baseDeadZone,
                    baseSensitivity = calibrationProfile.baseSensitivity,
                    baseSmoothing = calibrationProfile.baseSmoothing,
                    dynamicDeadZone = dynamicState.finalDeadZone,
                    dynamicSensitivity = dynamicState.finalSensitivity,
                    dynamicSmoothing = dynamicState.finalSmoothing,
                    isCalibrated = calibrationProfile.isCalibrated,
                    onInputRatioUpdate = { ratio ->
                    },
                    onMove = { movement ->
                        handleJoystickMove(movement)
                    },
                    onCenterClick = {
                        handleCenterClick()
                    },
                    onRelease = {}
                )
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: LocalPictogram?,
    defaultImageUrl: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val communicationViewModel: CommunicationViewModel = hiltViewModel()
    val subjectVersion by communicationViewModel.subjectVersion.collectAsStateWithLifecycle()
    val azulNeon = Color(0xFF00A3FF)

    Card(
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (subject != null) 12.dp else 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (subject != null)
            BorderStroke(2.dp, azulNeon)
        else
            null,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (subject != null) {
                    key("${subject.id}_${subjectVersion}_${System.currentTimeMillis()}") {
                        if (subject.imagePath.startsWith("http")) {
                            AsyncImage(
                                model = subject.imagePath,
                                contentDescription = subject.name,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val imageFile = File(context.filesDir, subject.imagePath)
                            if (imageFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageFile)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = subject.name,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(azulNeon.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = azulNeon
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subject.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulNeon,
                        maxLines = 1
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Seleccionar",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecciona",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "un sujeto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerbCard(
    verb: LocalPictogram?,
    defaultImageUrl: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val communicationViewModel: CommunicationViewModel = hiltViewModel()
    val verbVersion by communicationViewModel.verbVersion.collectAsStateWithLifecycle()
    val azulNeon = Color(0xFF00A3FF)

    Card(
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (verb != null) 12.dp else 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (verb != null)
            BorderStroke(2.dp, azulNeon)
        else
            null,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (verb != null) {
                    key("${verb.id}_${verbVersion}_${System.currentTimeMillis()}") {
                        if (verb.imagePath.startsWith("http")) {
                            AsyncImage(
                                model = verb.imagePath,
                                contentDescription = verb.name,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val imageFile = File(context.filesDir, verb.imagePath)
                            if (imageFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageFile)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = verb.name,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(azulNeon.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = azulNeon
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = verb.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulNeon,
                        maxLines = 1
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Seleccionar",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecciona",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "un verbo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ObjectCard(
    predicate: LocalPictogram? = null,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val azulNeon = Color(0xFF00A3FF)

    Card(
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (predicate != null) 12.dp else 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (predicate != null)
            BorderStroke(2.dp, azulNeon)
        else
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (predicate != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val pictogramForDisplay = Pictogram(
                        name = predicate.name,
                        imageUrl = if (predicate.imagePath.startsWith("http"))
                            predicate.imagePath
                        else
                            "",
                        isLocal = !predicate.imagePath.startsWith("http"),
                        localImagePath = if (!predicate.imagePath.startsWith("http"))
                            predicate.imagePath
                        else
                            ""
                    )

                    AsyncImage(
                        model = pictogramForDisplay.getDisplayImageUrl(),
                        contentDescription = predicate.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = predicate.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulNeon,
                        maxLines = 1
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Seleccionar",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecciona",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "un predicado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCarouselItem(
    category: Category,
    isSelected: Boolean,
    size: Dp,
    imageSize: Dp,
    pictogramSize: PictogramSize,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val azulNeon = Color(0xFF00A3FF)
    val cornerRadius = 10.dp

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            clip = false,
                            ambientColor = azulNeon.copy(alpha = 0.6f),
                            spotColor = azulNeon.copy(alpha = 0.7f)
                        )
                        .shadow(
                            elevation = 15.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            clip = false,
                            ambientColor = azulNeon.copy(alpha = 0.3f),
                            spotColor = azulNeon.copy(alpha = 0.4f)
                        )
                } else {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    )
                }
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(cornerRadius)
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                azulNeon.copy(alpha = 0.6f),
                                azulNeon.copy(alpha = 0.3f),
                                azulNeon.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .padding(8.dp)
        ) {
            AsyncImage(
                model = category.getDisplayImageUrl(),
                contentDescription = category.name,
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(if (isSelected) 8.dp else 4.dp))

            Text(
                text = category.name,
                fontSize = if (isSelected)
                    16.sp * pictogramSize.multiplier
                else
                    12.sp * pictogramSize.multiplier,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF2C3E50) else Color(0xFF666666),
                maxLines = 1
            )
        }
    }
}
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pictofly.data.model.Category
import com.pictofly.data.model.LocalPictogram
import com.pictofly.data.model.Pictogram
import com.pictofly.data.model.PictogramSize
import com.pictofly.ui.screens.joystick.DraggableJoystickButton
import com.pictofly.viewmodel.CategoryDetailViewModel
import com.pictofly.viewmodel.CommunicationViewModel
import com.pictofly.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun CategoryDetailScreen(
    category: Category,
    isLeftHanded: Boolean = false,
    calibrationSpeed: Float = 1.0f,
    onBackClick: () -> Unit,
    onPictogramSelected: (Pictogram) -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val carouselSize by settingsViewModel.carouselSize.collectAsState()

    val communicationViewModel: CommunicationViewModel = hiltViewModel()
    val selectedSubject by communicationViewModel.selectedSubject.collectAsStateWithLifecycle()
    val selectedVerb by communicationViewModel.selectedVerb.collectAsStateWithLifecycle()
    val subjectVersion by communicationViewModel.subjectVersion.collectAsStateWithLifecycle()
    val verbVersion by communicationViewModel.verbVersion.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val categoryDetailViewModel: CategoryDetailViewModel = hiltViewModel()
    val uiState by categoryDetailViewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val defaultSubjectImage = "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653772/yo_orhjd7.png"
    val defaultVerbImage = "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653773/yoquiero_b6e5x0.png"
    val azulNeon = Color(0xFF00A3FF)

    LaunchedEffect(selectedSubject, selectedVerb) {
    }

    LaunchedEffect(category) {
        categoryDetailViewModel.initialize(category)
    }

    if (uiState.shouldNavigateBack) {
        LaunchedEffect(Unit) {
            categoryDetailViewModel.resetBackNavigation()
            onBackClick()
        }
    }

    fun handleJoystickMove(movement: Offset) {
        categoryDetailViewModel.handleJoystickMove(
            android.graphics.PointF(movement.x, movement.y),
            calibrationSpeed
        )
    }

    LaunchedEffect(uiState.selectedPictogramIndex) {
        if (uiState.selectedPictogramIndex in uiState.pictograms.indices) {
            scope.launch {
                delay(50)
                val layoutInfo = lazyListState.layoutInfo
                val selectedItem = layoutInfo.visibleItemsInfo.find {
                    it.index == uiState.selectedPictogramIndex
                }
                if (selectedItem != null) {
                    val itemWidth = selectedItem.size
                    val viewportWidth = layoutInfo.viewportSize.width
                    val centerOffset = (viewportWidth - itemWidth) / 2
                    lazyListState.animateScrollToItem(
                        index = uiState.selectedPictogramIndex,
                        scrollOffset = -centerOffset
                    )
                } else {
                    lazyListState.animateScrollToItem(uiState.selectedPictogramIndex)
                }
            }
        }
    }

    fun handlePictogramClick(pictogram: Pictogram) {
        when (category.name) {
            "Sujeto" -> {
                val localPictogram = LocalPictogram(
                    id = "subject_${System.currentTimeMillis()}",
                    categoryId = "subject_category",
                    name = pictogram.name,
                    imagePath = if (pictogram.isLocal) pictogram.localImagePath else pictogram.imageUrl,
                    soundPath = null,
                    type = "subject",
                    createdAt = System.currentTimeMillis()
                )
                communicationViewModel.selectSubject(localPictogram)
                categoryDetailViewModel.speakText("Sujeto ${pictogram.name} seleccionado")
                onBackClick()
            }
            "Verbo" -> {
                val localPictogram = LocalPictogram(
                    id = "verb_${System.currentTimeMillis()}",
                    categoryId = "verb_category",
                    name = pictogram.name,
                    imagePath = if (pictogram.isLocal) pictogram.localImagePath else pictogram.imageUrl,
                    soundPath = null,
                    type = "verb",
                    createdAt = System.currentTimeMillis()
                )
                communicationViewModel.selectVerb(localPictogram)
                categoryDetailViewModel.speakText("Verbo ${pictogram.name} seleccionado")
                onBackClick()
            }
            else -> {
                categoryDetailViewModel.handlePictogramClick(pictogram)
                onPictogramSelected(pictogram)
            }
        }
    }

    fun handleCenterClick() {
        val selectedPictogram = uiState.selectedPictogram
        selectedPictogram?.let { pictogram ->
            when (category.name) {
                "Sujeto", "Verbo" -> handlePictogramClick(pictogram)
                else -> {
                    categoryDetailViewModel.handleCenterClick()
                    onPictogramSelected(pictogram)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            categoryDetailViewModel.stopAudio()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Frase Armada",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .padding(bottom = 8.dp)
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
                        SubjectDetailCard(
                            subject = selectedSubject,
                            defaultImageUrl = defaultSubjectImage,
                            subjectVersion = subjectVersion,
                            azulNeon = azulNeon
                        )
                        VerbDetailCard(
                            verb = selectedVerb,
                            defaultImageUrl = defaultVerbImage,
                            verbVersion = verbVersion,
                            azulNeon = azulNeon
                        )
                        ObjectDetailCard(
                            pictogram = uiState.selectedPictogramForPhrase
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pictogramas de ${category.name}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(top = 12.dp)
                        .padding(bottom = 8.dp)
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
                        itemsIndexed(uiState.pictograms) { index, pictogram ->
                            val isSelected = index == uiState.selectedPictogramIndex
                            PictogramCarouselItem(
                                pictogram = pictogram,
                                isSelected = isSelected,
                                pictogramSize = carouselSize,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                onClick = {
                                    handlePictogramClick(pictogram)
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
                    onMove = { movement ->
                        handleJoystickMove(movement)
                    },
                    onCenterClick = {
                        handleCenterClick()
                    },
                    onRelease = { }
                )
            }
        }
    }
}

@Composable
fun SubjectDetailCard(
    subject: LocalPictogram?,
    defaultImageUrl: String,
    subjectVersion: Int = 0,
    azulNeon: Color
) {
    val context = LocalContext.current
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
            null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                AsyncImage(
                                    model = defaultImageUrl,
                                    contentDescription = subject.name,
                                    modifier = Modifier.size(60.dp)
                                )
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
fun VerbDetailCard(
    verb: LocalPictogram?,
    defaultImageUrl: String,
    verbVersion: Int = 0,
    azulNeon: Color
) {
    val context = LocalContext.current
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
            null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                AsyncImage(
                                    model = defaultImageUrl,
                                    contentDescription = verb.name,
                                    modifier = Modifier.size(60.dp)
                                )
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
                    }  }
            }
        }
    }
}

@Composable
fun ObjectDetailCard(pictogram: Pictogram?) {
    val azulNeon = Color(0xFF00A3FF)
    Card(
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pictogram != null) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (pictogram != null)
            BorderStroke(2.dp, azulNeon)
        else
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (pictogram != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = pictogram.getDisplayImageUrl(),
                        contentDescription = pictogram.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pictogram.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulNeon,
                        maxLines = 1,
                        textAlign = TextAlign.Center
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
                        text = "un pictograma",
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
fun PictogramCarouselItem(
    pictogram: Pictogram,
    isSelected: Boolean,
    pictogramSize: PictogramSize = PictogramSize.MEDIUM,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val baseSize = if (isSelected) 180.dp else 80.dp
    val scaledSize = baseSize * pictogramSize.multiplier
    val baseImageSize = if (isSelected) 100.dp else 50.dp
    val scaledImageSize = baseImageSize * pictogramSize.multiplier
    val azulNeon = Color(0xFF00A3FF)
    val cornerRadius = 20.dp

    Box(
        modifier = modifier
            .size(scaledSize)
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            clip = false,
                            ambientColor = azulNeon.copy(alpha = 0.7f),
                            spotColor = azulNeon.copy(alpha = 0.8f)
                        )
                        .shadow(
                            elevation = 15.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            clip = false,
                            ambientColor = azulNeon.copy(alpha = 0.4f),
                            spotColor = azulNeon.copy(alpha = 0.5f)
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
            .background(color = Color.White, shape = RoundedCornerShape(cornerRadius))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = azulNeon.copy(alpha = 0.8f),
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
                .background(color = Color.White, shape = RoundedCornerShape(cornerRadius))
                .padding(8.dp)
        ) {
            AsyncImage(
                model = pictogram.getDisplayImageUrl(),
                contentDescription = pictogram.name,
                modifier = Modifier
                    .size(scaledImageSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(if (isSelected) 8.dp else 4.dp))
            Text(
                text = pictogram.name,
                fontSize = if (isSelected) 16.sp * pictogramSize.multiplier else 12.sp * pictogramSize.multiplier,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF2C3E50) else Color(0xFF666666),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}
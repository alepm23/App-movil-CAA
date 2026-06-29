package com.pictofly.ui.screens.main

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pictofly.data.model.Category
import com.pictofly.data.model.LocalPictogram
import com.pictofly.data.model.Pictogram
import com.pictofly.data.model.PictogramSize
import com.pictofly.ui.screens.joystick.DraggableJoystickButton
import com.pictofly.viewmodel.CategoryDetailViewModel
import com.pictofly.viewmodel.CommunicationViewModel
import com.pictofly.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryDetailScreen(
    category: Category,
    isLeftHanded: Boolean = false,
    calibrationSpeed: Float = 1.0f,
    onBackClick: () -> Unit,
    onPictogramSelected: (Pictogram) -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // SEGÚN TUS TESTS: carouselSize emite un objeto PictogramSize (SMALL, MEDIUM, etc.)
    val pictogramSizeState by settingsViewModel.carouselSize.collectAsState()

    // CÁLCULO DEL TAMAÑO CONFIGURADO:
    // Extraemos el valor base (ej. 80) y el multiplicador (ej. 1.3f) del Enum
    val finalConfiguredSize: Dp = remember(pictogramSizeState) {
        val baseValue = pictogramSizeState?.carouselSize?.toFloat() ?: 80f
        val multiplier = pictogramSizeState?.multiplier ?: 1.0f
        (baseValue * multiplier).dp
    }

    val communicationViewModel: CommunicationViewModel = hiltViewModel()
    val selectedSubject by communicationViewModel.selectedSubject.collectAsStateWithLifecycle()
    val selectedVerb by communicationViewModel.selectedVerb.collectAsStateWithLifecycle()
    val selectedPredicate by communicationViewModel.selectedPredicate.collectAsStateWithLifecycle()
    val subjectVersion by communicationViewModel.subjectVersion.collectAsStateWithLifecycle()
    val verbVersion by communicationViewModel.verbVersion.collectAsStateWithLifecycle()

    val categoryDetailViewModel: CategoryDetailViewModel = hiltViewModel()
    val uiState by categoryDetailViewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    val azulNeon = Color(0xFF00A3FF)
    val defaultSubjectImage = "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653772/yo_orhjd7.png"
    val defaultVerbImage = "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653773/yoquiero_b6e5x0.png"

    // Sincronización Dedo -> ViewModel
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo }
            .map { layoutInfo ->
                val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                layoutInfo.visibleItemsInfo
                    .minByOrNull { Math.abs((it.offset + it.size / 2) - viewportCenter) }
                    ?.index
            }
            .distinctUntilChanged()
            .collect { closestIndex ->
                closestIndex?.let { index ->
                    if (lazyListState.isScrollInProgress && uiState.selectedPictogramIndex != index) {
                        categoryDetailViewModel.updateSelectedIndex(index)
                    }
                }
            }
    }

    // Sincronización Joystick -> Scroll
    LaunchedEffect(uiState.selectedPictogramIndex) {
        if (uiState.selectedPictogramIndex in uiState.pictograms.indices && !lazyListState.isScrollInProgress) {
            val layoutInfo = lazyListState.layoutInfo
            val selectedItem = layoutInfo.visibleItemsInfo.find { it.index == uiState.selectedPictogramIndex }

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

    fun handlePictogramClick(pictogram: Pictogram) {
        categoryDetailViewModel.speakText(pictogram.name)
        when (category.name) {
            "Sujeto" -> {
                val local = LocalPictogram("sub_${System.currentTimeMillis()}", "cat_s", pictogram.name,
                    if (pictogram.isLocal) pictogram.localImagePath else pictogram.imageUrl, null, "subject", System.currentTimeMillis())
                communicationViewModel.selectSubject(local)
            }
            "Verbo" -> {
                val local = LocalPictogram("ver_${System.currentTimeMillis()}", "cat_v", pictogram.name,
                    if (pictogram.isLocal) pictogram.localImagePath else pictogram.imageUrl, null, "verb", System.currentTimeMillis())
                communicationViewModel.selectVerb(local)
            }
            else -> onPictogramSelected(pictogram)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 👇 BOTÓN DE RETROCESO AÑADIDO AQUÍ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFF2C3E50),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- PIZARRA ---
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Selecciona un pictograma",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)  // 👈 ESPACIO ABAJO
                )

                Spacer(modifier = Modifier.height(8.dp))  // 👈 O TRA ESPACER ADICIONAL
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF4CAF50), RoundedCornerShape(16.dp)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        SubjectDetailCard(selectedSubject, azulNeon, subjectVersion, defaultSubjectImage)
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        VerbDetailCard(selectedVerb, azulNeon, verbVersion, defaultVerbImage)
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        ObjectDetailCard(selectedPredicate?.let { Pictogram(it.name, it.imagePath, !it.imagePath.startsWith("http"), it.imagePath) })
                    }
                }
            }

            // --- CAROUSEL ---
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Pictogramas de ${category.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

                    // Padding dinámico basado en el tamaño configurado
                    val horizontalPadding = (screenWidth / 2) - (finalConfiguredSize / 2)

                    LazyRow(
                        state = lazyListState,
                        flingBehavior = snapFlingBehavior,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = horizontalPadding)
                    ) {
                        itemsIndexed(uiState.pictograms) { index, pictogram ->
                            val isSelected = index == uiState.selectedPictogramIndex
                            PictogramCarouselItem(
                                pictogram = pictogram,
                                isSelected = isSelected,
                                pictogramSize = finalConfiguredSize,
                                modifier = Modifier.padding(vertical = 8.dp),
                                onClick = { handlePictogramClick(pictogram) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = if (isLeftHanded) Alignment.BottomStart else Alignment.BottomEnd
            ) {
                DraggableJoystickButton(
                    buttonSize = 120.dp,
                    onMove = { handleJoystickMove(it) },
                    onCenterClick = { uiState.selectedPictogram?.let { handlePictogramClick(it) } },
                    onRelease = { }
                )
            }
        }
    }
}

@Composable
fun PictogramCarouselItem(
    pictogram: Pictogram,
    isSelected: Boolean,
    pictogramSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Efecto visual: Agrandar un 30% adicional si está seleccionado
    val scaleFactor = if (isSelected) 1.3f else 1.0f
    val finalSize = pictogramSize * scaleFactor
    val azulNeon = Color(0xFF00A3FF)

    Card(
        modifier = modifier
            .size(finalSize)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 2.dp),
        border = if (isSelected) BorderStroke(3.dp, azulNeon) else BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            AsyncImage(
                model = pictogram.getDisplayImageUrl(),
                contentDescription = pictogram.name,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Text(
                text = pictogram.name,
                // El texto también escala proporcionalmente al tamaño del pictograma
                fontSize = if (isSelected) (pictogramSize.value * 0.16f).sp else (pictogramSize.value * 0.12f).sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// --- CARDS DE LA PIZARRA ---

@Composable
fun SubjectDetailCard(subject: LocalPictogram?, azulNeon: Color, version: Int, defaultImg: String) {
    val context = LocalContext.current
    Card(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (subject != null) {
                AsyncImage(
                    model = if (subject.imagePath.startsWith("http")) subject.imagePath else File(context.filesDir, subject.imagePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                Icon(Icons.Default.Category, null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun VerbDetailCard(verb: LocalPictogram?, azulNeon: Color, version: Int, defaultImg: String) {
    val context = LocalContext.current
    Card(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (verb != null) {
                AsyncImage(
                    model = if (verb.imagePath.startsWith("http")) verb.imagePath else File(context.filesDir, verb.imagePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                Icon(Icons.Default.Category, null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun ObjectDetailCard(pictogram: Pictogram?) {
    val context = LocalContext.current
    Card(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (pictogram != null) {
                AsyncImage(
                    model = if (pictogram.imageUrl.startsWith("http")) pictogram.imageUrl else File(context.filesDir, pictogram.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                Icon(Icons.Default.Category, null, tint = Color.LightGray)
            }
        }
    }
}
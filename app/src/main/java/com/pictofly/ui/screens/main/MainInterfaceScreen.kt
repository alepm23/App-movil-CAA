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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pictofly.data.model.Category
import com.pictofly.data.model.LocalPictogram
import com.pictofly.data.model.Pictogram
import com.pictofly.data.model.PictogramSize
import com.pictofly.ui.navigation.NavigationRoutes
import com.pictofly.ui.screens.joystick.DraggableJoystickButton
import com.pictofly.viewmodel.CommunicationViewModel
import com.pictofly.viewmodel.MainViewModel
import com.pictofly.viewmodel.SettingsViewModel
import com.pictofly.utils.AdaptiveGrammarEngine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
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
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val pictogramSizeState by settingsViewModel.carouselSize.collectAsState()

    // Tamaño configurado (Igual que en CategoryDetail)
    val finalConfiguredSize: Dp = remember(pictogramSizeState) {
        val baseValue = pictogramSizeState?.carouselSize?.toFloat() ?: 80f
        val multiplier = pictogramSizeState?.multiplier ?: 1.0f
        (baseValue * multiplier).dp
    }

    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsState()
    val communicationViewModel: CommunicationViewModel = hiltViewModel()

    val selectedSubject by communicationViewModel.selectedSubject.collectAsStateWithLifecycle()
    val selectedVerb by communicationViewModel.selectedVerb.collectAsStateWithLifecycle()
    val selectedPredicate by communicationViewModel.selectedPredicate.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val adaptiveGrammarEngine = remember { AdaptiveGrammarEngine(context) }
    val lazyListState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    LaunchedEffect(Unit) {
        mainViewModel.setCommunicationViewModel(communicationViewModel)
        mainViewModel.initializeAudio(context)
        communicationViewModel.loadCommunicationPictograms()
    }

    // --- LOGICA DE SINCRONIZACIÓN (IMPORTANTE) ---

    // 1. Dedo/Scroll -> ViewModel: Si mueves la lista, el que queda en el centro se selecciona
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
                // ... dentro del collect
                closestIndex?.let { index ->
                    if (lazyListState.isScrollInProgress && uiState.selectedCategoryIndex != index) {
                        // CAMBIA ESTO:
                        mainViewModel.updateSelectedCategoryIndex(index)
                    }
                }
            }
    }

    // 2. Joystick -> Scroll: Si el joystick cambia el índice, la lista se mueve al centro
    LaunchedEffect(uiState.selectedCategoryIndex) {
        if (uiState.selectedCategoryIndex in uiState.categories.indices && !lazyListState.isScrollInProgress) {
            val layoutInfo = lazyListState.layoutInfo
            val selectedItem = layoutInfo.visibleItemsInfo.find { it.index == uiState.selectedCategoryIndex }

            if (selectedItem != null) {
                val itemWidth = selectedItem.size
                val viewportWidth = layoutInfo.viewportSize.width
                val centerOffset = (viewportWidth - itemWidth) / 2
                lazyListState.animateScrollToItem(uiState.selectedCategoryIndex, -centerOffset)
            } else {
                lazyListState.animateScrollToItem(uiState.selectedCategoryIndex)
            }
        }
    }

    // Lógica de Voz
    LaunchedEffect(selectedSubject, selectedVerb, selectedPredicate) {
        val s = selectedSubject?.name
        val v = selectedVerb?.name
        val p = selectedPredicate?.name
        if (s != null || v != null || p != null) {
            val resultado = adaptiveGrammarEngine.corregirFrase(s, v, p)
            mainViewModel.speakText(if (resultado.fueCorregido) resultado.fraseCorregida else resultado.fraseOriginal)
        }
    }

    // Overlay de Detalle
    if (uiState.showCategoryDetail && uiState.selectedCategory != null) {
        CategoryDetailScreen(
            category = uiState.selectedCategory!!,
            isLeftHanded = isLeftHanded,
            calibrationSpeed = calibrationSpeed,
            onBackClick = { mainViewModel.navigateBackFromCategoryDetail() },
            onPictogramSelected = { pictogram ->
                val local = LocalPictogram("pred_${System.currentTimeMillis()}", "pred_cat", pictogram.name,
                    if (pictogram.isLocal) pictogram.localImagePath else pictogram.imageUrl, null, "predicate", System.currentTimeMillis())
                communicationViewModel.selectPredicate(local)
            }
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Botón Configuración
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { mainViewModel.stopAudio(); onConfigClick() }, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Default.Settings, "Config", tint = Color(0xFF2C3E50), modifier = Modifier.size(30.dp))
                }
            }

// Pizarra (Frase Armada)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Frase Armada", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF4CAF50), RoundedCornerShape(16.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        SubjectCard(selectedSubject) {
                            communicationViewModel.clearSubject()
                        }
                        VerbCard(selectedVerb) {
                            communicationViewModel.clearVerb()
                        }
                        ObjectCard(selectedPredicate) {
                            communicationViewModel.clearPredicate()
                        }
                    }
                }
            }

            // Categorías (Carousel)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Categorías", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val horizontalPadding = (screenWidth / 2) - (finalConfiguredSize / 2)

                    LazyRow(
                        state = lazyListState,
                        flingBehavior = snapFlingBehavior,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = horizontalPadding)
                    ) {
                        itemsIndexed(uiState.categories) { index, category ->
                            val isSelected = index == uiState.selectedCategoryIndex

                            // El tamaño crece si está seleccionado
                            val scaleFactor = if (isSelected) 1.3f else 1.0f
                            val itemSize = finalConfiguredSize * scaleFactor

                            CategoryCarouselItem(
                                category = category,
                                isSelected = isSelected,
                                size = itemSize,
                                imageSize = itemSize * 0.6f,
                                modifier = Modifier.padding(vertical = 8.dp),
                                onClick = {
                                    // CAMBIA ESTO:
                                    mainViewModel.updateSelectedCategoryIndex(index)
                                    mainViewModel.handleCenterClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Joystick
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = if (isLeftHanded) Alignment.BottomStart else Alignment.BottomEnd) {
                DraggableJoystickButton(
                    buttonSize = 120.dp,
                    onMove = { mainViewModel.handleJoystickMove(android.graphics.PointF(it.x, it.y), calibrationSpeed) },
                    onCenterClick = { mainViewModel.handleCenterClick() },
                    onRelease = {}
                )
            }
        }
    }
}

@Composable
fun CategoryCarouselItem(
    category: Category,
    isSelected: Boolean,
    size: Dp,
    imageSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val azulNeon = Color(0xFF00A3FF)
    Card(
        modifier = modifier
            .size(size)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 12.dp else 2.dp),
        border = if (isSelected) BorderStroke(3.dp, azulNeon) else BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.name,
                modifier = Modifier.size(imageSize).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            if (size > 85.dp) {
                Text(
                    text = category.name,
                    fontSize = (size.value * 0.14f).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// --- COMPONENTES DE LA PIZARRA ---
@Composable
fun SubjectCard(
    subject: LocalPictogram?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.size(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (subject != null) {
                AsyncImage(
                    model = if (subject.imagePath.startsWith("http"))
                        subject.imagePath
                    else
                        File(context.filesDir, subject.imagePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                // Cuadro vacío - blanco
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun VerbCard(
    verb: LocalPictogram?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.size(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (verb != null) {
                AsyncImage(
                    model = if (verb.imagePath.startsWith("http"))
                        verb.imagePath
                    else
                        File(context.filesDir, verb.imagePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                // Cuadro vacío - blanco
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun ObjectCard(
    predicate: LocalPictogram?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.size(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (predicate != null) {
                AsyncImage(
                    model = if (predicate.imagePath.startsWith("http"))
                        predicate.imagePath
                    else
                        File(context.filesDir, predicate.imagePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                // Cuadro vacío - blanco
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }
}
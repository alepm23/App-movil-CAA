package com.pictofly.ui.screens.sound

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pictofly.viewmodel.SoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundInputScreen(
    onConfirm: (Int, Int) -> Unit,
    initialHz: Int? = null,
    initialDb: Int? = null,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val soundViewModel: SoundViewModel = hiltViewModel()
    val uiState by soundViewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF4CAF50)
    val lightGreen = Color(0xFFE8F5E9)
    val darkGreen = Color(0xFF2E7D32)
    val accentGreen = Color(0xFF81C784)
    val textColor = Color(0xFF2C3E50)


    LaunchedEffect(Unit) {
        soundViewModel.initializeAudio(context)

        if (initialDb != null && initialDb > 0) {
            soundViewModel.updateDbValue(initialDb.toString())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundViewModel.stopAudio()
        }
    }

    Scaffold(
        topBar = {
            if (showBackButton) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Ajustar Volumen",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkGreen
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = darkGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = lightGreen
                    )
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = lightGreen
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!showBackButton) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "🔊 Ajustar Volumen",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Encuentra el volumen ideal para el niño",
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔊",
                                fontSize = 28.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Intensidad del Sonido",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkGreen
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Rango: 1 - 120 dB",
                                fontSize = 16.sp,
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "Recomendado: 60-80 dB",
                                fontSize = 15.sp,
                                color = primaryGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val progress = uiState.dbValue.toFloatOrNull() ?: 0f
                            val progressWidth = (progress / 120) * 0.95f // 95% del ancho
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressWidth)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        when {
                                            progress < 60 -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                                            progress <= 80 -> Color(0xFF4CAF50).copy(alpha = 0.7f)
                                            else -> Color(0xFFF44336).copy(alpha = 0.7f)
                                        }
                                    )
                            )
                            Text(
                                text = "${uiState.dbValue.toIntOrNull() ?: 0} dB",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 16.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { soundViewModel.changeVolume(-5) },
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentGreen.copy(alpha = 0.8f)
                                ),
                                enabled = (uiState.dbValue.toIntOrNull() ?: 0) > 1 && !uiState.isTestingSound
                            ) {
                                Text(
                                    text = "-5",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            OutlinedTextField(
                                value = uiState.dbValue,
                                onValueChange = { soundViewModel.updateDbValue(it) },
                                label = { Text("dB") },
                                isError = uiState.dbError.isNotEmpty(),
                                supportingText = {
                                    if (uiState.dbError.isNotEmpty()) {
                                        Text(
                                            text = uiState.dbError,
                                            color = Color.Red,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                modifier = Modifier.width(100.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = { soundViewModel.changeVolume(5) },
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryGreen
                                ),
                                enabled = (uiState.dbValue.toIntOrNull() ?: 0) < 120 && !uiState.isTestingSound
                            ) {
                                Text(
                                    text = "+5",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { soundViewModel.testSound() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isTestingSound) Color(0xFFFBC02D) else accentGreen
                            ),
                            enabled = !uiState.isTestingSound && uiState.dbError.isEmpty() &&
                                    (uiState.dbValue.toIntOrNull() ?: 0) >= 1
                        ) {
                            if (uiState.isTestingSound) {
                                Text(
                                    text = "🎵 Probando sonido...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "▶️",
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "PROBAR SONIDO",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (uiState.testMessage.isNotEmpty()) {
                            Text(
                                text = uiState.testMessage,
                                fontSize = 14.sp,
                                color = if (uiState.isTestingSound) Color(0xFFF57C00) else primaryGreen,
                                fontWeight = if (uiState.isTestingSound) FontWeight.Medium else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (uiState.currentVolume > 0) {
                            Text(
                                text = "🔒 Volumen actual: ${uiState.currentVolume} dB",
                                fontSize = 14.sp,
                                color = darkGreen,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "⚠️ Configure un valor de volumen",
                                fontSize = 14.sp,
                                color = Color(0xFFF57C00),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val currentDb = uiState.dbValue.toIntOrNull() ?: 0
                        val hzValue = initialHz ?: 440

                        if (currentDb >= 1) {
                            soundViewModel.saveSoundConfiguration(hzValue, currentDb) {
                                onConfirm(hzValue, currentDb)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryGreen,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    ),
                    enabled = uiState.dbError.isEmpty() && uiState.dbValue.isNotEmpty() &&
                            !uiState.isTestingSound && (uiState.dbValue.toIntOrNull() ?: 0) >= 1
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🔊",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "CONFIRMAR VOLUMEN",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
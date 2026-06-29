package com.pictofly.ui.screens.joystick

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pictofly.ui.theme.*
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.pow
import com.pictofly.data.model.CalibrationProfile
import com.pictofly.viewmodel.AppViewModel

data class UserCalibrationProfile(
    val userId: String = "default",
    val userName: String = "",
    val calibrationDate: Long = System.currentTimeMillis(),
    val xMin: Float = 0f,
    val xMax: Float = 0f,
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val deadZone: Float = 0.15f,
    val sensitivity: Float = 1.0f,
    val smoothingFactor: Float = 0.3f,
    val avgSpeed: Float = 0f,
    val maxAcceleration: Float = 0f,
    val tremorIndex: Float = 0f,
    val stabilityScore: Float = 1.0f,
    val movementPattern: List<Offset> = emptyList(),
    val calibrationVersion: Int = 2
)

data class CalibrationResult(
    val xMin: Float = 0f,
    val xMax: Float = 0f,
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val avgSpeed: Float = 0f,
    val maxAcceleration: Float = 0f,
    val movementPattern: List<Offset> = emptyList(),
    val deadZone: Float = 0.15f,
    val sensitivity: Float = 1.0f,
    val smoothingFactor: Float = 0.3f,
    val tremorIndex: Float = 0f,
    val stabilityScore: Float = 1.0f,
    val normalizedRange: NormalizedRange = NormalizedRange()
)

data class NormalizedRange(
    val xMin: Float = 0f,
    val xMax: Float = 0f,
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val isCalibrated: Boolean = false
)

data class AdaptiveFilterConfig(
    var smoothingFactor: Float = 0.3f,
    var deadZone: Float = 0.15f,
    var sensitivity: Float = 1.0f,
    var noiseGate: Float = 0.02f,
    var predictionEnabled: Boolean = false
)

@Composable
fun rememberJoystickCalibrationState(
    userId: String = "default",
    savedProfile: UserCalibrationProfile? = null
): JoystickCalibrationState {
    return remember {
        JoystickCalibrationState(
            userId = userId,
            savedProfile = savedProfile
        )
    }
}

class JoystickCalibrationState(
    val userId: String = "default",
    savedProfile: UserCalibrationProfile? = null
) {
    var currentStep by mutableStateOf(0)
    var testStarted by mutableStateOf(false)
    var calibrationCompleted by mutableStateOf(false)
    var calibrationResult by mutableStateOf(CalibrationResult())
    var userProfile by mutableStateOf(
        savedProfile ?: UserCalibrationProfile(userId = userId)
    )

    var dynamicXMin by mutableStateOf(Float.POSITIVE_INFINITY)
    var dynamicXMax by mutableStateOf(Float.NEGATIVE_INFINITY)
    var dynamicYMin by mutableStateOf(Float.POSITIVE_INFINITY)
    var dynamicYMax by mutableStateOf(Float.NEGATIVE_INFINITY)
    var movementSamples by mutableStateOf<MutableList<MovementSample>>(mutableListOf())
    private val tremorBufferSize = 100
    var tremorBuffer by mutableStateOf<MutableList<MovementSample>>(mutableListOf())
    var normalizedPosition by mutableStateOf(Offset.Zero)
    var currentSpeed by mutableStateOf(0f)
    var previousSpeed by mutableStateOf(0f)
    var currentAcceleration by mutableStateOf(0f)
    var maxAcceleration by mutableStateOf(0f)
    var smoothedPosition by mutableStateOf(Offset.Zero)
    var rawPosition by mutableStateOf(Offset.Zero)
    var lastProcessTime by mutableStateOf(0L)
    var normalizedRange by mutableStateOf(NormalizedRange())
    var tremorIndex by mutableStateOf(0f)
    var tremorDetected by mutableStateOf(false)
    var tremorSeverity by mutableStateOf(TremorSeverity.NONE)
    var centerZoneSamples by mutableStateOf<List<Offset>>(emptyList())
    var holdStabilityMetrics by mutableStateOf(StabilityMetrics())
    var adaptiveSmoothing by mutableStateOf(0.3f)
    var adaptiveDeadZone by mutableStateOf(0.15f)
    var adaptiveSensitivity by mutableStateOf(1.0f)
    var speedPercentiles by mutableStateOf(PercentileMetrics())
    var accelerationPercentiles by mutableStateOf(PercentileMetrics())
    var holdTimer by mutableIntStateOf(0)
    var isHolding by mutableStateOf(false)
    var isInTargetPosition by mutableStateOf(false)
    var lastMovementTime by mutableStateOf(0L)
    var currentJoystickPosition by mutableStateOf(Offset.Zero)
    var movementHistory by mutableStateOf<List<Offset>>(emptyList())
    var movementTimestamps by mutableStateOf<List<Long>>(emptyList())
    var ttsReady by mutableStateOf(false)
    var ttsError by mutableStateOf(false)
    var tts: TextToSpeech? = null
    var filterConfig by mutableStateOf(AdaptiveFilterConfig())

    val calibrationSteps = listOf(
        "Sigue las instrucciones para calibrar tu movimiento",
        "Mantén 2 segundos en ARRIBA ↑",
        "Mantén 2 segundos en ABAJO ↓",
        "Mantén 2 segundos en IZQUIERDA ←",
        "Mantén 2 segundos en DERECHA →"
    )

    val targetPositions = listOf(
        Offset(0f, 0f),
        Offset(0f, -0.8f),
        Offset(0f, 0.8f),
        Offset(-0.8f, 0f),
        Offset(0.8f, 0f)
    )
}

data class MovementSample(
    val timestamp: Long,
    val rawPosition: Offset,
    val smoothedPosition: Offset,
    val speed: Float,
    val acceleration: Float,
    val isInCenter: Boolean = false
)

data class PercentileMetrics(
    val p25: Float = 0f,
    val p50: Float = 0f,
    val p75: Float = 0f,
    val p90: Float = 0f,
    val p95: Float = 0f,
    val mean: Float = 0f,
    val stdDev: Float = 0f
)

data class StabilityMetrics(
    val meanDeviation: Float = 0f,
    val maxDeviation: Float = 0f,
    val variance: Float = 0f,
    val stdDev: Float = 0f,
    val stabilityScore: Float = 1f,
    val recommendedSmoothing: Float = 0.3f,
    val recommendedDeadZone: Float = 0.15f
)

enum class TremorSeverity {
    NONE,
    MILD,
    MODERATE,
    SEVERE
}

class SignalProcessor {

    companion object {

        fun exponentialSmoothing(
            previous: Offset,
            current: Offset,
            factor: Float
        ): Offset {
            return Offset(
                x = previous.x + (current.x - previous.x) * factor.coerceIn(0.1f, 0.5f),
                y = previous.y + (current.y - previous.y) * factor.coerceIn(0.1f, 0.5f)
            )
        }

        fun calculateSpeedAndAcceleration(
            previousPos: Offset,
            currentPos: Offset,
            previousSpeed: Float,
            deltaTimeMs: Long
        ): Pair<Float, Float> {
            if (deltaTimeMs <= 0) return Pair(0f, 0f)

            val deltaTimeSec = deltaTimeMs / 1000f
            val distance = sqrt(
                (currentPos.x - previousPos.x).pow(2) +
                        (currentPos.y - previousPos.y).pow(2)
            )
            val speed = distance / deltaTimeSec
            val acceleration = if (previousSpeed > 0) {
                (speed - previousSpeed) / deltaTimeSec
            } else 0f

            return Pair(speed, acceleration)
        }

        fun analyzeTremor(
            samples: List<MovementSample>,
            centerRadius: Float = 0.2f
        ): Pair<Float, TremorSeverity> {
            if (samples.size < 10) return Pair(0f, TremorSeverity.NONE)

            val centerSamples = samples.filter { sample ->
                abs(sample.smoothedPosition.x) < centerRadius &&
                        abs(sample.smoothedPosition.y) < centerRadius
            }

            if (centerSamples.size < 5) return Pair(0f, TremorSeverity.NONE)

            val meanX = centerSamples.map { it.smoothedPosition.x }.average().toFloat()
            val meanY = centerSamples.map { it.smoothedPosition.y }.average().toFloat()

            val varianceX = centerSamples.map {
                (it.smoothedPosition.x - meanX).pow(2)
            }.average().toFloat()

            val varianceY = centerSamples.map {
                (it.smoothedPosition.y - meanY).pow(2)
            }.average().toFloat()

            val avgVariance = (varianceX + varianceY) / 2f
            val tremorIndex = sqrt(avgVariance) * 10f

            val directionChanges = detectDirectionChanges(centerSamples)
            val frequencyFactor = directionChanges / centerSamples.size.toFloat()
            val adjustedTremorIndex = tremorIndex * (1 + frequencyFactor)

            val severity = when {
                adjustedTremorIndex < 0.1f -> TremorSeverity.NONE
                adjustedTremorIndex < 0.3f -> TremorSeverity.MILD
                adjustedTremorIndex < 0.6f -> TremorSeverity.MODERATE
                else -> TremorSeverity.SEVERE
            }

            return Pair(adjustedTremorIndex, severity)
        }

        private fun detectDirectionChanges(samples: List<MovementSample>): Int {
            if (samples.size < 3) return 0

            var changes = 0
            var lastDirectionX = 0
            var lastDirectionY = 0

            for (i in 1 until samples.size) {
                val dx = samples[i].smoothedPosition.x - samples[i-1].smoothedPosition.x
                val dy = samples[i].smoothedPosition.y - samples[i-1].smoothedPosition.y

                val currentDirectionX = if (dx > 0) 1 else if (dx < 0) -1 else 0
                val currentDirectionY = if (dy > 0) 1 else if (dy < 0) -1 else 0

                if (lastDirectionX != 0 && currentDirectionX != 0 && currentDirectionX != lastDirectionX) {
                    changes++
                }
                if (lastDirectionY != 0 && currentDirectionY != 0 && currentDirectionY != lastDirectionY) {
                    changes++
                }

                lastDirectionX = currentDirectionX
                lastDirectionY = currentDirectionY
            }

            return changes
        }

        fun analyzeHoldStability(
            holdSamples: List<MovementSample>,
            targetPosition: Offset,
            holdDuration: Long
        ): StabilityMetrics {
            if (holdSamples.size < 2) {
                return StabilityMetrics()
            }

            val deviations = holdSamples.map { sample ->
                sqrt(
                    (sample.smoothedPosition.x - targetPosition.x).pow(2) +
                            (sample.smoothedPosition.y - targetPosition.y).pow(2)
                )
            }

            val meanDeviation = deviations.average().toFloat()
            val maxDeviation = deviations.maxOrNull() ?: 0f
            val variance = deviations.map {
                (it - meanDeviation).pow(2)
            }.average().toFloat()
            val stdDev = sqrt(variance)

            val stabilityScore = when {
                meanDeviation < 0.05f -> 1.0f
                meanDeviation < 0.1f -> 0.8f
                meanDeviation < 0.2f -> 0.6f
                meanDeviation < 0.3f -> 0.4f
                else -> 0.2f
            }.coerceIn(0.2f, 1.0f)

            val recommendedSmoothing = when {
                stdDev > 0.15f -> 0.4f
                stdDev > 0.08f -> 0.35f
                else -> 0.25f
            }.coerceIn(0.2f, 0.5f)

            val recommendedDeadZone = when {
                meanDeviation > 0.2f -> 0.25f
                meanDeviation > 0.1f -> 0.2f
                else -> 0.15f
            }.coerceIn(0.12f, 0.3f)

            return StabilityMetrics(
                meanDeviation = meanDeviation,
                maxDeviation = maxDeviation,
                variance = variance,
                stdDev = stdDev,
                stabilityScore = stabilityScore,
                recommendedSmoothing = recommendedSmoothing,
                recommendedDeadZone = recommendedDeadZone
            )
        }

        fun calculatePercentiles(values: List<Float>): PercentileMetrics {
            if (values.isEmpty()) return PercentileMetrics()

            val sorted = values.sorted()
            val size = sorted.size

            fun percentile(p: Float): Float {
                val index = (p * (size - 1)).toInt()
                return sorted[index.coerceIn(0, size - 1)]
            }

            val mean = values.average().toFloat()
            val variance = values.map { (it - mean).pow(2) }.average().toFloat()
            val stdDev = sqrt(variance)

            return PercentileMetrics(
                p25 = percentile(0.25f),
                p50 = percentile(0.5f),
                p75 = percentile(0.75f),
                p90 = percentile(0.9f),
                p95 = percentile(0.95f),
                mean = mean,
                stdDev = stdDev
            )
        }

        fun normalizePosition(
            rawPosition: Offset,
            xMin: Float,
            xMax: Float,
            yMin: Float,
            yMax: Float
        ): Offset {
            val xRange = if (xMax - xMin > 0.001f) xMax - xMin else 1f
            val yRange = if (yMax - yMin > 0.001f) yMax - yMin else 1f

            val normalizedX = (rawPosition.x - xMin) / xRange
            val normalizedY = (rawPosition.y - yMin) / yRange

            val scaledX = (normalizedX * 2) - 1
            val scaledY = (normalizedY * 2) - 1

            return Offset(
                x = scaledX.coerceIn(-1f, 1f),
                y = scaledY.coerceIn(-1f, 1f)
            )
        }
    }
}

fun calculateCalibrationMetrics(
    movementSamples: List<MovementSample>,
    userId: String = "default"
): UserCalibrationProfile {
    if (movementSamples.size < 2) {
        return UserCalibrationProfile(userId = userId)
    }

    val xValues = movementSamples.map { it.rawPosition.x }
    val yValues = movementSamples.map { it.rawPosition.y }
    val xMin = xValues.minOrNull() ?: 0f
    val xMax = xValues.maxOrNull() ?: 0f
    val yMin = yValues.minOrNull() ?: 0f
    val yMax = yValues.maxOrNull() ?: 0f
    val xRange = xMax - xMin
    val yRange = yMax - yMin
    val speeds = movementSamples.mapNotNull { it.speed }
    val accelerations = movementSamples.mapNotNull { it.acceleration }
    val avgSpeed = if (speeds.isNotEmpty()) speeds.average().toFloat() else 0f
    val maxAcceleration = if (accelerations.isNotEmpty())
        accelerations.maxOrNull() ?: 0f else 0f
    val speedPercentiles = SignalProcessor.calculatePercentiles(speeds)
    val accelPercentiles = SignalProcessor.calculatePercentiles(accelerations)
    val speedP75 = speedPercentiles.p75
    val speedP50 = speedPercentiles.p50
    val accelP90 = accelPercentiles.p90
    val (tremorIndex, tremorSeverity) = SignalProcessor.analyzeTremor(movementSamples)
    val deadZone = when {
        tremorSeverity == TremorSeverity.SEVERE -> 0.28f
        tremorSeverity == TremorSeverity.MODERATE -> 0.24f
        tremorSeverity == TremorSeverity.MILD -> 0.19f
        speedP75 < 0.3f -> 0.16f
        speedP75 > 1.5f -> 0.22f
        else -> 0.15f
    }.coerceIn(0.12f, 0.3f)

    val rangeEfficiency = ((xRange + yRange) / 2f).coerceIn(0.3f, 1.2f)

    val sensitivity = when {
        speedP50 < 0.4f -> 1.4f
        speedP50 > 1.8f -> 0.75f
        else -> 1.0f
    } * (1.2f / rangeEfficiency)

    val smoothingFactor = when {
        accelP90 > 3.0f -> 0.48f
        accelP90 > 2.0f -> 0.42f
        accelP90 > 1.0f -> 0.36f
        tremorSeverity >= TremorSeverity.MODERATE -> 0.4f
        else -> 0.28f
    }.coerceIn(0.2f, 0.5f)

    val stabilityScore = when (tremorSeverity) {
        TremorSeverity.NONE -> 0.9f
        TremorSeverity.MILD -> 0.7f
        TremorSeverity.MODERATE -> 0.5f
        TremorSeverity.SEVERE -> 0.3f
    }

    return UserCalibrationProfile(
        userId = userId,
        calibrationDate = System.currentTimeMillis(),
        xMin = xMin,
        xMax = xMax,
        yMin = yMin,
        yMax = yMax,
        deadZone = deadZone,
        sensitivity = sensitivity,
        smoothingFactor = smoothingFactor,
        avgSpeed = avgSpeed,
        maxAcceleration = maxAcceleration,
        tremorIndex = tremorIndex,
        stabilityScore = stabilityScore,
        movementPattern = movementSamples.map { it.rawPosition },
        calibrationVersion = 2
    )
}

@Composable
fun JoystickTestScreen(
    onComplete: (CalibrationResult) -> Unit,
    isLeftHanded: Boolean = false,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    userId: String = "default",
    savedProfile: UserCalibrationProfile? = null
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val savedLeftHanded = prefs.getBoolean("is_left_handed", false)

    // Usar la preferencia guardada si existe, sino usar el parámetro
    val finalLeftHanded = if (savedLeftHanded) savedLeftHanded else isLeftHanded

    // Guardar inmediatamente si se pasó isLeftHanded como true
    LaunchedEffect(isLeftHanded) {
        if (isLeftHanded) {
            prefs.edit().putBoolean("is_left_handed", true).apply()
        }
    }

    val state = rememberJoystickCalibrationState(
        userId = userId,
        savedProfile = savedProfile
    )

    LaunchedEffect(savedProfile) {
        savedProfile?.let { profile ->
            state.userProfile = profile
            state.adaptiveDeadZone = profile.deadZone
            state.adaptiveSmoothing = profile.smoothingFactor
            state.adaptiveSensitivity = profile.sensitivity
            state.filterConfig = AdaptiveFilterConfig(
                smoothingFactor = profile.smoothingFactor,
                deadZone = profile.deadZone,
                sensitivity = profile.sensitivity
            )
        }
    }

    val samplingFlow = remember {
        MutableSharedFlow<Offset>(extraBufferCapacity = 100)
    }

    LaunchedEffect(state.testStarted) {
        if (state.testStarted) {
            samplingFlow.collect { movement ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - state.lastMovementTime >= 50) {
                    processMovementSample(state, movement, currentTime)
                    state.lastMovementTime = currentTime
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            state.tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = state.tts?.setLanguage(Locale("es", "ES"))
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        state.ttsError = true
                    } else {
                        state.tts?.setSpeechRate(0.9f)
                        state.tts?.setPitch(1.1f)
                        state.ttsReady = true
                    }
                } else {
                    state.ttsError = true
                }
            }
        } catch (e: Exception) {
            state.ttsError = true
        }
    }

    fun speakInstruction(step: Int) {
        if (state.ttsReady && step in 1..4) {
            val text = when (step) {
                1 -> "Mantén dos segundos arriba"
                2 -> "Mantén dos segundos abajo"
                3 -> "Mantén dos segundos a la izquierda"
                4 -> "Mantén dos segundos a la derecha"
                else -> ""
            }
            state.tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step_$step")
        }
    }

    fun resetCalibration() {
        state.testStarted = false
        state.calibrationCompleted = false
        state.currentStep = 0
        state.movementSamples.clear()
        state.tremorBuffer.clear()
        state.centerZoneSamples = emptyList()
        state.holdTimer = 0
        state.isHolding = false
        state.isInTargetPosition = false
        state.currentJoystickPosition = Offset.Zero
        state.smoothedPosition = Offset.Zero
        state.rawPosition = Offset.Zero
        state.normalizedPosition = Offset.Zero
        state.currentSpeed = 0f
        state.previousSpeed = 0f
        state.currentAcceleration = 0f
        state.maxAcceleration = 0f
        state.dynamicXMin = Float.POSITIVE_INFINITY
        state.dynamicXMax = Float.NEGATIVE_INFINITY
        state.dynamicYMin = Float.POSITIVE_INFINITY
        state.dynamicYMax = Float.NEGATIVE_INFINITY
    }

    fun checkTargetPosition(currentPos: Offset): Boolean {
        if (state.currentStep !in state.targetPositions.indices) return false

        val targetPos = state.targetPositions[state.currentStep]

        val tolerance = when (state.tremorSeverity) {
            TremorSeverity.SEVERE -> 0.45f
            TremorSeverity.MODERATE -> 0.4f
            TremorSeverity.MILD -> 0.35f
            else -> 0.3f
        }

        return abs(currentPos.x - targetPos.x) < tolerance &&
                abs(currentPos.y - targetPos.y) < tolerance
    }

    fun advanceToNextStep() {
        if (state.currentStep in 1..3) {
            state.currentStep++
            speakInstruction(state.currentStep)
            state.isHolding = false
            state.isInTargetPosition = false
            state.holdTimer = 0
            state.currentJoystickPosition = Offset.Zero
        } else if (state.currentStep == 4) {
            completeCalibration(state)
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (showBackButton && onBack != null) {
                        IconButton(
                            onClick = { onBack() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = IconGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎮 Calibración de Joystick",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Text(
                            text = if (finalLeftHanded) "Mano zurda" else "Mano diestra",
                            fontSize = 12.sp,
                            color = DarkGreen.copy(alpha = 0.7f)
                        )
                        if (savedProfile != null) {
                            Text(
                                text = "Perfil: ${savedProfile.userName.takeIf { it.isNotEmpty() } ?: "Cargado"}",
                                fontSize = 10.sp,
                                color = InfoBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instrucción
            InstructionCard(
                currentStep = state.currentStep,
                calibrationSteps = state.calibrationSteps,
                isInTargetPosition = state.isInTargetPosition,
                tremorSeverity = state.tremorSeverity
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visualizador
            MovementVisualizerCard(
                currentJoystickPosition = if (state.filterConfig.smoothingFactor > 0)
                    state.smoothedPosition else state.normalizedPosition,
                rawPosition = state.rawPosition,
                movementHistory = state.movementSamples.map { it.rawPosition },
                testStarted = state.testStarted,
                currentStep = state.currentStep,
                isInTargetPosition = state.isInTargetPosition,
                tremorDetected = state.tremorDetected,
                tremorSeverity = state.tremorSeverity,
                normalizedRange = state.normalizedRange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hold progress
            if (state.currentStep in 1..4) {
                HoldProgressIndicator(
                    holdTimer = state.holdTimer,
                    stabilityScore = state.holdStabilityMetrics.stabilityScore,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Control area
            if (!state.calibrationCompleted) {
                ControlArea(
                    testStarted = state.testStarted,
                    isLeftHanded = finalLeftHanded,
                    onStartTest = {
                        state.testStarted = true
                        state.currentStep = 1
                        state.movementSamples.clear()
                        state.tremorBuffer.clear()
                        speakInstruction(1)
                    },
                    onReset = { resetCalibration() },
                    onJoystickMove = { movement ->
                        samplingFlow.tryEmit(movement)
                    },
                    onJoystickRelease = {
                        // Vacío intencional - manejado en processMovementSample
                    }
                )
            }

            // Resultados
            if (state.calibrationCompleted) {
                CalibrationResults(
                    calibrationData = state.calibrationResult,
                    userProfile = state.userProfile,
                    onComplete = {
                        onComplete(state.calibrationResult)
                    },
                    onRepeat = { resetCalibration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    prefs = prefs,
                    finalLeftHanded = finalLeftHanded
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Hold timer effect
    HoldTimerEffect(state)

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            state.tts?.stop()
            state.tts?.shutdown()
        }
    }
}

private fun processMovementSample(
    state: JoystickCalibrationState,
    movement: Offset,
    currentTime: Long
) {
    if (!state.testStarted) return

    state.rawPosition = movement

    val smoothingFactor = state.adaptiveSmoothing.coerceIn(0.1f, 0.5f)

    state.smoothedPosition = if (state.movementSamples.isEmpty()) {
        movement
    } else {
        SignalProcessor.exponentialSmoothing(
            previous = state.smoothedPosition,
            current = movement,
            factor = smoothingFactor
        )
    }

    val positionToUse = if (state.normalizedRange.isCalibrated) {
        SignalProcessor.normalizePosition(
            rawPosition = movement,
            xMin = state.normalizedRange.xMin,
            xMax = state.normalizedRange.xMax,
            yMin = state.normalizedRange.yMin,
            yMax = state.normalizedRange.yMax
        ).also {
            state.normalizedPosition = it
        }
    } else {
        movement.also {
            state.normalizedPosition = it
        }
    }

    state.currentJoystickPosition = positionToUse
    state.movementHistory = state.movementHistory + listOf(positionToUse)
    state.movementTimestamps = state.movementTimestamps + listOf(currentTime)

    val lastSample = state.movementSamples.lastOrNull()
    val deltaTime = lastSample?.let { currentTime - it.timestamp } ?: 0L

    val (speed, acceleration) = if (lastSample != null) {
        SignalProcessor.calculateSpeedAndAcceleration(
            previousPos = lastSample.rawPosition,
            currentPos = movement,
            previousSpeed = state.currentSpeed,
            deltaTimeMs = deltaTime
        )
    } else {
        Pair(0f, 0f)
    }

    state.previousSpeed = state.currentSpeed
    state.currentSpeed = speed
    state.currentAcceleration = acceleration
    state.maxAcceleration = maxOf(state.maxAcceleration, abs(acceleration))

    if (movement.x < state.dynamicXMin) state.dynamicXMin = movement.x
    if (movement.x > state.dynamicXMax) state.dynamicXMax = movement.x
    if (movement.y < state.dynamicYMin) state.dynamicYMin = movement.y
    if (movement.y > state.dynamicYMax) state.dynamicYMax = movement.y

    val isInCenter = abs(state.smoothedPosition.x) < 0.25f &&
            abs(state.smoothedPosition.y) < 0.25f

    val sample = MovementSample(
        timestamp = currentTime,
        rawPosition = movement,
        smoothedPosition = state.smoothedPosition,
        speed = speed,
        acceleration = acceleration,
        isInCenter = isInCenter
    )

    state.movementSamples.add(sample)

    state.tremorBuffer.add(sample)
    if (state.tremorBuffer.size > 100) {
        state.tremorBuffer.removeAt(0)
    }

    if (state.tremorBuffer.size >= 20 && state.movementSamples.size % 10 == 0) {
        val (tremorIdx, severity) = SignalProcessor.analyzeTremor(state.tremorBuffer)
        state.tremorIndex = tremorIdx
        state.tremorSeverity = severity
        state.tremorDetected = severity != TremorSeverity.NONE
        state.adaptiveDeadZone = when (severity) {
            TremorSeverity.SEVERE -> 0.28f
            TremorSeverity.MODERATE -> 0.24f
            TremorSeverity.MILD -> 0.19f
            TremorSeverity.NONE -> {
                if (state.adaptiveDeadZone > 0.18f) 0.16f else 0.14f
            }
        }.coerceIn(0.12f, 0.3f)

        state.filterConfig.deadZone = state.adaptiveDeadZone
    }

    val isNowInTarget = checkTargetPosition(state, positionToUse)

    if (isNowInTarget && !state.isInTargetPosition) {
        state.isHolding = true
        state.isInTargetPosition = true
        state.holdStabilityMetrics = StabilityMetrics()
    } else if (!isNowInTarget && state.isInTargetPosition) {
        state.isHolding = false
        state.holdTimer = 0
        state.isInTargetPosition = false
    }
}

private fun checkTargetPosition(
    state: JoystickCalibrationState,
    currentPos: Offset
): Boolean {
    if (state.currentStep !in state.targetPositions.indices) return false

    val targetPos = state.targetPositions[state.currentStep]

    val tolerance = when (state.tremorSeverity) {
        TremorSeverity.SEVERE -> 0.45f
        TremorSeverity.MODERATE -> 0.4f
        TremorSeverity.MILD -> 0.35f
        else -> 0.3f
    }

    return abs(currentPos.x - targetPos.x) < tolerance &&
            abs(currentPos.y - targetPos.y) < tolerance
}

private fun completeCalibration(state: JoystickCalibrationState) {
    val profile = calculateCalibrationMetrics(
        movementSamples = state.movementSamples,
        userId = state.userId
    )

    state.userProfile = profile
    state.calibrationCompleted = true
    state.normalizedRange = NormalizedRange(
        xMin = profile.xMin,
        xMax = profile.xMax,
        yMin = profile.yMin,
        yMax = profile.yMax,
        isCalibrated = true
    )

    state.calibrationResult = CalibrationResult(
        xMin = profile.xMin,
        xMax = profile.xMax,
        yMin = profile.yMin,
        yMax = profile.yMax,
        avgSpeed = profile.avgSpeed,
        maxAcceleration = profile.maxAcceleration,
        movementPattern = profile.movementPattern,
        deadZone = profile.deadZone,
        sensitivity = profile.sensitivity,
        smoothingFactor = profile.smoothingFactor,
        tremorIndex = profile.tremorIndex,
        stabilityScore = profile.stabilityScore,
        normalizedRange = state.normalizedRange
    )
}

@Composable
private fun HoldTimerEffect(state: JoystickCalibrationState) {
    val holdId = remember(state.isHolding, state.currentStep) {
        UUID.randomUUID().toString()
    }

    LaunchedEffect(holdId) {
        if (state.isHolding && state.currentStep in 1..4) {
            state.holdTimer = 0

            val holdSamples = mutableListOf<MovementSample>()
            val targetPosition = state.targetPositions[state.currentStep]

            while (state.isHolding && state.holdTimer < 2000 && state.isInTargetPosition) {
                delay(50)
                state.holdTimer += 50

                if (state.movementSamples.isNotEmpty()) {
                    holdSamples.add(state.movementSamples.last())
                }
            }

            if (holdSamples.size >= 5) {
                val stabilityMetrics = SignalProcessor.analyzeHoldStability(
                    holdSamples = holdSamples,
                    targetPosition = targetPosition,
                    holdDuration = state.holdTimer.toLong()
                )

                state.holdStabilityMetrics = stabilityMetrics

                if (stabilityMetrics.stabilityScore < 0.6f) {
                    state.adaptiveSmoothing = (state.adaptiveSmoothing + 0.05f)
                        .coerceIn(0.2f, 0.5f)
                    state.adaptiveDeadZone = (state.adaptiveDeadZone + 0.02f)
                        .coerceIn(0.15f, 0.3f)
                    state.filterConfig.smoothingFactor = state.adaptiveSmoothing
                    state.filterConfig.deadZone = state.adaptiveDeadZone
                }
            }

            if (state.holdTimer >= 2000 && state.isHolding && state.isInTargetPosition) {
                advanceToNextStep(state)
            }
        }
    }
}

private fun advanceToNextStep(state: JoystickCalibrationState) {
    if (state.currentStep in 1..3) {
        state.currentStep++
        state.isHolding = false
        state.isInTargetPosition = false
        state.holdTimer = 0
        state.currentJoystickPosition = Offset.Zero
    } else if (state.currentStep == 4) {
        completeCalibration(state)
    }
}

@Composable
private fun InstructionCard(
    currentStep: Int,
    calibrationSteps: List<String>,
    isInTargetPosition: Boolean,
    tremorSeverity: TremorSeverity = TremorSeverity.NONE
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentStep > 0) {
                Text(
                    text = "PASO ${currentStep.coerceAtLeast(1)} de 4",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InfoBlue,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = calibrationSteps[currentStep],
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = DarkGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (tremorSeverity != TremorSeverity.NONE) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (tremorSeverity) {
                            TremorSeverity.MILD -> WarningOrange.copy(alpha = 0.2f)
                            TremorSeverity.MODERATE -> WarningOrange.copy(alpha = 0.4f)
                            TremorSeverity.SEVERE -> Color.Red.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    )
                ) {
                    Text(
                        text = "⚡ Temblor ${tremorSeverity.name.lowercase()} detectado - Ajustando...",
                        fontSize = 12.sp,
                        color = when (tremorSeverity) {
                            TremorSeverity.MILD -> WarningOrange
                            TremorSeverity.MODERATE -> WarningOrange
                            TremorSeverity.SEVERE -> Color.Red
                            else -> Color.Transparent
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            if (currentStep in 1..4) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (currentStep) {
                        1 -> "⬆️ ARRIBA"
                        2 -> "⬇️ ABAJO"
                        3 -> "⬅️ IZQUIERDA"
                        4 -> "➡️ DERECHA"
                        else -> ""
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isInTargetPosition) SuccessGreen else InfoBlue
                )
            }
        }
    }
}

@Composable
private fun MovementVisualizerCard(
    currentJoystickPosition: Offset,
    rawPosition: Offset,
    movementHistory: List<Offset>,
    testStarted: Boolean,
    currentStep: Int,
    isInTargetPosition: Boolean,
    tremorDetected: Boolean,
    tremorSeverity: TremorSeverity,
    normalizedRange: NormalizedRange
) {
    Card(
        modifier = Modifier
            .size(300.dp)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EnhancedMovementVisualizer(
                modifier = Modifier.fillMaxSize(),
                currentPosition = currentJoystickPosition,
                rawPosition = rawPosition,
                movementHistory = movementHistory,
                isActive = testStarted,
                tremorDetected = tremorDetected,
                tremorSeverity = tremorSeverity,
                normalizedRange = normalizedRange
            )

            if (testStarted && currentStep in 1..4) {
                Box(
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInTargetPosition)
                                SuccessGreen.copy(alpha = 0.9f)
                            else WarningOrange.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = if (isInTargetPosition) "✓ EN POSICIÓN" else "→ MOVER AQUÍ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (normalizedRange.isCalibrated) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = InfoBlue.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "📊 Rango adaptado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedMovementVisualizer(
    modifier: Modifier = Modifier,
    currentPosition: Offset,
    rawPosition: Offset,
    movementHistory: List<Offset>,
    isActive: Boolean = true,
    tremorDetected: Boolean = false,
    tremorSeverity: TremorSeverity = TremorSeverity.NONE,
    normalizedRange: NormalizedRange = NormalizedRange()
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .border(2.dp, LightGreenBg),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = LightGreenBg.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1f
            )
            drawLine(
                color = LightGreenBg.copy(alpha = 0.3f),
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 1f
            )

            val deadZoneRadius = if (normalizedRange.isCalibrated) 0.15f else 0.12f
            drawCircle(
                color = LightGreenBg.copy(alpha = 0.1f),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.minDimension / 2 * deadZoneRadius,
                style = Stroke(width = 2f)
            )

            if (movementHistory.isNotEmpty()) {
                for (i in 1 until movementHistory.size) {
                    val prev = movementHistory[i - 1]
                    val current = movementHistory[i]

                    val prevPoint = Offset(
                        size.width / 2 + prev.x * size.width / 2,
                        size.height / 2 + prev.y * size.height / 2
                    )
                    val currentPoint = Offset(
                        size.width / 2 + current.x * size.width / 2,
                        size.height / 2 + current.y * size.height / 2
                    )

                    drawLine(
                        color = InfoBlue.copy(alpha = 0.2f),
                        start = prevPoint,
                        end = currentPoint,
                        strokeWidth = 1.5f
                    )
                }
            }

            if (isActive) {
                val currentPoint = Offset(
                    size.width / 2 + currentPosition.x * size.width / 2,
                    size.height / 2 + currentPosition.y * size.height / 2
                )

                val circleColor = when {
                    tremorSeverity == TremorSeverity.SEVERE -> Color.Red
                    tremorSeverity == TremorSeverity.MODERATE -> WarningOrange
                    tremorSeverity == TremorSeverity.MILD -> WarningOrange.copy(alpha = 0.8f)
                    else -> GreenBright
                }

                drawCircle(
                    color = circleColor,
                    center = currentPoint,
                    radius = 10f
                )

                drawCircle(
                    color = DarkGreen,
                    center = currentPoint,
                    radius = 10f,
                    style = Stroke(width = 2f)
                )

                val rawPoint = Offset(
                    size.width / 2 + rawPosition.x * size.width / 2,
                    size.height / 2 + rawPosition.y * size.height / 2
                )

                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    center = rawPoint,
                    radius = 4f
                )
            }
        }

        Text(
            text = "↑",
            modifier = Modifier.align(Alignment.TopCenter),
            color = DarkGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "↓",
            modifier = Modifier.align(Alignment.BottomCenter),
            color = DarkGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "←",
            modifier = Modifier.align(Alignment.CenterStart),
            color = DarkGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "→",
            modifier = Modifier.align(Alignment.CenterEnd),
            color = DarkGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HoldProgressIndicator(
    holdTimer: Int,
    stabilityScore: Float = 1f,
    modifier: Modifier = Modifier
) {
    val progress = holdTimer / 2000f
    val isComplete = progress >= 1f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isComplete) "¡PERFECTO! ✅"
                    else "MANTEN PRESIONADO: ${holdTimer/1000}s",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isComplete) SuccessGreen else DarkGreen
                )

                if (stabilityScore < 0.7f && !isComplete) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = WarningOrange.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "⚠️ Inestable",
                            fontSize = 12.sp,
                            color = WarningOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = when {
                    isComplete -> SuccessGreen
                    stabilityScore < 0.5f -> WarningOrange
                    else -> InfoBlue
                },
                trackColor = LightGreenBg.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = DarkGreen.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ControlArea(
    testStarted: Boolean,
    isLeftHanded: Boolean,
    onStartTest: () -> Unit,
    onReset: () -> Unit,
    onJoystickMove: (Offset) -> Unit,
    onJoystickRelease: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLeftHanded) {
                    JoystickElement(onJoystickMove, onJoystickRelease)
                    ActionButton(testStarted, onStartTest, onReset)
                } else {
                    ActionButton(testStarted, onStartTest, onReset)
                    JoystickElement(onJoystickMove, onJoystickRelease)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(testStarted: Boolean, onStartTest: () -> Unit, onReset: () -> Unit) {
    if (!testStarted) {
        Button(
            onClick = onStartTest,
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
            modifier = Modifier.width(150.dp)
        ){
            Text("COMENZAR", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange, contentColor = Color.White),
            modifier = Modifier.width(150.dp)
        ){
            Text("REINICIAR", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JoystickElement(onJoystickMove: (Offset) -> Unit, onJoystickRelease: () -> Unit) {
    DraggableJoystickButton(
        buttonSize = 120.dp,
        onMove = onJoystickMove,
        onRelease = onJoystickRelease,
        modifier = Modifier
    )
}

@Composable
private fun CalibrationResults(
    calibrationData: CalibrationResult,
    userProfile: UserCalibrationProfile,
    onComplete: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    prefs: android.content.SharedPreferences,
    finalLeftHanded: Boolean
) {
    val appViewModel: AppViewModel = hiltViewModel()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Calibración Completada!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            EnhancedCalibrationMetrics(
                profile = userProfile,
                result = calibrationData
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        // Guardar la preferencia de lateralidad
                        prefs.edit().putBoolean("is_left_handed", finalLeftHanded).apply()

                        val calibrationProfile = CalibrationProfile(
                            userId = userProfile.userId,
                            lastCalibrationDate = System.currentTimeMillis(),
                            xMin = userProfile.xMin,
                            xMax = userProfile.xMax,
                            yMin = userProfile.yMin,
                            yMax = userProfile.yMax,
                            baseDeadZone = userProfile.deadZone,
                            baseSensitivity = userProfile.sensitivity,
                            baseSmoothing = userProfile.smoothingFactor,
                            avgSpeed = userProfile.avgSpeed,
                            maxAcceleration = userProfile.maxAcceleration,
                            tremorIndex = userProfile.tremorIndex,
                            stabilityScore = userProfile.stabilityScore,
                            isCalibrated = true,
                            calibrationVersion = 3
                        )

                        appViewModel.saveCalibrationProfile(calibrationProfile)
                        onComplete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = White
                    ),
                    modifier = Modifier.width(140.dp)
                ) {
                    Text("FINALIZAR", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRepeat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InfoBlue,
                        contentColor = White
                    ),
                    modifier = Modifier.width(140.dp)
                ) {
                    Text("REPETIR", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EnhancedCalibrationMetrics(
    profile: UserCalibrationProfile,
    result: CalibrationResult
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LightGreenBg.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚙️ CONFIGURACIÓN ADAPTATIVA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                MetricRow("Zona muerta:", "${"%.2f".format(profile.deadZone)}")
                MetricRow("Sensibilidad:", "${"%.2f".format(profile.sensitivity)}")
                MetricRow("Suavizado:", "${"%.2f".format(profile.smoothingFactor)}")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LightGreenBg.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "RANGO ADAPTADO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                MetricRow("Rango X:", "${"%.2f".format(profile.xMin)} a ${"%.2f".format(profile.xMax)}")
                MetricRow("Rango Y:", "${"%.2f".format(profile.yMin)} a ${"%.2f".format(profile.yMax)}")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LightGreenBg.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "📊 ANÁLISIS DE MOVIMIENTO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                MetricRow("Velocidad prom.:", "${"%.2f".format(profile.avgSpeed)} u/s")
                MetricRow("Aceleración máx.:", "${"%.2f".format(profile.maxAcceleration)} u/s²")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Temblor:", color = TextSecondary, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val tremorColor = when {
                            profile.tremorIndex < 0.1f -> SuccessGreen
                            profile.tremorIndex < 0.3f -> WarningOrange
                            else -> Color.Red
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    tremorColor,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${"%.2f".format(profile.tremorIndex)} - ${
                                when {
                                    profile.tremorIndex < 0.1f -> "Mínimo"
                                    profile.tremorIndex < 0.3f -> "Moderado"
                                    else -> "Severo"
                                }
                            }",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                MetricRow("Estabilidad:", "${"%.0f".format(profile.stabilityScore * 100)}%")
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
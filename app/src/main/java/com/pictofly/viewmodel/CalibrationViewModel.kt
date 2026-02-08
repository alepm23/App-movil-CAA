package com.pictofly.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.CalibrationResult
import com.pictofly.repository.CalibrationRepository
import com.pictofly.ui.screens.joystick.MovementSample
import com.pictofly.ui.screens.joystick.TremorSeverity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Serializable
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.pow

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationRepository: CalibrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private val calibrationSteps = listOf(
        "Sigue las instrucciones para calibrar tu movimiento",
        "Mantén 3 segundos en ARRIBA ↑",
        "Mantén 3 segundos en ABAJO ↓",
        "Mantén 3 segundos en IZQUIERDA ←",
        "Mantén 3 segundos en DERECHA →"
    )

    private val targetPositions = listOf(
        Offset(0f, 0f),
        Offset(0f, -0.8f),
        Offset(0f, 0.8f),
        Offset(-0.8f, 0f),
        Offset(0.8f, 0f)
    )

    private var movementDataHistory: MutableList<MovementData> = mutableListOf()

    data class MovementData(
        val position: Offset,
        val timestamp: Long,
        val speed: Float,
        val acceleration: Float
    )

    fun startCalibration() {
        movementDataHistory.clear()

        _uiState.update {
            it.copy(
                testStarted = true,
                currentStep = 1,
                movementHistory = emptyList(),
                movementTimestamps = emptyList(),
                holdTimer = 0,
                isHolding = false,
                isInTargetPosition = false,
                currentJoystickPosition = Offset.Zero,
                currentSpeed = 0f,
                acceleration = 0f,
                tremorLevel = 0f,
                stability = 1f
            )
        }
    }

    fun handleJoystickMove(movement: Offset) {
        val currentTime = System.currentTimeMillis()
        val state = _uiState.value

        if (currentTime - state.lastMovementTime < 50) return

        val timeDiff = (currentTime - state.lastMovementTime).toFloat() / 1000f
        val lastPosition = state.currentJoystickPosition

        val distance = if (timeDiff > 0) {
            sqrt(
                (movement.x - lastPosition.x).pow(2) +
                        (movement.y - lastPosition.y).pow(2)
            )
        } else 0f

        val speed = if (timeDiff > 0) distance / timeDiff else 0f

        val acceleration = if (timeDiff > 0 && state.lastMovementTime > 0) {
            abs(speed - state.currentSpeed) / timeDiff
        } else 0f

        val recentMovements = state.movementHistory.takeLast(8)
        val tremorLevel = calculateTremorLevel(recentMovements + movement)
        val stability = calculateStability(movement, recentMovements)

        movementDataHistory.add(
            MovementData(
                position = movement,
                timestamp = currentTime,
                speed = speed,
                acceleration = acceleration
            )
        )

        val updatedHistory = state.movementHistory + listOf(movement)
        val updatedTimestamps = state.movementTimestamps + listOf(currentTime)
        val isNowInTarget = checkTargetPosition(movement, state.currentStep)

        _uiState.update {
            it.copy(
                currentJoystickPosition = movement,
                movementHistory = updatedHistory,
                movementTimestamps = updatedTimestamps,
                lastMovementTime = currentTime,
                isInTargetPosition = isNowInTarget,
                currentSpeed = speed,
                acceleration = acceleration,
                tremorLevel = tremorLevel,
                stability = stability,
                isHolding = if (isNowInTarget && !state.isInTargetPosition) {
                    startHoldTimer()
                    true
                } else if (!isNowInTarget && state.isInTargetPosition) {
                    resetHoldTimer()
                    false
                } else {
                    state.isHolding
                }
            )
        }
    }

    fun handleJoystickRelease() {
        val state = _uiState.value
        if (!state.testStarted || state.calibrationCompleted) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - state.lastMovementTime < 300) return

        val isNearCenter = abs(state.currentJoystickPosition.x) < 0.3f &&
                abs(state.currentJoystickPosition.y) < 0.3f

        if (isNearCenter && state.currentStep == 0 && !state.testStarted) {
            startCalibration()
        }

        movementDataHistory.add(
            MovementData(
                position = Offset.Zero,
                timestamp = currentTime,
                speed = 0f,
                acceleration = 0f
            )
        )
    }

    fun handleCenterClick() {
        val state = _uiState.value
        if (!state.testStarted && state.currentStep == 0) {
            startCalibration()
        }
    }

    fun resetCalibration() {
        movementDataHistory.clear()
        _uiState.update {
            CalibrationUiState(
                isLeftHanded = it.isLeftHanded,
                ttsError = it.ttsError
            )
        }
    }

    fun completeCalibration(): CalibrationResult {
        val samples = convertToMovementSamples(movementDataHistory)

        val profile = calibrationRepository.calculateCalibrationMetrics(
            movementSamples = samples,
            userId = "default"
        )

        val result = CalibrationResult(
            xMin = profile.xMin,
            xMax = profile.xMax,
            yMin = profile.yMin,
            yMax = profile.yMax,
            avgSpeed = profile.avgSpeed,
            maxForce = profile.maxAcceleration,
            movementPattern = emptyList(),
            deadZone = profile.baseDeadZone,
            sensitivity = profile.baseSensitivity
        )

        _uiState.update {
            it.copy(
                calibrationCompleted = true,
                calibrationResult = result
            )
        }

        return result
    }

    private fun checkTargetPosition(currentPos: Offset, step: Int): Boolean {
        if (step !in targetPositions.indices) return false
        val targetPos = targetPositions[step]
        val tolerance = 0.3f
        return abs(currentPos.x - targetPos.x) < tolerance &&
                abs(currentPos.y - targetPos.y) < tolerance
    }

    private fun startHoldTimer() {
        viewModelScope.launch {
            val step = _uiState.value.currentStep
            var timer = 0

            while (_uiState.value.isHolding &&
                _uiState.value.isInTargetPosition &&
                _uiState.value.currentStep == step) {

                delay(100)
                timer += 100
                _uiState.update { it.copy(holdTimer = timer) }

                if (timer >= 3000) {
                    advanceToNextStep()
                    break
                }
            }
        }
    }

    private fun resetHoldTimer() {
        _uiState.update { it.copy(holdTimer = 0, isHolding = false) }
    }

    private fun advanceToNextStep() {
        _uiState.update { currentState ->
            if (currentState.currentStep in 1..3) {
                currentState.copy(
                    currentStep = currentState.currentStep + 1,
                    isHolding = false,
                    isInTargetPosition = false,
                    holdTimer = 0,
                    currentJoystickPosition = Offset.Zero,
                    currentSpeed = 0f,
                    acceleration = 0f
                )
            } else if (currentState.currentStep == 4) {
                val samples = convertToMovementSamples(movementDataHistory)

                val profile = calibrationRepository.calculateCalibrationMetrics(
                    movementSamples = samples,
                    userId = "default"
                )

                val result = CalibrationResult(
                    xMin = profile.xMin,
                    xMax = profile.xMax,
                    yMin = profile.yMin,
                    yMax = profile.yMax,
                    avgSpeed = profile.avgSpeed,
                    maxForce = profile.maxAcceleration,
                    movementPattern = emptyList(),
                    deadZone = profile.baseDeadZone,
                    sensitivity = profile.baseSensitivity
                )

                currentState.copy(
                    calibrationCompleted = true,
                    calibrationResult = result
                )
            } else {
                currentState
            }
        }
    }

    fun updateTtsStatus(isReady: Boolean, hasError: Boolean) {
        _uiState.update {
            it.copy(
                isTtsReady = isReady,
                ttsError = hasError
            )
        }
    }

    fun setIsLeftHanded(isLeftHanded: Boolean) {
        _uiState.update {
            it.copy(isLeftHanded = isLeftHanded)
        }
    }

    private fun convertToMovementSamples(dataHistory: List<MovementData>): List<MovementSample> {
        return dataHistory.map { data ->
            MovementSample(
                timestamp = data.timestamp,
                rawPosition = data.position,
                smoothedPosition = data.position,
                speed = data.speed,
                acceleration = data.acceleration,
                isInCenter = abs(data.position.x) < 0.25f && abs(data.position.y) < 0.25f
            )
        }
    }

    private fun calculateTremorLevel(movements: List<Offset>): Float {
        if (movements.size < 5) return 0f
        val directionChanges = movements.windowed(2).sumByDouble { (prev, curr) ->
            val anglePrev = atan2(prev.y.toDouble(), prev.x.toDouble())
            val angleCurr = atan2(curr.y.toDouble(), curr.x.toDouble())
            val diff = abs(angleCurr - anglePrev)
            minOf(diff, 2 * PI - diff)
        }
        val avgChange = (directionChanges / (movements.size - 1)).toFloat()
        return (avgChange / PI.toFloat()).coerceIn(0f, 1f)
    }

    private fun calculateStability(currentPos: Offset, recentMovements: List<Offset>): Float {
        if (recentMovements.size < 3) return 1.0f
        val recent = recentMovements.takeLast(3) + listOf(currentPos)
        val avgX = recent.map { it.x }.average().toFloat()
        val avgY = recent.map { it.y }.average().toFloat()
        val variance = recent.fold(0.0) { acc, offset ->
            val dx = offset.x - avgX
            val dy = offset.y - avgY
            acc + (dx * dx + dy * dy)
        } / recent.size
        return (1.0f - (variance.toFloat() * 2f)).coerceIn(0f, 1f)
    }

    data class MovementSummary(
        val totalPoints: Int,
        val avgSpeed: Float,
        val maxSpeed: Float,
        val tremorLevel: Float,
        val stability: Float
    ) : Serializable

    fun getMovementSummary(): MovementSummary {
        val avgSpeed = if (movementDataHistory.isNotEmpty()) {
            movementDataHistory.map { it.speed }.average().toFloat()
        } else 0f
        val maxSpeed = movementDataHistory.maxOfOrNull { it.speed } ?: 0f
        return MovementSummary(
            totalPoints = movementDataHistory.size,
            avgSpeed = avgSpeed,
            maxSpeed = maxSpeed,
            tremorLevel = _uiState.value.tremorLevel,
            stability = _uiState.value.stability
        )
    }
}

data class CalibrationUiState(
    val currentStep: Int = 0,
    val testStarted: Boolean = false,
    val calibrationCompleted: Boolean = false,
    val currentJoystickPosition: Offset = Offset.Zero,
    val movementHistory: List<Offset> = emptyList(),
    val movementTimestamps: List<Long> = emptyList(),
    val holdTimer: Int = 0,
    val isHolding: Boolean = false,
    val isInTargetPosition: Boolean = false,
    val lastMovementTime: Long = 0L,
    val isLeftHanded: Boolean = false,
    val isTtsReady: Boolean = false,
    val ttsError: Boolean = false,
    val calibrationResult: CalibrationResult? = null,
    val currentSpeed: Float = 0f,
    val acceleration: Float = 0f,
    val tremorLevel: Float = 0f,
    val stability: Float = 1f
) {
    val currentInstruction: String
        get() = when {
            !testStarted && currentStep == 0 -> "Sigue las instrucciones para calibrar tu movimiento"
            currentStep in 1..4 -> listOf(
                "Mantén 3 segundos en ARRIBA ↑",
                "Mantén 3 segundos en ABAJO ↓",
                "Mantén 3 segundos en IZQUIERDA ←",
                "Mantén 3 segundos en DERECHA →"
            )[currentStep - 1]
            else -> ""
        }

    val targetDirection: String
        get() = when (currentStep) {
            1 -> "ARRIBA ↑"
            2 -> "ABAJO ↓"
            3 -> "IZQUIERDA ←"
            4 -> "DERECHA →"
            else -> ""
        }

    val holdProgress: Float
        get() = holdTimer / 3000f

    val totalProgress: Float
        get() = if (testStarted && !calibrationCompleted) {
            (currentStep - 1) / 4f
        } else 0f
}
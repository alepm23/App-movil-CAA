package com.pictofly.ui.screens.joystick

import androidx.compose.runtime.*
import com.pictofly.data.model.CalibrationProfile
import com.pictofly.utils.DynamicAdaptationManager

@Composable
fun rememberDynamicJoystickState(
    calibrationProfile: CalibrationProfile
): DynamicJoystickState {
    return remember(calibrationProfile) {
        DynamicJoystickState(calibrationProfile)
    }
}

class DynamicJoystickState(
    private val calibrationProfile: CalibrationProfile
) {
    private val adaptationManager = DynamicAdaptationManager()
    var finalDeadZone by mutableStateOf(calibrationProfile.baseDeadZone)
        private set

    var finalSensitivity by mutableStateOf(calibrationProfile.baseSensitivity)
        private set

    var finalSmoothing by mutableStateOf(calibrationProfile.baseSmoothing)
        private set

    var currentTremorIndex by mutableStateOf(calibrationProfile.tremorIndex)
        private set

    var currentStabilityScore by mutableStateOf(calibrationProfile.stabilityScore)
        private set

    var lastAdaptationTime by mutableStateOf(System.currentTimeMillis())
        private set

    init {
        adaptationManager.initialize(
            baseTremor = calibrationProfile.tremorIndex,
            baseStability = calibrationProfile.stabilityScore
        )
        updateFinalValues()
    }

    fun updateMetrics(
        tremorIndex: Float,
        stabilityScore: Float,
        inputRatio: Float,
        currentTime: Long
    ) {
        currentTremorIndex = tremorIndex
        currentStabilityScore = stabilityScore
        adaptationManager.updateMaxInputRatio(inputRatio)

        if (adaptationManager.evaluateAndAdapt(
                currentTremorIndex = tremorIndex,
                currentStabilityScore = stabilityScore,
                currentTimeMs = currentTime
            )
        ) {
            updateFinalValues()
            lastAdaptationTime = currentTime
        }
    }

    private fun updateFinalValues() {
        val (deadZone, sensitivity, smoothing) = adaptationManager.getFinalValues(
            baseDeadZone = calibrationProfile.baseDeadZone,
            baseSensitivity = calibrationProfile.baseSensitivity,
            baseSmoothing = calibrationProfile.baseSmoothing
        )

        finalDeadZone = deadZone
        finalSensitivity = sensitivity
        finalSmoothing = smoothing
    }

    fun resetDynamicValues() {
        adaptationManager.resetDynamicValues()
        updateFinalValues()
    }

    fun calculateInputRatio(rawPosition: androidx.compose.ui.geometry.Offset): Float {
        val distance = kotlin.math.sqrt(rawPosition.x * rawPosition.x + rawPosition.y * rawPosition.y)
        return distance.coerceIn(0f, 1f)
    }
}
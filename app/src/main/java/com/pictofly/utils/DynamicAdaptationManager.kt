package com.pictofly.utils

import kotlin.math.min
import kotlin.math.max

class DynamicAdaptationManager {

    var dynamicDeadZone: Float = 0f
        private set

    var dynamicSensitivity: Float = 1f
        private set

    var dynamicSmoothing: Float = 0f
        private set

    private val MAX_DEADZONE_DELTA = 0.05f   // ±0.05
    private val MIN_SENSITIVITY = 0.85f      // 0.85 - 1.15
    private val MAX_SENSITIVITY = 1.15f
    private val MIN_SMOOTHING_DELTA = -0.1f  // -0.1 a +0.15
    private val MAX_SMOOTHING_DELTA = 0.15f
    private var sessionStartTime = System.currentTimeMillis()
    private var lastAdaptationTime = sessionStartTime
    private var maxInputRatio: Float = 0f
    private var baseTremorIndex: Float = 0f
    private var baseStabilityScore: Float = 1f

    fun initialize(baseTremor: Float, baseStability: Float) {
        baseTremorIndex = baseTremor
        baseStabilityScore = baseStability
        resetDynamicValues()
    }

    fun resetDynamicValues() {
        dynamicDeadZone = 0f
        dynamicSensitivity = 1f
        dynamicSmoothing = 0f
        maxInputRatio = 0f
    }

    fun updateMaxInputRatio(ratio: Float) {
        if (ratio > maxInputRatio) {
            maxInputRatio = min(ratio, 1f)
        }
    }

    fun evaluateAndAdapt(
        currentTremorIndex: Float,
        currentStabilityScore: Float,
        currentTimeMs: Long
    ): Boolean {
        if (currentTimeMs - lastAdaptationTime < 30_000) {
            return false
        }

        var adapted = false

        val sessionDuration = (currentTimeMs - sessionStartTime) / 1000f
        if (sessionDuration > 120 && maxInputRatio < 0.7f) {
            dynamicSensitivity = min(dynamicSensitivity + 0.02f, MAX_SENSITIVITY)
            adapted = true
        }

        if (baseTremorIndex > 0) {
            val tremorIncrease = (currentTremorIndex - baseTremorIndex) / baseTremorIndex
            if (tremorIncrease > 0.3f) {
                dynamicSmoothing = min(dynamicSmoothing + 0.02f, MAX_SMOOTHING_DELTA)
                dynamicDeadZone = min(dynamicDeadZone + 0.01f, MAX_DEADZONE_DELTA)
                adapted = true
            }
        }

        if (currentStabilityScore > baseStabilityScore + 0.2f) {
            dynamicSmoothing = max(dynamicSmoothing - 0.01f, MIN_SMOOTHING_DELTA)
            adapted = true
        }

        lastAdaptationTime = currentTimeMs
        return adapted
    }

    fun getFinalValues(baseDeadZone: Float, baseSensitivity: Float, baseSmoothing: Float): Triple<Float, Float, Float> {
        val finalDeadZone = (baseDeadZone + dynamicDeadZone).coerceIn(0.1f, 0.35f)
        val finalSensitivity = (baseSensitivity * dynamicSensitivity).coerceIn(0.6f, 1.5f)
        val finalSmoothing = (baseSmoothing + dynamicSmoothing).coerceIn(0.15f, 0.55f)

        return Triple(finalDeadZone, finalSensitivity, finalSmoothing)
    }
}
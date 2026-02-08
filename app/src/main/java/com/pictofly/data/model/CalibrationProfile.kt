package com.pictofly.data.model

import androidx.compose.ui.geometry.Offset

data class CalibrationProfile(
    val userId: String = "default_user",  // ahora si vamos a poder identificar al usuario que hizo la calibracion
    val lastCalibrationDate: Long = System.currentTimeMillis(),
    val xMin: Float = 0f,
    val xMax: Float = 0f,
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val baseDeadZone: Float = 0.15f,
    val baseSensitivity: Float = 1.0f,
    val baseSmoothing: Float = 0.3f, //suavizado
    val avgSpeed: Float = 0f,
    val maxAcceleration: Float = 0f,
    val tremorIndex: Float = 0f,
    val stabilityScore: Float = 1.0f,
    val isCalibrated: Boolean = false,
    val calibrationVersion: Int = 3
) {
    fun toNormalizedRange(): NormalizedRange = NormalizedRange(
        xMin = xMin,
        xMax = xMax,
        yMin = yMin,
        yMax = yMax,
        isCalibrated = isCalibrated
    )
}

data class NormalizedRange(
    val xMin: Float = 0f,
    val xMax: Float = 0f,
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val isCalibrated: Boolean = false
)
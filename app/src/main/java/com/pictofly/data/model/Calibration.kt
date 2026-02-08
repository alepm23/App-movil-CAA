package com.pictofly.data.model

import androidx.compose.ui.geometry.Offset

data class CalibrationResult(
    val xMin: Float = 0f,
    val xMax: Float = 0f,
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val avgSpeed: Float = 0f,
    val maxForce: Float = 0f,
    val movementPattern: List<Offset> = emptyList(),
    val deadZone: Float = 0.15f,
    val sensitivity: Float = 1.0f
)

data class CalibrationData(
    val movementHistory: List<Offset> = emptyList(),
    val movementTimestamps: List<Long> = emptyList(),
    val currentStep: Int = 0,  //paso actual
    val holdTimer: Int = 0,      //T presionando
    val isHolding: Boolean = false // P S/N
)
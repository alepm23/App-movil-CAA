package com.pictofly.repository

import androidx.compose.ui.geometry.Offset
import com.pictofly.data.local.CalibrationDataSource
import com.pictofly.data.model.CalibrationProfile
import com.pictofly.ui.screens.joystick.MovementSample
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

enum class TremorSeverity {
    NONE,
    MILD,//leve
    MODERATE,
    SEVERE
}
//solo vamos a organizar los datos para mandar a la UI
@Singleton
class CalibrationRepository @Inject constructor(
    private val calibrationDataSource: CalibrationDataSource
) {
    fun getCalibrationProfile(): Flow<CalibrationProfile> =
        calibrationDataSource.calibrationProfile //observamos
    suspend fun saveCalibrationProfile(profile: CalibrationProfile) {
        calibrationDataSource.saveCalibrationProfile(profile)
    }
    suspend fun clearCalibrationProfile() {
        calibrationDataSource.clearCalibrationData()
    }
    suspend fun getCurrentCalibrationProfile(): CalibrationProfile =
        calibrationDataSource.getCurrentCalibrationProfile()
    suspend fun hasCalibration(): Boolean =
        calibrationDataSource.hasCalibration()

    fun calculateCalibrationMetrics(
        movementSamples: List<MovementSample>,
        userId: String = "default_user" //si no llega a pasar usaremos default
    ): CalibrationProfile {
        if (movementSamples.size < 2) {
            return CalibrationProfile(userId = userId, isCalibrated = false)
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
        val speedPercentiles = calculatePercentiles(speeds)
        val accelPercentiles = calculatePercentiles(accelerations)
        val speedP75 = speedPercentiles.p75
        val speedP50 = speedPercentiles.p50
        val accelP90 = accelPercentiles.p90
        val (tremorIndex, tremorSeverity) = analyzeTremor(movementSamples) //aca la magnitud y severidad
        val deadZone = when {
            tremorSeverity == TremorSeverity.SEVERE -> 0.28f
            tremorSeverity == TremorSeverity.MODERATE -> 0.24f
            tremorSeverity == TremorSeverity.MILD -> 0.19f
            speedP75 < 0.3f -> 0.16f
            speedP75 > 1.5f -> 0.22f
            else -> 0.15f
        }.coerceIn(0.12f, 0.3f) //aca vamos a evitar q deadzone quede pequeño o dema grande

        val rangeEfficiency = ((xRange + yRange) / 2f).coerceIn(0.3f, 1.2f)

        val sensitivity = when {
            speedP50 < 0.4f -> 1.4f
            speedP50 > 1.8f -> 0.75f
            else -> 1.0f
        } * (1.2f / rangeEfficiency)
        //suavizacion
        val smoothingFactor = when {
            accelP90 > 3.0f -> 0.48f
            accelP90 > 2.0f -> 0.42f
            accelP90 > 1.0f -> 0.36f
            tremorSeverity >= TremorSeverity.MODERATE -> 0.4f //si hay temblor severo o moderado aplicamos
            else -> 0.28f
        }.coerceIn(0.2f, 0.5f)
        //vamos a puntuacion de la estabilidad
        val stabilityScore = when (tremorSeverity) {
            TremorSeverity.NONE -> 0.9f
            TremorSeverity.MILD -> 0.7f
            TremorSeverity.MODERATE -> 0.5f
            TremorSeverity.SEVERE -> 0.3f
        }

        return CalibrationProfile(
            userId = userId,
            lastCalibrationDate = System.currentTimeMillis(),
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            baseDeadZone = deadZone,
            baseSensitivity = sensitivity,
            baseSmoothing = smoothingFactor,
            avgSpeed = avgSpeed,
            maxAcceleration = maxAcceleration,
            tremorIndex = tremorIndex,
            stabilityScore = stabilityScore,
            isCalibrated = true,
            calibrationVersion = 3
        )
    }

    private fun calculatePercentiles(values: List<Float>): PercentileMetrics {
        if (values.isEmpty()) return PercentileMetrics() //por defecto

        val sorted = values.sorted()
        val size = sorted.size

        fun percentile(p: Float): Float {
            val index = (p * (size - 1)).toInt()
            return sorted[index.coerceIn(0, size - 1)] //no debe ser mayor al ultimo indice
        }

        val mean = values.average().toFloat()//promedio    suma de tod/cantidad
        val variance = values.map { (it - mean).pow(2) }.average().toFloat()    //(x1−mean)^2+(x2−mean)^2+../n
        val stdDev = sqrt(variance)//desviacion estandar      la raiz de la varianza

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

    private fun analyzeTremor(
        samples: List<MovementSample>,
        centerRadius: Float = 0.2f
    ): Pair<Float, TremorSeverity> {
        if (samples.size < 10) return Pair(0f, TremorSeverity.NONE)

        val centerSamples = samples.filter { sample ->
            abs(sample.smoothedPosition.x) < centerRadius &&
                    abs(sample.smoothedPosition.y) < centerRadius
        }
        //si habria muchos datos no seria confiable calcular estadistica
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

        var changes = 0          //contamos cambios
        var lastDirectionX = 0   //ultima
        var lastDirectionY = 0
        //necesitamos calcular el punto anterior para ello y calcular cuanto se movio
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
            lastDirectionX = currentDirectionX//act el valor anterior
            lastDirectionY = currentDirectionY
        }
        return changes //total
    }

    data class PercentileMetrics(
        val p25: Float = 0f,
        val p50: Float = 0f,
        val p75: Float = 0f,
        val p90: Float = 0f,
        val p95: Float = 0f,
        val mean: Float = 0f,
        val stdDev: Float = 0f
    )
}
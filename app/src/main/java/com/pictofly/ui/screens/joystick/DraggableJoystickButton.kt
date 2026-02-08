package com.pictofly.ui.screens.joystick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.*

@Composable
fun DraggableJoystickButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 140.dp,
    onMove: (Offset) -> Unit = {},
    onCenterClick: () -> Unit = {},
    onRelease: () -> Unit = {},
    enabled: Boolean = true,
    isLeftHanded: Boolean = false,

    baseDeadZone: Float = 0.15f,
    baseSensitivity: Float = 1.0f,
    baseSmoothing: Float = 0.3f,

    dynamicDeadZone: Float = 0f,
    dynamicSensitivity: Float = 1f,
    dynamicSmoothing: Float = 0f,

    isCalibrated: Boolean = false,


    onInputRatioUpdate: ((Float) -> Unit)? = null
) {
    var joystickPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var smoothedPosition by remember { mutableStateOf(Offset.Zero) }

    val finalDeadZone = (baseDeadZone + dynamicDeadZone).coerceIn(0.1f, 0.35f)
    val finalSensitivity = (baseSensitivity * dynamicSensitivity).coerceIn(0.6f, 1.5f)
    val finalSmoothing = (baseSmoothing + dynamicSmoothing).coerceIn(0.15f, 0.55f)

    fun applyDeadZone(offset: Offset): Offset {
        val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
        return if (distance < finalDeadZone) {
            Offset.Zero
        } else {
            val scale = (distance - finalDeadZone) / (1f - finalDeadZone)
            val angle = atan2(offset.y, offset.x)
            Offset(
                x = cos(angle).toFloat() * scale,
                y = sin(angle).toFloat() * scale
            )
        }
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .background(Color.Transparent)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val rawX = ((offset.x - size.width / 2) / (size.width / 2)).coerceIn(-1f, 1f)
                        val rawY = ((offset.y - size.height / 2) / (size.height / 2)).coerceIn(-1f, 1f)

                        joystickPosition = Offset(rawX, rawY)

                        val distance = sqrt(rawX * rawX + rawY * rawY)
                        onInputRatioUpdate?.invoke(distance.coerceIn(0f, 1f))

                        val withDeadZone = applyDeadZone(joystickPosition)

                        val adjustedX = (withDeadZone.x * finalSensitivity).coerceIn(-1f, 1f)
                        val adjustedY = (withDeadZone.y * finalSensitivity).coerceIn(-1f, 1f)
                        val adjustedPosition = Offset(adjustedX, adjustedY)

                        smoothedPosition = if (smoothedPosition == Offset.Zero) {
                            adjustedPosition
                        } else {
                            Offset(
                                x = smoothedPosition.x + (adjustedPosition.x - smoothedPosition.x) * finalSmoothing,
                                y = smoothedPosition.y + (adjustedPosition.y - smoothedPosition.y) * finalSmoothing
                            )
                        }

                        onMove(smoothedPosition)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val newX = (joystickPosition.x + dragAmount.x / (size.width / 2)).coerceIn(-1f, 1f)
                        val newY = (joystickPosition.y + dragAmount.y / (size.height / 2)).coerceIn(-1f, 1f)

                        joystickPosition = Offset(newX, newY)

                        val distance = sqrt(newX * newX + newY * newY)
                        onInputRatioUpdate?.invoke(distance.coerceIn(0f, 1f))

                        val withDeadZone = applyDeadZone(joystickPosition)

                        val adjustedX = (withDeadZone.x * finalSensitivity).coerceIn(-1f, 1f)
                        val adjustedY = (withDeadZone.y * finalSensitivity).coerceIn(-1f, 1f)
                        val adjustedPosition = Offset(adjustedX, adjustedY)

                        smoothedPosition = Offset(
                            x = smoothedPosition.x + (adjustedPosition.x - smoothedPosition.x) * finalSmoothing,
                            y = smoothedPosition.y + (adjustedPosition.y - smoothedPosition.y) * finalSmoothing
                        )

                        onMove(smoothedPosition)
                    },
                    onDragEnd = {
                        isDragging = false
                        joystickPosition = Offset.Zero
                        smoothedPosition = Offset.Zero
                        onMove(Offset.Zero)
                        onRelease()
                    }
                )
            }
            .clickable(
                enabled = enabled && !isDragging,
                onClick = onCenterClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minSize = min(size.width, size.height)
            val baseRadius = minSize / 2f - 10f

            drawCircle(
                color = if (enabled) Color.LightGray.copy(alpha = 0.3f)
                else Color.Gray.copy(alpha = 0.2f),
                radius = baseRadius,
                center = center,
                style = Fill
            )

            drawCircle(
                color = if (enabled) Color.DarkGray else Color.Gray,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            drawLine(
                color = if (enabled) Color.Gray.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                start = Offset(center.x, center.y - baseRadius),
                end = Offset(center.x, center.y + baseRadius),
                strokeWidth = 1f
            )
            drawLine(
                color = if (enabled) Color.Gray.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                start = Offset(center.x - baseRadius, center.y),
                end = Offset(center.x + baseRadius, center.y),
                strokeWidth = 1f
            )

            val deadZoneRadius = baseRadius * finalDeadZone
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = deadZoneRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            val innerRadius = baseRadius * 0.4f
            val innerCenter = if (enabled && isDragging) {
                Offset(
                    center.x + (joystickPosition.x * baseRadius * 0.8f),
                    center.y + (joystickPosition.y * baseRadius * 0.8f)
                )
            } else {
                center
            }

            val joystickColor = when {
                !enabled -> Color.Gray
                isDragging -> Color(0xFF4CAF50) // Verde
                else -> Color(0xFF2196F3) // Azul
            }

            drawCircle(
                color = joystickColor,
                radius = innerRadius,
                center = innerCenter,
                style = Fill
            )

            if (isCalibrated) {
                drawCircle(
                    color = Color.Green.copy(alpha = 0.3f),
                    radius = baseRadius + 5f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }

        if (enabled) {

            Text(
                text = "^",
                color = Color.White,
                fontSize = 30.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            Text(
                text = "v",
                color = Color.White,
                fontSize = 30.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "<",
                color = Color.White,
                fontSize = 30.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            )
            Text(
                text = ">",
                color = Color.White,
                fontSize = 30.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }
}
package com.pictofly.data.model

import java.util.Locale

data class TTSConfig(
    val speed: Float = 1.0f,
    val pitch: Float = 1.1f, //tono
    val language: Locale = Locale("es", "ES"),
    val volumeDb: Int = 70
)
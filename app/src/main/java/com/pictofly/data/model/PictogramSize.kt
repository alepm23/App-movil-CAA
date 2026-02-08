package com.pictofly.data.model

enum class PictogramSize(
    val displayName: String,
    val multiplier: Float,
    val carouselSize: Int,          // Solo para carrusel
    val carouselImageSize: Int,
    val sentenceSize: Int = 80,     // Frase Armada
    val sentenceImageSize: Int = 60
) {
    SMALL(
        displayName = "Pequeño",
        multiplier = 0.8f,
        carouselSize = 64,
        carouselImageSize = 48
    ),
    MEDIUM(
        displayName = "Mediano",
        multiplier = 1.0f,
        carouselSize = 80,
        carouselImageSize = 60
    ),
    LARGE(
        displayName = "Grande",
        multiplier = 1.2f,
        carouselSize = 96,
        carouselImageSize = 72
    ),
    EXTRA_LARGE(
        displayName = "Extra Grande",
        multiplier = 1.5f,
        carouselSize = 120,
        carouselImageSize = 90
    );

    companion object {
        fun fromName(name: String): PictogramSize {
            return values().find { it.displayName == name } ?: MEDIUM
        }

        fun fromMultiplier(multiplier: Float): PictogramSize {
            return values().find { it.multiplier == multiplier } ?: MEDIUM
        }
    }
}
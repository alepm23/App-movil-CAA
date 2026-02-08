package com.pictofly.data.model

data class UserSettings(
    val soundHz: Int = 0,
    val soundDb: Int = 0,
    val isRightHanded: Boolean = true,
    val hasFullMovement: Boolean = true,
    val calibrationSpeed: Float = 1.0f,
    val isConfigured: Boolean = false,
    val consentShown: Boolean = false
)

data class SoundConfiguration(
    val hz: Int = 440,
    val db: Int = 70
)

data class PhysioConfiguration(
    val isRightHanded: Boolean = true,
    val hasFullMovement: Boolean = true,
    val calibrationSpeed: Float = 1.0f
)
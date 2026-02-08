package com.pictofly.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val KEY_SOUND_HZ = intPreferencesKey("sound_hz")
    val KEY_SOUND_DB = intPreferencesKey("sound_db")
    val KEY_IS_RIGHT_HANDED = booleanPreferencesKey("is_right_handed")
    val KEY_HAS_FULL_MOVEMENT = booleanPreferencesKey("has_full_movement")
    val KEY_CALIBRATION_SPEED = floatPreferencesKey("calibration_speed")
    val KEY_IS_CONFIGURED = booleanPreferencesKey("is_configured")
    val KEY_CONSENT_SHOWN = booleanPreferencesKey("consent_shown")
    val KEY_CALIBRATION_PROFILE = stringPreferencesKey("calibration_profile")
    val KEY_CALIBRATION_IS_CALIBRATED = booleanPreferencesKey("calibration_is_calibrated")
}
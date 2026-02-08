package com.pictofly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.pictofly.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class UserPreferencesDataSource(private val context: Context) {

    val userSettings: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                soundHz = preferences[PreferencesKeys.KEY_SOUND_HZ] ?: 0,
                soundDb = preferences[PreferencesKeys.KEY_SOUND_DB] ?: 0,
                isRightHanded = preferences[PreferencesKeys.KEY_IS_RIGHT_HANDED] ?: true,
                hasFullMovement = preferences[PreferencesKeys.KEY_HAS_FULL_MOVEMENT] ?: true,
                calibrationSpeed = preferences[PreferencesKeys.KEY_CALIBRATION_SPEED] ?: 1.0f,
                isConfigured = preferences[PreferencesKeys.KEY_IS_CONFIGURED] ?: false,
                consentShown = preferences[PreferencesKeys.KEY_CONSENT_SHOWN] ?: false
            )
        }

    suspend fun saveConfiguration(
        hz: Int,
        db: Int,
        isRightHanded: Boolean,
        hasFullMovement: Boolean,
        calibrationSpeed: Float
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SOUND_HZ] = hz
            preferences[PreferencesKeys.KEY_SOUND_DB] = db
            preferences[PreferencesKeys.KEY_IS_RIGHT_HANDED] = isRightHanded
            preferences[PreferencesKeys.KEY_HAS_FULL_MOVEMENT] = hasFullMovement
            preferences[PreferencesKeys.KEY_CALIBRATION_SPEED] = calibrationSpeed
            preferences[PreferencesKeys.KEY_IS_CONFIGURED] = true
        }
    }

    suspend fun saveConsentShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CONSENT_SHOWN] = true
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getCurrentSettings(): UserSettings {
        return userSettings.first()
    }
}
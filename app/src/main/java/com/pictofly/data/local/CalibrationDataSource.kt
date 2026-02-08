package com.pictofly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.pictofly.data.model.CalibrationProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.calibrationDataStore: DataStore<Preferences> by preferencesDataStore(name = "calibration")

@Singleton
class CalibrationDataSource @Inject constructor(
    private val context: Context
) {
    private val gson = Gson()

    val calibrationProfile: Flow<CalibrationProfile> = context.calibrationDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val profileJson = preferences[PreferencesKeys.KEY_CALIBRATION_PROFILE]

            if (!profileJson.isNullOrBlank()) {
                try {
                    gson.fromJson(profileJson, CalibrationProfile::class.java)
                } catch (e: Exception) {
                    CalibrationProfile(isCalibrated = false)
                }
            } else {
                CalibrationProfile(isCalibrated = false)
            }
        }

    suspend fun saveCalibrationProfile(profile: CalibrationProfile) {
        context.calibrationDataStore.edit { preferences ->
            val profileJson = gson.toJson(profile)
            preferences[PreferencesKeys.KEY_CALIBRATION_PROFILE] = profileJson
            preferences[PreferencesKeys.KEY_CALIBRATION_IS_CALIBRATED] = profile.isCalibrated
        }
    }

    suspend fun clearCalibrationData() {
        context.calibrationDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.KEY_CALIBRATION_PROFILE)
            preferences.remove(PreferencesKeys.KEY_CALIBRATION_IS_CALIBRATED)
        }
    }

    suspend fun getCurrentCalibrationProfile(): CalibrationProfile {
        return try {
            val preferences = context.calibrationDataStore.data
                .catch { emit(emptyPreferences()) }
                .first()

            val profileJson = preferences[PreferencesKeys.KEY_CALIBRATION_PROFILE]

            if (!profileJson.isNullOrBlank()) {
                gson.fromJson(profileJson, CalibrationProfile::class.java)
            } else {
                CalibrationProfile(isCalibrated = false)
            }
        } catch (e: Exception) {
            CalibrationProfile(isCalibrated = false)
        }
    }

    suspend fun hasCalibration(): Boolean {
        return try {
            val preferences = context.calibrationDataStore.data
                .catch { emit(emptyPreferences()) }
                .first()
            preferences[PreferencesKeys.KEY_CALIBRATION_IS_CALIBRATED] ?: false
        } catch (e: Exception) {
            false
        }
    }
}
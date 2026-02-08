package com.pictofly.repository

import com.pictofly.data.local.UserPreferencesDataSource
import com.pictofly.data.model.UserSettings
import com.pictofly.data.model.SoundConfiguration
import com.pictofly.data.model.PhysioConfiguration
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UserSettingsRepository {
    val userSettings: Flow<UserSettings>
    suspend fun saveConfiguration(
        hz: Int,
        db: Int,
        isRightHanded: Boolean,
        hasFullMovement: Boolean,
        calibrationSpeed: Float
    )
    suspend fun saveConsentShown()
    suspend fun clearAllData()
    suspend fun getCurrentSettings(): UserSettings
    suspend fun saveSoundConfiguration(soundConfig: SoundConfiguration)
    suspend fun savePhysioConfiguration(physioConfig: PhysioConfiguration)
}

class UserSettingsRepositoryImpl @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : UserSettingsRepository {

    override val userSettings: Flow<UserSettings>
        get() = userPreferencesDataSource.userSettings

    override suspend fun saveConfiguration(
        hz: Int,
        db: Int,
        isRightHanded: Boolean,
        hasFullMovement: Boolean,
        calibrationSpeed: Float
    ) {
        userPreferencesDataSource.saveConfiguration(
            hz = hz,
            db = db,
            isRightHanded = isRightHanded,
            hasFullMovement = hasFullMovement,
            calibrationSpeed = calibrationSpeed
        )
    }

    override suspend fun saveConsentShown() {
        userPreferencesDataSource.saveConsentShown()
    }

    override suspend fun clearAllData() {
        userPreferencesDataSource.clearAllData()
    }

    override suspend fun getCurrentSettings(): UserSettings {
        return userPreferencesDataSource.getCurrentSettings()
    }

    override suspend fun saveSoundConfiguration(soundConfig: SoundConfiguration) {
        val currentSettings = getCurrentSettings()
        saveConfiguration(
            hz = soundConfig.hz,
            db = soundConfig.db,
            isRightHanded = currentSettings.isRightHanded,
            hasFullMovement = currentSettings.hasFullMovement,
            calibrationSpeed = currentSettings.calibrationSpeed
        )
    }

    override suspend fun savePhysioConfiguration(physioConfig: PhysioConfiguration) {
        val currentSettings = getCurrentSettings()
        saveConfiguration(
            hz = currentSettings.soundHz,
            db = currentSettings.soundDb,
            isRightHanded = physioConfig.isRightHanded,
            hasFullMovement = physioConfig.hasFullMovement,
            calibrationSpeed = physioConfig.calibrationSpeed
        )
    }
}
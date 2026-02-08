package com.pictofly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.repository.UserSettingsRepository
import com.pictofly.repository.CalibrationRepository
import com.pictofly.data.model.CalibrationProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val calibrationRepository: CalibrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userSettingsRepository.userSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        soundHz = if (settings.soundHz > 0) settings.soundHz else 440,
                        soundDb = if (settings.soundDb > 0) settings.soundDb else 70,
                        isRightHanded = settings.isRightHanded,
                        calibrationSpeed = settings.calibrationSpeed,
                        isConfigured = settings.isConfigured,
                        consentShown = settings.consentShown,
                        hasFullMovement = settings.hasFullMovement
                    )
                }
            }
        }

        viewModelScope.launch {
            calibrationRepository.getCalibrationProfile().collect { profile ->
                _uiState.update {
                    it.copy(calibrationProfile = profile)
                }
            }
        }
    }

    fun saveConfiguration(
        hz: Int,
        db: Int,
        isRightHanded: Boolean,
        hasFullMovement: Boolean,
        calibrationSpeed: Float
    ) {
        viewModelScope.launch {
            userSettingsRepository.saveConfiguration(
                hz = hz,
                db = db,
                isRightHanded = isRightHanded,
                hasFullMovement = hasFullMovement,
                calibrationSpeed = calibrationSpeed
            )
        }
    }

    fun saveConsentShown() {
        viewModelScope.launch {
            userSettingsRepository.saveConsentShown()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            userSettingsRepository.clearAllData()
        }
    }

    suspend fun getCurrentSettings() = userSettingsRepository.getCurrentSettings()

    fun saveCalibrationProfile(profile: CalibrationProfile) {
        viewModelScope.launch {
            calibrationRepository.saveCalibrationProfile(profile)
        }
    }
}

data class AppUiState(
    val soundHz: Int = 440,
    val soundDb: Int = 70,
    val isRightHanded: Boolean = true,
    val calibrationSpeed: Float = 1.0f,
    val isConfigured: Boolean = false,
    val consentShown: Boolean = false,
    val hasFullMovement: Boolean = true,
    val calibrationProfile: CalibrationProfile = CalibrationProfile(isCalibrated = false)
)
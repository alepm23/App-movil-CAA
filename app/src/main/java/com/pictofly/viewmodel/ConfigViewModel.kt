package com.pictofly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.repository.UserSettingsRepository
import com.pictofly.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    fun loadCurrentConfiguration() {
        viewModelScope.launch {
            val settings = userSettingsRepository.getCurrentSettings()
            _uiState.update {
                it.copy(
                    soundHz = settings.soundHz,
                    soundDb = settings.soundDb,
                    isRightHanded = settings.isRightHanded,
                    calibrationSpeed = settings.calibrationSpeed,
                    isLoading = false
                )
            }
        }
    }

    fun showLogoutDialog() {
        _uiState.update {
            it.copy(showLogoutDialog = true)
        }
    }

    fun hideLogoutDialog() {
        _uiState.update {
            it.copy(showLogoutDialog = false)
        }
    }
}

data class ConfigUiState(
    val soundHz: Int = 440,
    val soundDb: Int = 70,
    val isRightHanded: Boolean = true,
    val calibrationSpeed: Float = 1.0f,
    val isLoading: Boolean = true,
    val showLogoutDialog: Boolean = false
)
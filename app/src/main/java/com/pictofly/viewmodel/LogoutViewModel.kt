package com.pictofly.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.repository.CalibrationRepository
import com.pictofly.repository.CleanupRepository
import com.pictofly.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    data class Success(val message: String) : LogoutState()
    data class Error(val message: String) : LogoutState()
}

@HiltViewModel
class LogoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsRepository: UserSettingsRepository,
    private val cleanupRepository: CleanupRepository,
    private val calibrationRepository: CalibrationRepository
) : ViewModel() {

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    fun performLogout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _logoutState.value = LogoutState.Loading
                calibrationRepository.clearCalibrationProfile()
                cleanupRepository.cleanAllUserData()
                userSettingsRepository.clearAllData()
                delay(300)
                _logoutState.value = LogoutState.Success("Sesión cerrada correctamente")
                onComplete()
            } catch (e: Exception) {
                Log.e("LogoutVM", "Error en logout: ${e.message}")
                _logoutState.value = LogoutState.Error("Error al cerrar sesión: ${e.message}")
            }
        }
    }

    fun resetState() {
        _logoutState.value = LogoutState.Idle
    }
}
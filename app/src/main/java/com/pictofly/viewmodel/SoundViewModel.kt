package com.pictofly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoundUiState())
    val uiState: StateFlow<SoundUiState> = _uiState.asStateFlow()

    private var lastManualInput: String = ""

    fun initializeAudio(context: android.content.Context) {
        audioRepository.initialize(context) { isInitialized ->
            if (isInitialized) {
                _uiState.update { it.copy(isAudioInitialized = true) }
            } else {
                _uiState.update { it.copy(ttsError = true) }
            }
        }
    }

    fun updateDbValue(newValue: String) {
        if (newValue.all { char -> char.isDigit() } && newValue.length <= 3) {
            _uiState.update {
                it.copy(
                    dbValue = newValue,
                    dbError = validateDbInput(newValue)
                )
            }
            lastManualInput = newValue

            if (newValue.isNotEmpty() && _uiState.value.dbError.isEmpty() && !_uiState.value.isTestingSound) {
                viewModelScope.launch {
                    delay(800)
                    if (_uiState.value.dbValue == newValue) {
                        testSound(newValue.toIntOrNull() ?: 0)
                    }
                }
            }
        }
    }

    fun changeVolume(adjustment: Int) {
        val current = _uiState.value.dbValue.toIntOrNull() ?: 0
        val minDb = 1
        val maxDb = 120
        val newDb = (current + adjustment).coerceIn(minDb, maxDb)

        _uiState.update {
            it.copy(
                dbValue = newDb.toString(),
                dbError = ""
            )
        }
        lastManualInput = newDb.toString()

        testSound(newDb)
    }

    fun testSound(db: Int? = null) {
        val currentDb = db ?: _uiState.value.dbValue.toIntOrNull() ?: 0
        val minDb = 1

        if (currentDb < minDb) {
            _uiState.update {
                it.copy(
                    testMessage = "Configure un valor de dB primero (mínimo $minDb dB)"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                currentVolume = currentDb,
                isTestingSound = true,
                testMessage = "Probando sonido a $currentDb dB..."
            )
        }

        viewModelScope.launch {
            // 👈 FORZAR VOLUMEN AL MÁXIMO DEL SISTEMA
            audioRepository.setMaxVolume()
            delay(100)

            // Aplicar ajuste de dB
            audioRepository.adjustAndLockVolume(currentDb)
            audioRepository.setVolumeFromDb(currentDb)
            delay(100)

            // Reproducir sonido de prueba
            audioRepository.speak(
                "Hola, este es el volumen de prueba a $currentDb decibeles",
                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                "test_sound"
            )

            delay(2000)
            _uiState.update {
                it.copy(
                    isTestingSound = false,
                    testMessage = ""
                )
            }
        }
    }

    fun saveSoundConfiguration(hz: Int, db: Int, onComplete: () -> Unit) {
        val minDb = 1
        if (db >= minDb) {
            audioRepository.setMaxVolume()  // Forzar máximo
            audioRepository.adjustAndLockVolume(db)
            audioRepository.setVolumeFromDb(db)
            audioRepository.speak(
                "Volumen configurado a $db decibeles",
                0,
                "config_saved"
            )
            onComplete()
        }
    }

    private fun validateDbInput(input: String): String {
        val minDb = 1
        val maxDb = 120

        return when {
            input.isEmpty() -> "Ingrese un valor"
            input.toIntOrNull() == null -> "Solo se permiten números"
            input.toInt() < minDb -> "Mínimo $minDb dB"
            input.toInt() > maxDb -> "Máximo $maxDb dB"
            else -> ""
        }
    }

    fun stopAudio() {
        audioRepository.stop()
    }

    fun shutdownAudio() {
        audioRepository.shutdown()
    }
}

data class SoundUiState(
    val dbValue: String = "",
    val dbError: String = "",
    val isTestingSound: Boolean = false,
    val testMessage: String = "",
    val currentVolume: Int = 0,
    val isAudioInitialized: Boolean = false,
    val ttsError: Boolean = false
)
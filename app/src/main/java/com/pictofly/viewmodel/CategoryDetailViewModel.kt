package com.pictofly.viewmodel

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.Category
import com.pictofly.data.model.Pictogram
import com.pictofly.repository.AudioRepository
import com.pictofly.repository.CategoryRepository
import com.pictofly.repository.SettingsRepository
import com.pictofly.utils.CategoryEventBus
import com.pictofly.utils.CategoryEvent
import com.pictofly.utils.AdaptiveGrammarEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val categoryEventBus: CategoryEventBus,
    private val adaptiveGrammarEngine: AdaptiveGrammarEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    private var lastMoveTime: Long = 0L
    private var lastSpokenIndex: Int = -1
    private var currentCategoryName: String = ""

    // CONTROL DE JOYSTICK - IMPRESCINDIBLE
    private var joystickDirection: Int = 0 // 0=centro, 1=derecha, -1=izquierda
    private var isMoving: Boolean = false
    private val MOVE_DELAY_MS = 400L // 0.4 segundos entre movimientos

    private var currentSubjectName: String = "Yo"
    private var currentVerbName: String = ""

    init {
        viewModelScope.launch {
            val subject = settingsRepository.getSelectedSubjectName()
            val verb = settingsRepository.getSelectedVerbName()
            currentSubjectName = subject ?: "Yo"
            currentVerbName = verb ?: ""

            categoryEventBus.events.collect { event ->
                when (event) {
                    is CategoryEvent.PictogramsUpdated -> {
                        if (event.categoryName == currentCategoryName) {
                            loadPictograms()
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    fun updateSelectedIndex(index: Int) {
        if (index in _uiState.value.pictograms.indices && index != _uiState.value.selectedPictogramIndex) {
            _uiState.update { it.copy(selectedPictogramIndex = index) }
            speakCurrentPictogram()
        }
    }

    private suspend fun refreshSettings() {
        currentSubjectName = settingsRepository.getSelectedSubjectName() ?: "Yo"
        currentVerbName = settingsRepository.getSelectedVerbName() ?: ""
    }

    private fun updateVisualPhrase(pictogram: Pictogram) {
        viewModelScope.launch {
            refreshSettings()
            val resultado = adaptiveGrammarEngine.corregirFrase(
                sujeto = currentSubjectName,
                verbo = if (currentVerbName.isBlank()) null else currentVerbName,
                predicado = pictogram.name
            )
            _uiState.update { it.copy(currentFullPhrase = resultado.fraseCorregida) }
        }
    }

    fun initialize(category: Category) {
        currentCategoryName = category.name
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            categoryRepository.getPictogramsByCategory(category.name)
                .collect { pictograms ->
                    _uiState.update { it.copy(
                        category = category,
                        pictograms = pictograms,
                        selectedPictogramIndex = if (pictograms.isNotEmpty()) 0 else -1,
                        isLoading = false
                    )}
                }
        }
    }

    // --- LÓGICA DE MOVIMIENTO SIMPLIFICADA ---
    fun handleJoystickMove(movement: android.graphics.PointF, calibrationSpeed: Float) {
        val currentTime = System.currentTimeMillis()

        // Zona muerta
        val isCentered = Math.abs(movement.x) < 0.3f && Math.abs(movement.y) < 0.3f

        if (isCentered) {
            // Resetear dirección cuando vuelve al centro
            if (joystickDirection != 0) {
                joystickDirection = 0
                isMoving = false
            }
            return
        }

        // Detectar dirección solo horizontal
        val newDirection = when {
            movement.x > 0.5f -> 1  // Derecha (umbral reducido de 0.6 a 0.5)
            movement.x < -0.5f -> -1 // Izquierda
            else -> 0
        }

        if (newDirection == 0) return

        // Si cambió la dirección, reiniciamos
        if (newDirection != joystickDirection) {
            joystickDirection = newDirection
            isMoving = false
            lastMoveTime = 0
            return
        }

        // Si ya estamos moviendo, ignoramos
        if (isMoving) return

        // Delay fijo de 300ms para navegación fluida pero controlada
        val moveDelay = 300L
        val timeSinceLastMove = currentTime - lastMoveTime

        if (timeSinceLastMove >= moveDelay) {
            when (newDirection) {
                1 -> {
                    moveRight()
                    lastMoveTime = currentTime
                    isMoving = true
                    // Desbloquear después del delay
                    viewModelScope.launch {
                        delay(moveDelay)
                        isMoving = false
                    }
                }
                -1 -> {
                    moveLeft()
                    lastMoveTime = currentTime
                    isMoving = true
                    viewModelScope.launch {
                        delay(moveDelay)
                        isMoving = false
                    }
                }
            }
        }
    }

    private fun moveRight() {
        _uiState.update { state ->
            if (state.pictograms.isEmpty()) return@update state
            val newIndex = (state.selectedPictogramIndex + 1) % state.pictograms.size
            state.copy(selectedPictogramIndex = newIndex)
        }
        speakCurrentPictogram()
    }

    private fun moveLeft() {
        _uiState.update { state ->
            if (state.pictograms.isEmpty()) return@update state
            val newIndex = (state.selectedPictogramIndex - 1 + state.pictograms.size) % state.pictograms.size
            state.copy(selectedPictogramIndex = newIndex)
        }
        speakCurrentPictogram()
    }

    private fun speakCurrentPictogram() {
        val currentIndex = _uiState.value.selectedPictogramIndex
        if (currentIndex != lastSpokenIndex && currentIndex in _uiState.value.pictograms.indices) {
            val name = _uiState.value.pictograms[currentIndex].name
            audioRepository.speak(name, TextToSpeech.QUEUE_FLUSH, "pictogram_$name")
            lastSpokenIndex = currentIndex
        }
    }

    fun handleCenterClick() {
        _uiState.value.selectedPictogram?.let { handlePictogramSelection(it) }
    }

    fun handlePictogramClick(pictogram: Pictogram) {
        handlePictogramSelection(pictogram)
    }

    private fun handlePictogramSelection(pictogram: Pictogram) {
        _uiState.update { it.copy(selectedPictogramForPhrase = pictogram) }
        updateVisualPhrase(pictogram)
        speakCompletePhrase(pictogram)
    }

    private fun speakCompletePhrase(pictogram: Pictogram) {
        viewModelScope.launch {
            refreshSettings()
            val resultado = adaptiveGrammarEngine.corregirFrase(
                sujeto = currentSubjectName,
                verbo = if (currentVerbName.isBlank()) null else currentVerbName,
                predicado = pictogram.name
            )

            _uiState.update { it.copy(currentFullPhrase = resultado.fraseCorregida) }

            audioRepository.speak(
                text = resultado.fraseCorregida,
                queueMode = TextToSpeech.QUEUE_FLUSH,
                utteranceId = "phrase_${System.currentTimeMillis()}"
            )

            _uiState.update { it.copy(enableBackMode = true) }
            delay(3000)
            if (_uiState.value.enableBackMode) {
                audioRepository.speak("Para volver, mueve el joystick hacia arriba", TextToSpeech.QUEUE_ADD, "rem")
            }
        }
    }

    private fun handleBackNavigation() {
        audioRepository.speak("Volviendo atrás", TextToSpeech.QUEUE_FLUSH, "back")
        viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(shouldNavigateBack = true) }
        }
    }

    private fun loadPictograms() {
        viewModelScope.launch {
            categoryRepository.getPictogramsByCategory(currentCategoryName).collect { picts ->
                _uiState.update { it.copy(pictograms = picts) }
            }
        }
    }

    fun resetBackNavigation() = _uiState.update { it.copy(shouldNavigateBack = false) }
    fun speakText(text: String) = audioRepository.speak(text, TextToSpeech.QUEUE_FLUSH, "txt")
    fun stopAudio() = audioRepository.stop()
}

data class CategoryDetailUiState(
    val category: Category? = null,
    val pictograms: List<Pictogram> = emptyList(),
    val selectedPictogramIndex: Int = -1,
    val selectedPictogramForPhrase: Pictogram? = null,
    val currentFullPhrase: String = "",
    val enableBackMode: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val isLoading: Boolean = true
) {
    val selectedPictogram: Pictogram? get() = pictograms.getOrNull(selectedPictogramIndex)
}
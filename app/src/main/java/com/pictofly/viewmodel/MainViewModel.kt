package com.pictofly.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.Category
import com.pictofly.data.model.Pictogram
import com.pictofly.data.model.CalibrationProfile
import com.pictofly.repository.AudioRepository
import com.pictofly.repository.CategoryRepository
import com.pictofly.repository.CalibrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val audioRepository: AudioRepository,
    private val calibrationRepository: CalibrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var lastMoveTime: Long = 0L
    private var lastSpokenCategoryIndex: Int = -1

    init {
        loadCategories()
        loadCalibrationProfile()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .collect { categories ->
                    _uiState.update { currentState ->
                        val newIndex = if (categories.isNotEmpty() && currentState.selectedCategoryIndex == -1) {
                            0
                        } else {
                            currentState.selectedCategoryIndex.coerceIn(-1, categories.size - 1)
                        }

                        currentState.copy(
                            categories = categories,
                            selectedCategoryIndex = newIndex
                        )
                    }
                }
        }
    }

    private fun loadCalibrationProfile() {
        viewModelScope.launch {
            calibrationRepository.getCalibrationProfile().collect { profile ->
                _uiState.update {
                    it.copy(calibrationProfile = profile)
                }
            }
        }
    }

    fun initializeAudio(context: Context) {
        Log.d("MainViewModel", "Inicializando audio...")
        audioRepository.initialize(context) { isInitialized ->
            if (isInitialized) {
                viewModelScope.launch {
                    delay(500)
                    speakInstructions()
                }
            } else {
                Log.e("MainViewModel", "Error al inicializar audio")
            }
        }
    }

    private fun speakInstructions() {
        Log.d("MainViewModel", "Reproduciendo instrucciones")
        audioRepository.speak(
            text = "Pantalla principal. Navega entre categorías con izquierda y derecha. Click en centro para seleccionar categoría.",
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "main_screen_instructions"
        )
    }

    fun speakText(text: String) {
        Log.d("MainViewModel", "Reproduciendo: $text")
        audioRepository.speak(
            text = text,
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "phrase_${System.currentTimeMillis()}"
        )
    }

    fun handleJoystickMove(movement: android.graphics.PointF, calibrationSpeed: Float) {
        val currentTime = System.currentTimeMillis()
        val minTimeBetweenMoves = (500L / calibrationSpeed).toLong()

        if (currentTime - lastMoveTime > minTimeBetweenMoves) {
            when {
                movement.x > 0.3f -> moveRight()
                movement.x < -0.3f -> moveLeft()
            }
            lastMoveTime = currentTime
        }
    }

    private fun moveRight() {
        _uiState.update { currentState ->
            if (currentState.categories.isEmpty()) return@update currentState

            val newIndex = (currentState.selectedCategoryIndex + 1) % currentState.categories.size
            currentState.copy(selectedCategoryIndex = newIndex)
        }
        speakCurrentCategory()
    }

    private fun moveLeft() {
        _uiState.update { currentState ->
            if (currentState.categories.isEmpty()) return@update currentState

            val newIndex = (currentState.selectedCategoryIndex - 1 + currentState.categories.size) %
                    currentState.categories.size
            currentState.copy(selectedCategoryIndex = newIndex)
        }
        speakCurrentCategory()
    }

    private fun speakCurrentCategory() {
        val currentState = _uiState.value
        val category = currentState.selectedCategory
        val currentIndex = currentState.selectedCategoryIndex

        if (currentIndex != lastSpokenCategoryIndex) {
            category?.let { safeCategory ->
                audioRepository.speak(
                    text = safeCategory.name,
                    queueMode = TextToSpeech.QUEUE_FLUSH,
                    utteranceId = "category_${safeCategory.name}"
                )
            }
            lastSpokenCategoryIndex = currentIndex
        }
    }

    fun handleCenterClick() {
        val selectedCat = _uiState.value.selectedCategory
        selectedCat?.let { category ->
            _uiState.update { currentState ->
                currentState.copy(
                    showCategoryDetail = true
                )
            }

            audioRepository.speak(
                text = "Entrando a ${category.name}",
                queueMode = TextToSpeech.QUEUE_FLUSH,
                utteranceId = "click_${category.name}"
            )
        }
    }

    fun navigateBackFromCategoryDetail() {
        _uiState.update { currentState ->
            currentState.copy(
                showCategoryDetail = false
            )
        }

        viewModelScope.launch {
            delay(300)
            audioRepository.speak(
                text = "Pantalla principal. Navega entre categorías con izquierda y derecha.",
                queueMode = TextToSpeech.QUEUE_FLUSH,
                utteranceId = "back_to_main"
            )
        }
    }

    fun stopAudio() {
        audioRepository.stop()
    }
}

data class MainUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryIndex: Int = -1,
    val showCategoryDetail: Boolean = false,
    val selectedPictogramForPhrase: Pictogram? = null,
    val calibrationProfile: CalibrationProfile = CalibrationProfile(isCalibrated = false)
) {
    val selectedCategory: Category?
        get() = if (selectedCategoryIndex in categories.indices) {
            categories[selectedCategoryIndex]
        } else {
            null
        }
}
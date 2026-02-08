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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.pictofly.utils.CategoryEvent
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val categoryEventBus: CategoryEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    private var lastMoveTime: Long = 0L
    private var lastSpokenIndex: Int = -1
    private var currentCategoryName: String = ""

    private var currentSubjectName: String = "Yo"
    private var currentVerbName: String = "quiero"

    init {
        viewModelScope.launch {
            loadSelectedSubjectAndVerb()

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

    private suspend fun loadSelectedSubjectAndVerb() {
        val subjectName = settingsRepository.getSelectedSubjectName()
        val verbName = settingsRepository.getSelectedVerbName()
        currentSubjectName = subjectName ?: "Yo"
        currentVerbName = verbName ?: "quiero"
    }

    fun initialize(category: Category) {
        currentCategoryName = category.name
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            categoryRepository.getPictogramsByCategory(category.name)
                .collect { pictograms ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            category = category,
                            pictograms = pictograms,
                            selectedPictogramIndex = if (pictograms.isNotEmpty()) 0 else -1,
                            isLoading = false
                        )
                    }

                    delay(300)

                    if (pictograms.isNotEmpty()) {
                        audioRepository.speak(
                            text = "Estás en ${category.name}. Tienes ${pictograms.size} pictogramas. " +
                                    "Navega con izquierda y derecha. Click en centro para seleccionar. " +
                                    "Para volver, mueve el joystick hacia arriba.",
                            queueMode = TextToSpeech.QUEUE_FLUSH,
                            utteranceId = "category_instructions_${category.name}"
                        )
                    } else {
                        audioRepository.speak(
                            text = "La categoría ${category.name} no tiene pictogramas.",
                            queueMode = TextToSpeech.QUEUE_FLUSH,
                            utteranceId = "empty_category_${category.name}"
                        )
                    }
                }
        }
    }

    fun speakText(text: String) {
        audioRepository.speak(
            text = text,
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "feedback_${System.currentTimeMillis()}"
        )
    }

    private fun loadPictograms() {
        viewModelScope.launch {
            categoryRepository.getPictogramsByCategory(currentCategoryName)
                .collect { pictograms ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            pictograms = pictograms,
                            selectedPictogramIndex = if (pictograms.isNotEmpty()) 0 else -1
                        )
                    }
                }
        }
    }

    fun handleJoystickMove(movement: android.graphics.PointF, calibrationSpeed: Float) {
        val currentTime = System.currentTimeMillis()
        val minTimeBetweenMoves = (500L / calibrationSpeed).toLong()

        if (currentTime - lastMoveTime > minTimeBetweenMoves) {
            when {
                movement.x > 0.3f -> moveRight()
                movement.x < -0.3f -> moveLeft()
                movement.y < -0.4f -> handleBackNavigation()
            }
            lastMoveTime = currentTime
        }
    }

    private fun moveRight() {
        _uiState.update { currentState ->
            if (currentState.pictograms.isEmpty()) return@update currentState
            val newIndex = (currentState.selectedPictogramIndex + 1) % currentState.pictograms.size
            currentState.copy(selectedPictogramIndex = newIndex)
        }
        speakCurrentPictogram()
    }

    private fun moveLeft() {
        _uiState.update { currentState ->
            if (currentState.pictograms.isEmpty()) return@update currentState
            val newIndex = (currentState.selectedPictogramIndex - 1 + currentState.pictograms.size) %
                    currentState.pictograms.size
            currentState.copy(selectedPictogramIndex = newIndex)
        }
        speakCurrentPictogram()
    }

    private fun handleBackNavigation() {
        audioRepository.speak(
            text = "Volviendo atrás",
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "going_back"
        )

        viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(shouldNavigateBack = true) }
        }
    }

    private fun speakCurrentPictogram() {
        val currentIndex = _uiState.value.selectedPictogramIndex
        if (currentIndex != lastSpokenIndex && currentIndex in _uiState.value.pictograms.indices) {
            val pictogramName = _uiState.value.pictograms[currentIndex].name
            audioRepository.speak(
                text = pictogramName,
                queueMode = TextToSpeech.QUEUE_FLUSH,
                utteranceId = "pictogram_$pictogramName"
            )
            lastSpokenIndex = currentIndex
        }
    }

    fun handleCenterClick() {
        val selectedPictogram = _uiState.value.selectedPictogram
        selectedPictogram?.let { pictogram ->
            _uiState.update {
                it.copy(
                    selectedPictogramForPhrase = pictogram
                )
            }
            speakCompletePhrase(pictogram)
        }
    }

    fun handlePictogramClick(pictogram: Pictogram) {
        _uiState.update {
            it.copy(
                selectedPictogramForPhrase = pictogram
            )
        }
        speakCompletePhrase(pictogram)
    }

    private fun speakCompletePhrase(pictogram: Pictogram) {
        viewModelScope.launch {
            loadSelectedSubjectAndVerb()
            val phrase = "$currentSubjectName $currentVerbName ${pictogram.name}"
            audioRepository.speak(
                text = phrase,
                queueMode = TextToSpeech.QUEUE_FLUSH,
                utteranceId = "phrase_${System.currentTimeMillis()}"
            )
            _uiState.update { it.copy(enableBackMode = true) }
            delay(3000)
            if (_uiState.value.enableBackMode) {
                audioRepository.speak(
                    text = "Para volver, mueve el joystick hacia arriba",
                    queueMode = TextToSpeech.QUEUE_ADD,
                    utteranceId = "reminder_back"
                )
            }
        }
    }

    fun resetBackNavigation() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }

    fun stopAudio() {
        audioRepository.stop()
    }
}

data class CategoryDetailUiState(
    val category: Category? = null,
    val pictograms: List<Pictogram> = emptyList(),
    val selectedPictogramIndex: Int = -1,
    val selectedPictogramForPhrase: Pictogram? = null,
    val enableBackMode: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val isLoading: Boolean = true
) {
    val selectedPictogram: Pictogram?
        get() = if (selectedPictogramIndex in pictograms.indices) {
            pictograms[selectedPictogramIndex]
        } else {
            null
        }
}
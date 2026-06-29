package com.pictofly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import com.pictofly.repository.LocalContentRepository
import com.pictofly.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.delay
import com.pictofly.utils.PictogramEventBus
import com.pictofly.utils.PictogramEvent

data class CommunicationScreenState(
    val pictograms: List<LocalPictogram> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CommunicationViewModel @Inject constructor(
    private val repository: LocalContentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _communicationScreenState = MutableStateFlow(CommunicationScreenState())
    val communicationScreenState: StateFlow<CommunicationScreenState> = _communicationScreenState.asStateFlow()

    // Manejo de Sujeto con versión para forzar recomposición
    private val _selectedSubject = MutableStateFlow<Pair<LocalPictogram?, Int>>(null to 0)
    val selectedSubject: StateFlow<LocalPictogram?> = _selectedSubject
        .map { it.first }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val subjectVersion: StateFlow<Int> = _selectedSubject
        .map { it.second }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Manejo de Verbo con versión
    private val _selectedVerb = MutableStateFlow<Pair<LocalPictogram?, Int>>(null to 0)
    val selectedVerb: StateFlow<LocalPictogram?> = _selectedVerb
        .map { it.first }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val verbVersion: StateFlow<Int> = _selectedVerb
        .map { it.second }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _selectedPredicate = MutableStateFlow<LocalPictogram?>(null)
    val selectedPredicate: StateFlow<LocalPictogram?> = _selectedPredicate.asStateFlow()

    private var subjectVersionValue = 0
    private var verbVersionValue = 0
    private var communicationCategoryId: String? = null
    private val eventBus = PictogramEventBus

    init {
        viewModelScope.launch {
            initializeCommunicationCategory()
            restoreSelectionsFromPreferences()

            launch {
                eventBus.events.collect { event ->
                    when (event) {
                        is PictogramEvent.PictogramDeleted -> {
                            validateSelections()
                            forceUiRefresh()
                        }
                    }
                }
            }
        }
    }

    private suspend fun initializeCommunicationCategory() {
        try {
            val categories = repository.localCategoriesWithCount.first()
            val existingCategory = categories.find { it.name == "Comunicación Personalizada" }

            communicationCategoryId = if (existingCategory != null) {
                existingCategory.id
            } else {
                val newCategory = LocalCategory(
                    id = "",
                    name = "Comunicación Personalizada",
                    imagePath = "",
                    color = "#FF4081",
                    pictogramCount = 0,
                    createdAt = System.currentTimeMillis()
                )
                repository.addCategory(newCategory)
            }
            loadCommunicationPictograms()
        } catch (e: Exception) {
            Log.e("CommunicationVM", "Error inicializando categoría", e)
        }
    }

    fun loadCommunicationPictograms() {
        viewModelScope.launch {
            try {
                communicationCategoryId?.let { id ->
                    repository.getPictogramsByCategoryId(id).collect { pictograms ->
                        _communicationScreenState.update { state ->
                            state.copy(pictograms = pictograms, isLoading = false)
                        }
                        restoreSelectionsFromPreferences()
                    }
                }
            } catch (e: Exception) {
                _communicationScreenState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun restoreSelectionsFromPreferences() {
        val subjectId = settingsRepository.getSelectedSubjectId()
        val verbId = settingsRepository.getSelectedVerbId()
        val pictograms = _communicationScreenState.value.pictograms

        if (subjectId != null) {
            pictograms.find { it.id == subjectId && it.type == "subject" }?.let {
                subjectVersionValue++
                _selectedSubject.value = it to subjectVersionValue
            }
        }

        if (verbId != null) {
            pictograms.find { it.id == verbId && it.type == "verb" }?.let {
                verbVersionValue++
                _selectedVerb.value = it to verbVersionValue
            }
        }
    }

    private suspend fun validateSelections() {
        val pictograms = _communicationScreenState.value.pictograms
        val currentSubject = _selectedSubject.value.first
        val currentVerb = _selectedVerb.value.first

        if (currentSubject != null && pictograms.none { it.id == currentSubject.id }) {
            selectSubject(null)
        }
        if (currentVerb != null && pictograms.none { it.id == currentVerb.id }) {
            selectVerb(null)
        }
    }

    // --- FUNCIONES DE SELECCIÓN CORREGIDAS ---

    fun selectSubject(pictogram: LocalPictogram?) {
        viewModelScope.launch {
            subjectVersionValue++
            _selectedSubject.value = pictogram to subjectVersionValue

            // Persistir para que el motor gramatical lo vea
            settingsRepository.saveSelectedSubjectId(pictogram?.id)
            settingsRepository.saveSelectedSubjectName(pictogram?.name)
        }
    }

    fun selectVerb(pictogram: LocalPictogram?) {
        viewModelScope.launch {
            verbVersionValue++
            _selectedVerb.value = pictogram to verbVersionValue

            // Persistir para que el motor gramatical lo vea
            settingsRepository.saveSelectedVerbId(pictogram?.id)
            settingsRepository.saveSelectedVerbName(pictogram?.name)
        }
    }

    // En CommunicationViewModel.kt
    fun selectPredicate(pictogram: LocalPictogram?) {
        _selectedPredicate.value = pictogram
    }

    // En CommunicationViewModel.kt - Reemplaza la función clearSelections() existente

    fun clearSelections() {
        viewModelScope.launch {
            // Actualizar versiones para forzar recomposición
            subjectVersionValue++
            verbVersionValue++

            // Limpiar directamente los MutableStateFlow
            _selectedSubject.value = null to subjectVersionValue
            _selectedVerb.value = null to verbVersionValue
            _selectedPredicate.value = null

            // Limpiar preferencias
            settingsRepository.saveSelectedSubjectId(null)
            settingsRepository.saveSelectedVerbId(null)
            settingsRepository.saveSelectedSubjectName(null)
            settingsRepository.saveSelectedVerbName(null)

            Log.d("CommunicationVM", "✅ Pizarra limpiada: Sujeto=null, Verbo=null, Predicado=null")
        }
    }

    fun clearPredicate() {
        _selectedPredicate.value = null
    }
    fun clearSubject() {      // 👈 AGREGA ESTO
        viewModelScope.launch {
            subjectVersionValue++
            _selectedSubject.value = null to subjectVersionValue
            settingsRepository.saveSelectedSubjectId(null)
            settingsRepository.saveSelectedSubjectName(null)
            Log.d("CommunicationVM", "Sujeto limpiado")
        }
    }

    fun clearVerb() {         // 👈 AGREGA ESTO
        viewModelScope.launch {
            verbVersionValue++
            _selectedVerb.value = null to verbVersionValue
            settingsRepository.saveSelectedVerbId(null)
            settingsRepository.saveSelectedVerbName(null)
            Log.d("CommunicationVM", "Verbo limpiado")
        }
    }
    fun forceUiRefresh() {
        viewModelScope.launch {
            subjectVersionValue++
            verbVersionValue++
            _selectedSubject.value = _selectedSubject.value.first to subjectVersionValue
            _selectedVerb.value = _selectedVerb.value.first to verbVersionValue
        }
    }

    fun getSelectedSubject(): LocalPictogram? = _selectedSubject.value.first
    fun getSelectedVerb(): LocalPictogram? = _selectedVerb.value.first
    fun getSelectedPredicate(): LocalPictogram? = _selectedPredicate.value
}
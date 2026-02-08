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
            e.printStackTrace()
        }
    }

    private suspend fun validateSelections() {
        val pictograms = _communicationScreenState.value.pictograms
        val currentSubject = _selectedSubject.value.first
        val currentVerb = _selectedVerb.value.first

        var changed = false

        if (currentSubject != null) {
            val stillExists = pictograms.any { it.id == currentSubject.id && it.type == "subject" }
            if (!stillExists) {
                subjectVersionValue++
                _selectedSubject.value = null to subjectVersionValue
                settingsRepository.saveSelectedSubjectId(null)
                settingsRepository.saveSelectedSubjectName(null)
                changed = true
            }
        }

        if (currentVerb != null) {
            val stillExists = pictograms.any { it.id == currentVerb.id && it.type == "verb" }
            if (!stillExists) {
                verbVersionValue++
                _selectedVerb.value = null to verbVersionValue
                settingsRepository.saveSelectedVerbId(null)
                settingsRepository.saveSelectedVerbName(null)
                changed = true
            }
        }

        if (changed) {
        }
    }

    fun loadCommunicationPictograms() {
        viewModelScope.launch {
            try {
                if (communicationCategoryId != null) {
                    repository.getPictogramsByCategoryId(communicationCategoryId!!).collect { pictograms ->
                        _communicationScreenState.update { state ->
                            state.copy(pictograms = pictograms, isLoading = false)
                        }
                        restoreSelectionsFromPreferences()
                        validateSelections()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _communicationScreenState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun restoreSelectionsFromPreferences() {
        val subjectId = settingsRepository.getSelectedSubjectId()
        val verbId = settingsRepository.getSelectedVerbId()
        val pictograms = _communicationScreenState.value.pictograms

        var changed = false

        if (subjectId != null) {
            val subject = pictograms.find { it.id == subjectId && it.type == "subject" }
            if (subject != null) {
                subjectVersionValue++
                _selectedSubject.value = subject to subjectVersionValue
                settingsRepository.saveSelectedSubjectName(subject.name)
                changed = true
            }
        }

        if (verbId != null) {
            val verb = pictograms.find { it.id == verbId && it.type == "verb" }
            if (verb != null) {
                verbVersionValue++
                _selectedVerb.value = verb to verbVersionValue
                settingsRepository.saveSelectedVerbName(verb.name)
                changed = true
            }
        }

        if (changed) {
            delay(100)
            forceUiRefresh()
        }
    }

    fun selectSubject(pictogram: LocalPictogram) {
        viewModelScope.launch {
            subjectVersionValue++
            _selectedSubject.value = pictogram to subjectVersionValue
            settingsRepository.saveSelectedSubjectId(pictogram.id)
            settingsRepository.saveSelectedSubjectName(pictogram.name)
        }
    }

    fun selectVerb(pictogram: LocalPictogram) {
        viewModelScope.launch {
            verbVersionValue++
            _selectedVerb.value = pictogram to verbVersionValue
            settingsRepository.saveSelectedVerbId(pictogram.id)
            settingsRepository.saveSelectedVerbName(pictogram.name)
        }
    }

    fun forceRefreshSelectedSubject() {
        val current = _selectedSubject.value.first
        if (current != null) {
            subjectVersionValue++
            _selectedSubject.value = current to subjectVersionValue
        }
    }

    fun forceRefreshSelectedVerb() {
        val current = _selectedVerb.value.first
        if (current != null) {
            verbVersionValue++
            _selectedVerb.value = current to verbVersionValue
        }
    }

    fun forceUiRefresh() {
        viewModelScope.launch {
            val currentSubject = _selectedSubject.value.first
            val currentVerb = _selectedVerb.value.first

            if (currentSubject != null) {
                subjectVersionValue++
                _selectedSubject.value = currentSubject to subjectVersionValue
            }

            if (currentVerb != null) {
                verbVersionValue++
                _selectedVerb.value = currentVerb to verbVersionValue
            }
        }
    }

    fun getSelectedSubject(): LocalPictogram? = _selectedSubject.value.first
    fun getSelectedVerb(): LocalPictogram? = _selectedVerb.value.first

    fun clearSelections() {
        viewModelScope.launch {
            subjectVersionValue++
            verbVersionValue++
            _selectedSubject.value = null to subjectVersionValue
            _selectedVerb.value = null to verbVersionValue
            settingsRepository.clearSessionData()
        }
    }
}
package com.pictofly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.PictogramSize
import com.pictofly.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val carouselSize: StateFlow<PictogramSize> = settingsRepository.pictogramSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.getPictogramSize()
        )

    val sentenceSize: PictogramSize = PictogramSize.MEDIUM

    fun updatePictogramSize(newSize: PictogramSize) {
        viewModelScope.launch {
            settingsRepository.savePictogramSize(newSize)
        }
    }

    fun getSentenceSize(): Int = sentenceSize.sentenceSize  // 80dp

    fun getSentenceImageSize(): Int = sentenceSize.sentenceImageSize  // 60dp
}
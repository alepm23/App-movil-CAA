package com.pictofly.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.Pictogram
import com.pictofly.data.model.LocalPictogram
import com.pictofly.repository.CategoryRepository
import com.pictofly.repository.LocalContentRepository
import com.pictofly.repository.UserPictogramOverrideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryPictogramsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val localContentRepository: LocalContentRepository,
    private val userOverrideRepository: UserPictogramOverrideRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryPictogramsUiState())
    val uiState: StateFlow<CategoryPictogramsUiState> = _uiState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val pictogramIdMap = mutableMapOf<String, String>()

    fun loadPictogramsForCategory(categoryName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, categoryName = categoryName)

            pictogramIdMap.clear()

            val allLocalPictograms = localContentRepository.getAllPictogramsSync()

            allLocalPictograms.forEach { localPictogram ->
                pictogramIdMap[localPictogram.name] = localPictogram.id
            }

            categoryRepository.getPictogramsByCategory(categoryName)
                .collect { pictograms ->
                    _uiState.value = CategoryPictogramsUiState(
                        pictograms = pictograms,
                        isLoading = false,
                        categoryName = categoryName
                    )
                }
        }
    }

    fun deletePictogram(pictogram: Pictogram, categoryName: String) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading("Eliminando pictograma")

                if (pictogram.isLocal || pictogram.createdByUser) {
                    val localPictogramId = findLocalPictogramId(pictogram.name)

                    if (localPictogramId != null) {
                        localContentRepository.deletePictogram(localPictogramId)
                        _operationState.value = OperationState.Success("Pictograma eliminado permanentemente")
                         } else {
                        val deleted = deleteAllLocalPictogramsByName(pictogram.name)
                        if (deleted) {
                            _operationState.value = OperationState.Success("Pictograma eliminado permanentemente")
                        } else {
                            userOverrideRepository.hidePredefinedPictogram(categoryName, pictogram.name)
                            _operationState.value = OperationState.Success("Pictograma ocultado")
                        }
                    }
                } else {
                    userOverrideRepository.hidePredefinedPictogram(categoryName, pictogram.name)
                    _operationState.value = OperationState.Success("Pictograma ocultado")
                }

                loadPictogramsForCategory(categoryName)

            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Error al eliminar: ${e.message}")
            }
        }
    }

    private suspend fun findLocalPictogramId(pictogramName: String): String? {
        if (pictogramIdMap.containsKey(pictogramName)) {
            return pictogramIdMap[pictogramName]
        }

        val allLocalPictograms = localContentRepository.getAllPictogramsSync()
        val found = allLocalPictograms.find { it.name == pictogramName }?.id

        if (found != null) {
        }

        return found
    }

    private suspend fun deleteAllLocalPictogramsByName(pictogramName: String): Boolean {
        return try {
            val allLocalPictograms = localContentRepository.getAllPictogramsSync()
            val pictogramsToDelete = allLocalPictograms.filter { it.name == pictogramName }

            if (pictogramsToDelete.isNotEmpty()) {
                pictogramsToDelete.forEach { localPictogram ->
                    localContentRepository.deletePictogram(localPictogram.id)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("CategoryPictogramsVM", "Error en deleteAllLocalPictogramsByName: ${e.message}")
            false
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
package com.pictofly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.Category
import com.pictofly.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllCategoriesUiState())
    val uiState: StateFlow<AllCategoriesUiState> = _uiState.asStateFlow()

    init {
        loadAllCategories()
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { allCategories ->
                val visibleCategories = allCategories.filterNot { category ->
                    if (category.isLocal) {
                        category.name == "Comunicación Personalizada" ||
                                category.name.startsWith("Extensión: ")
                    } else {
                        false
                    }
                }

                _uiState.update {
                    it.copy(
                        categories = visibleCategories,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshCategories() {
        loadAllCategories()
    }
}

data class AllCategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)
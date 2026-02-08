package com.pictofly.viewmodel

import com.pictofly.data.model.Pictogram

data class CategoryPictogramsUiState(
    val pictograms: List<Pictogram> = emptyList(),
    val isLoading: Boolean = true,
    val categoryName: String = ""
)
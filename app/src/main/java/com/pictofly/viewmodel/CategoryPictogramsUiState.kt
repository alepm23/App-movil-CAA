package com.pictofly.viewmodel

import com.pictofly.data.model.Pictogram
//contenedor de datos de picto q se mostrara para la ui
data class CategoryPictogramsUiState(
    val pictograms: List<Pictogram> = emptyList(),
    val isLoading: Boolean = true,
    val categoryName: String = ""
)
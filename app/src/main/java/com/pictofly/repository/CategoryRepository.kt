package com.pictofly.repository

import com.pictofly.data.model.Category
import com.pictofly.data.model.Pictogram
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getPictogramsByCategory(categoryName: String): Flow<List<Pictogram>>
    fun getCategoryByName(name: String): Flow<Category?>
}
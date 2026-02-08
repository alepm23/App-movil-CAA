package com.pictofly.repository

import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.flow.Flow

interface LocalContentRepository {
    val localCategoriesWithCount: Flow<List<LocalCategory>>
    val localPictograms: Flow<List<LocalPictogram>>
    fun getPictogramsByCategoryId(categoryId: String): Flow<List<LocalPictogram>>
    fun getPictogramsByCategoryName(categoryName: String): Flow<List<LocalPictogram>>

    suspend fun addCategory(category: LocalCategory): String
    suspend fun deleteCategory(categoryId: String)
    suspend fun addPictogram(pictogram: LocalPictogram): String
    suspend fun deletePictogram(pictogramId: String)
    suspend fun getAllPictogramsSync(): List<LocalPictogram>
}
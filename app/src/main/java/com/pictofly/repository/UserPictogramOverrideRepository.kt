package com.pictofly.repository

import com.pictofly.data.model.LocalPictogram

interface UserPictogramOverrideRepository {
    suspend fun hidePredefinedPictogram(categoryName: String, pictogramName: String)
    suspend fun showPredefinedPictogram(categoryName: String, pictogramName: String)
    suspend fun addLocalPictogramToPredefinedCategory(
        categoryName: String,
        pictogram: LocalPictogram
    )
    suspend fun getOverridesForCategory(categoryName: String): UserCategoryOverrides
    suspend fun removeUserAddedPictogram(categoryName: String, pictogramName: String)
    suspend fun markCategoryAsDeleted(categoryName: String)
    suspend fun isCategoryDeleted(categoryName: String): Boolean
    suspend fun restoreCategory(categoryName: String)
    suspend fun getAllOverrides(): Map<String, UserCategoryOverrides>
}

data class UserCategoryOverrides(
    val categoryName: String,
    val deleted: Boolean = false,
    val hiddenPictograms: List<String> = emptyList(),
    val addedLocalPictograms: List<LocalPictogram> = emptyList()
)
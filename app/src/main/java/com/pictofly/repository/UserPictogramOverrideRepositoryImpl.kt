package com.pictofly.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_overrides")

@Singleton
class UserPictogramOverrideRepositoryImpl @Inject constructor(
    private val context: Context
) : UserPictogramOverrideRepository {

    private val gson = Gson()
    private val overrideType = object : TypeToken<Map<String, UserCategoryOverrides>>() {}.type
    //hide-ocultar
    override suspend fun hidePredefinedPictogram(categoryName: String, pictogramName: String) {
        val overrides = getOverrides()
        val categoryOverride = overrides[categoryName] ?: UserCategoryOverrides(categoryName)

        val updatedHidden = if (categoryOverride.hiddenPictograms.contains(pictogramName)) {
            categoryOverride.hiddenPictograms
        } else {
            categoryOverride.hiddenPictograms + pictogramName
        }

        val updatedOverride = categoryOverride.copy(hiddenPictograms = updatedHidden)
        saveOverrides(overrides + (categoryName to updatedOverride))
    }
    //mostrar
    override suspend fun showPredefinedPictogram(categoryName: String, pictogramName: String) {
        val overrides = getOverrides()
        val categoryOverride = overrides[categoryName] ?: return

        val updatedHidden = categoryOverride.hiddenPictograms.filter { it != pictogramName }
        val updatedOverride = categoryOverride.copy(hiddenPictograms = updatedHidden)

        saveOverrides(overrides + (categoryName to updatedOverride))
    }

    override suspend fun addLocalPictogramToPredefinedCategory(
        categoryName: String,
        pictogram: LocalPictogram
    ) {
        val overrides = getOverrides()
        val categoryOverride = overrides[categoryName] ?: UserCategoryOverrides(categoryName)

        val updatedAdded = categoryOverride.addedLocalPictograms + pictogram
        val updatedOverride = categoryOverride.copy(addedLocalPictograms = updatedAdded)

        saveOverrides(overrides + (categoryName to updatedOverride))
    }

    override suspend fun getOverridesForCategory(categoryName: String): UserCategoryOverrides {
        return getOverrides()[categoryName] ?: UserCategoryOverrides(categoryName)
    }

    override suspend fun removeUserAddedPictogram(categoryName: String, pictogramName: String) {
        val overrides = getOverrides()
        val categoryOverride = overrides[categoryName] ?: return

        val updatedAdded = categoryOverride.addedLocalPictograms.filter { it.name != pictogramName }
        val updatedOverride = categoryOverride.copy(addedLocalPictograms = updatedAdded)

        saveOverrides(overrides + (categoryName to updatedOverride))
    }

    override suspend fun markCategoryAsDeleted(categoryName: String) {
        val overrides = getOverrides()
        val categoryOverride = overrides[categoryName] ?: UserCategoryOverrides(categoryName)

        val updatedOverride = categoryOverride.copy(deleted = true)
        saveOverrides(overrides + (categoryName to updatedOverride))
    }
    //antes de mostrar hay q saber si esta oculta o eliminada
    override suspend fun isCategoryDeleted(categoryName: String): Boolean {
        return getOverrides()[categoryName]?.deleted ?: false
    }
    //2.0
    override suspend fun restoreCategory(categoryName: String) {
        val overrides = getOverrides()
        val categoryOverride = overrides[categoryName] ?: return

        val updatedOverride = categoryOverride.copy(deleted = false)
        saveOverrides(overrides + (categoryName to updatedOverride))
    }

    override suspend fun getAllOverrides(): Map<String, UserCategoryOverrides> {
        return getOverrides()
    }
//Dependeresmos de esta
    private suspend fun getOverrides(): Map<String, UserCategoryOverrides> {
        return try {
            val json = context.dataStore.data
                .map { it[stringPreferencesKey("category_overrides")] ?: "{}" }
                .first()
            gson.fromJson(json, overrideType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun saveOverrides(overrides: Map<String, UserCategoryOverrides>) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(overrides)
            preferences[stringPreferencesKey("category_overrides")] = json
        }
    }
}
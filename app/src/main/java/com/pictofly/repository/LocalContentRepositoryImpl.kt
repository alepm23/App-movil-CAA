package com.pictofly.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "local_content")

@Singleton
class LocalContentRepositoryImpl @Inject constructor(
    private val context: Context
) : LocalContentRepository {

    private val gson = Gson()
    private val categoryType = object : TypeToken<List<LocalCategory>>() {}.type
    private val pictogramType = object : TypeToken<List<LocalPictogram>>() {}.type

    private val _localCategories = MutableStateFlow<List<LocalCategory>>(emptyList())
    override val localCategoriesWithCount: Flow<List<LocalCategory>>
        get() = _localCategories.asStateFlow()

    private val _localPictograms = MutableStateFlow<List<LocalPictogram>>(emptyList())
    override val localPictograms: Flow<List<LocalPictogram>>
        get() = _localPictograms.asStateFlow()

    override fun getPictogramsByCategoryId(categoryId: String): Flow<List<LocalPictogram>> {
        return _localPictograms.map { pictograms ->
            pictograms.filter { it.categoryId == categoryId }
        }
    }

    override fun getPictogramsByCategoryName(categoryName: String): Flow<List<LocalPictogram>> {
        return combine(_localCategories, _localPictograms) { categories, pictograms ->
            val category = categories.find { it.name == categoryName }
            if (category != null) {
                pictograms.filter { it.categoryId == category.id }
            } else {
                emptyList()
            }
        }
    }
             //corutina
    override suspend fun getAllPictogramsSync(): List<LocalPictogram> {
        return getPictograms()
    }

    init {
        loadFromDataStore()
    }

    private fun loadFromDataStore() {
        context.dataStore.data
            .map { preferences -> //contiene DTS
                val categoriesJson = preferences[stringPreferencesKey("categories")] ?: "[]"
                val pictogramsJson = preferences[stringPreferencesKey("pictograms")] ?: "[]"
                try {
                    val categories = gson.fromJson<List<LocalCategory>>(categoriesJson, categoryType) ?: emptyList()
                    val pictograms = gson.fromJson<List<LocalPictogram>>(pictogramsJson, pictogramType) ?: emptyList()
                    val updatedCategories = categories.map { category ->
                        val count = pictograms.count { it.categoryId == category.id }
                        category.copy(pictogramCount = count)
                    }
                    _localPictograms.value = pictograms
                    updatedCategories
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .onEach { categories ->
                _localCategories.value = categories
            }
            .catch { error ->
                _localCategories.value = emptyList()
                _localPictograms.value = emptyList()
            }
            .launchIn(CoroutineScope(Dispatchers.IO))
    }

    private suspend fun saveData(categories: List<LocalCategory>, pictograms: List<LocalPictogram>) {
        context.dataStore.edit { preferences ->
            val categoriesJson = gson.toJson(categories)
            val pictogramsJson = gson.toJson(pictograms)
            preferences[stringPreferencesKey("categories")] = categoriesJson
            preferences[stringPreferencesKey("pictograms")] = pictogramsJson
        }
        val updatedCategories = categories.map { category ->
            val count = pictograms.count { it.categoryId == category.id }
            category.copy(pictogramCount = count)
        }
        _localCategories.value = updatedCategories
        _localPictograms.value = pictograms
    }

    private suspend fun getPictograms(): List<LocalPictogram> {
        return try {
            val json = context.dataStore.data
                .map { it[stringPreferencesKey("pictograms")] ?: "[]" }
                .first()
            gson.fromJson<List<LocalPictogram>>(json, pictogramType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getCategories(): List<LocalCategory> {
        return try {
            val json = context.dataStore.data
                .map { it[stringPreferencesKey("categories")] ?: "[]" }
                .first()
            gson.fromJson<List<LocalCategory>>(json, categoryType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addCategory(category: LocalCategory): String {
        val newId = System.currentTimeMillis().toString()
        val newCategory = category.copy(id = newId)

        val currentCategories = getCategories().toMutableList()
        val currentPictograms = getPictograms()

        currentCategories.add(newCategory)
        saveData(currentCategories, currentPictograms)

        Log.d("Repository", "Categoría creada: ${newCategory.name} (ID: $newId)")
        return newId
    }

    override suspend fun deleteCategory(categoryId: String) {
        try {
            val currentCategories = getCategories().toMutableList()
            val currentPictograms = getPictograms().toMutableList()
            val categoryToDelete = currentCategories.find { it.id == categoryId }

            if (categoryToDelete != null) {
                val pictogramsToDelete = currentPictograms.filter { it.categoryId == categoryId }
                pictogramsToDelete.forEach { pictogram ->
                    deleteLocalImageFile(pictogram.imagePath)
                }
                currentPictograms.removeAll { it.categoryId == categoryId }
                deleteLocalImageFile(categoryToDelete.imagePath)
                currentCategories.removeAll { it.id == categoryId }
                saveData(currentCategories, currentPictograms)

            } else {
                Log.e("Repository", "No se encontró categoría con ID: $categoryId")
            }

        } catch (e: Exception) {
            Log.e("Repository", "Error eliminando categoría: ${e.message}")
            throw e
        }
    }
    private fun deleteLocalImageFile(imagePath: String) {
        try {
            if (imagePath.isNotEmpty()) {
                if (imagePath.startsWith("http://") ||
                    imagePath.startsWith("https://") ||
                    imagePath.startsWith("content://") ||
                    imagePath.contains("res.cloudinary.com") ||
                    imagePath.contains("cloudinary.com")) {
                    Log.d("Repository", "URL/URI - No se elimina: $imagePath")
                    return
                }
                val file = when {
                    imagePath.startsWith("file://") -> {
                        File(imagePath.substringAfter("file://"))
                    }
                    imagePath.startsWith("/") -> {
                        val f = File(imagePath)
                        if (f.exists()) f else null
                    }
                    else -> {
                        File(context.filesDir, imagePath)
                    }
                }

                file?.let {
                    if (it.exists()) {
                        val deleted = it.delete()
                        if (deleted) {
                            Log.d("Repository", "eliminado: ${it.name}")
                        } else {
                            Log.e("Repository", "No se pudo eliminar: ${it.path}")
                        }
                    } else {
                        Log.d("Repository", "archivo no existe: $imagePath")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error con archivo: ${e.message}")
        }
    }

    override suspend fun addPictogram(pictogram: LocalPictogram): String {
        val newId = System.currentTimeMillis().toString()
        val newPictogram = pictogram.copy(id = newId)

        val currentCategories = getCategories()
        val currentPictograms = getPictograms().toMutableList()

        currentPictograms.add(newPictogram)
        saveData(currentCategories, currentPictograms)

        Log.d("Repository", "Pictograma creado: ${newPictogram.name}")
        return newId
    }

    override suspend fun deletePictogram(pictogramId: String) {
        try {
            val currentCategories = getCategories()
            val currentPictograms = getPictograms().toMutableList()
            val pictogramToDelete = currentPictograms.find { it.id == pictogramId }

            if (pictogramToDelete != null) {
                deleteLocalImageFile(pictogramToDelete.imagePath)
                currentPictograms.removeAll { it.id == pictogramId }

                saveData(currentCategories, currentPictograms)

                Log.d("Repository", "Pictograma eliminado: ${pictogramToDelete.name}")
            }

        } catch (e: Exception) {
            Log.e("Repository", "Error eliminando pictograma: ${e.message}")
            throw e
        }
    }
}
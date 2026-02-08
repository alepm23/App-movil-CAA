package com.pictofly.repository

import android.content.Context
import android.util.Log
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupRepository @Inject constructor(
    private val context: Context,
    private val localContentRepository: LocalContentRepository,
    private val userOverrideRepository: UserPictogramOverrideRepository,
    private val settingsRepository: SettingsRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    suspend fun cleanAllUserData() {
        withContext(Dispatchers.IO) {
            try {
                val defaultCategoryNames = listOf(
                    "Sujeto", "Verbo", "Frutas", "Emociones", "Higiene", "Juegos",
                    "Comida", "Bebidas"
                )

                val categoriesToKeep = mutableListOf<String>()
                val categoriesToDelete = mutableListOf<LocalCategory>()
                val allLocalCategories = localContentRepository.localCategoriesWithCount.first()

                allLocalCategories.forEach { category ->
                    val shouldKeep = when {
                        category.name == "Comunicación Personalizada" -> true
                        category.name in defaultCategoryNames -> true

                        category.name.startsWith("Extensión: ") -> {
                            categoriesToDelete.add(category)
                            return@forEach
                        }
                        else -> {
                            categoriesToDelete.add(category)
                            return@forEach
                        }
                    }

                    if (shouldKeep) {
                        categoriesToKeep.add(category.id)
                    }
                }

                val allPictograms = localContentRepository.getAllPictogramsSync()

                val pictogramsToDelete = allPictograms.filter { pictogram ->
                    pictogram.categoryId !in categoriesToKeep
                }

                pictogramsToDelete.forEach { pictogram ->
                    deleteLocalImageFile(pictogram.imagePath)
                }

                categoriesToDelete.forEach { category ->
                    deleteLocalImageFile(category.imagePath)
                }

                pictogramsToDelete.forEach { pictogram ->
                    try {
                        localContentRepository.deletePictogram(pictogram.id)
                    } catch (e: Exception) {
                    }
                }

                categoriesToDelete.forEach { category ->
                    try {
                        localContentRepository.deleteCategory(category.id)
                    } catch (e: Exception) {
                    }
                }

                resetAllUserOverrides()
                clearSubjectAndVerbSelections()
                settingsRepository.clearSessionData()
                userSettingsRepository.clearAllData()
                cleanupTempDirectories()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private suspend fun clearSubjectAndVerbSelections() {
        try {
            val allPictograms = localContentRepository.getAllPictogramsSync()
            val allLocalCategories = localContentRepository.localCategoriesWithCount.first()
            val communicationCategory = allLocalCategories.find { it.name == "Comunicacion Personalizada" }

            if (communicationCategory != null) {
                val communicationPictograms = allPictograms.filter {
                    it.categoryId == communicationCategory.id
                }
              communicationPictograms.forEach { pictogram ->
                    deleteLocalImageFile(pictogram.imagePath)
                }
                communicationPictograms.forEach { pictogram ->
                    try {
                        localContentRepository.deletePictogram(pictogram.id)
                    } catch (e: Exception) {
                    }
                }
            }

            settingsRepository.saveSelectedSubjectId(null)
            settingsRepository.saveSelectedSubjectName(null)
            settingsRepository.saveSelectedVerbId(null)
            settingsRepository.saveSelectedVerbName(null)
            settingsRepository.clearSessionData()
        } catch (e: Exception) {

        }
    }

    private suspend fun resetAllUserOverrides() {
        try {
            val allOverrides = getAllOverrides()

            allOverrides.keys.forEach { categoryName ->
                try {
                    if (userOverrideRepository.isCategoryDeleted(categoryName)) {
                        userOverrideRepository.restoreCategory(categoryName)
                    }
                    val overrides = userOverrideRepository.getOverridesForCategory(categoryName)
                    overrides.hiddenPictograms.forEach { pictogramName ->
                        userOverrideRepository.showPredefinedPictogram(categoryName, pictogramName)
                    }
                    overrides.addedLocalPictograms.forEach { pictogram ->
                        userOverrideRepository.removeUserAddedPictogram(categoryName, pictogram.name)
                        deleteLocalImageFile(pictogram.imagePath)
                    }

                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun getAllOverrides(): Map<String, UserCategoryOverrides> {
        return try {
            userOverrideRepository.getAllOverrides()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun deleteLocalImageFile(imagePath: String) {
        try {
            if (imagePath.isEmpty()) return

            if (imagePath.startsWith("http://") ||
                imagePath.startsWith("https://") ||
                imagePath.startsWith("content://") ||
                imagePath.contains("res.cloudinary.com") ||
                imagePath.contains("cloudinary.com")) {
                return
            }

            val file = when {
                imagePath.startsWith("file://") -> File(imagePath.substringAfter("file://"))
                imagePath.startsWith("/") -> File(imagePath)
                else -> File(context.filesDir, imagePath)
            }

            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun cleanupTempDirectories() {
        try {
            val dirsToClean = listOf(
                "local_images",
                "communication_images"
            )

            dirsToClean.forEach { dirName ->
                val dir = File(context.filesDir, dirName)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()
                    files?.forEach { file ->
                        if (file.isFile) {
                            val deleted = file.delete()
                            if (deleted) {
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }
}
package com.pictofly.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pictofly.data.model.Category
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import com.pictofly.repository.LocalContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.pictofly.utils.CategoryEventBus
import com.pictofly.utils.CategoryEvent
import javax.inject.Inject
import android.util.Log
import com.pictofly.repository.UserPictogramOverrideRepository

sealed class OperationState {
    object Idle : OperationState()
    data class Loading(val message: String) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

data class AddCategoryState(
    val currentStep: Int = 1,
    val categoryName: String = "",
    val categoryImageUri: Uri? = null,
    val nameError: String? = null,
    val imageError: String? = null,
    val isFlowActive: Boolean = false
)

data class AddPictogramState(
    val currentStep: Int = 1,
    val categoryId: String = "",
    val pictogramName: String = "",
    val pictogramImageUri: Uri? = null,
    val persistedUriString: String? = null,
    val nameError: String? = null,
    val imageError: String? = null,
    val isFlowActive: Boolean = false,
    val addedPictograms: List<LocalPictogram> = emptyList(),
    val selectedType: String = "subject"
)

data class CommunicationModeState(
    val pictograms: List<LocalPictogram> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPictogram: LocalPictogram? = null,
    val showAddDialog: Boolean = false,
    val pictogramName: String = "",
    val pictogramImageUri: Uri? = null,
    val selectedType: String = "subject",
    val showDeleteDialog: Boolean = false,
    val pictogramToDelete: LocalPictogram? = null
)

@HiltViewModel
class LocalContentViewModel @Inject constructor(
    private val repository: LocalContentRepository,
    private val userOverrideRepository: UserPictogramOverrideRepository,
    private val context: Context,
    private val categoryEventBus: CategoryEventBus
) : ViewModel() {

    private val _uiState = mutableStateOf(LocalContentUiState())
    val uiState: State<LocalContentUiState> = _uiState

    private val _allCategoriesState = mutableStateOf(LocalContentUiState())
    val allCategoriesState: State<LocalContentUiState> = _allCategoriesState

    private val _addCategoryState = MutableStateFlow(AddCategoryState())
    val addCategoryState: StateFlow<AddCategoryState> = _addCategoryState.asStateFlow()

    private val _addPictogramState = MutableStateFlow(AddPictogramState())
    val addPictogramState: StateFlow<AddPictogramState> = _addPictogramState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _communicationModeState = MutableStateFlow(CommunicationModeState())
    val communicationModeState: StateFlow<CommunicationModeState> = _communicationModeState.asStateFlow()

    private var communicationCategoryId: String? = null

    init {
        loadCategories()
        loadAllCategoriesForInternalUse()
        initializeCommunicationCategory()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.localCategoriesWithCount.collect { allCategories ->
                val visibleCategories = allCategories.filterNot { category ->
                    category.name == "Comunicación Personalizada" ||
                            category.name.startsWith("Extensión: ")
                }

                _uiState.value = LocalContentUiState(
                    categories = visibleCategories,
                    isLoading = false
                )
            }
        }
    }

    private fun loadAllCategoriesForInternalUse() {
        viewModelScope.launch {
            repository.localCategoriesWithCount.collect { allCategories ->
                _allCategoriesState.value = LocalContentUiState(
                    categories = allCategories,
                    isLoading = false
                )
            }
        }
    }

    fun forceReloadCategories() {
        loadCategories()
        loadAllCategoriesForInternalUse()
    }

    private fun initializeCommunicationCategory() {
        viewModelScope.launch {
            try {
                val categories = _allCategoriesState.value.categories
                val existingCategory = categories.find { it.name == "Comunicación Personalizada" }

                if (existingCategory != null) {
                    communicationCategoryId = existingCategory.id
                } else {
                    val newCategory = LocalCategory(
                        id = "",
                        name = "Comunicación Personalizada",
                        imagePath = "",
                        color = "#FF4081",
                        pictogramCount = 0,
                        createdAt = System.currentTimeMillis()
                    )

                    val newId = repository.addCategory(newCategory)
                    communicationCategoryId = newId
                    loadAllCategoriesForInternalUse()
                }

                loadCommunicationPictograms()

            } catch (e: Exception) {
                Log.e("CommunicationVM", "Error inicializando categoría: ${e.message}")
            }
        }
    }

    fun loadCommunicationPictograms() {
        viewModelScope.launch {
            try {
                _communicationModeState.value = _communicationModeState.value.copy(
                    isLoading = true
                )

                if (communicationCategoryId != null) {
                    repository.getPictogramsByCategoryId(communicationCategoryId!!).collect { pictograms ->
                        _communicationModeState.value = _communicationModeState.value.copy(
                            pictograms = pictograms,
                            isLoading = false
                        )
                    }
                } else {
                    _communicationModeState.value = _communicationModeState.value.copy(
                        pictograms = emptyList(),
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _communicationModeState.value = _communicationModeState.value.copy(
                    pictograms = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun updatePictogramType(type: String) {
        _communicationModeState.value = _communicationModeState.value.copy(
            selectedType = type
        )
    }

    fun showAddPictogramDialog() {
        _communicationModeState.value = _communicationModeState.value.copy(
            showAddDialog = true,
            pictogramName = "",
            pictogramImageUri = null,
            selectedType = "subject"
        )
    }

    fun hideAddPictogramDialog() {
        _communicationModeState.value = _communicationModeState.value.copy(
            showAddDialog = false,
            pictogramName = "",
            pictogramImageUri = null
        )
    }

    fun updatePictogramName(name: String) {
        _communicationModeState.value = _communicationModeState.value.copy(
            pictogramName = name
        )
    }

    fun updatePictogramImageUri(uri: Uri?) {
        _communicationModeState.value = _communicationModeState.value.copy(
            pictogramImageUri = uri
        )
    }

    fun saveCommunicationPictogram(context: Context) {
        viewModelScope.launch {
            val state = _communicationModeState.value

            if (state.pictogramName.isBlank()) {
                _operationState.value = OperationState.Error("El nombre no puede estar vacío")
                return@launch
            }

            if (state.pictogramImageUri == null) {
                _operationState.value = OperationState.Error("Debes seleccionar una imagen")
                return@launch
            }

            if (communicationCategoryId == null) {
                _operationState.value = OperationState.Error("Error: categoría no disponible")
                return@launch
            }

            _operationState.value = OperationState.Loading("Guardando pictograma...")

            try {
                val imageFileName = "communication_${System.currentTimeMillis()}.jpg"
                val imagesDir = File(context.filesDir, "communication_images")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                val imagePath = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(state.pictogramImageUri!!)?.use { inputStream ->
                        val outputFile = File(imagesDir, imageFileName)
                        outputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        "communication_images/$imageFileName"
                    }
                } ?: throw Exception("Error al guardar la imagen")

                val newPictogram = LocalPictogram(
                    id = "",
                    categoryId = communicationCategoryId!!,
                    name = state.pictogramName,
                    imagePath = imagePath,
                    soundPath = null,
                    type = state.selectedType,
                    createdAt = System.currentTimeMillis()
                )

                repository.addPictogram(newPictogram)

                categoryEventBus.emit(CategoryEvent.PictogramsUpdated("Comunicación Personalizada"))

                _operationState.value = OperationState.Success("Pictograma guardado exitosamente")

                hideAddPictogramDialog()
                loadCommunicationPictograms()

                delay(1000)
                _operationState.value = OperationState.Idle

            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    fun showDeletePictogramDialog(pictogram: LocalPictogram) {
        _communicationModeState.value = _communicationModeState.value.copy(
            showDeleteDialog = true,
            pictogramToDelete = pictogram
        )
    }

    fun hideDeletePictogramDialog() {
        _communicationModeState.value = _communicationModeState.value.copy(
            showDeleteDialog = false,
            pictogramToDelete = null
        )
    }

    fun deleteCommunicationPictogram() {
        viewModelScope.launch {
            val pictogram = _communicationModeState.value.pictogramToDelete
            if (pictogram == null) {
                _operationState.value = OperationState.Error("No hay pictograma para eliminar")
                return@launch
            }

            _operationState.value = OperationState.Loading("Eliminando pictograma...")

            try {
                val deletedId = pictogram.id
                val deletedType = pictogram.type
                val deletedName = pictogram.name

                repository.deletePictogram(pictogram.id)

                _operationState.value = OperationState.Success("Pictograma eliminado")

                hideDeletePictogramDialog()
                loadCommunicationPictograms()

                delay(300)

                delay(1000)
                _operationState.value = OperationState.Idle

            } catch (e: Exception) {
                Log.e("CommunicationVM", "Error eliminando pictograma: ${e.message}")
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    fun getSubjectPictograms(): List<LocalPictogram> {
        return _communicationModeState.value.pictograms.filter { it.type == "subject" }
    }

    fun getVerbPictograms(): List<LocalPictogram> {
        return _communicationModeState.value.pictograms.filter { it.type == "verb" }
    }

    fun startAddCategoryFlow() {
        _addCategoryState.value = AddCategoryState(
            currentStep = 1,
            categoryName = "",
            categoryImageUri = null,
            nameError = null,
            imageError = null,
            isFlowActive = true
        )
    }

    fun cancelAddCategoryFlow() {
        _addCategoryState.value = AddCategoryState()
        _operationState.value = OperationState.Idle
    }

    fun setCategoryName(name: String) {
        val error = if (name.length < 2) {
            "Nombre muy corto (mínimo 2 caracteres)"
        } else if (name.length > 50) {
            "Nombre muy largo (máximo 50 caracteres)"
        } else {
            null
        }

        _addCategoryState.value = _addCategoryState.value.copy(
            categoryName = name,
            nameError = error
        )
    }

    fun setCategoryImageUri(uri: Uri) {
        _addCategoryState.value = _addCategoryState.value.copy(
            categoryImageUri = uri,
            imageError = null
        )
    }

    fun clearCategoryImage() {
        _addCategoryState.value = _addCategoryState.value.copy(
            categoryImageUri = null
        )
    }

    fun goToNextStep() {
        val currentState = _addCategoryState.value
        when (currentState.currentStep) {
            1 -> {
                if (currentState.categoryName.isNotEmpty() && currentState.nameError == null) {
                    _addCategoryState.value = currentState.copy(currentStep = 2)
                }
            }
            2 -> {
                if (currentState.categoryImageUri != null) {
                    _addCategoryState.value = currentState.copy(currentStep = 3)
                } else {
                    _addCategoryState.value = currentState.copy(
                        imageError = "Debes seleccionar una imagen"
                    )
                }
            }
        }
    }

    fun goToPreviousStep() {
        val currentState = _addCategoryState.value
        if (currentState.currentStep > 1) {
            _addCategoryState.value = currentState.copy(
                currentStep = currentState.currentStep - 1
            )
        }
    }

    fun completeCategory(
        context: Context,
        onSuccess: (categoryId: String) -> Unit
    ) {
        viewModelScope.launch {
            _completeCategory(context, onSuccess)
        }
    }

    private suspend fun _completeCategory(
        context: Context,
        onSuccess: (categoryId: String) -> Unit
    ) {
        val state = _addCategoryState.value

        if (state.categoryName.isEmpty() || state.categoryImageUri == null) {
            _operationState.value = OperationState.Error("Datos incompletos")
            return
        }

        _operationState.value = OperationState.Loading("Creando categoría...")

        try {
            val imageFileName = "category_${System.currentTimeMillis()}.jpg"

            val imagePath = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(state.categoryImageUri!!)?.use { inputStream ->
                    val imagesDir = File(context.filesDir, "local_images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()

                    val outputFile = File(imagesDir, imageFileName)
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    "local_images/$imageFileName"
                }
            } ?: throw Exception("Error al guardar la imagen")

            val newCategory = LocalCategory(
                id = "",
                name = state.categoryName,
                imagePath = imagePath,
                color = "#4CAF50",
                pictogramCount = 0,
                createdAt = System.currentTimeMillis()
            )

            val categoryId = repository.addCategory(newCategory)

            _operationState.value = OperationState.Success("Categoría creada exitosamente")
            _addCategoryState.value = state.copy(currentStep = 4)

            delay(1000)
            onSuccess(categoryId)

        } catch (e: Exception) {
            _operationState.value = OperationState.Error("Error: ${e.message}")
        }
    }

    fun startAddPictogramsFlow(categoryId: String) {
        _addPictogramState.value = AddPictogramState(
            currentStep = 1,
            categoryId = categoryId,
            pictogramName = "",
            pictogramImageUri = null,
            persistedUriString = null,
            nameError = null,
            imageError = null,
            isFlowActive = true,
            addedPictograms = emptyList(),
            selectedType = "subject"
        )
    }

    fun updateAddPictogramType(type: String) {
        _addPictogramState.value = _addPictogramState.value.copy(
            selectedType = type
        )
    }

    fun setPictogramName(name: String) {
        val error = if (name.length < 2) {
            "Nombre muy corto (mínimo 2 caracteres)"
        } else if (name.length > 30) {
            "Nombre muy largo (máximo 30 caracteres)"
        } else {
            null
        }

        _addPictogramState.value = _addPictogramState.value.copy(
            pictogramName = name,
            nameError = error
        )
    }

    fun setPictogramImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                    }
                }

                _addPictogramState.value = _addPictogramState.value.copy(
                    pictogramImageUri = uri,
                    persistedUriString = uri.toString(),
                    imageError = null
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al setear URI", e)
            }
        }
    }

    fun restorePictogramImageUri() {
        val uriString = _addPictogramState.value.persistedUriString
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                _addPictogramState.value = _addPictogramState.value.copy(
                    pictogramImageUri = uri
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al restaurar URI", e)
            }
        }
    }

    fun clearPictogramImage() {
        _addPictogramState.value = _addPictogramState.value.copy(
            pictogramImageUri = null,
            persistedUriString = null
        )
    }

    fun addCurrentPictogram(
        context: Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _addCurrentPictogram(context, onSuccess)
        }
    }

    private suspend fun getCategoryNameById(categoryId: String): String {

        val categories = _allCategoriesState.value.categories
        categories.find { it.id == categoryId }?.let {
            return it.name
        }

        return try {
            val allCategories = repository.localCategoriesWithCount.first()
            val category = allCategories.find { it.id == categoryId }
            if (category != null) {
                category.name
            } else {
                "Categoría"
            }
        } catch (e: Exception) {
            Log.e("CategoryName", "Error obteniendo nombre: ${e.message}")
            "Categoría"
        }
    }

    private suspend fun _addCurrentPictogram(
        context: Context,
        onSuccess: () -> Unit
    ) {
        val state = _addPictogramState.value

        if (!state.categoryId.matches(Regex("\\d+"))) {
            Log.e("AddPictogram", "ERROR: categoryId no es un numero válido: ${state.categoryId}")
        } else {
        }

        if (state.pictogramName.isEmpty() || state.pictogramImageUri == null) {
            _operationState.value = OperationState.Error("Datos incompletos")
            return
        }

        _operationState.value = OperationState.Loading("Agregando pictograma...")

        try {
            val imageFileName = "pictogram_${System.currentTimeMillis()}.jpg"

            val imagePath = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(state.pictogramImageUri!!)?.use { inputStream ->
                    val imagesDir = File(context.filesDir, "local_images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()

                    val outputFile = File(imagesDir, imageFileName)
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    "local_images/$imageFileName"
                }
            } ?: throw Exception("Error al guardar la imagen")

            val newPictogram = LocalPictogram(
                id = "",
                categoryId = state.categoryId,
                name = state.pictogramName,
                imagePath = imagePath,
                soundPath = null,
                type = state.selectedType,
                createdAt = System.currentTimeMillis()
            )

            val pictogramId = repository.addPictogram(newPictogram)
            val updatedPictograms = state.addedPictograms + newPictogram.copy(id = pictogramId)

            _addPictogramState.value = state.copy(
                currentStep = 2,
                pictogramName = "",
                pictogramImageUri = null,
                persistedUriString = null,
                addedPictograms = updatedPictograms
            )

            val categoryName = getCategoryNameById(state.categoryId)
            if (categoryName != "Categoría") {
                categoryEventBus.emit(CategoryEvent.PictogramsUpdated(categoryName))
            } else {
                Log.e("EventBus", "No se pudo obtener nombre para categoryId: ${state.categoryId}")
            }

            _operationState.value = OperationState.Success("Pictograma agregado")
            onSuccess()

        } catch (e: Exception) {
            Log.e("AddPictogram", "Error al guardar pictograma: ${e.message}")
            e.printStackTrace()
            _operationState.value = OperationState.Error("Error: ${e.message}")
        }
    }

    fun addAnotherPictogram() {
        _addPictogramState.value = _addPictogramState.value.copy(
            currentStep = 1,
            pictogramName = "",
            pictogramImageUri = null,
            persistedUriString = null,
            nameError = null,
            imageError = null
        )
        _operationState.value = OperationState.Idle
    }

    fun skipAddPictograms(onComplete: () -> Unit) {
        _addPictogramState.value = AddPictogramState()
        _operationState.value = OperationState.Idle
        onComplete()
    }

    fun completeAddPictogramsFlow(onComplete: () -> Unit) {
        viewModelScope.launch {
            _operationState.value = OperationState.Success("Pictogramas guardados")
            delay(1000)
            _addPictogramState.value = AddPictogramState()
            _operationState.value = OperationState.Idle
            onComplete()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Eliminando categoría...")
            try {
                repository.deleteCategory(categoryId)
                _operationState.value = OperationState.Success("Categoría eliminada")
                loadCategories()
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Error al eliminar: ${e.message}")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    fun deleteCategoryPermanently(category: Category) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Eliminando categoria")

            try {
                if (!category.isLocal) {
                    _operationState.value = OperationState.Error("Solo se pueden eliminar categorías locales")
                    return@launch
                }

                val localCategories = repository.localCategoriesWithCount.first()

                val localCategory = localCategories.find { localCat ->
                    localCat.name.equals(category.name, ignoreCase = true) ||
                            (category.localImagePath.isNotEmpty() &&
                                    localCat.imagePath.contains(category.localImagePath)) ||
                            (category.localFileUri != null &&
                                    category.localFileUri.contains(localCat.id))
                }

                if (localCategory != null) {
                    repository.deleteCategory(localCategory.id)
                } else {
                    val foundByName = localCategories.find { it.name == category.name }

                    if (foundByName != null) {
                        repository.deleteCategory(foundByName.id)
                    } else {
                        _operationState.value = OperationState.Error("No se encontro la categoria")
                        return@launch
                    }
                }

                loadCategories()
                forceReloadCategories()

                delay(300)
                _operationState.value = OperationState.Success("Categoría eliminada")

                delay(2000)
                _operationState.value = OperationState.Idle

            } catch (e: Exception) {
                Log.e("DeleteCategory", "Error: ${e.message}")
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    private fun deleteImageFile(context: Context, imagePath: String) {
        try {
            val file = if (imagePath.startsWith("/")) {
                File(imagePath)
            } else if (imagePath.isNotEmpty()) {
                File(context.filesDir, imagePath)
            } else {
                null
            }

            file?.let {
                if (it.exists()) {
                    val deleted = it.delete()
                    if (deleted) {
                    } else {
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeleteFile", "Error eliminando archivo: ${e.message}")
        }
    }

    fun hideDefaultCategory(categoryName: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Ocultando categoría...")

            try {
                userOverrideRepository.markCategoryAsDeleted(categoryName)

                forceReloadCategories()

                _operationState.value = OperationState.Success("Categoría '$categoryName' oculta")

                delay(2000)
                _operationState.value = OperationState.Idle

            } catch (e: Exception) {
                Log.e("LocalContentVM", "Error ocultando categoría: ${e.message}")
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    fun restoreDefaultCategory(categoryName: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Restaurando categoría...")

            try {
                userOverrideRepository.restoreCategory(categoryName)

                forceReloadCategories()

                _operationState.value = OperationState.Success("Categoría '$categoryName' restaurada")

                delay(2000)
                _operationState.value = OperationState.Idle

            } catch (e: Exception) {
                Log.e("LocalContentVM", "Error restaurando categoría: ${e.message}")
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    suspend fun isCategoryHidden(categoryName: String): Boolean {
        return userOverrideRepository.isCategoryDeleted(categoryName)
    }

    suspend fun getPictogramsForPredefinedCategory(categoryName: String): List<LocalPictogram> {
        return try {
            val localCategories = repository.localCategoriesWithCount.first()
            val extensionCategoryName = "Extensión: $categoryName"
            val extensionCategory = localCategories.find { it.name == extensionCategoryName }

            if (extensionCategory != null) {
                val allPictograms = repository.localPictograms.first()
                allPictograms.filter { it.categoryId == extensionCategory.id }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("LocalContentVM", "Error obteniendo pictogramas de extensión: ${e.message}")
            emptyList()
        }
    }

    fun cleanEmptyExtensions() {
        viewModelScope.launch {
            try {
                val localCategories = repository.localCategoriesWithCount.first()
                val allPictograms = repository.localPictograms.first()

                localCategories.forEach { category ->
                    if (category.name.startsWith("Extensión: ")) {
                        val pictogramCount = allPictograms.count { it.categoryId == category.id }

                        if (pictogramCount == 0) {
                            repository.deleteCategory(category.id)
                            Log.d("Cleanup", "Eliminada extensión vacía: ${category.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Cleanup", "Error limpiando extensiones: ${e.message}")
            }
        }
    }

    fun navigateToAddPictograms(
        category: Category,
        context: Context,
        onCategoryReady: (categoryId: String?) -> Unit
    ) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Preparando categoría...")

            try {
                if (category.isLocal) {
                    val foundCategory = findLocalCategory(category)
                    if (foundCategory != null) {
                        onCategoryReady(foundCategory.id)
                    } else {
                        _operationState.value = OperationState.Error("Categoría no encontrada")
                    }
                    return@launch
                }

                println("Es una categoría PREDETERMINADA: ${category.name}")

                val localCategories = repository.localCategoriesWithCount.first()
                val extensionCategoryName = "Extensión: ${category.name}"
                val existingExtension = localCategories.find { it.name == extensionCategoryName }

                if (existingExtension != null) {
                    onCategoryReady(existingExtension.id)
                } else {
                    onCategoryReady(null)
                    _operationState.value = OperationState.Idle
                }

            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    private suspend fun findLocalCategory(category: Category): LocalCategory? {
        val localCategories = repository.localCategoriesWithCount.first()

        localCategories.find { it.name == category.name }?.let { return it }

        if (category.localImagePath.isNotEmpty()) {
            localCategories.find {
                it.imagePath == category.localImagePath ||
                        category.localImagePath.contains(it.imagePath) ||
                        it.imagePath.contains(category.localImagePath)
            }?.let { return it }
        }

        localCategories.find { localCat ->
            localCat.name.equals(category.name, ignoreCase = true) ||
                    localCat.name.replace(" ", "").equals(category.name.replace(" ", ""), ignoreCase = true)
        }?.let { return it }

        return null
    }

    fun getPictogramsForCategory(categoryId: String): List<LocalPictogram> {
        return emptyList()
    }

    fun deletePictograms(categoryId: String, pictogramIds: List<String>) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Eliminando pictogramas...")
            try {
                pictogramIds.forEach { pictogramId ->
                    repository.deletePictogram(pictogramId)
                }
                _operationState.value = OperationState.Success("Pictogramas eliminados")
                loadCategories()
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Error al eliminar: ${e.message}")
            }
        }
    }

    fun deleteAllPictogramsFromCategory(categoryId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Eliminando todos los pictogramas...")
            try {
                val pictograms = getPictogramsForCategory(categoryId)
                val pictogramIds = pictograms.map { it.id }
                pictogramIds.forEach { pictogramId ->
                    repository.deletePictogram(pictogramId)
                }
                _operationState.value = OperationState.Success("Todos los pictogramas eliminados")
                loadCategories()
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Error: ${e.message}")
            }
        }
    }

    suspend fun findExtensionCategoryId(categoryName: String): String? {
        return try {
            val extensionName = "Extensión: $categoryName"
            val categories = repository.localCategoriesWithCount.first()
            categories.find { it.name == extensionName }?.id
        } catch (e: Exception) {
            Log.e("LocalContentVM", "Error buscando extensión: ${e.message}")
            null
        }
    }

    suspend fun createExtensionCategory(categoryName: String): String? {
        return try {
            val extensionCategory = LocalCategory(
                id = "",
                name = "Extensión: $categoryName",
                imagePath = "",
                color = "#4CAF50",
                pictogramCount = 0,
                createdAt = System.currentTimeMillis()
            )

            val newId = repository.addCategory(extensionCategory)
            forceReloadCategories()

            newId
        } catch (e: Exception) {
            Log.e("LocalContentVM", "Error creando extensión: ${e.message}")
            null
        }
    }
    suspend fun findOrCreateExtensionCategory(categoryName: String): String? {
        val existingId = findExtensionCategoryId(categoryName)
        if (existingId != null) {
            return existingId
        }
        return createExtensionCategory(categoryName)
    }

    fun findLocalCategoryId(category: Category, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val categoryId = findLocalCategoryIdSync(category)
            onResult(categoryId)
        }
    }

    suspend fun findLocalCategoryIdSync(category: Category): String? {
        return try {
            val localCategories = repository.localCategoriesWithCount.first()

            localCategories.find { it.name == category.name }?.let { return it.id }

            localCategories.find {
                it.name.equals(category.name, ignoreCase = true)
            }?.let { return it.id }

            if (category.localImagePath.isNotEmpty()) {
                localCategories.find {
                    it.imagePath == category.localImagePath ||
                            category.localImagePath.contains(it.imagePath)
                }?.let { return it.id }
            }

            null
        } catch (e: Exception) {
            Log.e("LocalContentVM", "Error buscando categoría: ${e.message}")
            null
        }
    }

    fun ensureCategoryExists(categoryIdOrName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val localCategories = repository.localCategoriesWithCount.first()
                val isNumericId = categoryIdOrName.matches(Regex("\\d+"))

                if (isNumericId) {
                    val existsById = localCategories.any { it.id == categoryIdOrName }
                    if (existsById) {
                        onResult(true, categoryIdOrName)
                        return@launch
                    }
                }

                val categoryByName = localCategories.find { it.name == categoryIdOrName }
                if (categoryByName != null) {
                    onResult(true, categoryByName.id)
                    return@launch
                }

                val extensionName = "Extensión: $categoryIdOrName"
                val extensionCategory = localCategories.find { it.name == extensionName }
                if (extensionCategory != null) {
                    onResult(true, extensionCategory.id)
                    return@launch
                }

                val newExtensionId = createExtensionCategory(categoryIdOrName)

                if (newExtensionId != null) {
                    onResult(true, newExtensionId)
                } else {
                    onResult(false, "")
                }

            } catch (e: Exception) {
                Log.e("EnsureCategory", "Error: ${e.message}")
                onResult(false, "")
            }
        }
    }

    fun showError(message: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Error(message)
            delay(3000)
            _operationState.value = OperationState.Idle
        }
    }
}

data class LocalContentUiState(
    val categories: List<LocalCategory> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
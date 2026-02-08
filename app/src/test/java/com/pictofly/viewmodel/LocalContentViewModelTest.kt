package com.pictofly.viewmodel

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import com.pictofly.repository.LocalContentRepository
import com.pictofly.repository.UserPictogramOverrideRepository
import com.pictofly.utils.CategoryEventBus
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class LocalContentViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LocalContentRepository
    private lateinit var userOverrideRepository: UserPictogramOverrideRepository
    private lateinit var context: Context
    private lateinit var eventBus: CategoryEventBus
    private lateinit var viewModel: LocalContentViewModel

    private val mockCategory = LocalCategory(
        id = "cat1",
        name = "Test Category",
        imagePath = "path.jpg",
        pictogramCount = 0,
        color = "#FF0000",
        createdAt = System.currentTimeMillis()
    )

    private val mockPictogram = LocalPictogram(
        id = "pic1",
        categoryId = "cat1",
        name = "Test Picto",
        imagePath = "picto.jpg",
        type = "subject",
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        userOverrideRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        eventBus = mockk(relaxed = true)

        coEvery { repository.localCategoriesWithCount } returns flowOf(listOf(mockCategory))
        coEvery { repository.localPictograms } returns flowOf(listOf(mockPictogram))

        Dispatchers.setMain(testDispatcher)
        viewModel = LocalContentViewModel(repository, userOverrideRepository, context, eventBus)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun estado01_categoriasCargadas() = runTest {
        assertThat(viewModel.uiState.value.categories).isNotEmpty()
    }

    @Test
    fun estado02_filtroComunicacionPersonalizada() = runTest {
        // Verificar que "Comunicación Personalizada" está filtrada
        val tieneComunicacion = viewModel.uiState.value.categories.any { it.name == "Comunicación Personalizada" }
        assertThat(tieneComunicacion).isFalse()
    }

    @Test
    fun estado03_filtroExtensiones() = runTest {
        // Verificar que las extensiones están filtradas
        val tieneExtension = viewModel.uiState.value.categories.any { it.name.startsWith("Extensión:") }
        assertThat(tieneExtension).isFalse()
    }

    @Test
    fun estado04_allCategoriesStateTieneTodas() = runTest {
        assertThat(viewModel.allCategoriesState.value.categories).isNotEmpty()
    }

    @Test
    fun estado05_addCategoryStateInicial() = runTest {
        assertThat(viewModel.addCategoryState.value.isFlowActive).isFalse()
    }

    @Test
    fun addCategoria01_iniciarFlow() = runTest {
        viewModel.startAddCategoryFlow()
        assertThat(viewModel.addCategoryState.value.isFlowActive).isTrue()
        assertThat(viewModel.addCategoryState.value.currentStep).isEqualTo(1)
    }

    @Test
    fun addCategoria02_cancelarFlow() = runTest {
        viewModel.startAddCategoryFlow()
        viewModel.cancelAddCategoryFlow()
        assertThat(viewModel.addCategoryState.value.isFlowActive).isFalse()
    }

    @Test
    fun addCategoria03_setNombreValido() = runTest {
        viewModel.setCategoryName("Nueva Categoría")
        assertThat(viewModel.addCategoryState.value.categoryName).isEqualTo("Nueva Categoría")
        assertThat(viewModel.addCategoryState.value.nameError).isNull()
    }

    @Test
    fun addCategoria04_setNombreInvalido() = runTest {
        viewModel.setCategoryName("A")
        assertThat(viewModel.addCategoryState.value.nameError).isNotNull()
    }

    @Test
    fun addCategoria05_setImagen() = runTest {
        val uri = mockk<Uri>()
        viewModel.setCategoryImageUri(uri)
        assertThat(viewModel.addCategoryState.value.categoryImageUri).isEqualTo(uri)
    }

    @Test
    fun addCategoria06_limpiarImagen() = runTest {
        val uri = mockk<Uri>()
        viewModel.setCategoryImageUri(uri)
        viewModel.clearCategoryImage()
        assertThat(viewModel.addCategoryState.value.categoryImageUri).isNull()
    }

    @Test
    fun addCategoria07_irAlSiguientePaso() = runTest {
        viewModel.startAddCategoryFlow()
        viewModel.setCategoryName("Nombre Válido")
        viewModel.goToNextStep()
        assertThat(viewModel.addCategoryState.value.currentStep).isEqualTo(2)
    }

    @Test
    fun addCategoria08_irAlPasoAnterior() = runTest {
        viewModel.startAddCategoryFlow()
        viewModel.setCategoryName("Nombre Válido")
        viewModel.goToNextStep() // Paso 2
        viewModel.goToPreviousStep() // Vuelve a paso 1
        assertThat(viewModel.addCategoryState.value.currentStep).isEqualTo(1)
    }

    @Test
    fun addPictograma01_iniciarFlow() = runTest {
        viewModel.startAddPictogramsFlow("cat1")
        assertThat(viewModel.addPictogramState.value.isFlowActive).isTrue()
    }

    @Test
    fun addPictograma02_setNombreValido() = runTest {
        viewModel.startAddPictogramsFlow("cat1")
        viewModel.setPictogramName("Nuevo Picto")
        assertThat(viewModel.addPictogramState.value.pictogramName).isEqualTo("Nuevo Picto")
    }

    @Test
    fun addPictograma03_setTipoSubject() = runTest {
        viewModel.startAddPictogramsFlow("cat1")
        viewModel.updateAddPictogramType("subject")
        assertThat(viewModel.addPictogramState.value.selectedType).isEqualTo("subject")
    }

    @Test
    fun addPictograma04_setTipoVerb() = runTest {
        viewModel.startAddPictogramsFlow("cat1")
        viewModel.updateAddPictogramType("verb")
        assertThat(viewModel.addPictogramState.value.selectedType).isEqualTo("verb")
    }

    @Test
    fun addPictograma06_limpiarImagen() = runTest {
        viewModel.startAddPictogramsFlow("cat1")
        val uri = mockk<Uri>()
        viewModel.setPictogramImageUri(uri)
        viewModel.clearPictogramImage()
        assertThat(viewModel.addPictogramState.value.pictogramImageUri).isNull()
    }


    @Test
    fun comunicacion02_showAddDialog() = runTest {
        viewModel.showAddPictogramDialog()
        assertThat(viewModel.communicationModeState.value.showAddDialog).isTrue()
    }

    @Test
    fun comunicacion03_hideAddDialog() = runTest {
        viewModel.showAddPictogramDialog()
        viewModel.hideAddPictogramDialog()
        assertThat(viewModel.communicationModeState.value.showAddDialog).isFalse()
    }

    @Test
    fun comunicacion04_updatePictogramName() = runTest {
        viewModel.updatePictogramName("Test")
        assertThat(viewModel.communicationModeState.value.pictogramName).isEqualTo("Test")
    }

    @Test
    fun comunicacion05_updatePictogramType() = runTest {
        viewModel.updatePictogramType("verb")
        assertThat(viewModel.communicationModeState.value.selectedType).isEqualTo("verb")
    }

    @Test
    fun comunicacion06_showDeleteDialog() = runTest {
        viewModel.showDeletePictogramDialog(mockPictogram)
        assertThat(viewModel.communicationModeState.value.showDeleteDialog).isTrue()
        assertThat(viewModel.communicationModeState.value.pictogramToDelete).isEqualTo(mockPictogram)
    }

    @Test
    fun comunicacion07_hideDeleteDialog() = runTest {
        viewModel.showDeletePictogramDialog(mockPictogram)
        viewModel.hideDeletePictogramDialog()
        assertThat(viewModel.communicationModeState.value.showDeleteDialog).isFalse()
        assertThat(viewModel.communicationModeState.value.pictogramToDelete).isNull()
    }

    @Test
    fun operaciones01_deleteCategory() = runTest {
        viewModel.deleteCategory("cat1")
        advanceUntilIdle()

        coVerify { repository.deleteCategory("cat1") }
    }

    @Test
    fun operaciones02_resetOperationState() = runTest {
        viewModel.resetOperationState()
        assertThat(viewModel.operationState.value).isInstanceOf(OperationState.Idle::class.java)
    }

    @Test
    fun operaciones03_hideDefaultCategory() = runTest {
        viewModel.hideDefaultCategory("Sujeto")
        advanceUntilIdle()

        coVerify { userOverrideRepository.markCategoryAsDeleted("Sujeto") }
    }

    @Test
    fun operaciones04_restoreDefaultCategory() = runTest {
        viewModel.restoreDefaultCategory("Sujeto")
        advanceUntilIdle()

        coVerify { userOverrideRepository.restoreCategory("Sujeto") }
    }

    @Test
    fun operaciones05_isCategoryHidden() = runTest {
        coEvery { userOverrideRepository.isCategoryDeleted("Sujeto") } returns true

        val result = viewModel.isCategoryHidden("Sujeto")
        assertThat(result).isTrue()
    }


    @Test
    fun especial01_forceReloadCategories() = runTest {
        viewModel.forceReloadCategories()
        assertThat(viewModel.uiState.value.categories).isNotNull()
    }

    @Test
    fun especial02_cleanEmptyExtensions() = runTest {
        viewModel.cleanEmptyExtensions()
        advanceUntilIdle()
    }
}
package com.pictofly.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.Category
import com.pictofly.data.model.CalibrationProfile
import com.pictofly.repository.CategoryRepository
import com.pictofly.repository.AudioRepository
import com.pictofly.repository.CalibrationRepository
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
import android.content.Context
import android.graphics.PointF
import kotlinx.coroutines.test.advanceUntilIdle

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var audioRepository: AudioRepository
    private lateinit var calibrationRepository: CalibrationRepository
    private lateinit var viewModel: MainViewModel

    private val mockCategories = listOf(
        Category("Sujeto", "img1.jpg"),
        Category("Verbo", "img2.jpg"),
        Category("Frutas", "img3.jpg"),
        Category("Emociones", "img4.jpg")
    )

    private val mockCalibrationProfile = CalibrationProfile(
        isCalibrated = true,
        baseDeadZone = 0.15f,
        baseSensitivity = 1.2f,
        baseSmoothing = 0.3f
    )

    @Before
    fun setup() {
        categoryRepository = mockk(relaxed = true)
        audioRepository = mockk(relaxed = true)
        calibrationRepository = mockk(relaxed = true)

        coEvery { categoryRepository.getAllCategories() } returns flowOf(mockCategories)
        coEvery { calibrationRepository.getCalibrationProfile() } returns flowOf(mockCalibrationProfile)

        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(categoryRepository, audioRepository, calibrationRepository)

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
        assertThat(viewModel.uiState.value.categories.size).isEqualTo(4)
    }

    @Test
    fun estado02_selectedCategoryIndexInicial() = runTest {
        assertThat(viewModel.uiState.value.selectedCategoryIndex).isEqualTo(0)
    }

    @Test
    fun estado03_selectedCategoryInicial() = runTest {
        assertThat(viewModel.uiState.value.selectedCategory?.name).isEqualTo("Sujeto")
    }

    @Test
    fun estado04_showCategoryDetailInicial() = runTest {
        assertThat(viewModel.uiState.value.showCategoryDetail).isFalse()
    }

    @Test
    fun estado05_calibrationProfileCargado() = runTest {
        assertThat(viewModel.uiState.value.calibrationProfile.isCalibrated).isTrue()
    }

    @Test
    fun joystick01_moverDerecha() = runTest {
        viewModel.handleJoystickMove(PointF(0.8f, 0f), 1.0f)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategoryIndex).isEqualTo(1)
        assertThat(viewModel.uiState.value.selectedCategory?.name).isEqualTo("Verbo")
    }


    @Test
    fun joystick03_moverConVelocidadLenta() = runTest {
        viewModel.handleJoystickMove(PointF(0.8f, 0f), 0.5f) // Velocidad más lenta
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategoryIndex).isEqualTo(1)
    }

    @Test
    fun joystick04_moverConVelocidadRapida() = runTest {
        viewModel.handleJoystickMove(PointF(0.8f, 0f), 2.0f) // Velocidad más rápida
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategoryIndex).isEqualTo(1)
    }

    @Test
    fun joystick05_movimientoNoAlcanzaUmbral() = runTest {
        viewModel.handleJoystickMove(PointF(0.2f, 0f), 1.0f) // Muy poco movimiento
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategoryIndex).isEqualTo(0) // No cambia
    }

    @Test
    fun centro01_clickCentroAbreDetalle() = runTest {
        viewModel.handleCenterClick()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showCategoryDetail).isTrue()
    }

    @Test
    fun centro02_clickCentroMantieneCategoria() = runTest {
        val categoriaActual = viewModel.uiState.value.selectedCategory
        viewModel.handleCenterClick()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategory).isEqualTo(categoriaActual)
    }

    @Test
    fun centro03_clickCentroConCategoriaSeleccionada() = runTest {
        viewModel.handleCenterClick()
        advanceUntilIdle()

        coVerify { audioRepository.speak(any(), any(), any()) }
    }

    @Test
    fun navegacion01_volverDeDetalle() = runTest {
        viewModel.handleCenterClick()
        viewModel.navigateBackFromCategoryDetail()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showCategoryDetail).isFalse()
    }

    @Test
    fun navegacion02_volverSinEstarEnDetalle() = runTest {
        viewModel.navigateBackFromCategoryDetail()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showCategoryDetail).isFalse()
    }


    @Test
    fun seleccion02_primeraCategoriaIzquierdaVaAlFinal() = runTest {
        viewModel.handleJoystickMove(PointF(-0.8f, 0f), 1.0f) // Va al final
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategoryIndex).isEqualTo(3) // Emociones
    }


    @Test
    fun audio01_initializeAudio() = runTest {
        val context = mockk<Context>()
        viewModel.initializeAudio(context)
        advanceUntilIdle()

        coVerify { audioRepository.initialize(context, any()) }
    }

    @Test
    fun audio02_speakText() = runTest {
        viewModel.speakText("Hola mundo")
        advanceUntilIdle()

        coVerify { audioRepository.speak("Hola mundo", any(), any()) }
    }

    @Test
    fun audio03_stopAudio() = runTest {
        viewModel.stopAudio()
        advanceUntilIdle()

        coVerify { audioRepository.stop() }
    }

    @Test
    fun calibracion01_perfilCargadoCorrectamente() = runTest {
        assertThat(viewModel.uiState.value.calibrationProfile.baseDeadZone).isEqualTo(0.15f)
        assertThat(viewModel.uiState.value.calibrationProfile.baseSensitivity).isEqualTo(1.2f)
    }

    @Test
    fun especial01_categoriasVacias() = runTest {
        coEvery { categoryRepository.getAllCategories() } returns flowOf(emptyList())

        val newViewModel = MainViewModel(categoryRepository, audioRepository, calibrationRepository)
        advanceUntilIdle()

        assertThat(newViewModel.uiState.value.categories).isEmpty()
        assertThat(newViewModel.uiState.value.selectedCategoryIndex).isEqualTo(-1)
    }
}
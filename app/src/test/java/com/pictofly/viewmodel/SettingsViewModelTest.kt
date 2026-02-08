package com.pictofly.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.PictogramSize
import com.pictofly.repository.SettingsRepository
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
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        settingsRepository = mockk(relaxed = true)

        every { settingsRepository.pictogramSizeFlow } returns flowOf(
            PictogramSize.MEDIUM,
            PictogramSize.LARGE,
            PictogramSize.SMALL
        )

        every { settingsRepository.getPictogramSize() } returns PictogramSize.MEDIUM

        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(settingsRepository)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun estado01_carouselSizeInicial() = runTest {
        assertThat(viewModel.carouselSize.value).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun estado02_sentenceSizeFijo() = runTest {
        assertThat(viewModel.sentenceSize).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun estado03_getSentenceSizeDevuelve80() = runTest {
        assertThat(viewModel.getSentenceSize()).isEqualTo(80)
    }

    @Test
    fun estado04_getSentenceImageSizeDevuelve60() = runTest {
        assertThat(viewModel.getSentenceImageSize()).isEqualTo(60)
    }

    @Test
    fun updateSize01_cambiarASmall_verificaLlamada() = runTest {
        viewModel.updatePictogramSize(PictogramSize.SMALL)
        advanceUntilIdle()
        coVerify { settingsRepository.savePictogramSize(PictogramSize.SMALL) }
    }

    @Test
    fun updateSize02_cambiarAMedium_verificaLlamada() = runTest {
        viewModel.updatePictogramSize(PictogramSize.MEDIUM)
        advanceUntilIdle()
        coVerify { settingsRepository.savePictogramSize(PictogramSize.MEDIUM) }
    }

    @Test
    fun updateSize03_cambiarALarge_verificaLlamada() = runTest {
        viewModel.updatePictogramSize(PictogramSize.LARGE)
        advanceUntilIdle()
        coVerify { settingsRepository.savePictogramSize(PictogramSize.LARGE) }
    }

    @Test
    fun updateSize04_cambiarAExtraLarge_verificaLlamada() = runTest {
        viewModel.updatePictogramSize(PictogramSize.EXTRA_LARGE)
        advanceUntilIdle()
        coVerify { settingsRepository.savePictogramSize(PictogramSize.EXTRA_LARGE) }
    }

    @Test
    fun updateSize05_llamarConSmall_noDebeFallar() = runTest {
        viewModel.updatePictogramSize(PictogramSize.SMALL)
        advanceUntilIdle()
    }

    @Test
    fun updateSize06_llamarConMedium_noDebeFallar() = runTest {
        viewModel.updatePictogramSize(PictogramSize.MEDIUM)
        advanceUntilIdle()
    }

    @Test
    fun updateSize07_llamarConLarge_noDebeFallar() = runTest {
        viewModel.updatePictogramSize(PictogramSize.LARGE)
        advanceUntilIdle()
    }

    @Test
    fun updateSize08_llamarConExtraLarge_noDebeFallar() = runTest {
        viewModel.updatePictogramSize(PictogramSize.EXTRA_LARGE)
        advanceUntilIdle()
    }

    @Test
    fun flow01_suscribirseAlFlow() = runTest {
        verify(atLeast = 1) { settingsRepository.pictogramSizeFlow }
    }

    @Test
    fun flow02_valorInicialEsMedium() = runTest {
        assertThat(viewModel.carouselSize.value).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun flow03_carouselSizeNoEsNull() = runTest {
        assertThat(viewModel.carouselSize.value).isNotNull()
    }

    @Test
    fun flow04_sentenceSizeNoCambia() = runTest {
        viewModel.updatePictogramSize(PictogramSize.LARGE)
        advanceUntilIdle()
        assertThat(viewModel.sentenceSize).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun flow05_multipleActualizaciones() = runTest {
        viewModel.updatePictogramSize(PictogramSize.SMALL)
        viewModel.updatePictogramSize(PictogramSize.LARGE)
        viewModel.updatePictogramSize(PictogramSize.MEDIUM)
        advanceUntilIdle()
        coVerify(atLeast = 3) { settingsRepository.savePictogramSize(any()) }
    }

    @Test
    fun tamano01_getSentenceSizeSiempre80() = runTest {
        assertThat(viewModel.getSentenceSize()).isEqualTo(80)
        viewModel.updatePictogramSize(PictogramSize.LARGE)
        assertThat(viewModel.getSentenceSize()).isEqualTo(80)
    }

    @Test
    fun tamano02_getSentenceImageSizeSiempre60() = runTest {
        assertThat(viewModel.getSentenceImageSize()).isEqualTo(60)
        viewModel.updatePictogramSize(PictogramSize.SMALL)
        assertThat(viewModel.getSentenceImageSize()).isEqualTo(60)
    }


    @Test
    fun valores01_multiplierDefault() = runTest {
        assertThat(viewModel.carouselSize.value.multiplier).isEqualTo(1.0f)
    }

    @Test
    fun valores02_carouselSizeDefault() = runTest {
        assertThat(viewModel.carouselSize.value.carouselSize).isEqualTo(80)
    }

    @Test
    fun valores03_carouselImageSizeDefault() = runTest {
        assertThat(viewModel.carouselSize.value.carouselImageSize).isEqualTo(60)
    }

    @Test
    fun valores04_displayNameDefault() = runTest {
        assertThat(viewModel.carouselSize.value.displayName).isEqualTo("Mediano")
    }

    @Test
    fun comportamiento01_actualizarVariasVeces() = runTest {
        viewModel.updatePictogramSize(PictogramSize.SMALL)
        viewModel.updatePictogramSize(PictogramSize.LARGE)
        viewModel.updatePictogramSize(PictogramSize.EXTRA_LARGE)
        advanceUntilIdle()
        coVerify(exactly = 3) { settingsRepository.savePictogramSize(any()) }
    }

    @Test
    fun comportamiento02_actualizarConMismoValor() = runTest {
        viewModel.updatePictogramSize(PictogramSize.MEDIUM)
        viewModel.updatePictogramSize(PictogramSize.MEDIUM)
        advanceUntilIdle()
        coVerify(exactly = 2) { settingsRepository.savePictogramSize(PictogramSize.MEDIUM) }
    }

    @Test
    fun comportamiento03_noCambiaSiNoSeLlama() = runTest {
        assertThat(viewModel.carouselSize.value).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun comportamiento04_sentenceSizeInmutable() = runTest {
        assertThat(viewModel.sentenceSize).isEqualTo(PictogramSize.MEDIUM)
        assertThat(viewModel.sentenceSize).isSameInstanceAs(PictogramSize.MEDIUM)
    }
}
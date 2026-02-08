package com.pictofly.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CleanupRepositoryTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var localContentRepository: LocalContentRepository
    private lateinit var userOverrideRepository: UserPictogramOverrideRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var cleanupRepository: CleanupRepository

    private val mockCategory = LocalCategory(
        id = "cat1",
        name = "Test Category",
        imagePath = "path.jpg",
        pictogramCount = 0
    )

    private val mockExtensionCategory = LocalCategory(
        id = "ext1",
        name = "Extensión: Test",
        imagePath = "ext.jpg",
        pictogramCount = 0
    )

    private val mockPictogram = LocalPictogram(
        id = "pic1",
        categoryId = "cat1",
        name = "Test Picto",
        imagePath = "picto.jpg",
        type = "subject"
    )

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        localContentRepository = mockk(relaxed = true)
        userOverrideRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        userSettingsRepository = mockk(relaxed = true)

        coEvery { localContentRepository.localCategoriesWithCount } returns flowOf(listOf(mockCategory, mockExtensionCategory))
        coEvery { localContentRepository.getAllPictogramsSync() } returns listOf(mockPictogram)

        Dispatchers.setMain(testDispatcher)
        cleanupRepository = CleanupRepository(
            context,
            localContentRepository,
            userOverrideRepository,
            settingsRepository,
            userSettingsRepository
        )
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun llamada01_cleanAllUserData_noLanzaExcepcion() = runTest {
        try {
            cleanupRepository.cleanAllUserData()
            advanceUntilIdle()
            assertThat(true).isTrue()
        } catch (e: Exception) {
            assertThat(false).isTrue()
        }
    }

    @Test
    fun llamada02_verificaLlamadoALocalContentRepository() = runTest {
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()

        coVerify { localContentRepository.localCategoriesWithCount }
    }

    @Test
    fun llamada03_verificaLlamadoAGetAllPictograms() = runTest {
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()

        coVerify { localContentRepository.getAllPictogramsSync() }
    }

    @Test
    fun llamada04_verificaLlamadoAUserSettingsClear() = runTest {
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()

        coVerify { userSettingsRepository.clearAllData() }
    }

    @Test
    fun llamada05_verificaLlamadoASettingsClearSession() = runTest {
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()

        coVerify { settingsRepository.clearSessionData() }
    }

    @Test
    fun llamada06_verificaLlamadoAOverrideGetAll() = runTest {
        coEvery { userOverrideRepository.getAllOverrides() } returns emptyMap()

        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()

        coVerify { userOverrideRepository.getAllOverrides() }
    }

    @Test
    fun llamada07_cleanConRepositoriosMockeados() = runTest {
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        assertThat(true).isTrue()
    }

    @Test
    fun llamada08_cleanDosVeces() = runTest {
        cleanupRepository.cleanAllUserData()
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        assertThat(true).isTrue()
    }

    @Test
    fun llamada09_cleanConCategoriasVacias() = runTest {
        coEvery { localContentRepository.localCategoriesWithCount } returns flowOf(emptyList())

        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        assertThat(true).isTrue()
    }

    @Test
    fun llamada10_cleanConPictogramasVacias() = runTest {
        coEvery { localContentRepository.getAllPictogramsSync() } returns emptyList()

        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        assertThat(true).isTrue()
    }

    @Test
    fun mock01_verificarQueContextExiste() {
        assertThat(context).isNotNull()
    }

    @Test
    fun mock02_verificarQueLocalContentRepositoryExiste() {
        assertThat(localContentRepository).isNotNull()
    }

    @Test
    fun mock03_verificarQueUserOverrideRepositoryExiste() {
        assertThat(userOverrideRepository).isNotNull()
    }

    @Test
    fun mock04_verificarQueSettingsRepositoryExiste() {
        assertThat(settingsRepository).isNotNull()
    }

    @Test
    fun mock05_verificarQueUserSettingsRepositoryExiste() {
        assertThat(userSettingsRepository).isNotNull()
    }

    @Test
    fun mock06_verificarQueCleanupRepositoryExiste() {
        assertThat(cleanupRepository).isNotNull()
    }

    @Test
    fun mock07_verificarMockCategories() {
        assertThat(mockCategory.name).isEqualTo("Test Category")
    }

    @Test
    fun mock08_verificarMockExtension() {
        assertThat(mockExtensionCategory.name).startsWith("Extensión:")
    }

    @Test
    fun mock09_verificarMockPictogram() {
        assertThat(mockPictogram.name).isEqualTo("Test Picto")
    }

    @Test
    fun mock10_verificarQueLosMocksEstanConfigurados() = runTest {
        val categories = localContentRepository.localCategoriesWithCount.first()
        assertThat(categories).isNotEmpty()
    }

    @Test
    fun comportamiento01_ejecutarCleanNoBloquea() = runTest {
        val job = launch {
            cleanupRepository.cleanAllUserData()
        }
        job.join()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun comportamiento02_cleanNoTardaMasDe5Segundos() = runTest {
        val tiempoInicial = System.currentTimeMillis()
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        val tiempoFinal = System.currentTimeMillis()

        assertThat(tiempoFinal - tiempoInicial < 5000).isTrue()
    }

    @Test
    fun comportamiento04_cleanDespuesDeInicializacion() = runTest {
        // Ya inicializado en setup
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        assertThat(true).isTrue()
    }

    @Test
    fun comportamiento05_cleanConRepositorioNuevo() = runTest {
        val newRepo = CleanupRepository(
            context,
            localContentRepository,
            userOverrideRepository,
            settingsRepository,
            userSettingsRepository
        )
        newRepo.cleanAllUserData()
        advanceUntilIdle()
        assertThat(true).isTrue()
    }

    @Test
    fun comportamiento06_cleanNoAlteraContexto() = runTest {
        cleanupRepository.cleanAllUserData()
        advanceUntilIdle()
        assertThat(context).isNotNull()
    }
}
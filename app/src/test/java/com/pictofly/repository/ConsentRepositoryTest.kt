package com.pictofly.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.UserSettings
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
class ConsentRepositoryTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var consentRepository: ConsentRepository

    private val mockSettings = UserSettings(
        soundHz = 440,
        soundDb = 70,
        isRightHanded = true,
        hasFullMovement = true,
        calibrationSpeed = 1.0f,
        isConfigured = true,
        consentShown = false
    )

    @Before
    fun setup() {
        userSettingsRepository = mockk(relaxed = true)

        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings
        coEvery { userSettingsRepository.userSettings } returns flowOf(mockSettings)

        Dispatchers.setMain(testDispatcher)
        consentRepository = ConsentRepositoryImpl(userSettingsRepository)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }


    @Test
    fun hasConsent01_cuandoNoSeHaMostrado() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = false)

        val result = consentRepository.hasConsentBeenShown()
        assertThat(result).isFalse()
    }

    @Test
    fun hasConsent02_cuandoSeHaMostrado() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = true)

        val result = consentRepository.hasConsentBeenShown()
        assertThat(result).isTrue()
    }

    @Test
    fun hasConsent03_verificaLlamadaARepository() = runTest {
        consentRepository.hasConsentBeenShown()

        coVerify { userSettingsRepository.getCurrentSettings() }
    }

    @Test
    fun hasConsent04_despuesDeGuardar() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = true)

        val result = consentRepository.hasConsentBeenShown()
        assertThat(result).isTrue()
    }

    @Test
    fun hasConsent05_multiplesLlamadas() = runTest {
        consentRepository.hasConsentBeenShown()
        consentRepository.hasConsentBeenShown()
        consentRepository.hasConsentBeenShown()

        coVerify(exactly = 3) { userSettingsRepository.getCurrentSettings() }
    }

    @Test
    fun saveConsent01_guardarConsent() = runTest {
        consentRepository.saveConsentShown()
        advanceUntilIdle()

        coVerify { userSettingsRepository.saveConsentShown() }
    }

    @Test
    fun saveConsent02_guardarDosVeces() = runTest {
        consentRepository.saveConsentShown()
        consentRepository.saveConsentShown()
        advanceUntilIdle()

        coVerify(exactly = 2) { userSettingsRepository.saveConsentShown() }
    }

    @Test
    fun saveConsent03_guardarYVerificar() = runTest {
        consentRepository.saveConsentShown()
        advanceUntilIdle()

        coVerify(exactly = 1) { userSettingsRepository.saveConsentShown() }
    }

    @Test
    fun saveConsent04_noAfectaOtrosValores() = runTest {
        consentRepository.saveConsentShown()
        advanceUntilIdle()
        coVerify(exactly = 0) { userSettingsRepository.saveConfiguration(any(), any(), any(), any(), any()) }
    }

    @Test
    fun clearConsent01_limpiarDatos() = runTest {
        consentRepository.clearConsentData()
        advanceUntilIdle()

        coVerify { userSettingsRepository.clearAllData() }
    }

    @Test
    fun clearConsent02_limpiarDosVeces() = runTest {
        consentRepository.clearConsentData()
        consentRepository.clearConsentData()
        advanceUntilIdle()

        coVerify(exactly = 2) { userSettingsRepository.clearAllData() }
    }

    @Test
    fun clearConsent03_limpiarYVerificar() = runTest {
        consentRepository.clearConsentData()
        advanceUntilIdle()

        coVerify(exactly = 1) { userSettingsRepository.clearAllData() }
    }

    @Test
    fun clearConsent04_despuesDeGuardar() = runTest {
        consentRepository.saveConsentShown()
        consentRepository.clearConsentData()
        advanceUntilIdle()

        coVerify { userSettingsRepository.saveConsentShown() }
        coVerify { userSettingsRepository.clearAllData() }
    }

    @Test
    fun combinado01_guardarYVerificar() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = true)

        consentRepository.saveConsentShown()
        val result = consentRepository.hasConsentBeenShown()

        assertThat(result).isTrue()
    }

    @Test
    fun combinado02_limpiarYVerificar() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = false)

        consentRepository.clearConsentData()
        val result = consentRepository.hasConsentBeenShown()

        assertThat(result).isFalse()
    }

    @Test
    fun combinado03_guardarLimpiarGuardar() = runTest {
        consentRepository.saveConsentShown()
        consentRepository.clearConsentData()
        consentRepository.saveConsentShown()
        advanceUntilIdle()

        coVerify(exactly = 2) { userSettingsRepository.saveConsentShown() }
        coVerify(exactly = 1) { userSettingsRepository.clearAllData() }
    }

    @Test
    fun combinado04_hasConsentDespuesDeClear() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = false)

        consentRepository.clearConsentData()
        val result = consentRepository.hasConsentBeenShown()

        assertThat(result).isFalse()
    }

    @Test
    fun combinado05_hasConsentDespuesDeSave() = runTest {
        coEvery { userSettingsRepository.getCurrentSettings() } returns mockSettings.copy(consentShown = true)

        consentRepository.saveConsentShown()
        val result = consentRepository.hasConsentBeenShown()

        assertThat(result).isTrue()
    }
}
package com.pictofly.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.local.CalibrationDataSource
import com.pictofly.data.model.CalibrationProfile
import com.pictofly.ui.screens.joystick.MovementSample
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.geometry.Offset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CalibrationRepositoryTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dataSource: CalibrationDataSource
    private lateinit var repository: CalibrationRepository

    private val mockProfile = CalibrationProfile(
        userId = "test_user",
        lastCalibrationDate = System.currentTimeMillis(),
        xMin = -5.2f,
        xMax = 4.8f,
        yMin = -3.1f,
        yMax = 5.7f,
        baseDeadZone = 0.18f,
        baseSensitivity = 1.2f,
        baseSmoothing = 0.35f,
        avgSpeed = 0.75f,
        maxAcceleration = 2.3f,
        tremorIndex = 0.15f,
        stabilityScore = 0.82f,
        isCalibrated = true,
        calibrationVersion = 3
    )

    @Before
    fun setup() {
        dataSource = mockk(relaxed = true)

        coEvery { dataSource.calibrationProfile } returns flowOf(mockProfile)
        coEvery { dataSource.getCurrentCalibrationProfile() } returns mockProfile
        coEvery { dataSource.hasCalibration() } returns true

        Dispatchers.setMain(testDispatcher)
        repository = CalibrationRepository(dataSource)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun flow01_getCalibrationProfile() = runTest {
        val profile = repository.getCalibrationProfile().first()
        assertThat(profile).isEqualTo(mockProfile)
        assertThat(profile.isCalibrated).isTrue()
    }

    @Test
    fun flow02_perfilConDatosCompletos() = runTest {
        val profile = repository.getCalibrationProfile().first()
        assertThat(profile.xMin).isEqualTo(-5.2f)
        assertThat(profile.baseDeadZone).isEqualTo(0.18f)
    }

    @Test
    fun flow03_perfilNoEsNull() = runTest {
        val profile = repository.getCalibrationProfile().first()
        assertThat(profile).isNotNull()
    }

    @Test
    fun flow04_userIdCorrecto() = runTest {
        val profile = repository.getCalibrationProfile().first()
        assertThat(profile.userId).isEqualTo("test_user")
    }

    @Test
    fun flow05_versionCorrecta() = runTest {
        val profile = repository.getCalibrationProfile().first()
        assertThat(profile.calibrationVersion).isEqualTo(3)
    }

    @Test
    fun guardar01_saveCalibrationProfile() = runTest {
        repository.saveCalibrationProfile(mockProfile)
        advanceUntilIdle()

        coVerify { dataSource.saveCalibrationProfile(mockProfile) }
    }

    @Test
    fun guardar02_savePerfilVacio() = runTest {
        val emptyProfile = CalibrationProfile()
        repository.saveCalibrationProfile(emptyProfile)
        advanceUntilIdle()

        coVerify { dataSource.saveCalibrationProfile(emptyProfile) }
    }

    @Test
    fun guardar03_savePerfilConUserId() = runTest {
        val profile = CalibrationProfile(userId = "nuevo_user", isCalibrated = true)
        repository.saveCalibrationProfile(profile)
        advanceUntilIdle()

        coVerify { dataSource.saveCalibrationProfile(profile) }
    }

    @Test
    fun guardar04_saveMultipleVeces() = runTest {
        repeat(3) {
            repository.saveCalibrationProfile(mockProfile)
        }
        advanceUntilIdle()

        coVerify(exactly = 3) { dataSource.saveCalibrationProfile(mockProfile) }
    }

    @Test
    fun guardar05_saveYVerificarLlamada() = runTest {
        repository.saveCalibrationProfile(mockProfile)
        advanceUntilIdle()

        coVerify(exactly = 1) { dataSource.saveCalibrationProfile(mockProfile) }
    }

    @Test
    fun limpiar01_clearCalibrationProfile() = runTest {
        repository.clearCalibrationProfile()
        advanceUntilIdle()

        coVerify { dataSource.clearCalibrationData() }
    }

    @Test
    fun limpiar02_clearDosVeces() = runTest {
        repository.clearCalibrationProfile()
        repository.clearCalibrationProfile()
        advanceUntilIdle()

        coVerify(exactly = 2) { dataSource.clearCalibrationData() }
    }

    @Test
    fun limpiar03_clearSinDatosPrevios() = runTest {
        repository.clearCalibrationProfile()
        advanceUntilIdle()
    }

    @Test
    fun obtener01_getCurrentCalibrationProfile() = runTest {
        val profile = repository.getCurrentCalibrationProfile()
        assertThat(profile).isEqualTo(mockProfile)
    }

    @Test
    fun obtener02_getCurrentNoEsNull() = runTest {
        val profile = repository.getCurrentCalibrationProfile()
        assertThat(profile).isNotNull()
    }

    @Test
    fun obtener03_getCurrentConDatos() = runTest {
        val profile = repository.getCurrentCalibrationProfile()
        assertThat(profile.xMin).isEqualTo(-5.2f)
    }

    @Test
    fun has01_hasCalibrationTrue() = runTest {
        val has = repository.hasCalibration()
        assertThat(has).isTrue()
    }

    @Test
    fun has02_hasCalibrationFalse() = runTest {
        coEvery { dataSource.hasCalibration() } returns false
        val has = repository.hasCalibration()
        assertThat(has).isFalse()
    }

    @Test
    fun has03_hasCalibrationDespuesDeGuardar() = runTest {
        coEvery { dataSource.hasCalibration() } returns true
        val has = repository.hasCalibration()
        assertThat(has).isTrue()
    }

    @Test
    fun metricas01_calcularConMuestrasVacias() = runTest {
        val samples = emptyList<MovementSample>()
        val profile = repository.calculateCalibrationMetrics(samples, "test")

        assertThat(profile.isCalibrated).isFalse()
        assertThat(profile.userId).isEqualTo("test")
    }

    @Test
    fun metricas02_calcularConUnaMuestra() = runTest {
        val samples = listOf(
            MovementSample(
                timestamp = 1000,
                rawPosition = Offset(0.5f, 0.3f),
                smoothedPosition = Offset(0.5f, 0.3f),
                speed = 0f,
                acceleration = 0f,
                isInCenter = true
            )
        )
        val profile = repository.calculateCalibrationMetrics(samples, "test")

        assertThat(profile.isCalibrated).isFalse()
    }

    @Test
    fun metricas03_calcularConDosMuestras() = runTest {
        val samples = listOf(
            MovementSample(
                timestamp = 1000,
                rawPosition = Offset(0.5f, 0.3f),
                smoothedPosition = Offset(0.5f, 0.3f),
                speed = 0f,
                acceleration = 0f,
                isInCenter = true
            ),
            MovementSample(
                timestamp = 1100,
                rawPosition = Offset(0.6f, 0.4f),
                smoothedPosition = Offset(0.6f, 0.4f),
                speed = 1.2f,
                acceleration = 0f,
                isInCenter = false
            )
        )
        val profile = repository.calculateCalibrationMetrics(samples, "test")

        assertThat(profile.isCalibrated).isTrue()
        assertThat(profile.xMin).isEqualTo(0.5f)
        assertThat(profile.xMax).isEqualTo(0.6f)
    }

    @Test
    fun metricas04_calcularConMuestrasEstables() = runTest {
        val samples = (1..20).map { i ->
            MovementSample(
                timestamp = 1000L + i * 50,
                rawPosition = Offset(0.1f, 0.1f),
                smoothedPosition = Offset(0.1f, 0.1f),
                speed = 0f,
                acceleration = 0f,
                isInCenter = true
            )
        }
        val profile = repository.calculateCalibrationMetrics(samples, "test")

        assertThat(profile.isCalibrated).isTrue()
        assertThat(profile.stabilityScore).isGreaterThan(0.5f)
    }

    @Test
    fun operaciones01_guardarYRecuperar() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        repository.saveCalibrationProfile(profile)

        coEvery { dataSource.getCurrentCalibrationProfile() } returns profile
        val recuperado = repository.getCurrentCalibrationProfile()

        assertThat(recuperado.isCalibrated).isTrue()
    }

    @Test
    fun operaciones02_guardarYLimpiar() = runTest {
        repository.saveCalibrationProfile(mockProfile)
        repository.clearCalibrationProfile()

        coVerify { dataSource.saveCalibrationProfile(mockProfile) }
        coVerify { dataSource.clearCalibrationData() }
    }

    @Test
    fun operaciones03_flowDespuesDeGuardar() = runTest {
        val nuevoProfile = CalibrationProfile(isCalibrated = true)
        coEvery { dataSource.calibrationProfile } returns flowOf(nuevoProfile)

        val profile = repository.getCalibrationProfile().first()
        assertThat(profile.isCalibrated).isTrue()
    }
}
package com.pictofly.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.CalibrationProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationDataSourceTest {

    private lateinit var context: Context
    private lateinit var dataSource: CalibrationDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = CalibrationDataSource(context)
    }

    @After
    fun tearDown() {
    }


    @Test
    fun estado02_sinCalibracion_hasCalibrationEsFalse() = runTest {
        val hasCalibration = dataSource.hasCalibration()
        assertThat(hasCalibration).isFalse()
    }

    @Test
    fun estado04_valoresPorDefecto_xMinEsCero() = runTest {
        val profile = dataSource.calibrationProfile.first()
        assertThat(profile.xMin).isEqualTo(0f)
    }

    @Test
    fun estado05_valoresPorDefecto_xMaxEsCero() = runTest {
        val profile = dataSource.getCurrentCalibrationProfile()
        assertThat(profile.xMax).isEqualTo(0f)
    }


    @Test
    fun guardar01_perfilCompleto() = runTest {
        val profile = CalibrationProfile(
            userId = "user123",
            lastCalibrationDate = 123456789L,
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

        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.calibrationProfile.first()
        assertThat(saved.userId).isEqualTo("user123")
        assertThat(saved.xMin).isEqualTo(-5.2f)
        assertThat(saved.baseDeadZone).isEqualTo(0.18f)
        assertThat(saved.isCalibrated).isTrue()
    }

    @Test
    fun guardar02_soloCamposObligatorios() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isTrue()
    }

    @Test
    fun guardar03_conUserIdPersonalizado() = runTest {
        val profile = CalibrationProfile(
            userId = "custom_user",
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.userId).isEqualTo("custom_user")
    }

    @Test
    fun guardar04_conTimestampPersonalizado() = runTest {
        val timestamp = 987654321L
        val profile = CalibrationProfile(
            lastCalibrationDate = timestamp,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.lastCalibrationDate).isEqualTo(timestamp)
    }

    @Test
    fun guardar05_conRangosPersonalizados() = runTest {
        val profile = CalibrationProfile(
            xMin = -10.5f,
            xMax = 9.5f,
            yMin = -8.2f,
            yMax = 7.8f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.xMin).isEqualTo(-10.5f)
        assertThat(saved.xMax).isEqualTo(9.5f)
        assertThat(saved.yMin).isEqualTo(-8.2f)
        assertThat(saved.yMax).isEqualTo(7.8f)
    }

    @Test
    fun guardar06_conParametrosBase() = runTest {
        val profile = CalibrationProfile(
            baseDeadZone = 0.25f,
            baseSensitivity = 1.5f,
            baseSmoothing = 0.45f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.baseDeadZone).isEqualTo(0.25f)
        assertThat(saved.baseSensitivity).isEqualTo(1.5f)
        assertThat(saved.baseSmoothing).isEqualTo(0.45f)
    }

    @Test
    fun guardar07_conMetricas() = runTest {
        val profile = CalibrationProfile(
            avgSpeed = 1.25f,
            maxAcceleration = 3.5f,
            tremorIndex = 0.32f,
            stabilityScore = 0.65f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.avgSpeed).isEqualTo(1.25f)
        assertThat(saved.maxAcceleration).isEqualTo(3.5f)
        assertThat(saved.tremorIndex).isEqualTo(0.32f)
        assertThat(saved.stabilityScore).isEqualTo(0.65f)
    }

    @Test
    fun guardar08_conVersion() = runTest {
        val profile = CalibrationProfile(
            calibrationVersion = 5,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.calibrationVersion).isEqualTo(5)
    }

    @Test
    fun guardar09_sobrescribirPerfil() = runTest {
        val profile1 = CalibrationProfile(userId = "user1", isCalibrated = true)
        val profile2 = CalibrationProfile(userId = "user2", isCalibrated = true)

        dataSource.saveCalibrationProfile(profile1)
        dataSource.saveCalibrationProfile(profile2)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.userId).isEqualTo("user2")
    }

    @Test
    fun guardar10_guardarYRecuperarMultiple() = runTest {
        repeat(3) {
            val profile = CalibrationProfile(
                userId = "user$it",
                isCalibrated = true
            )
            dataSource.saveCalibrationProfile(profile)

            val saved = dataSource.getCurrentCalibrationProfile()
            assertThat(saved.userId).isEqualTo("user$it")
        }
    }


    @Test
    fun flow01_flowEmiteValorInicial() = runTest {
        val profile = dataSource.calibrationProfile.first()
        assertThat(profile).isNotNull()
    }

    @Test
    fun flow02_flowEmiteDespuesDeGuardar() = runTest {
        val emissions = mutableListOf<CalibrationProfile>()
        val job = launch {
            dataSource.calibrationProfile.collect {
                emissions.add(it)
            }
        }

        delay(100)

        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        delay(100)
        job.cancel()

        assertThat(emissions.size).isAtLeast(2)
    }

    @Test
    fun flow03_flowEmiteValoresCorrectos() = runTest {
        val profile1 = CalibrationProfile(userId = "flow1", isCalibrated = true)
        val profile2 = CalibrationProfile(userId = "flow2", isCalibrated = true)

        dataSource.saveCalibrationProfile(profile1)
        val saved1 = dataSource.calibrationProfile.first()

        dataSource.saveCalibrationProfile(profile2)
        val saved2 = dataSource.calibrationProfile.first()

        assertThat(saved1.userId).isEqualTo("flow1")
        assertThat(saved2.userId).isEqualTo("flow2")
    }

    @Test
    fun flow04_flowNoBloqueante() = runTest {
        val job = launch {
            dataSource.calibrationProfile.collect {
                // Solo recolectar
            }
        }

        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        delay(100)
        job.cancel()
    }

    @Test
    fun obtener01_getCurrentProfileDespuesDeGuardar() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isTrue()
    }

    @Test
    fun obtener03_getCurrentProfileDespuesDeLimpiar() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)
        dataSource.clearCalibrationData()

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isFalse()
    }

    @Test
    fun obtener04_getCurrentProfileConDatosCompletos() = runTest {
        val original = CalibrationProfile(
            userId = "get_test",
            xMin = -1.5f,
            xMax = 2.5f,
            baseDeadZone = 0.22f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(original)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.userId).isEqualTo("get_test")
        assertThat(saved.xMin).isEqualTo(-1.5f)
        assertThat(saved.xMax).isEqualTo(2.5f)
        assertThat(saved.baseDeadZone).isEqualTo(0.22f)
    }


    @Test
    fun has01_hasCalibrationFalseInicial() = runTest {
        assertThat(dataSource.hasCalibration()).isFalse()
    }

    @Test
    fun has02_hasCalibrationTrueDespuesDeGuardar() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        assertThat(dataSource.hasCalibration()).isTrue()
    }

    @Test
    fun has03_hasCalibrationFalseDespuesDeLimpiar() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)
        dataSource.clearCalibrationData()

        assertThat(dataSource.hasCalibration()).isFalse()
    }

    @Test
    fun has04_hasCalibrationConPerfilNoCalibrado() = runTest {
        val profile = CalibrationProfile(isCalibrated = false)
        dataSource.saveCalibrationProfile(profile)

        assertThat(dataSource.hasCalibration()).isFalse()
    }


    @Test
    fun limpiar01_clearCalibrationDataEliminaPerfil() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)
        dataSource.clearCalibrationData()

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isFalse()
    }

    @Test
    fun limpiar02_clearCalibrationDataSinDatosPrevios() = runTest {
        dataSource.clearCalibrationData()

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isFalse()
    }

    @Test
    fun limpiar03_clearCalibrationDataMultiple() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)
        dataSource.clearCalibrationData()
        dataSource.clearCalibrationData()

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isFalse()
    }

    @Test
    fun limpiar04_clearDespuesDeGuardarVariasVeces() = runTest {
        repeat(3) {
            val profile = CalibrationProfile(userId = "user$it", isCalibrated = true)
            dataSource.saveCalibrationProfile(profile)
        }

        dataSource.clearCalibrationData()

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isFalse()
    }


    @Test
    fun especial01_guardarConUserIdVacio() = runTest {
        val profile = CalibrationProfile(userId = "", isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.userId).isEmpty()
    }

    @Test
    fun especial02_guardarConValoresExtremos() = runTest {
        val profile = CalibrationProfile(
            xMin = -1000f,
            xMax = 1000f,
            yMin = -1000f,
            yMax = 1000f,
            baseDeadZone = 0.99f,
            baseSensitivity = 10f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.xMin).isEqualTo(-1000f)
        assertThat(saved.baseSensitivity).isEqualTo(10f)
    }

    @Test
    fun especial03_guardarConValoresNegativos() = runTest {
        val profile = CalibrationProfile(
            avgSpeed = -0.5f,
            maxAcceleration = -1.2f,
            tremorIndex = -0.1f,
            stabilityScore = -0.3f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.avgSpeed).isEqualTo(-0.5f)
    }

    @Test
    fun especial04_guardarConVersionCero() = runTest {
        val profile = CalibrationProfile(calibrationVersion = 0, isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.calibrationVersion).isEqualTo(0)
    }

    @Test
    fun especial05_guardarConVersionNegativa() = runTest {
        val profile = CalibrationProfile(calibrationVersion = -1, isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.calibrationVersion).isEqualTo(-1)
    }

    @Test
    fun especial06_guardarSinIsCalibrated() = runTest {
        val profile = CalibrationProfile() // isCalibrated false por defecto
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        assertThat(saved.isCalibrated).isFalse()
    }

    @Test
    fun especial07_flowMantieneValores() = runTest {
        val profile = CalibrationProfile(userId = "flow_test", isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        val saved1 = dataSource.calibrationProfile.first()
        val saved2 = dataSource.calibrationProfile.first()

        assertThat(saved1.userId).isEqualTo("flow_test")
        assertThat(saved2.userId).isEqualTo("flow_test")
    }

    @Test
    fun especial08_getCurrentProfileDespuesDeFlow() = runTest {
        val profile = CalibrationProfile(isCalibrated = true)
        dataSource.saveCalibrationProfile(profile)

        dataSource.calibrationProfile.first() // Consumir flow
        val saved = dataSource.getCurrentCalibrationProfile()

        assertThat(saved.isCalibrated).isTrue()
    }

    @Test
    fun especial09_toNormalizedRange() = runTest {
        val profile = CalibrationProfile(
            xMin = -5f,
            xMax = 5f,
            yMin = -4f,
            yMax = 4f,
            isCalibrated = true
        )
        dataSource.saveCalibrationProfile(profile)

        val saved = dataSource.getCurrentCalibrationProfile()
        val range = saved.toNormalizedRange()

        assertThat(range.xMin).isEqualTo(-5f)
        assertThat(range.xMax).isEqualTo(5f)
        assertThat(range.isCalibrated).isTrue()
    }

    @Test
    fun especial10_guardarYRecuperarMuchasVeces() = runTest {
        repeat(10) {
            val profile = CalibrationProfile(
                userId = "user$it",
                calibrationVersion = it,
                isCalibrated = true
            )
            dataSource.saveCalibrationProfile(profile)

            val saved = dataSource.getCurrentCalibrationProfile()
            assertThat(saved.userId).isEqualTo("user$it")
            assertThat(saved.calibrationVersion).isEqualTo(it)
        }
    }
}
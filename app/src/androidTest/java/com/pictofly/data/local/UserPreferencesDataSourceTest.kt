package com.pictofly.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataSourceTest {

    private lateinit var context: Context
    private lateinit var dataSource: UserPreferencesDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = UserPreferencesDataSource(context)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun estado01_valoresPorDefecto() = runTest {
        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(0)
        assertThat(settings.soundDb).isEqualTo(0)
        assertThat(settings.isRightHanded).isTrue()
        assertThat(settings.hasFullMovement).isTrue()
        assertThat(settings.calibrationSpeed).isEqualTo(1.0f)
        assertThat(settings.isConfigured).isFalse()
        assertThat(settings.consentShown).isFalse()
    }

    @Test
    fun estado02_flowNoNulo() = runTest {
        val settings = dataSource.userSettings.first()
        assertThat(settings).isNotNull()
    }

    @Test
    fun estado03_soundHzPorDefectoCero() = runTest {
        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(440)
    }

    @Test
    fun estado04_soundDbPorDefectoCero() = runTest {
        val settings = dataSource.userSettings.first()
        assertThat(settings.soundDb).isEqualTo(70)
    }

    @Test
    fun estado05_calibrationSpeedPorDefectoUno() = runTest {
        val settings = dataSource.userSettings.first()
        assertThat(settings.calibrationSpeed).isEqualTo(1.0f)
    }

    @Test
    fun guardar01_configuracionCompleta() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.8f)

        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(440)
        assertThat(settings.soundDb).isEqualTo(70)
        assertThat(settings.isRightHanded).isFalse()
        assertThat(settings.hasFullMovement).isFalse()
        assertThat(settings.calibrationSpeed).isEqualTo(0.8f)
        assertThat(settings.isConfigured).isTrue()
    }

    @Test
    fun guardar02_soloAudio() = runTest {
        dataSource.saveConfiguration(880, 80, true, true, 1.0f)

        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(880)
        assertThat(settings.soundDb).isEqualTo(80)
    }

    @Test
    fun guardar03_soloFisica() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.5f)

        val settings = dataSource.userSettings.first()
        assertThat(settings.isRightHanded).isFalse()
        assertThat(settings.hasFullMovement).isFalse()
        assertThat(settings.calibrationSpeed).isEqualTo(0.5f)
    }

    @Test
    fun guardar04_actualizarConfiguracion() = runTest {
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        dataSource.saveConfiguration(880, 80, false, false, 0.5f)

        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(880)
        assertThat(settings.isRightHanded).isFalse()
    }

    @Test
    fun guardar05_isConfiguredTrue() = runTest {
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        val settings = dataSource.userSettings.first()
        assertThat(settings.isConfigured).isTrue()
    }

    @Test
    fun guardar06_valoresMinimos() = runTest {
        dataSource.saveConfiguration(0, 0, true, true, 0.0f)
        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(0)
        assertThat(settings.calibrationSpeed).isEqualTo(0.0f)
    }

    @Test
    fun guardar07_valoresMaximos() = runTest {
        dataSource.saveConfiguration(1000, 120, true, true, 5.0f)
        val settings = dataSource.userSettings.first()
        assertThat(settings.soundHz).isEqualTo(1000)
        assertThat(settings.soundDb).isEqualTo(120)
        assertThat(settings.calibrationSpeed).isEqualTo(5.0f)
    }

    @Test
    fun guardar08_flowEmiteDespuesDeGuardar() = runTest {
        val emissions = mutableListOf<UserSettings>()
        val job = launch {
            dataSource.userSettings.collect {
                emissions.add(it)
            }
        }

        delay(100)
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        delay(100)
        job.cancel()

        assertThat(emissions.size).isAtLeast(2)
    }

    @Test
    fun guardar09_multiplesGuardados() = runTest {
        repeat(3) {
            dataSource.saveConfiguration(440 + it, 70 + it, true, true, 1.0f)
            val settings = dataSource.userSettings.first()
            assertThat(settings.soundHz).isEqualTo(440 + it)
        }
    }

    @Test
    fun consent01_guardarConsentShown() = runTest {
        dataSource.saveConsentShown()
        val settings = dataSource.userSettings.first()
        assertThat(settings.consentShown).isTrue()
    }

    @Test
    fun consent02_consentNoAfectaOtrosValores() = runTest {
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        dataSource.saveConsentShown()

        val settings = dataSource.userSettings.first()
        assertThat(settings.consentShown).isTrue()
        assertThat(settings.soundHz).isEqualTo(440)
    }

    @Test
    fun consent03_consentMultipleVeces() = runTest {
        dataSource.saveConsentShown()
        dataSource.saveConsentShown()
        val settings = dataSource.userSettings.first()
        assertThat(settings.consentShown).isTrue()
    }

    @Test
    fun get01_getCurrentSettingsDespuesDeGuardar() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.8f)
        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(440)
        assertThat(settings.isRightHanded).isFalse()
    }


    @Test
    fun get03_getCurrentSettingsDespuesDeActualizar() = runTest {
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        dataSource.saveConfiguration(880, 80, false, false, 0.5f)
        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(880)
    }

    @Test
    fun limpiar01_clearAllData() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.8f)
        dataSource.saveConsentShown()
        dataSource.clearAllData()

        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(0)
        assertThat(settings.isRightHanded).isTrue()
        assertThat(settings.consentShown).isFalse()
    }

    @Test
    fun limpiar02_clearAllDataSinDatosPrevios() = runTest {
        dataSource.clearAllData()
        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(0)
    }

    @Test
    fun limpiar03_clearAllDataMultiple() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.8f)
        dataSource.clearAllData()
        dataSource.clearAllData()
        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(0)
    }

    @Test
    fun especial01_flowEmiteValoresConsistentes() = runTest {
        val settings1 = dataSource.userSettings.first()
        val settings2 = dataSource.userSettings.first()
        assertThat(settings1).isEqualTo(settings2)
    }

    @Test
    fun especial02_flowNoBloqueante() = runTest {
        val job = launch {
            dataSource.userSettings.collect {
                // Solo recolectar
            }
        }
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        delay(100)
        job.cancel()
    }

    @Test
    fun especial03_guardarConNull() = runTest {
        dataSource.saveConfiguration(440, 70, true, true, 1.0f)
        val settings = dataSource.getCurrentSettings()
        assertThat(settings).isNotNull()
    }

    @Test
    fun especial04_operacionesSecuenciales() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.8f)
        dataSource.saveConsentShown()

        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(440)
        assertThat(settings.isRightHanded).isFalse()
        assertThat(settings.consentShown).isTrue()
    }

    @Test
    fun especial05_valoresPorDefectoDespuesDeClear() = runTest {
        dataSource.saveConfiguration(440, 70, false, false, 0.8f)
        dataSource.clearAllData()
        val settings = dataSource.getCurrentSettings()
        assertThat(settings.soundHz).isEqualTo(0)
        assertThat(settings.isRightHanded).isTrue()
        assertThat(settings.hasFullMovement).isTrue()
        assertThat(settings.calibrationSpeed).isEqualTo(1.0f)
    }
}
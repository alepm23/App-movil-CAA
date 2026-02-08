package com.pictofly.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.PictogramSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository
    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("pictofly_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        repository = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        prefs.edit().clear().apply()
    }

    @Test
    fun audio01_guardarConfiguracionAudio() {
        repository.saveAudioConfiguration(440, 70)
        assertThat(repository.getAudioHz()).isEqualTo(440)
        assertThat(repository.getAudioDb()).isEqualTo(70)
    }

    @Test
    fun audio02_valoresPorDefectoAudio() {
        assertThat(repository.getAudioHz()).isEqualTo(440)
        assertThat(repository.getAudioDb()).isEqualTo(70)
    }

    @Test
    fun audio03_actualizarSoloHzMantieneDb() {
        repository.saveAudioConfiguration(440, 70)
        repository.saveAudioConfiguration(880, 70)
        assertThat(repository.getAudioHz()).isEqualTo(880)
        assertThat(repository.getAudioDb()).isEqualTo(70)
    }

    @Test
    fun audio04_actualizarSoloDbMantieneHz() {
        repository.saveAudioConfiguration(440, 70)
        repository.saveAudioConfiguration(440, 80)
        assertThat(repository.getAudioHz()).isEqualTo(440)
        assertThat(repository.getAudioDb()).isEqualTo(80)
    }

    @Test
    fun fisica01_guardarConfiguracionFisicaCompleta() {
        repository.savePhysioConfiguration(false, false, 0.8f)
        assertThat(repository.isRightHanded()).isFalse()
        assertThat(repository.hasFullMovement()).isFalse()
        assertThat(repository.getCalibrationSpeed()).isEqualTo(0.8f)
    }

    @Test
    fun fisica02_valoresPorDefectoFisica() {
        assertThat(repository.isRightHanded()).isTrue()
        assertThat(repository.hasFullMovement()).isTrue()
        assertThat(repository.getCalibrationSpeed()).isEqualTo(1.0f)
    }

    @Test
    fun fisica03_cambiarSoloLateralidad() {
        repository.savePhysioConfiguration(false, true, 1.0f)
        assertThat(repository.isRightHanded()).isFalse()
        assertThat(repository.hasFullMovement()).isTrue()
        assertThat(repository.getCalibrationSpeed()).isEqualTo(1.0f)
    }

    @Test
    fun fisica04_cambiarSoloMovimiento() {
        repository.savePhysioConfiguration(true, false, 1.0f)
        assertThat(repository.isRightHanded()).isTrue()
        assertThat(repository.hasFullMovement()).isFalse()
        assertThat(repository.getCalibrationSpeed()).isEqualTo(1.0f)
    }

    @Test
    fun fisica05_cambiarSoloVelocidad() {
        repository.savePhysioConfiguration(true, true, 0.5f)
        assertThat(repository.isRightHanded()).isTrue()
        assertThat(repository.hasFullMovement()).isTrue()
        assertThat(repository.getCalibrationSpeed()).isEqualTo(0.5f)
    }

    @Test
    fun tamano01_guardarTamanioSmall() = runTest {
        repository.savePictogramSize(PictogramSize.SMALL)
        val size = repository.getPictogramSize()
        assertThat(size).isEqualTo(PictogramSize.SMALL)
    }

    @Test
    fun tamano02_guardarTamanioMedium() = runTest {
        repository.savePictogramSize(PictogramSize.MEDIUM)
        val size = repository.getPictogramSize()
        assertThat(size).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun tamano03_guardarTamanioLarge() = runTest {
        repository.savePictogramSize(PictogramSize.LARGE)
        val size = repository.getPictogramSize()
        assertThat(size).isEqualTo(PictogramSize.LARGE)
    }

    @Test
    fun tamano04_guardarTamanioExtraLarge() = runTest {
        repository.savePictogramSize(PictogramSize.EXTRA_LARGE)
        val size = repository.getPictogramSize()
        assertThat(size).isEqualTo(PictogramSize.EXTRA_LARGE)
    }

    @Test
    fun tamano05_valorPorDefectoEsMedium() = runTest {
        val size = repository.getPictogramSize()
        assertThat(size).isEqualTo(PictogramSize.MEDIUM)
    }

    @Test
    fun tamano06_flowEmiteValoresCorrectos() = runTest {
        assertThat(repository.getPictogramSize()).isEqualTo(PictogramSize.MEDIUM)

        repository.savePictogramSize(PictogramSize.SMALL)
        assertThat(repository.getPictogramSize()).isEqualTo(PictogramSize.SMALL)

        repository.savePictogramSize(PictogramSize.LARGE)
        assertThat(repository.getPictogramSize()).isEqualTo(PictogramSize.LARGE)
    }

    @Test
    fun tamano07_getPictogramSizeMultiplierRetornaMultiplicadorCorrecto() {
        repository.savePictogramSize(PictogramSize.EXTRA_LARGE)
        assertThat(repository.getPictogramSizeMultiplier()).isEqualTo(1.5f)
    }

    @Test
    fun estado01_guardarYObtenerIsConfigured() {
        repository.setConfigured(true)
        assertThat(repository.isConfigured()).isTrue()

        repository.setConfigured(false)
        assertThat(repository.isConfigured()).isFalse()
    }

    @Test
    fun estado02_guardarYObtenerConsentShown() {
        repository.setConsentShown(true)
        assertThat(repository.isConsentShown()).isTrue()

        repository.setConsentShown(false)
        assertThat(repository.isConsentShown()).isFalse()
    }

    @Test
    fun estado03_valoresPorDefectoDeEstado() {
        assertThat(repository.isConfigured()).isFalse()
        assertThat(repository.isConsentShown()).isFalse()
    }

    @Test
    fun estado04_clearSessionDataNoAfectaEstadoApp() {
        repository.setConfigured(true)
        repository.setConsentShown(true)

        repository.clearSessionData()

        assertThat(repository.isConfigured()).isTrue()
        assertThat(repository.isConsentShown()).isTrue()
    }

    // ============================================================
    // SECCIÓN 5: SELECCIÓN (9 tests)
    // ============================================================

    @Test
    fun seleccion01_guardarYObtenerSubjectId() {
        repository.saveSelectedSubjectId("subject_123")
        assertThat(repository.getSelectedSubjectId()).isEqualTo("subject_123")
    }

    @Test
    fun seleccion02_guardarYObtenerSubjectName() {
        repository.saveSelectedSubjectName("Yo")
        assertThat(repository.getSelectedSubjectName()).isEqualTo("Yo")
    }

    @Test
    fun seleccion03_guardarYObtenerVerbId() {
        repository.saveSelectedVerbId("verb_123")
        assertThat(repository.getSelectedVerbId()).isEqualTo("verb_123")
    }

    @Test
    fun seleccion04_guardarYObtenerVerbName() {
        repository.saveSelectedVerbName("Quiero")
        assertThat(repository.getSelectedVerbName()).isEqualTo("Quiero")
    }

    @Test
    fun seleccion05_valoresPorDefectoSonNull() {
        assertThat(repository.getSelectedSubjectId()).isNull()
        assertThat(repository.getSelectedSubjectName()).isNull()
        assertThat(repository.getSelectedVerbId()).isNull()
        assertThat(repository.getSelectedVerbName()).isNull()
    }

    @Test
    fun seleccion06_hasSelectedSubjectTrueCuandoHaySujeto() {
        assertThat(repository.hasSelectedSubject()).isFalse()
        repository.saveSelectedSubjectId("subject_123")
        assertThat(repository.hasSelectedSubject()).isTrue()
    }

    @Test
    fun seleccion07_hasSelectedVerbTrueCuandoHayVerbo() {
        assertThat(repository.hasSelectedVerb()).isFalse()
        repository.saveSelectedVerbId("verb_123")
        assertThat(repository.hasSelectedVerb()).isTrue()
    }

    @Test
    fun seleccion08_guardarSubjectConNullLimpia() {
        repository.saveSelectedSubjectId("subject_123")
        repository.saveSelectedSubjectId(null)
        assertThat(repository.getSelectedSubjectId()).isNull()
    }

    @Test
    fun seleccion09_guardarVerbConNullLimpia() {
        repository.saveSelectedVerbId("verb_123")
        repository.saveSelectedVerbId(null)
        assertThat(repository.getSelectedVerbId()).isNull()
    }

    @Test
    fun limpieza01_clearSessionDataLimpiaSoloSeleccion() {
        repository.saveSelectedSubjectId("subject_123")
        repository.saveSelectedVerbId("verb_123")
        repository.saveAudioConfiguration(440, 70)
        repository.savePhysioConfiguration(false, false, 0.8f)
        repository.clearSessionData()
        assertThat(repository.getSelectedSubjectId()).isNull()
        assertThat(repository.getSelectedVerbId()).isNull()
        assertThat(repository.getAudioHz()).isEqualTo(440)
        assertThat(repository.isRightHanded()).isFalse()
    }

    @Test
    fun limpieza02_clearSessionDataMantienePictogramSize() {
        repository.savePictogramSize(PictogramSize.LARGE)
        repository.saveSelectedSubjectId("subject_123")

        repository.clearSessionData()

        assertThat(repository.getPictogramSize()).isEqualTo(PictogramSize.LARGE)
    }

    @Test
    fun limpieza03_clearAllDataLimpiaTodo() {
        // Guardar todos los tipos de datos
        repository.saveSelectedSubjectId("subject_123")
        repository.saveAudioConfiguration(440, 70)
        repository.savePhysioConfiguration(false, false, 0.8f)
        repository.savePictogramSize(PictogramSize.LARGE)
        repository.setConfigured(true)
        repository.clearAllData()

        assertThat(repository.getSelectedSubjectId()).isNull()
        assertThat(repository.getAudioHz()).isEqualTo(440)
        assertThat(repository.isRightHanded()).isTrue()
        assertThat(repository.getPictogramSize()).isEqualTo(PictogramSize.MEDIUM)
        assertThat(repository.isConfigured()).isFalse()
    }

    @Test
    fun limpieza04_clearAllDataDespuesDeMultiplesOperaciones() {
        repository.saveSelectedSubjectId("subject_1")
        repository.saveSelectedSubjectId("subject_2")
        repository.saveAudioConfiguration(440, 70)
        repository.saveAudioConfiguration(880, 80)
        repository.clearAllData()
        assertThat(repository.getSelectedSubjectId()).isNull()
        assertThat(repository.getAudioHz()).isEqualTo(440)
        assertThat(repository.getAudioDb()).isEqualTo(70)
    }

    @Test
    fun extra01_getAudioHzRetornaUltimoValorGuardado() {
        repository.saveAudioConfiguration(440, 70)
        repository.saveAudioConfiguration(880, 70)
        assertThat(repository.getAudioHz()).isEqualTo(880)
    }

    @Test
    fun extra02_getAudioDbRetornaUltimoValorGuardado() {
        repository.saveAudioConfiguration(440, 70)
        repository.saveAudioConfiguration(440, 80)
        assertThat(repository.getAudioDb()).isEqualTo(80)
    }

    @Test
    fun extra03_multiplesCambiosEnLateralidad() {
        repository.savePhysioConfiguration(true, true, 1.0f)
        repository.savePhysioConfiguration(false, true, 1.0f)
        repository.savePhysioConfiguration(true, true, 1.0f)
        assertThat(repository.isRightHanded()).isTrue()
    }

    @Test
    fun extra04_multiplesCambiosEnMovimiento() {
        repository.savePhysioConfiguration(true, true, 1.0f)
        repository.savePhysioConfiguration(true, false, 1.0f)
        repository.savePhysioConfiguration(true, true, 1.0f)
        assertThat(repository.hasFullMovement()).isTrue()
    }

    @Test
    fun extra05_multiplesCambiosEnVelocidad() {
        repository.savePhysioConfiguration(true, true, 1.0f)
        repository.savePhysioConfiguration(true, true, 0.5f)
        repository.savePhysioConfiguration(true, true, 1.5f)
        assertThat(repository.getCalibrationSpeed()).isEqualTo(1.5f)
    }

    @Test
    fun extra06_fromMultiplierMapeaCorrectamente() {
        assertThat(PictogramSize.fromMultiplier(0.8f)).isEqualTo(PictogramSize.SMALL)
        assertThat(PictogramSize.fromMultiplier(1.0f)).isEqualTo(PictogramSize.MEDIUM)
        assertThat(PictogramSize.fromMultiplier(1.2f)).isEqualTo(PictogramSize.LARGE)
        assertThat(PictogramSize.fromMultiplier(1.5f)).isEqualTo(PictogramSize.EXTRA_LARGE)
    }

    @Test
    fun extra07_fromNameMapeaCorrectamente() {
        assertThat(PictogramSize.fromName("Pequeño")).isEqualTo(PictogramSize.SMALL)
        assertThat(PictogramSize.fromName("Mediano")).isEqualTo(PictogramSize.MEDIUM)
        assertThat(PictogramSize.fromName("Grande")).isEqualTo(PictogramSize.LARGE)
        assertThat(PictogramSize.fromName("Extra Grande")).isEqualTo(PictogramSize.EXTRA_LARGE)
    }

    @Test
    fun extra08_valoresDeDisplayNameSonCorrectos() {
        assertThat(PictogramSize.SMALL.displayName).isEqualTo("Pequeño")
        assertThat(PictogramSize.MEDIUM.displayName).isEqualTo("Mediano")
        assertThat(PictogramSize.LARGE.displayName).isEqualTo("Grande")
        assertThat(PictogramSize.EXTRA_LARGE.displayName).isEqualTo("Extra Grande")
    }

    @Test
    fun extra09_carouselSizeTieneValoresCorrectos() {
        assertThat(PictogramSize.SMALL.carouselSize).isEqualTo(64)
        assertThat(PictogramSize.MEDIUM.carouselSize).isEqualTo(80)
        assertThat(PictogramSize.LARGE.carouselSize).isEqualTo(96)
        assertThat(PictogramSize.EXTRA_LARGE.carouselSize).isEqualTo(120)
    }

    @Test
    fun extra10_sentenceSizeEsFijoParaTodos() {
        assertThat(PictogramSize.SMALL.sentenceSize).isEqualTo(80)
        assertThat(PictogramSize.MEDIUM.sentenceSize).isEqualTo(80)
        assertThat(PictogramSize.LARGE.sentenceSize).isEqualTo(80)
        assertThat(PictogramSize.EXTRA_LARGE.sentenceSize).isEqualTo(80)
    }

    @Test
    fun extra11_getPictogramSizeDespuesDeClearSessionData() {
        repository.savePictogramSize(PictogramSize.LARGE)
        repository.clearSessionData()
        assertThat(repository.getPictogramSize()).isEqualTo(PictogramSize.LARGE)
    }

    @Test
    fun extra12_flujoCompletoDeSeleccionYLimpieza() {
        repository.saveSelectedSubjectId("subject_123")
        repository.saveSelectedVerbId("verb_123")
        assertThat(repository.hasSelectedSubject()).isTrue()
        assertThat(repository.hasSelectedVerb()).isTrue()
        repository.clearSessionData()
        assertThat(repository.hasSelectedSubject()).isFalse()
        assertThat(repository.hasSelectedVerb()).isFalse()
    }
}
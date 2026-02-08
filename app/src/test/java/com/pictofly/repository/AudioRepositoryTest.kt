package com.pictofly.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.audio.TTSDataSource
import com.pictofly.data.audio.VolumeDataSource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.coroutines.launch
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
class AudioRepositoryTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var ttsDataSource: TTSDataSource
    private lateinit var volumeDataSource: VolumeDataSource
    private lateinit var audioRepository: AudioRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        ttsDataSource = mockk(relaxed = true)
        volumeDataSource = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { volumeDataSource.getCurrentVolumeInDb() } returns 70
        every { volumeDataSource.isMuted() } returns false

        Dispatchers.setMain(testDispatcher)
        audioRepository = AudioRepositoryImpl(ttsDataSource, volumeDataSource)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun init01_initializeLlamado() = runTest {
        audioRepository.initialize(context)
        advanceUntilIdle()

        coVerify { ttsDataSource.initialize(context, any()) }
        coVerify { volumeDataSource.initialize(context) }
    }

    @Test
    fun init02_volumenInicialActualizado() = runTest {
        audioRepository.initialize(context)
        advanceUntilIdle()

        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(70)
    }

    @Test
    fun volumen01_setVolumeFromDb() = runTest {
        audioRepository.setVolumeFromDb(80)
        advanceUntilIdle()

        coVerify { ttsDataSource.setVolumeFromDb(80) }
        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(80)
    }

    @Test
    fun volumen02_adjustAndLockVolume() = runTest {
        audioRepository.adjustAndLockVolume(85)
        advanceUntilIdle()

        coVerify { volumeDataSource.adjustAndLockVolume(85) }
        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(85)
    }

    @Test
    fun volumen03_getCurrentVolumeInDb() = runTest {
        every { volumeDataSource.getCurrentVolumeInDb() } returns 75

        val volume = audioRepository.getCurrentVolumeInDb()
        assertThat(volume).isEqualTo(75)
    }

    @Test
    fun volumen04_isMuted() = runTest {
        every { volumeDataSource.isMuted() } returns true

        val muted = audioRepository.isMuted()
        assertThat(muted).isTrue()
    }

    @Test
    fun volumen05_flowVolumenActualizado() = runTest {
        audioRepository.setVolumeFromDb(90)
        advanceUntilIdle()

        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(90)
    }

    @Test
    fun volumen06_multiplesCambiosVolumen() = runTest {
        audioRepository.setVolumeFromDb(50)
        audioRepository.setVolumeFromDb(60)
        audioRepository.setVolumeFromDb(70)
        advanceUntilIdle()

        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(70)
        coVerify(exactly = 3) { ttsDataSource.setVolumeFromDb(any()) }
    }

    @Test
    fun volumen07_cambiarVolumenVariasVeces() = runTest {
        repeat(5) {
            audioRepository.setVolumeFromDb(50 + it * 5)
        }
        advanceUntilIdle()
        coVerify(exactly = 5) { ttsDataSource.setVolumeFromDb(any()) }
    }

    @Test
    fun volumen08_volumenNoCambiaSinLlamada() = runTest {
        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(70)
    }

    @Test
    fun volumen09_ajustarYVerificar() = runTest {
        audioRepository.adjustAndLockVolume(95)
        advanceUntilIdle()
        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(95)
    }

    @Test
    fun volumen10_volumenLimites() = runTest {
        audioRepository.setVolumeFromDb(0)
        audioRepository.setVolumeFromDb(100)
        advanceUntilIdle()
        coVerify { ttsDataSource.setVolumeFromDb(0) }
        coVerify { ttsDataSource.setVolumeFromDb(100) }
    }

    @Test
    fun hablar03_speakSinInicializar() = runTest {
        // No inicializar
        audioRepository.speak("No debería hablar")
        advanceUntilIdle()

        coVerify(exactly = 0) { ttsDataSource.speak(any(), any(), any()) }
    }

    @Test
    fun hablar04_speakTextoVacio() = runTest {
        audioRepository.initialize(context)
        advanceUntilIdle()

        audioRepository.speak("")
        advanceUntilIdle()

        coVerify(exactly = 0) { ttsDataSource.speak(any(), any(), any()) }
    }

    @Test
    fun control01_stop() = runTest {
        audioRepository.stop()
        advanceUntilIdle()

        coVerify { ttsDataSource.stop() }
    }

    @Test
    fun control02_shutdown() = runTest {
        audioRepository.shutdown()
        advanceUntilIdle()

        coVerify { ttsDataSource.shutdown() }
    }

    @Test
    fun control03_isSpeaking() = runTest {
        every { ttsDataSource.isSpeaking() } returns true

        val speaking = audioRepository.isSpeaking()
        assertThat(speaking).isTrue()
    }

    @Test
    fun control04_isSpeakingFalse() = runTest {
        every { ttsDataSource.isSpeaking() } returns false

        val speaking = audioRepository.isSpeaking()
        assertThat(speaking).isFalse()
    }

    @Test
    fun control05_setOnUtteranceProgressListener() = runTest {
        val listener = mockk<UtteranceProgressListener>(relaxed = true)
        audioRepository.setOnUtteranceProgressListener(listener)

        coVerify { ttsDataSource.setOnUtteranceProgressListener(listener) }
    }

    @Test
    fun control06_stopMultiple() = runTest {
        audioRepository.stop()
        audioRepository.stop()
        audioRepository.stop()
        advanceUntilIdle()
        coVerify(exactly = 3) { ttsDataSource.stop() }
    }

    @Test
    fun estado01_volumenInicial() = runTest {
        assertThat(audioRepository.currentVolumeDb.value).isEqualTo(70)
    }

    @Test
    fun estado02_flowEmiteValores() = runTest {
        val emissions = mutableListOf<Int>()
        val job = launch {
            audioRepository.currentVolumeDb.collect { emissions.add(it) }
        }

        audioRepository.setVolumeFromDb(80)
        advanceUntilIdle()
        audioRepository.setVolumeFromDb(90)
        advanceUntilIdle()

        job.cancel()
        assertThat(emissions.size).isAtLeast(2)
    }

    @Test
    fun estado03_flowValorInicial() = runTest {
        val job = launch {
            audioRepository.currentVolumeDb.collect {
                assertThat(it).isEqualTo(70)
            }
        }
        job.cancel()
    }

    @Test
    fun especial02_volumenLimites() = runTest {
        audioRepository.setVolumeFromDb(-10)
        audioRepository.setVolumeFromDb(200)
        advanceUntilIdle()

        coVerify { ttsDataSource.setVolumeFromDb(-10) }
        coVerify { ttsDataSource.setVolumeFromDb(200) }
    }

    @Test
    fun especial03_operacionesSinInicializar() = runTest {
        audioRepository.speak("Test")
        audioRepository.stop()
        audioRepository.shutdown()
        advanceUntilIdle()
    }

    @Test
    fun especial05_shutdownYLuegoSpeak() = runTest {
        audioRepository.initialize(context)
        advanceUntilIdle()
        audioRepository.shutdown()
        audioRepository.speak("Después de shutdown")
        advanceUntilIdle()
        coVerify { ttsDataSource.shutdown() }
    }
}
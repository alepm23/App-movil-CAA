package com.pictofly

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pictofly.repository.AudioRepository
import com.pictofly.ui.navigation.AppNavigation
import com.pictofly.ui.theme.PictoFlyTheme
import com.pictofly.utils.VolumeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var audioRepository: AudioRepository

    private var volumeButtonsLocked = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "Aplicación iniciada")
        VolumeManager.initialize(this)
        audioRepository.initialize(this) { isInitialized ->
            Log.d("MainActivity", "Audio inicializado: $isInitialized")
            if (isInitialized) {
                audioRepository.adjustAndLockVolume(70)
            }
        }

        setContent {
            PictoFlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "Bloqueando botones de volumen")
        volumeButtonsLocked = true
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "App en segundo plano")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "App destruida")
        volumeButtonsLocked = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeButtonsLocked) {
                    Log.d("MainActivity", "Botones de volumen bloqueados")
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeButtonsLocked) {
                    true
                } else {
                    super.onKeyUp(keyCode, event)
                }
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }
}
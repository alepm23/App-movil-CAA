package com.pictofly.repository

import android.content.Context
import android.content.SharedPreferences
import com.pictofly.data.model.PictogramSize
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("pictofly_settings", Context.MODE_PRIVATE)
    }

    private companion object {
        const val KEY_AUDIO_HZ = "audio_hz"
        const val KEY_AUDIO_DB = "audio_db"
        const val DEFAULT_AUDIO_HZ = 440
        const val DEFAULT_AUDIO_DB = 70

        const val KEY_IS_RIGHT_HANDED = "is_right_handed"
        const val KEY_HAS_FULL_MOVEMENT = "has_full_movement"
        const val KEY_CALIBRATION_SPEED = "calibration_speed"
        const val DEFAULT_IS_RIGHT_HANDED = true
        const val DEFAULT_HAS_FULL_MOVEMENT = true
        const val DEFAULT_CALIBRATION_SPEED = 1.0f

        const val KEY_PICTOGRAM_SIZE = "pictogram_size"
        const val DEFAULT_SIZE_MULTIPLIER = 1.0f

        const val KEY_IS_CONFIGURED = "is_configured"
        const val KEY_CONSENT_SHOWN = "consent_shown"
        const val DEFAULT_IS_CONFIGURED = false
        const val DEFAULT_CONSENT_SHOWN = false

        const val KEY_SELECTED_SUBJECT_ID = "selected_subject_id"
        const val KEY_SELECTED_VERB_ID = "selected_verb_id"
        const val KEY_SELECTED_SUBJECT_NAME = "selected_subject_name"
        const val KEY_SELECTED_VERB_NAME = "selected_verb_name"
        const val DEFAULT_SELECTED_ID = ""
        const val DEFAULT_SELECTED_NAME = ""
    }

    fun saveAudioConfiguration(hz: Int, db: Int) {
        prefs.edit().apply {
            putInt(KEY_AUDIO_HZ, hz)
            putInt(KEY_AUDIO_DB, db)
            apply()
        }
    }

    fun getAudioHz(): Int = prefs.getInt(KEY_AUDIO_HZ, DEFAULT_AUDIO_HZ)
    fun getAudioDb(): Int = prefs.getInt(KEY_AUDIO_DB, DEFAULT_AUDIO_DB)

    fun savePhysioConfiguration(isRightHanded: Boolean, hasFullMovement: Boolean, calibrationSpeed: Float) {
        prefs.edit().apply {
            putBoolean(KEY_IS_RIGHT_HANDED, isRightHanded)
            putBoolean(KEY_HAS_FULL_MOVEMENT, hasFullMovement)
            putFloat(KEY_CALIBRATION_SPEED, calibrationSpeed)
            apply()
        }
    }

    fun isRightHanded(): Boolean = prefs.getBoolean(KEY_IS_RIGHT_HANDED, DEFAULT_IS_RIGHT_HANDED)
    fun hasFullMovement(): Boolean = prefs.getBoolean(KEY_HAS_FULL_MOVEMENT, DEFAULT_HAS_FULL_MOVEMENT)
    fun getCalibrationSpeed(): Float = prefs.getFloat(KEY_CALIBRATION_SPEED, DEFAULT_CALIBRATION_SPEED)

    val pictogramSizeFlow: Flow<PictogramSize> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PICTOGRAM_SIZE) {
                trySend(getPictogramSize())
                Log.d("SettingsRepo", "Flow emite: ${getPictogramSize().displayName}")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getPictogramSize())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun savePictogramSize(size: PictogramSize) {
        prefs.edit().putFloat(KEY_PICTOGRAM_SIZE, size.multiplier).apply()
        Log.d("SettingsRepo", "Guardado: ${size.displayName}")
    }

    fun getPictogramSize(): PictogramSize {
        val multiplier = prefs.getFloat(KEY_PICTOGRAM_SIZE, DEFAULT_SIZE_MULTIPLIER)
        return PictogramSize.fromMultiplier(multiplier)
    }

    fun getPictogramSizeMultiplier(): Float = prefs.getFloat(KEY_PICTOGRAM_SIZE, DEFAULT_SIZE_MULTIPLIER)
    fun setConfigured(configured: Boolean) {
        prefs.edit().putBoolean(KEY_IS_CONFIGURED, configured).apply()
    }

    fun isConfigured(): Boolean = prefs.getBoolean(KEY_IS_CONFIGURED, DEFAULT_IS_CONFIGURED)
    fun setConsentShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT_SHOWN, shown).apply()
    }

    fun isConsentShown(): Boolean = prefs.getBoolean(KEY_CONSENT_SHOWN, DEFAULT_CONSENT_SHOWN)

    fun saveSelectedSubjectId(id: String?) {
        prefs.edit().putString(KEY_SELECTED_SUBJECT_ID, id ?: DEFAULT_SELECTED_ID).apply()
    }

    fun getSelectedSubjectId(): String? {
        val id = prefs.getString(KEY_SELECTED_SUBJECT_ID, DEFAULT_SELECTED_ID)
        return if (id.isNullOrEmpty()) null else id
    }

    fun saveSelectedSubjectName(name: String?) {
        prefs.edit().putString(KEY_SELECTED_SUBJECT_NAME, name ?: DEFAULT_SELECTED_NAME).apply()
    }

    fun getSelectedSubjectName(): String? {
        val name = prefs.getString(KEY_SELECTED_SUBJECT_NAME, DEFAULT_SELECTED_NAME)
        return if (name.isNullOrEmpty()) null else name
    }

    fun saveSelectedVerbId(id: String?) {
        prefs.edit().putString(KEY_SELECTED_VERB_ID, id ?: DEFAULT_SELECTED_ID).apply()
    }

    fun getSelectedVerbId(): String? {
        val id = prefs.getString(KEY_SELECTED_VERB_ID, DEFAULT_SELECTED_ID)
        return if (id.isNullOrEmpty()) null else id
    }

    fun saveSelectedVerbName(name: String?) {
        prefs.edit().putString(KEY_SELECTED_VERB_NAME, name ?: DEFAULT_SELECTED_NAME).apply()
    }

    fun getSelectedVerbName(): String? {
        val name = prefs.getString(KEY_SELECTED_VERB_NAME, DEFAULT_SELECTED_NAME)
        return if (name.isNullOrEmpty()) null else name
    }

    fun clearSessionData() {
        prefs.edit().apply {
            remove(KEY_SELECTED_SUBJECT_ID)
            remove(KEY_SELECTED_VERB_ID)
            remove(KEY_SELECTED_SUBJECT_NAME)
            remove(KEY_SELECTED_VERB_NAME)
            apply()
        }
    }

    fun hasSelectedSubject(): Boolean {
        return getSelectedSubjectId() != null
    }

    fun hasSelectedVerb(): Boolean {
        return getSelectedVerbId() != null
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
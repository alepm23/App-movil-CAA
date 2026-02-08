package com.pictofly.repository

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ConsentRepository {
    suspend fun hasConsentBeenShown(): Boolean
    suspend fun saveConsentShown()
    suspend fun clearConsentData()
}

class ConsentRepositoryImpl @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) : ConsentRepository {

    override suspend fun hasConsentBeenShown(): Boolean {
        return userSettingsRepository.getCurrentSettings().consentShown
    }

    override suspend fun saveConsentShown() {
        userSettingsRepository.saveConsentShown()
    }

    override suspend fun clearConsentData() {
        userSettingsRepository.clearAllData()
    }
}
package com.pictofly.di

import android.content.Context
import com.pictofly.data.audio.TTSDataSource
import com.pictofly.data.audio.TTSDataSourceImpl
import com.pictofly.data.audio.VolumeDataSource
import com.pictofly.data.audio.VolumeDataSourceImpl
import com.pictofly.data.local.UserPreferencesDataSource
import com.pictofly.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.pictofly.data.local.CalibrationDataSource

@Module
@InstallIn(SingletonComponent::class) //vive mientras la app vive
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideUserPreferencesDataSource(@ApplicationContext context: Context): UserPreferencesDataSource =
        UserPreferencesDataSource(context)

    @Provides
    @Singleton
    fun provideTTSDataSource(): TTSDataSource = TTSDataSourceImpl()

    @Provides
    @Singleton
    fun provideVolumeDataSource(): VolumeDataSource = VolumeDataSourceImpl()

    @Provides
    @Singleton
    fun provideAudioRepository(
        ttsDataSource: TTSDataSource,
        volumeDataSource: VolumeDataSource
    ): AudioRepository = AudioRepositoryImpl(ttsDataSource, volumeDataSource)

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        userPreferencesDataSource: UserPreferencesDataSource
    ): UserSettingsRepository = UserSettingsRepositoryImpl(userPreferencesDataSource)

    @Provides
    @Singleton
    fun provideConsentRepository(
        userSettingsRepository: UserSettingsRepository
    ): ConsentRepository = ConsentRepositoryImpl(userSettingsRepository)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideLocalContentRepository(
        @ApplicationContext context: Context
    ): LocalContentRepository = LocalContentRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideUserPictogramOverrideRepository(
        @ApplicationContext context: Context
    ): UserPictogramOverrideRepository = UserPictogramOverrideRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideCleanupRepository(
        @ApplicationContext context: Context,
        localContentRepository: LocalContentRepository,
        userOverrideRepository: UserPictogramOverrideRepository,
        settingsRepository: SettingsRepository,
        userSettingsRepository: UserSettingsRepository
    ): CleanupRepository = CleanupRepository(
        context = context,
        localContentRepository = localContentRepository,
        userOverrideRepository = userOverrideRepository,
        settingsRepository = settingsRepository,
        userSettingsRepository = userSettingsRepository
    )

    @Provides
    @Singleton
    fun provideCalibrationDataSource(
        @ApplicationContext context: Context
    ): CalibrationDataSource = CalibrationDataSource(context)

    @Provides
    @Singleton
    fun provideCalibrationRepository(
        calibrationDataSource: CalibrationDataSource
    ): CalibrationRepository = CalibrationRepository(calibrationDataSource)
}
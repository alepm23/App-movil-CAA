package com.pictofly.di

import com.pictofly.utils.CategoryEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EventBusModule {

    @Provides
    @Singleton
    fun provideCategoryEventBus(): CategoryEventBus = CategoryEventBus()
}
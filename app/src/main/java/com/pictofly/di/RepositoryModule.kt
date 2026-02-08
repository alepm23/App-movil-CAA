package com.pictofly.di

import com.pictofly.repository.CategoryRepository
import com.pictofly.repository.CategoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindCategoryRepository(
        repository: CategoryRepositoryImpl
    ): CategoryRepository
}
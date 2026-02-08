package com.pictofly.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<CategoryEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: CategoryEvent) {
        _events.emit(event)
    }
}

sealed class CategoryEvent {
    object CategoriesUpdated : CategoryEvent()
    data class CategoryUpdated(val categoryName: String) : CategoryEvent()
    data class PictogramsUpdated(val categoryName: String) : CategoryEvent()
}
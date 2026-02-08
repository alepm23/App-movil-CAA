package com.pictofly.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Singleton

@Singleton
object PictogramEventBus {
    private val _events = MutableSharedFlow<PictogramEvent>()
    val events = _events.asSharedFlow()

    suspend fun emitEvent(event: PictogramEvent) {
        _events.emit(event)
    }
}

sealed class PictogramEvent {
    data class PictogramDeleted(
        val pictogramId: String,
        val pictogramType: String,
        val pictogramName: String
    ) : PictogramEvent()
}
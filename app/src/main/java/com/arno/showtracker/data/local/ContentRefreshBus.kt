package com.arno.showtracker.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Fired when a setting that affects content (country filters, genres) changes, so open screens can reload. */
@Singleton
class ContentRefreshBus @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    suspend fun notifyChanged() {
        _events.emit(Unit)
    }
}

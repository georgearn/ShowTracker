package com.georgearn.showtracker.ui.screens.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UserMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null
)

/**
 * App-wide snackbar queue. Lives above any single screen so an "Undo" still works after the
 * user navigates away from the screen that triggered it. Channel (not SharedFlow) so a message
 * posted while the UI is stopped is buffered instead of dropped.
 */
@Singleton
class UserMessageBus @Inject constructor() {
    private val channel = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = channel.receiveAsFlow()

    fun post(message: UserMessage) {
        channel.trySend(message)
    }
}

package com.example.nova.conversation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTimeoutManager @Inject constructor() {
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startTimeout(seconds: Int, onTimeout: () -> Unit) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(seconds * 1000L)
            onTimeout()
        }
    }

    fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}

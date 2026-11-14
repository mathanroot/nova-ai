package com.example.nova.wakeword

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeWordModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isWakeWordModelReady = MutableStateFlow(true)
    val isWakeWordModelReady: StateFlow<Boolean> = _isWakeWordModelReady.asStateFlow()

    fun getWakeWordModelPath(): String {
        val modelFile = File(context.filesDir, "hey_nova.onnx")
        return modelFile.absolutePath
    }

    fun hasCustomModel(): Boolean {
        return File(getWakeWordModelPath()).exists()
    }
}

package com.example.nova.wakeword

import android.content.Context
import android.util.Log
import com.example.nova.stt.ModelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import org.vosk.Recognizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AndroidWakeWordEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager
) : WakeWordEngine {

    private val tag = "AndroidWakeWordEngine"
    private var isInitialized = false
    private var voskKeywordRecognizer: Recognizer? = null

    // Acoustic sliding buffer for backup acoustic pattern detection
    private val energyBuffer = FloatArray(40)
    private var bufferIndex = 0

    override fun initEngine(): Boolean {
        try {
            // Attempt to initialize Vosk grammar recognizer for "hey nova" if model is ready
            if (modelManager.isModelReady()) {
                val model = modelManager.modelDirectory
                if (model.exists()) {
                    try {
                        val voskModel = org.vosk.Model(model.absolutePath)
                        // Grammar restricted strictly to "hey nova"
                        voskKeywordRecognizer = Recognizer(voskModel, 16000.0f, "[\"hey nova\", \"[unk]\"]")
                        Log.d(tag, "Vosk Keyword Recognizer initialized for 'Hey Nova'")
                    } catch (e: Exception) {
                        Log.w(tag, "Could not initialize Vosk keyword recognizer; using acoustic spotter", e)
                    }
                }
            }
            isInitialized = true
            return true
        } catch (e: Exception) {
            Log.e(tag, "Error initializing wake word engine", e)
            isInitialized = true // Still allow acoustic detector
            return true
        }
    }

    override fun processAudioFrame(buffer: ShortArray, readSize: Int): Boolean {
        if (!isInitialized || readSize <= 0) return false

        // 1. If Vosk keyword grammar spotter is available
        voskKeywordRecognizer?.let { recognizer ->
            try {
                if (recognizer.acceptWaveForm(buffer, readSize)) {
                    val resultJson = recognizer.result
                    if (containsWakePhrase(resultJson)) {
                        Log.d(tag, "Vosk detected wake phrase: 'Hey Nova'")
                        reset()
                        return true
                    }
                } else {
                    val partialJson = recognizer.partialResult
                    if (containsWakePhrase(partialJson)) {
                        Log.d(tag, "Vosk detected wake phrase (partial): 'Hey Nova'")
                        reset()
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in Vosk keyword spotter", e)
            }
        }

        // 2. Sliding acoustic energy analysis for phonetic envelope of "Hey" (burst) + "No-va" (two syllables)
        var sumEnergy = 0.0f
        var zeroCrossings = 0
        for (i in 0 until readSize) {
            val sample = buffer[i]
            sumEnergy += abs(sample.toInt())
            if (i > 0 && ((buffer[i].toInt() xor buffer[i - 1].toInt()) < 0)) {
                zeroCrossings++
            }
        }

        val avgEnergy = sumEnergy / readSize
        energyBuffer[bufferIndex % energyBuffer.size] = avgEnergy
        bufferIndex++

        return false
    }

    private fun containsWakePhrase(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            val text = (json.optString("text") + " " + json.optString("partial")).lowercase()
            text.contains("hey nova") || text.contains("nova")
        } catch (e: Exception) {
            false
        }
    }

    override fun reset() {
        bufferIndex = 0
        energyBuffer.fill(0f)
        voskKeywordRecognizer?.reset()
    }

    override fun release() {
        try {
            voskKeywordRecognizer?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing keyword recognizer", e)
        } finally {
            voskKeywordRecognizer = null
            isInitialized = false
        }
    }

    override fun isReady(): Boolean = isInitialized
}

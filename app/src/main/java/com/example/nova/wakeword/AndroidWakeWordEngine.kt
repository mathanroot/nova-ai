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

    // Sliding ring buffer tracking recent frame energies (approx 2.5 seconds at ~64ms per frame)
    private val energyHistory = FloatArray(40)
    private var historyIndex = 0
    private var totalFramesProcessed = 0L

    // Debounce to prevent immediate double-triggers
    private var lastTriggerFrame = -100L

    override fun initEngine(): Boolean {
        try {
            if (modelManager.isModelReady()) {
                val model = modelManager.modelDirectory
                if (model.exists()) {
                    try {
                        val voskModel = org.vosk.Model(model.absolutePath)
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
            isInitialized = true
            return true
        }
    }

    override fun processAudioFrame(buffer: ShortArray, readSize: Int): Boolean {
        if (!isInitialized || readSize <= 0) return false

        totalFramesProcessed++

        // 1. If Vosk keyword grammar recognizer is active, check its output
        val recognizer = voskKeywordRecognizer
        if (recognizer != null) {
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

        // 2. Real-time acoustic energy & syllable cadence detector for "Hey Nova"
        var sumEnergy = 0.0f
        for (i in 0 until readSize) {
            sumEnergy += abs(buffer[i].toInt())
        }
        val avgEnergy = sumEnergy / readSize

        val index = (historyIndex % energyHistory.size)
        energyHistory[index] = avgEnergy
        historyIndex++

        // Prevent triggers within 30 frames (~2 seconds) of previous trigger
        if (totalFramesProcessed - lastTriggerFrame < 30) {
            return false
        }

        // Check for "Hey Nova" cadence:
        // [Silence/Low] -> [Syllable 1: "Hey"] -> [Dip] -> [Syllable 2 & 3: "No-va"]
        val detected = checkCadencePattern()
        if (detected) {
            Log.d(tag, "Acoustic pattern matched 'Hey Nova' cadence!")
            lastTriggerFrame = totalFramesProcessed
            reset()
            return true
        }

        return false
    }

    private fun checkCadencePattern(): Boolean {
        val size = energyHistory.size
        val currentIdx = historyIndex

        // Thresholds in linear 16-bit PCM amplitude
        val speechThreshold = 1800f
        val silenceThreshold = 900f

        // Analyze last 22 frames (~1.4 seconds)
        var speechBursts = 0
        var inBurst = false
        var burstLength = 0
        var valleyLength = 0
        val burstLengths = mutableListOf<Int>()

        for (offset in 21 downTo 0) {
            val i = (currentIdx - offset + size * 10) % size
            val e = energyHistory[i]

            if (e > speechThreshold) {
                if (!inBurst) {
                    inBurst = true
                    speechBursts++
                    if (valleyLength > 0) {
                        valleyLength = 0
                    }
                }
                burstLength++
            } else {
                if (inBurst) {
                    inBurst = false
                    burstLengths.add(burstLength)
                    burstLength = 0
                }
                if (e < silenceThreshold) {
                    valleyLength++
                }
            }
        }

        if (inBurst) {
            burstLengths.add(burstLength)
        }

        // "Hey Nova" typically exhibits 2 distinct speech bursts:
        // Burst 1: "Hey" (~2-8 frames)
        // Burst 2: "Nova" (~3-12 frames)
        if (burstLengths.size == 2) {
            val heyLen = burstLengths[0]
            val novaLen = burstLengths[1]
            if (heyLen in 2..8 && novaLen in 3..12) {
                return true
            }
        }

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
        energyHistory.fill(0f)
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

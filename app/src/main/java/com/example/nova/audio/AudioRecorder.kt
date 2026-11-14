package com.example.nova.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(
    private val audioLevelMonitor: AudioLevelMonitor? = null,
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private val tag = "AudioRecorder"
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)

    val bufferSize: Int = run {
        val minSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        maxOf(minSize, sampleRate * 2) // At least 1 second buffer
    }

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, onAudioChunk: (buffer: ShortArray, readSize: Int) -> Unit): Boolean {
        if (isRecording.get()) {
            Log.w(tag, "AudioRecorder is already recording")
            return true
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "Failed to initialize AudioRecord")
                stopAndRelease()
                return false
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(1024)
                while (isActive && isRecording.get()) {
                    val record = audioRecord ?: break
                    val readSize = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readSize > 0) {
                        audioLevelMonitor?.processAudioFrame(shortBuffer, readSize)
                        onAudioChunk(shortBuffer, readSize)
                    } else if (readSize == AudioRecord.ERROR_INVALID_OPERATION || readSize == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(tag, "AudioRecord read error: $readSize")
                        break
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(tag, "Exception starting AudioRecorder", e)
            stopAndRelease()
            return false
        }
    }

    fun stopAndRelease() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
            audioLevelMonitor?.reset()
        }
    }

    fun isRecordingActive(): Boolean = isRecording.get()
}

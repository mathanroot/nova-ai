package com.example.nova.wakeword

interface WakeWordEngine {
    fun initEngine(): Boolean
    fun processAudioFrame(buffer: ShortArray, readSize: Int): Boolean
    fun reset()
    fun release()
    fun isReady(): Boolean
}

package com.example.nova.stt

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ModelStatus {
    UNINITIALIZED,
    CHECKING,
    READY,
    NOT_FOUND,
    CORRUPTED,
    ERROR
}

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "ModelManager"
    private val _modelStatus = MutableStateFlow(ModelStatus.UNINITIALIZED)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private var loadedVoskModel: Model? = null

    val modelDirectory: File by lazy {
        File(context.filesDir, "vosk-model")
    }

    suspend fun getOrLoadModel(): Model? = withContext(Dispatchers.IO) {
        if (loadedVoskModel != null) {
            return@withContext loadedVoskModel
        }

        _modelStatus.value = ModelStatus.CHECKING
        val targetDir = modelDirectory

        if (!targetDir.exists() || !isValidModelDir(targetDir)) {
            // Attempt to check if assets contain model or zip
            val extracted = tryExtractModelFromAssets()
            if (!extracted) {
                Log.w(tag, "Vosk model not found at ${targetDir.absolutePath}")
                _modelStatus.value = ModelStatus.NOT_FOUND
                return@withContext null
            }
        }

        try {
            Log.d(tag, "Loading Vosk Model from: ${targetDir.absolutePath}")
            val model = Model(targetDir.absolutePath)
            loadedVoskModel = model
            _modelStatus.value = ModelStatus.READY
            Log.d(tag, "Vosk Model loaded successfully!")
            return@withContext model
        } catch (e: Exception) {
            Log.e(tag, "Failed to load Vosk model", e)
            _modelStatus.value = ModelStatus.CORRUPTED
            return@withContext null
        }
    }

    private fun isValidModelDir(dir: File): Boolean {
        if (!dir.isDirectory) return false
        val files = dir.list() ?: return false
        // Valid vosk model directory must contain am, conf, or model files
        val hasConf = File(dir, "conf").exists() || File(dir, "model.conf").exists()
        val hasAmOrModel = File(dir, "am").exists() || File(dir, "final.mdl").exists() || files.isNotEmpty()
        return hasConf || hasAmOrModel
    }

    private fun tryExtractModelFromAssets(): Boolean {
        try {
            val assetManager = context.assets
            val list = assetManager.list("") ?: return false
            val modelZipName = list.firstOrNull { it.startsWith("vosk-model") && it.endsWith(".zip") }
            if (modelZipName != null) {
                Log.d(tag, "Found model zip in assets: $modelZipName. Extracting...")
                val inputStream = assetManager.open(modelZipName)
                extractZip(inputStream, modelDirectory)
                return isValidModelDir(modelDirectory)
            }
        } catch (e: Exception) {
            Log.e(tag, "Could not extract model from assets", e)
        }
        return false
    }

    private fun extractZip(inputStream: InputStream, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(destDir, entry.name)
                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { fos ->
                        zipIn.copyTo(fos)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    fun isModelReady(): Boolean = loadedVoskModel != null
}

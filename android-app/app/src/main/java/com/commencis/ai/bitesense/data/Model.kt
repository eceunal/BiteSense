package com.commencis.ai.bitesense.data

import android.content.Context
import java.io.File

/**
 * Model data class for the AI model
 */
data class Model(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val description: String,
    val downloadFileName: String,
    val supportsImage: Boolean = true
) {
    fun getPath(context: Context): String {
        // Always use external storage path for MediaPipe
        val modelDir = File(context.getExternalFilesDir(null), "models")
        return File(modelDir, downloadFileName).absolutePath
    }
    
    fun isModelFileExists(context: Context): Boolean {
        // Check assets first
        if (isInAssets(context)) {
            return true
        }
        // Then check external storage
        return File(getPath(context)).exists()
    }
    
    fun isInAssets(context: Context): Boolean {
        return try {
            context.assets.list("")?.contains(downloadFileName) ?: false
        } catch (e: Exception) {
            false
        }
    }
}

// Model for BiteSense AI - Gemma 3n E2B
val BITESENSE_MODEL = Model(
    name = "Gemma 3n E2B",
    modelId = "google/gemma-3n-E2B-it-litert-preview",
    modelFile = "gemma-3n-E2B-it-int4.task",
    description = "AI model for analyzing insect bites with image understanding capabilities",
    downloadFileName = "gemma-3n-e2b.task",
    supportsImage = true
)


package com.commencis.ai.bitesense.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified repository for managing model from assets
 */
@Singleton
class ModelDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ModelDownloadRepo"
    }

    /**
     * Copy model from assets to external storage if needed
     */
    suspend fun copyModelFromAssetsIfNeeded(model: Model): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if model exists in external storage
            val externalPath = model.getPath(context)
            val externalFile = java.io.File(externalPath)
            
            // If already exists in external storage, no need to copy
            if (externalFile.exists()) {
                Log.d(TAG, "Model already exists in external storage: $externalPath")
                return@withContext true
            }
            
            // Check if model exists in assets
            if (!model.isInAssets(context)) {
                Log.d(TAG, "Model not found in assets: ${model.downloadFileName}")
                return@withContext false
            }
            
            // Create parent directory
            externalFile.parentFile?.mkdirs()
            
            // Copy from assets to external storage
            Log.d(TAG, "Copying model from assets to: $externalPath")
            context.assets.open(model.downloadFileName).use { inputStream ->
                FileOutputStream(externalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Log.d(TAG, "Model copied successfully from assets to: $externalPath")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets", e)
            return@withContext false
        }
    }
}
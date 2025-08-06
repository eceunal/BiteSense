package com.commencis.ai.bitesense.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class CameraHandler(private val context: Context) {
    private val _currentPhotoUri = MutableStateFlow<Uri?>(null)
    val currentPhotoUri: StateFlow<Uri?> = _currentPhotoUri.asStateFlow()

    fun createPhotoUri(): Uri {
        val photoFile = File(
            context.cacheDir,
            "captured_bite_${System.currentTimeMillis()}.jpg"
        )
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        
        _currentPhotoUri.value = uri
        return uri
    }

    fun clearPhotoUri() {
        _currentPhotoUri.value = null
    }

    fun deletePhotoFile(uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Ignore deletion errors
        }
    }
}
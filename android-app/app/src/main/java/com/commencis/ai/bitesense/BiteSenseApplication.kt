package com.commencis.ai.bitesense

import android.app.Application
import android.util.Log
import com.commencis.ai.bitesense.ai.MediaPipeModelManager
import com.commencis.ai.bitesense.data.ModelDownloadRepository
import com.commencis.ai.bitesense.data.BITESENSE_MODEL
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BiteSenseApplication : Application() {
    
    @Inject
    lateinit var mediaPipeModelManager: MediaPipeModelManager
    
    @Inject
    lateinit var modelDownloadRepository: ModelDownloadRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize model in background as soon as app starts
        applicationScope.launch {
            try {
                Log.d("BiteSenseApp", "Starting model initialization on app launch")
                
                // Copy model from assets if needed
                val copied = modelDownloadRepository.copyModelFromAssetsIfNeeded(BITESENSE_MODEL)
                Log.d("BiteSenseApp", "Model copy from assets result: $copied")
                
                // Initialize MediaPipe model
                val startTime = System.currentTimeMillis()
                mediaPipeModelManager.initializeModel()
                val initTime = System.currentTimeMillis() - startTime
                
                Log.d("BiteSenseApp", "Model initialized successfully in ${initTime}ms")
            } catch (e: Exception) {
                Log.e("BiteSenseApp", "Failed to initialize model on startup", e)
                // Don't crash the app, model will be initialized when needed
            }
        }
    }
}
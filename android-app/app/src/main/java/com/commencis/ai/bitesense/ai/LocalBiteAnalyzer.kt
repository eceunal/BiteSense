package com.commencis.ai.bitesense.ai

import android.net.Uri
import javax.inject.Inject

/**
 * Local implementation of BiteAnalyzer that uses the on-device Gemma 3n model.
 * This provides full offline functionality for bite analysis with privacy protection.
 * 
 * Uses a two-step approach:
 * 1. Detect insect type from image using Gemma 3n
 * 2. Generate detailed analysis based on detected type using Gemma 3n
 */
class LocalBiteAnalyzer @Inject constructor(
    private val modelManager: MediaPipeModelManager
) : BiteAnalyzer {
    
    // Callback for when insect type is detected (for UI updates)
    var onInsectDetected: ((String) -> Unit)? = null
    
    // Callback for streaming updates
    var onStreamingUpdate: ((PartialBiteAnalysis) -> Unit)? = null
    
    override suspend fun analyzeBiteImage(imageUri: Uri): BiteAnalysisResult? {
        // Step 1: Detect insect type
        val detectedType = modelManager.detectInsectType(imageUri)
        
        if (detectedType == null || detectedType == "unknown") {
            // If detection fails or no bite detected, notify UI
            onInsectDetected?.invoke("no_bites")
            return null
        }
        
        // Notify UI about the detected insect type
        onInsectDetected?.invoke(detectedType)
        
        // Optional: Add a small delay to show the detection UI
        kotlinx.coroutines.delay(1500)
        
        // Step 2: Generate detailed analysis using the detected type
        return if (onStreamingUpdate != null) {
            // Use streaming if callback is provided
            modelManager.generateBiteAnalysisFromTypeStreaming(
                insectType = detectedType,
                onPartialUpdate = onStreamingUpdate!!
            )
        } else {
            // Fall back to non-streaming
            modelManager.generateBiteAnalysisFromType(detectedType)
        }
    }
}
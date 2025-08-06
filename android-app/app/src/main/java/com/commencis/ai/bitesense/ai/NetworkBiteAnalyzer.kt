package com.commencis.ai.bitesense.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.commencis.ai.bitesense.network.BiteSenseApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Network implementation of BiteAnalyzer that uses a hybrid approach:
 * 1. Sends image to server for insect type detection
 * 2. Uses on-device Gemma 3n to generate detailed analysis based on the detected type
 * 
 * This provides fast detection with privacy-preserving on-device generation.
 */
class NetworkBiteAnalyzer @Inject constructor(
    private val api: BiteSenseApiService,
    private val modelManager: MediaPipeModelManager,
    @ApplicationContext private val context: Context
) : BiteAnalyzer {
    
    companion object {
        private const val TAG = "NetworkBiteAnalyzer"
    }
    
    // Callback for when insect type is detected
    var onInsectDetected: ((String) -> Unit)? = null
    
    // Callback for streaming updates
    var onStreamingUpdate: ((PartialBiteAnalysis) -> Unit)? = null
    
    override suspend fun analyzeBiteImage(imageUri: Uri): BiteAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            // Step 1: Prepare image for network upload
            val imageFile = getFileFromUri(imageUri)
            val requestBody = imageFile.asRequestBody("image/*".toMediaType())
            val imagePart = MultipartBody.Part.createFormData("file", imageFile.name, requestBody)
            val systemPrompt = """
                Analyze the provided insect-bite image and identify which insect caused it.
                Use the quick-reference table of characteristic skin findings to help choose the correct insect type.
                
                Insect (enum value) | Distinctive skin presentation without relying on exact size measurements
                mosquitos : Soft, round, puffy bumps that rise within minutes; often show a pinpoint center; produce intense itching that eases after a couple of days.
                bed bugs : Multiple itchy red bumps arranged in a straight or zig-zag line ("breakfast-lunch-dinner" pattern); each lesion may show a tiny dark dot in the middle.
                chiggers : Bright-red bumps or small blisters with a firm yellowish cap; found where clothing presses the skin (waistband, sock line); itching becomes severe within hours.
                spider : One or a few swollen red plaques with two closely spaced puncture marks; may expand outward or form a blister or dark center over time.
                fleas : Groups of red bumps each surrounded by a pale halo; commonly located on ankles and lower legs in clusters of three or four; very itchy.
                tick : Firm red bump at the bite site; the tick itself may still be attached; days later, watch for a slowly enlarging ring or "bull's-eye" pattern.
                ants : Red, itchy bumps that may blister; often found on exposed skin like hands or feet; can be painful and itchy.
                
                Return one of: mosquitos | bed bugs | chiggers | spider | fleas | tick | ants
                
                If no bite is visible or you cannot determine, return: no_bites
                
                Return ONLY the insect name, nothing else.
            """.trimIndent().toRequestBody("text/plain".toMediaType())
            
            // Step 2: Get insect type from network
            Log.d(TAG, "Calling /predict endpoint")
            val prediction = api.predictInsect(imagePart, systemPrompt)
            Log.d(TAG, "Detected insect type: ${prediction.insectType}")
            
            // Check if no bite was detected
            if (prediction.insectType == "unknown" || prediction.insectType == "no_bites") {
                // Notify UI about no bites detected
                withContext(Dispatchers.Main) {
                    onInsectDetected?.invoke("no_bites")
                }
                // Clean up temp file
                imageFile.delete()
                return@withContext null
            }
            
            // Notify UI about the detected insect type
            withContext(Dispatchers.Main) {
                onInsectDetected?.invoke(prediction.insectType)
            }
            
            // Optional: Add a small delay to show the detection UI
            kotlinx.coroutines.delay(500)
            
            // Step 3: Use on-device Gemma 3n to generate detailed analysis with streaming
            Log.d(TAG, "Generating detailed analysis with on-device Gemma 3n (streaming)")
            val analysisResult = if (onStreamingUpdate != null) {
                // Use streaming if callback is provided
                modelManager.generateBiteAnalysisFromTypeStreaming(
                    insectType = prediction.insectType,
                    onPartialUpdate = { partial ->
                        // Callback is already called on Main dispatcher by MediaPipeModelManager
                        onStreamingUpdate?.invoke(partial)
                    }
                )
            } else {
                // Fall back to non-streaming
                modelManager.generateBiteAnalysisFromType(
                    insectType = prediction.insectType
                )
            }
            
            // Clean up temp file
            imageFile.delete()
            
            if (analysisResult == null) {
                Log.e(TAG, "On-device Gemma 3n failed to generate analysis")
            }
            
            analysisResult
        } catch (e: Exception) {
            Log.e(TAG, "Network analysis failed", e)
            null
        }
    }
    
    /**
     * Converts a content URI to a compressed temporary file for network upload
     */
    private fun getFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
            
        // Decode the image
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        // Calculate compression parameters
        val maxWidth = 1024
        val maxHeight = 1024
        val quality = 85 // JPEG quality (0-100)
        
        // Calculate new dimensions while maintaining aspect ratio
        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        var newWidth = originalBitmap.width
        var newHeight = originalBitmap.height
        
        if (originalBitmap.width > maxWidth || originalBitmap.height > maxHeight) {
            if (aspectRatio > 1) {
                // Landscape
                newWidth = maxWidth
                newHeight = (maxWidth / aspectRatio).toInt()
            } else {
                // Portrait or square
                newHeight = maxHeight
                newWidth = (maxHeight * aspectRatio).toInt()
            }
        }
        
        // Resize the bitmap if needed
        val resizedBitmap = if (newWidth != originalBitmap.width || newHeight != originalBitmap.height) {
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }
        
        // Create compressed file
        val tempFile = File(context.cacheDir, "compressed_image_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { outputStream ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        
        // Log compression results
        val originalSize = originalBitmap.byteCount / 1024 // KB
        val compressedSize = tempFile.length() / 1024 // KB
        Log.d(TAG, "Image compression: ${originalBitmap.width}x${originalBitmap.height} ($originalSize KB) -> ${newWidth}x${newHeight} ($compressedSize KB)")
        
        // Recycle bitmaps to free memory
        if (resizedBitmap != originalBitmap) {
            resizedBitmap.recycle()
        }
        originalBitmap.recycle()
        
        return tempFile
    }
}
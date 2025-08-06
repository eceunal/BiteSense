package com.commencis.ai.bitesense.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import com.commencis.ai.bitesense.data.BITESENSE_MODEL
import com.commencis.ai.bitesense.data.ModelDownloadRepository
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manager for MediaPipe Gemma 3n model following Google Gallery's approach
 * Handles on-device AI inference for bite detection and analysis
 */
@Singleton
class MediaPipeModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadRepository: ModelDownloadRepository
) {
    companion object {
        private const val TAG = "MediaPipeModelManager"
    }

    private var llmInference: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null

    // Simple initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()


    /**
     * Initialize the model and create session
     */
    suspend fun initializeModel() = withContext(Dispatchers.IO) {
        // Skip if already initialized
        if (_isInitialized.value) {
            Log.d(TAG, "Model already initialized")
            return@withContext
        }

        try {
            // Copy model from assets if needed
            val copied = modelDownloadRepository.copyModelFromAssetsIfNeeded(BITESENSE_MODEL)
            if (!copied) {
                throw IllegalStateException("Model file not available in assets")
            }

            val modelPath = BITESENSE_MODEL.getPath(context)
            Log.d(TAG, "Initializing model from: $modelPath")

            // Log initialization start time
            val startTime = System.currentTimeMillis()

            // Create LlmInference options with memory-efficient settings
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(256 * 16)
                .setPreferredBackend(LlmInference.Backend.GPU)
                .setMaxNumImages(1) // Support 1 image for bite analysis
                .build()

            Log.d(TAG, "Creating LlmInference...")
            llmInference = LlmInference.createFromOptions(context, options)
            Log.d(TAG, "LlmInference created in ${System.currentTimeMillis() - startTime}ms")

            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Model successfully initialized in ${totalTime}ms")
            _isInitialized.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            _isInitialized.value = false
            throw e
        }
    }

    /**
     * Create session if needed
     */
    private suspend fun ensureSession() = withContext(Dispatchers.Default) {
        if (llmSession == null && llmInference != null) {
            // Yield before creating session to ensure UI responsiveness
            yield()

            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(true)
                        .build()
                )
                .build()

            llmSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
            Log.d(TAG, "Created new session")

            // Yield after session creation
            yield()
        }
    }

    /**
     * Generate response with yielding to prevent UI blocking
     * MediaPipe's generateResponse() can block even on background threads
     */
    private suspend fun generateResponseWithYield(): String? = withContext(Dispatchers.Default) {
        try {
            // Yield before heavy operation
            yield()

            // Run the blocking call on IO dispatcher to minimize impact
            val response = withContext(Dispatchers.IO) {
                llmSession?.generateResponse()
            }

            // Yield after operation
            yield()

            response
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            null
        }
    }

    /**
     * Parse the JSON response from the model
     */
    private fun parseAnalysisResponse(response: String): BiteAnalysisResult? {
        return try {
            // Extract JSON from response (model might include extra text)
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}")

            if (jsonStart == -1 || jsonEnd == -1) {
                Log.e(TAG, "No JSON found in response: $response")
                return null
            }

            val jsonString = response.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonString)

            BiteAnalysisResult(
                insectType = json.optString("insectType"),
                severity = json.optString("severity"),
                expectedDuration = json.optString("expectedDuration"),
                characteristics = json.optJSONArray("characteristics")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }.orEmpty(),
                treatments = json.optJSONArray("treatments")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }.orEmpty(),
                timeline = json.optJSONObject("timeline")?.let { obj ->
                    obj.keys().asSequence().associate { key ->
                        key to obj.getString(key)
                    }
                }.orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: $response", e)
            null
        }
    }

    /**
     * Generate chat response with streaming support
     */
    suspend fun generateChatResponse(
        prompt: String,
        imageUri: Uri?,
        onPartialUpdate: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.Default) {
        try {
            // Wait for initialization if needed
            if (!_isInitialized.value) {
                Log.d(TAG, "Waiting for model initialization...")
                _isInitialized.first { it }
            }

            // Ensure we have a session
            ensureSession()

            val finalPrompt = """
                $prompt
                
                Do not generate markdown or HTML, just plain text.
            """.trimIndent()
            // Add the prompt to session
            llmSession?.addQueryChunk(finalPrompt)

            // Add image if provided
            imageUri?.let {
                try {
                    Log.d(TAG, "Adding image to chat session: $it")
                    val compressedBitmap = compressBitmap(it)
                    val mpImage = BitmapImageBuilder(compressedBitmap).build()
                    llmSession?.addImage(mpImage)
                    Log.d(TAG, "Image added successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add image to chat session", e)
                }
            }

            // If no streaming callback provided, use the original approach
            if (onPartialUpdate == null) {
                val response = generateResponseWithYield()
                return@withContext response ?: "I'm sorry, I couldn't generate a response. Please try again."
            }

            // Use streaming approach with callback
            val fullResponse = suspendCancellableCoroutine<String?> { continuation ->
                val responseBuilder = StringBuilder()

                // Generate response asynchronously with streaming callback
                llmSession?.generateResponseAsync { partialResult, done ->
                    try {
                        // Append to full response
                        responseBuilder.append(partialResult)

                        // Call the update callback for each chunk
                        onPartialUpdate(partialResult)

                        // Complete when done
                        if (done) {
                            Log.d(TAG, "Chat streaming complete. Total response length: ${responseBuilder.length}")
                            continuation.resume(responseBuilder.toString())
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in chat streaming callback", e)
                        if (done) {
                            continuation.resume(responseBuilder.toString())
                        }
                    }
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Chat streaming generation cancelled")
                }
            }

            fullResponse ?: "I'm sorry, I couldn't generate a response. Please try again."

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate chat response", e)
            "I encountered an error while processing your request. Please try again."
        }
    }

    private suspend fun compressBitmap(imageUri: Uri): Bitmap = withContext(Dispatchers.Default) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw IllegalArgumentException("Cannot open image stream")

        inputStream.use { stream ->
            // First decode just the bounds to get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(stream, null, options)

            // Calculate original image size in memory (width * height * 4 bytes for ARGB_8888)
            val originalSizeBytes = options.outWidth.toLong() * options.outHeight.toLong() * 4
            val originalSizeMB = originalSizeBytes / (1024.0 * 1024.0)

            // Calculate sample size to reduce image size
            val targetWidth = 1024
            val targetHeight = 1024
            val sampleSize = calculateInSampleSize(options, targetWidth, targetHeight)

            Log.d(
                TAG, "Original image: ${options.outWidth}x${options.outHeight} " +
                        "(${
                            String.format(
                                "%.2f",
                                originalSizeMB
                            )
                        }MB in memory), using sample size: $sampleSize"
            )

            // Reopen stream for actual decoding
            val secondStream = context.contentResolver.openInputStream(imageUri)
                ?: throw IllegalArgumentException("Cannot reopen image stream")

            secondStream.use { decodeStream ->
                // Decode with sample size
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888 // MediaPipe requires ARGB_8888
                }

                val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
                    ?: throw IllegalArgumentException("Failed to decode bitmap")

                // Further compress if needed
                if (bitmap.width > targetWidth || bitmap.height > targetHeight) {
                    val scale = minOf(
                        targetWidth.toFloat() / bitmap.width,
                        targetHeight.toFloat() / bitmap.height
                    )
                    val newWidth = (bitmap.width * scale).toInt()
                    val newHeight = (bitmap.height * scale).toInt()

                    val scaledBitmap = bitmap.scale(newWidth, newHeight)
                    if (scaledBitmap != bitmap) {
                        bitmap.recycle()
                    }

                    // Calculate final size
                    val finalSizeBytes = newWidth.toLong() * newHeight.toLong() * 4
                    val finalSizeMB = finalSizeBytes / (1024.0 * 1024.0)

                    Log.d(
                        TAG,
                        "Compressed image from ${bitmap.width}x${bitmap.height} to ${newWidth}x${newHeight} " +
                                "(${
                                    String.format(
                                        "%.2f",
                                        originalSizeMB
                                    )
                                }MB → ${String.format("%.2f", finalSizeMB)}MB, " +
                                "${
                                    String.format(
                                        "%.1f",
                                        (1 - finalSizeMB / originalSizeMB) * 100
                                    )
                                }% reduction)"
                    )
                    scaledBitmap
                } else {
                    // Calculate actual decoded size
                    val decodedSizeBytes = bitmap.width.toLong() * bitmap.height.toLong() * 4
                    val decodedSizeMB = decodedSizeBytes / (1024.0 * 1024.0)

                    Log.d(
                        TAG, "Using image at ${bitmap.width}x${bitmap.height} " +
                                "(${
                                    String.format(
                                        "%.2f",
                                        originalSizeMB
                                    )
                                }MB → ${String.format("%.2f", decodedSizeMB)}MB)"
                    )
                    bitmap
                }
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Detect insect type from image (simple classification)
     * Returns one of: mosquitos | bed bugs | chiggers | spider | fleas | tick
     */
    suspend fun detectInsectType(imageUri: Uri): String? = withContext(Dispatchers.Default) {
        try {
            // Wait for initialization if needed
            if (!_isInitialized.value) {
                Log.d(TAG, "Waiting for model initialization...")
                _isInitialized.first { it }
            }

            // For detection, always create a fresh session
            llmSession?.close()
            llmSession = null
            ensureSession()

            // Detection prompt with characteristic descriptions
            val prompt = """
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
            """.trimIndent()

            // Add text query first
            yield() // Yield before adding query
            llmSession?.addQueryChunk(prompt)
            yield() // Yield after adding query

            // Add image to session
            try {
                Log.d(TAG, "Adding image for detection: $imageUri")
                val compressedBitmap = compressBitmap(imageUri)
                val mpImage = BitmapImageBuilder(compressedBitmap).build()
                llmSession?.addImage(mpImage)
                Log.d(TAG, "Image added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add image for detection", e)
                return@withContext null
            }

            // Generate response
            val response = generateResponseWithYield()

            if (response.isNullOrEmpty()) {
                Log.e(TAG, "Empty response from detection model")
                return@withContext null
            }

            // Clean up response and validate
            val detectedType = response.trim().lowercase()
            val validTypes = setOf("mosquitos", "bed bugs", "chiggers", "spider", "fleas", "tick")

            return@withContext if (validTypes.contains(detectedType)) {
                // Normalize to title case
                detectedType.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
            } else {
                Log.w(TAG, "Invalid insect type detected: $detectedType")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect insect type", e)
            null
        }
    }

    /**
     * Generate bite analysis for a known insect type
     * Used by NetworkBiteAnalyzer and LocalBiteAnalyzer for detailed analysis generation
     * Note: This method does not require the image since the insect type is already known
     */
    suspend fun generateBiteAnalysisFromType(
        insectType: String
    ): BiteAnalysisResult? = withContext(Dispatchers.Default) {
        try {
            // For analysis with known type, always create a fresh session
            llmSession?.close()
            llmSession = null
            ensureSession()

            // Create focused prompt using the known insect type
            val prompt = """
                Generate a detailed medical analysis for a $insectType bite.
                
                Respond ONLY with a complete JSON object that matches the schema below.
                Focus on typical characteristics, treatments, and timeline specific to $insectType bites.
                
                {
                  "insectType": "$insectType",
                  "severity": "one of Low | Moderate | High",
                  "expectedDuration": "3–5 days, 1–2 weeks, ...",
                  "characteristics": ["typical $insectType bite characteristic 1", "characteristic 2", "..."],
                  "treatments": ["treatment 1", "treatment 2", "..."],
                  "timeline": {
                    "Day 1-2": "brief description",
                    "Day 3-4": "brief description",
                    "...": "..."
                  }
                }
            """.trimIndent()

            // Add text query only - no image needed since we already know the insect type
            yield() // Yield before adding query
            llmSession?.addQueryChunk(prompt)
            yield() // Yield after adding query

            // Generate response
            val response = generateResponseWithYield()

            if (response.isNullOrEmpty()) {
                Log.e(TAG, "Empty response from model for type: $insectType")
                return@withContext null
            }

            // Parse JSON response
            return@withContext parseAnalysisResponse(response)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate analysis for type: $insectType", e)
            null
        }
    }

    /**
     * Generate bite analysis for a known insect type with streaming updates
     * Uses generateResponseAsync to provide progressive UI updates
     */
    suspend fun generateBiteAnalysisFromTypeStreaming(
        insectType: String,
        onPartialUpdate: (PartialBiteAnalysis) -> Unit
    ): BiteAnalysisResult? = withContext(Dispatchers.Default) {
        try {
            // For analysis with known type, always create a fresh session
            llmSession?.close()
            llmSession = null
            ensureSession()

            // Create focused prompt using the known insect type
            val prompt = """
                Generate a detailed medical analysis for a $insectType bite.
                
                Respond ONLY with a complete JSON object that matches the schema below.
                Focus on typical characteristics, treatments, and timeline specific to $insectType bites.
                Make sure to provide VALID JSON without any additional text.
                {
                  "insectType": "$insectType",
                  "severity": "String(either Low or Moderate or High)",
                  "expectedDuration": "String(3–5 days, 1–2 weeks, etc.)",
                  "characteristics": ["typical $insectType bite characteristic 1", "characteristic 2", "..."],
                  "treatments": ["treatment 1", "treatment 2", "..."],
                  "timeline": {
                    "Day 1-2": "brief description",
                    "Day 3-4": "brief description",
                    "...": "..."
                  }
                }
            """.trimIndent()

            // Add text query only - no image needed since we already know the insect type
            yield() // Yield before adding query
            llmSession?.addQueryChunk(prompt)
            yield() // Yield after adding query

            // Use suspendCancellableCoroutine to convert callback to suspend function
            val fullResponse = suspendCancellableCoroutine<String?> { continuation ->
                val responseBuilder = StringBuilder()
                val parser = StreamingBiteAnalysisParser(insectType)

                // Generate response asynchronously with streaming callback
                llmSession?.generateResponseAsync { partialResult, done ->
                    try {
                        // Append to full response
                        responseBuilder.append(partialResult)

                        // Parse streaming chunk
                        val hasUpdates = parser.parseChunk(partialResult)

                        // Update UI if we have new data
                        if (hasUpdates) {
                            val partial = parser.getPartialAnalysis()
                            Log.d(
                                TAG,
                                "Streaming update - Severity: ${partial.severity}"
                            )

                            // Call the update callback directly - it's already on a background thread
                            // The callback handler is responsible for dispatching to Main if needed
                            onPartialUpdate(partial)
                        }

                        // Complete when done
                        if (done) {
                            Log.d(
                                TAG,
                                "Streaming complete. Total response length: ${responseBuilder.length}"
                            )
                            continuation.resume(responseBuilder.toString())
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in streaming callback", e)
                        if (done) {
                            continuation.resume(responseBuilder.toString())
                        }
                    }
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Streaming generation cancelled")
                    // MediaPipe doesn't provide a way to cancel ongoing generation
                    // but we can at least stop processing updates
                }
            }

            // Parse complete response for final result
            return@withContext fullResponse?.let { parseAnalysisResponse(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate streaming analysis for type: $insectType", e)
            null
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            llmSession?.close()
            llmSession = null

            llmInference?.close()
            llmInference = null

            _isInitialized.value = false
            Log.d(TAG, "Cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

/**
 * Result from bite analysis
 */
data class BiteAnalysisResult(
    val insectType: String,
    val severity: String,
    val expectedDuration: String,
    val characteristics: List<String>,
    val treatments: List<String>,
    val timeline: Map<String, String>
)
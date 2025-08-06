package com.commencis.ai.bitesense.ui.result

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commencis.ai.bitesense.ai.BiteAnalyzerFactory
import com.commencis.ai.bitesense.ai.LocalBiteAnalyzer
import com.commencis.ai.bitesense.ai.MediaPipeModelManager
import com.commencis.ai.bitesense.ai.NetworkBiteAnalyzer
import com.commencis.ai.bitesense.ai.PartialBiteAnalysis
import com.commencis.ai.bitesense.data.BiteHistoryRepository
import com.commencis.ai.bitesense.data.BiteRecord
import com.commencis.ai.bitesense.ui.components.AnalysisMode
import com.commencis.ai.bitesense.ui.theme.SeverityHigh
import com.commencis.ai.bitesense.ui.theme.SeverityLow
import com.commencis.ai.bitesense.ui.theme.SeverityModerate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BiteAnalysis(
    val biteName: String,
    val severity: String,
    val severityColor: Color,
    val expectedDuration: String,
    val characteristics: List<String>,
    val treatments: List<String>,
    val timeline: Map<String, String>,
)

data class ResultUiState(
    val imageUri: Uri? = null,
    val biteAnalysis: BiteAnalysis? = null,
    val existingBiteRecord: BiteRecord? = null,
    val biteRecordId: String? = null,
    val saveError: String? = null,
    val isAnalyzing: Boolean = true,
    val loadingMessage: String = "Analyzing bite...",
    val detectedInsectType: String? = null,
    val showInsectDetection: Boolean = false,
    val isLoadingDetails: Boolean = false, // New flag for partial loading
    // Section completion flags for streaming
    val isCharacteristicsComplete: Boolean = false,
    val isTreatmentsComplete: Boolean = false,
    val isTimelineComplete: Boolean = false
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: BiteHistoryRepository,
    private val modelManager: MediaPipeModelManager,
    private val biteAnalyzerFactory: BiteAnalyzerFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun initializeWithImage(imageUri: Uri, existingBiteRecordId: String? = null, analysisMode: String? = null) {
        if (existingBiteRecordId != null) {
            // Load from existing record
            loadExistingAnalysis(existingBiteRecordId)
        } else {
            // Analyze new image
            _uiState.update { it.copy(imageUri = imageUri, isAnalyzing = true) }
            
            // Parse analysis mode
            val mode = analysisMode?.let {
                try {
                    AnalysisMode.valueOf(it)
                } catch (e: Exception) {
                    AnalysisMode.NETWORK // Default fallback
                }
            } ?: AnalysisMode.NETWORK
            
            if (!modelManager.isInitialized.value) {
                // Observe model initialization
                observeModelInitialization(imageUri, mode)
            } else {
                // Model is already initialized, analyze the image
                viewModelScope.launch(Dispatchers.Default) {
                    analyzeNewImage(imageUri, mode)
                }
            }
        }
    }

    private fun observeModelInitialization(imageUri: Uri, mode: AnalysisMode) {
        viewModelScope.launch(Dispatchers.Default) {
            modelManager.isInitialized.collect { initialized ->
                if (initialized) {
                    // Model is ready, analyze the image
                    analyzeNewImage(imageUri, mode)
                } else {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = true,
                            loadingMessage = "Initializing AI model..."
                        )
                    }
                }
            }
        }
    }

    private fun loadExistingAnalysis(biteRecordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Load existing record from repository
            val biteRecord =
                repository.biteHistory.first().find { it.id == biteRecordId }
            if (biteRecord != null) {
                val analysis = BiteAnalysis(
                    biteName = biteRecord.biteName,
                    severity = biteRecord.severity,
                    severityColor = when (biteRecord.severity) {
                        "High" -> SeverityHigh
                        "Moderate" -> SeverityModerate
                        "Low" -> SeverityLow
                        else -> SeverityModerate
                    },
                    expectedDuration = biteRecord.expectedDuration,
                    characteristics = biteRecord.characteristics,
                    treatments = biteRecord.treatments,
                    timeline = biteRecord.timeline
                )

                _uiState.update {
                    it.copy(
                        biteAnalysis = analysis,
                        isAnalyzing = false,
                        existingBiteRecord = biteRecord,
                        imageUri = biteRecord.imageUri.toUri(),
                        biteRecordId = biteRecord.id,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(saveError = "Existing bite record not found")
                }
            }
        }
    }

    private fun handleInsectDetection(insectType: String) {
        // Check if no bites were detected
        if (insectType == "no_bites") {
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    showInsectDetection = false,
                    isLoadingDetails = false,
                    loadingMessage = "",
                    biteAnalysis = null,
                    detectedInsectType = "no_bites"
                )
            }
            return
        }
        
        // Create partial analysis with just the insect type
        val partialAnalysis = BiteAnalysis(
            biteName = formatInsectName(insectType),
            severity = "",
            severityColor = SeverityModerate,
            expectedDuration = "",
            characteristics = emptyList(),
            treatments = emptyList(),
            timeline = emptyMap()
        )
        
        _uiState.update {
            it.copy(
                biteAnalysis = partialAnalysis,
                detectedInsectType = insectType,
                isAnalyzing = false, // Stop showing loading screen
                isLoadingDetails = true, // Show shimmer for details
                showInsectDetection = false // Hide detection UI
            )
        }
    }
    
    /**
     * Handle streaming updates from the LLM
     * Updates UI progressively as data becomes available
     */
    private fun handleStreamingUpdate(partial: PartialBiteAnalysis) {
        // Always update with whatever data we have so far
        // The UI will show shimmer for empty fields
        val existingAnalysis = _uiState.value.biteAnalysis
        
        val analysis = BiteAnalysis(
            biteName = formatInsectName(partial.biteName),
            severity = partial.severity ?: existingAnalysis?.severity ?: "",
            severityColor = when (partial.severity ?: existingAnalysis?.severity) {
                "High" -> SeverityHigh
                "Low" -> SeverityLow
                else -> SeverityModerate
            },
            expectedDuration = partial.expectedDuration ?: existingAnalysis?.expectedDuration ?: "",
            characteristics = if (partial.characteristics.isNotEmpty()) {
                partial.characteristics.toList()
            } else {
                existingAnalysis?.characteristics ?: emptyList()
            },
            treatments = if (partial.treatments.isNotEmpty()) {
                partial.treatments.toList()
            } else {
                existingAnalysis?.treatments ?: emptyList()
            },
            timeline = if (partial.timeline.isNotEmpty()) {
                partial.timeline.toMap()
            } else {
                existingAnalysis?.timeline ?: emptyMap()
            }
        )
        
        _uiState.update { state ->
            state.copy(
                biteAnalysis = analysis,
                // Only stop loading details when streaming is complete
                isLoadingDetails = !partial.isComplete,
                // Update section completion flags
                isCharacteristicsComplete = partial.isCharacteristicsComplete,
                isTreatmentsComplete = partial.isTreatmentsComplete,
                isTimelineComplete = partial.isTimelineComplete
            )
        }
        
        // Save when streaming is complete
        if (partial.isComplete) {
            _uiState.value.imageUri?.let { uri ->
                saveBiteAnalysis(uri, analysis)
            }
        }
    }
    
    private suspend fun analyzeNewImage(imageUri: Uri, mode: AnalysisMode) {
        // Check if model is initialized
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                loadingMessage = when (mode) {
                    AnalysisMode.NETWORK -> "Analyzing your photo with cloud AI..."
                    AnalysisMode.LOCAL_LLM -> "Analyzing with on-device Gemma 3n..."
                }
            )
        }

        // Get the appropriate analyzer
        val analyzer = biteAnalyzerFactory.getAnalyzer(mode)
        
        // Set up callbacks for both network and local analyzers
        when (analyzer) {
            is NetworkBiteAnalyzer -> {
                analyzer.onInsectDetected = { insectType ->
                    handleInsectDetection(insectType)
                }
                analyzer.onStreamingUpdate = { partial ->
                    viewModelScope.launch(Dispatchers.Main) {
                        handleStreamingUpdate(partial)
                    }
                }
            }
            is LocalBiteAnalyzer -> {
                analyzer.onInsectDetected = { insectType ->
                    handleInsectDetection(insectType)
                }
                analyzer.onStreamingUpdate = { partial ->
                    viewModelScope.launch(Dispatchers.Main) {
                        handleStreamingUpdate(partial)
                    }
                }
            }
        }
        
        // Use the analyzer to analyze the image
        val analysisResult = analyzer.analyzeBiteImage(imageUri)

        // Only update state with final result if we're not using streaming
        // (streaming updates are handled by callbacks)
        val hasStreamingCallback = when (analyzer) {
            is NetworkBiteAnalyzer -> analyzer.onStreamingUpdate != null
            is LocalBiteAnalyzer -> analyzer.onStreamingUpdate != null
            else -> false
        }
        
        if (!hasStreamingCallback) {
            // Non-streaming mode: update state with final result
            val biteAnalysis = if (analysisResult != null) {
                BiteAnalysis(
                    biteName = formatInsectName(analysisResult.insectType),
                    severity = analysisResult.severity,
                    severityColor = when (analysisResult.severity) {
                        "High" -> SeverityHigh
                        "Low" -> SeverityLow
                        else -> SeverityModerate
                    },
                    expectedDuration = analysisResult.expectedDuration,
                    characteristics = analysisResult.characteristics,
                    treatments = analysisResult.treatments,
                    timeline = analysisResult.timeline
                )
            } else {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        loadingMessage = "Error analyzing image. Please try again."
                    )
                }
                return
            }

            _uiState.update {
                it.copy(
                    biteAnalysis = biteAnalysis,
                    isAnalyzing = false,
                    showInsectDetection = false,
                    detectedInsectType = null,
                    isLoadingDetails = false // Stop shimmer loading
                )
            }
        } else if (analysisResult == null) {
            // Streaming mode but analysis failed
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    loadingMessage = "Error analyzing image. Please try again.",
                    isLoadingDetails = false
                )
            }
        }

        // Automatically save for new captures
        if (!hasStreamingCallback) {
            val biteAnalysis = _uiState.value.biteAnalysis
            if (biteAnalysis != null) {
                saveBiteAnalysis(imageUri, biteAnalysis)
            }
        }
    }

    private fun saveBiteAnalysis(imageUri: Uri, analysis: BiteAnalysis) {
        // Only save if it's a new capture (not an existing record)
        if (_uiState.value.existingBiteRecord != null) {
            // Log to help debug - in production, use proper logging
            println("ResultViewModel: Skipping save - viewing existing bite record")
            return
        }

        println("ResultViewModel: Saving new bite record")

        viewModelScope.launch {
            _uiState.update { it.copy(saveError = null) }

            try {
                val id = repository.addBiteRecord(
                    imageUri = imageUri.toString(),
                    biteName = analysis.biteName,
                    severity = analysis.severity,
                    expectedDuration = analysis.expectedDuration,
                    characteristics = analysis.characteristics,
                    treatments = analysis.treatments,
                    timeline = analysis.timeline,
                )

                _uiState.update {
                    it.copy(
                        biteRecordId = id
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        saveError = "Failed to save bite record"
                    )
                }
            }
        }
    }
    
    private fun formatInsectName(insectType: String): String {
        return when (insectType.lowercase()) {
            "mosquito", "mosquitos" -> "Mosquito"
            "bed bug", "bed bugs", "bedbug", "bedbugs", "bed_bugs", "bed_bug" -> "Bed Bug"
            "chigger", "chiggers" -> "Chigger"
            "spider", "spiders" -> "Spider"
            "flea", "fleas" -> "Flea"
            "tick", "ticks" -> "Tick"
            "ant", "ants" -> "Ant"
            else -> insectType.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        }
    }
}
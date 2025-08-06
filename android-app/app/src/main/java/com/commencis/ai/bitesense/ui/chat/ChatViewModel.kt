package com.commencis.ai.bitesense.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.commencis.ai.bitesense.ai.MediaPipeModelManager
import com.commencis.ai.bitesense.ai.BiteAnalysisResult
import android.util.Log
import androidx.core.net.toUri
import com.commencis.ai.bitesense.data.BiteHistoryRepository
import com.commencis.ai.bitesense.data.BiteRecord
import com.commencis.ai.bitesense.ui.result.BiteAnalysis
import com.commencis.ai.bitesense.ui.theme.SeverityHigh
import com.commencis.ai.bitesense.ui.theme.SeverityLow
import com.commencis.ai.bitesense.ui.theme.SeverityModerate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val capturedImageUri: Uri? = null,
    val isModelLoading: Boolean = false,
    val modelError: String? = null,
    val biteContext: BiteRecord? = null
)

sealed class ChatUiEvent {
    data class InputTextChanged(val text: String) : ChatUiEvent()
    data object SendMessageClicked : ChatUiEvent()
    data object CloseClicked : ChatUiEvent()
    data class InitializeWithImage(val imageUri: Uri?) : ChatUiEvent()
    data class InitializeWithBiteContext(
        val imageUri: Uri?,
        val biteRecordId: String
    ) : ChatUiEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: BiteHistoryRepository,
    private val modelManager: MediaPipeModelManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Add initial greeting message
        _uiState.update {
            it.copy(
                messages = listOf(
                    Message(
                        text = "Hello! I'm here to help you identify and understand insect bites. You can describe your symptoms or ask me any questions.",
                        isUser = false
                    )
                )
            )
        }
    }

    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.InputTextChanged -> {
                _uiState.update { it.copy(inputText = event.text) }
            }

            ChatUiEvent.SendMessageClicked -> {
                sendMessage()
            }

            ChatUiEvent.CloseClicked -> {
                // Handle close - will be propagated to parent
            }

            is ChatUiEvent.InitializeWithImage -> {
                _uiState.update { it.copy(capturedImageUri = event.imageUri) }
            }
            
            is ChatUiEvent.InitializeWithBiteContext -> {
                initializeWithBiteContext(event.imageUri, event.biteRecordId)
            }
        }
    }

    private fun initializeWithBiteContext(imageUri: Uri?, biteRecordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Load existing record from repository
            val biteAnalysis =
                repository.biteHistory.first().find { it.id == biteRecordId }
            if (biteAnalysis != null) {
                _uiState.update {
                    it.copy(
                        capturedImageUri = biteAnalysis.imageUri.toUri(),
                        biteContext = biteAnalysis,
                        messages = listOf(
                            Message(
                                text = "I've analyzed your insect bite. Based on my analysis, it appears to be a ${biteAnalysis.biteName} with ${biteAnalysis.severity} severity.\n\nFeel free to ask me any questions about your bite, treatment options, or expected recovery time.",
                                isUser = false
                            )
                        )
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        modelError = "Existing bite record not found",
                        messages = listOf(
                            Message(
                                text = "I couldn't find any existing analysis for this bite. Please try analyzing a new image.",
                                isUser = false
                            )
                        )
                    )
                }
            }
        }
    }

    private fun sendMessage() {
        val currentText = _uiState.value.inputText.trim()
        if (currentText.isEmpty()) return

        // Add user message
        _uiState.update { state ->
            state.copy(
                messages = state.messages + Message(text = currentText, isUser = true),
                inputText = "",
                isLoading = true
            )
        }

        // Generate AI response with streaming
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Add initial empty AI message that will be updated with streaming text
                val aiMessageIndex = _uiState.value.messages.size
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + Message(
                            text = "",
                            isUser = false,
                            isStreaming = true
                        )
                    )
                }

                val response = generateAIResponseStreaming(currentText) { partialText ->
                    // Update the AI message with streaming text
                    _uiState.update { state ->
                        val updatedMessages = state.messages.toMutableList()
                        if (aiMessageIndex < updatedMessages.size) {
                            val currentMessage = updatedMessages[aiMessageIndex]
                            updatedMessages[aiMessageIndex] = currentMessage.copy(
                                text = currentMessage.text + partialText,
                                isStreaming = true
                            )
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
                
                // Mark streaming as complete
                _uiState.update { state ->
                    val updatedMessages = state.messages.toMutableList()
                    if (aiMessageIndex < updatedMessages.size) {
                        updatedMessages[aiMessageIndex] = updatedMessages[aiMessageIndex].copy(
                            isStreaming = false
                        )
                    }
                    state.copy(
                        messages = updatedMessages,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate response", e)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + Message(
                            text = "I'm sorry, I encountered an error generating a response. Please try again.",
                            isUser = false
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }
    
    private suspend fun generateAIResponseStreaming(
        userMessage: String,
        onPartialUpdate: (String) -> Unit
    ): String {
        // Build context for the conversation
        val context = buildConversationContext(userMessage)
        val imageUri = _uiState.value.capturedImageUri
        
        // Use MediaPipe to generate response with streaming
        return modelManager.generateChatResponse(
            prompt = context,
            imageUri = imageUri,
            onPartialUpdate = onPartialUpdate
        )
    }
    
    private fun buildConversationContext(userMessage: String): String {
        val biteContext = _uiState.value.biteContext
        val messages = _uiState.value.messages
        
        return buildString {
            appendLine("You are BiteSense AI, an expert assistant for insect bite identification and treatment.")
            
            // Add bite context if available
            if (biteContext != null) {
                appendLine("\nCurrent bite analysis context:")
                appendLine("- Insect type: ${biteContext.biteName}")
                appendLine("- Severity: ${biteContext.severity}")
                appendLine("- Expected duration: ${biteContext.expectedDuration}")
                appendLine("- Characteristics: ${biteContext.characteristics.joinToString(", ")}")
                appendLine("- Recommended treatments: ${biteContext.treatments.joinToString(", ")}")
            }
            
            // Add conversation history (last 5 messages for context)
            appendLine("\nConversation history:")
            messages.takeLast(5).forEach { msg ->
                if (msg.isUser) {
                    appendLine("User: ${msg.text}")
                } else {
                    appendLine("Assistant: ${msg.text}")
                }
            }
            
            appendLine("\nUser: $userMessage")
            appendLine("\nProvide a helpful, accurate response. If discussing the analyzed bite, reference the context provided. Keep responses concise and friendly.")
        }
    }
}
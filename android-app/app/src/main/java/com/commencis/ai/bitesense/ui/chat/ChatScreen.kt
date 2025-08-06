package com.commencis.ai.bitesense.ui.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commencis.ai.bitesense.ui.theme.SurfaceWhite
import com.commencis.ai.bitesense.ui.theme.TertiarySurface
import com.commencis.ai.bitesense.ui.theme.TextPrimary
import com.commencis.ai.bitesense.ui.theme.TextSecondary
import com.commencis.ai.bitesense.ai.BiteAnalysisResult

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    capturedImageUri: Uri? = null,
    biteRecordId: String? = null,
    onClose: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Initialize with context
    LaunchedEffect(capturedImageUri, biteRecordId) {
        if (biteRecordId != null) {
            viewModel.onEvent(ChatUiEvent.InitializeWithBiteContext(capturedImageUri, biteRecordId))
        } else {
            viewModel.onEvent(ChatUiEvent.InitializeWithImage(capturedImageUri))
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TertiarySurface)
            .systemBarsPadding()
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    spotColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chat with AI Assistant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.isModelLoading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Loading AI model...",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            IconButton(
                onClick = {
                    viewModel.onEvent(ChatUiEvent.CloseClicked)
                    onClose()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        }

        // AI Disclaimer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0) // Light amber background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF57C00), // Orange accent
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI responses are for informational purposes only and should not replace professional medical advice.",
                    fontSize = 11.sp,
                    color = Color(0xFF795548), // Brown text
                    lineHeight = 16.sp
                )
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages) { message ->
                MessageCard(message = message)
            }
        }

        // Input field
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.onEvent(ChatUiEvent.InputTextChanged(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        "Type your message...",
                        color = TextSecondary
                    ) 
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = TextPrimary,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = TertiarySurface
                ),
                maxLines = 3
            )

            IconButton(
                onClick = { viewModel.onEvent(ChatUiEvent.SendMessageClicked) },
                enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = if (uiState.inputText.isNotBlank() && !uiState.isLoading) 4.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = TextPrimary.copy(alpha = 0.25f)
                    )
                    .clip(CircleShape)
                    .background(
                        if (uiState.inputText.isNotBlank() && !uiState.isLoading) {
                            TextPrimary
                        } else {
                            Color.LightGray.copy(alpha = 0.3f)
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
        }
    }
}

@Composable
private fun MessageCard(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    ),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                    ambientColor = Color.Black.copy(alpha = 0.05f)
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) TextPrimary else SurfaceWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text.ifEmpty { 
                        if (message.isStreaming && !message.isUser) "..." else "" 
                    },
                    color = if (message.isUser) Color.White else TextPrimary,
                    fontSize = 14.sp
                )
                
                // Show typing indicator for streaming messages
                if (message.isStreaming && !message.isUser && message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
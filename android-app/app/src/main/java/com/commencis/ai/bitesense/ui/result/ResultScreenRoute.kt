package com.commencis.ai.bitesense.ui.result

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.commencis.ai.bitesense.ai.BiteAnalysisResult

@Composable
fun ResultScreenRoute(
    imageUri: Uri,
    biteRecordId: String?,
    analysisMode: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load image only once when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.initializeWithImage(imageUri, biteRecordId, analysisMode)
    }

    ResultScreen(
        onAskQuestion = {
            val recordId = uiState.biteRecordId
            if (recordId != null) {
                onNavigateToChat(recordId)
            }
        },
        onClose = onNavigateBack,
        uiState = uiState,
    )
}
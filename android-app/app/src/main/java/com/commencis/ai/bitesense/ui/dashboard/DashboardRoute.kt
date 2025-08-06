package com.commencis.ai.bitesense.ui.dashboard

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.commencis.ai.bitesense.data.BiteRecord
import com.commencis.ai.bitesense.util.rememberPermissionHandler

@Composable
fun DashboardRoute(
    onNavigateToResult: (Uri, String) -> Unit, // Added analysis mode parameter
    onNavigateToChat: () -> Unit,
    onNavigateToBiteDetail: (BiteRecord) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPhotoUri by viewModel.currentPhotoUri.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                onNavigateToResult(uri, uiState.selectedAnalysisMode.name)
            }
        } else {
            // Photo capture was cancelled
            android.util.Log.d("DashboardRoute", "Photo capture cancelled")
        }
    }

    val cameraPermissionHandler = rememberPermissionHandler(
        permission = Manifest.permission.CAMERA,
        onPermissionGrant = {
            viewModel.onEvent(DashboardUiEvent.LaunchCameraRequested)
        }
    )

    // Observe UI effects
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DashboardUiEffect.LaunchCamera -> {
                    cameraLauncher.launch(effect.uri)
                }
            }
        }
    }

    DashboardScreen(
        onTabSelected = { tab -> viewModel.onEvent(DashboardUiEvent.TabSelected(tab)) },
        onScanClick = {
            if (cameraPermissionHandler.isGranted) {
                viewModel.onEvent(DashboardUiEvent.LaunchCameraRequested)
            } else {
                cameraPermissionHandler.requestPermission()
            }
        },
        onChatClick = onNavigateToChat,
        onBiteClick = onNavigateToBiteDetail
    )
}
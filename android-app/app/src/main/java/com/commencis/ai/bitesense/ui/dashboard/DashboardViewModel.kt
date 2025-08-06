package com.commencis.ai.bitesense.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commencis.ai.bitesense.data.BiteHistoryRepository
import com.commencis.ai.bitesense.data.BiteRecord
import com.commencis.ai.bitesense.ui.components.AnalysisMode
import com.commencis.ai.bitesense.util.CameraHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val recentBites: List<BiteRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAnalysisMode: AnalysisMode = AnalysisMode.NETWORK,
    val selectedTab: Int = 0
)

sealed class DashboardUiEvent {
    data class BiteClicked(val biteRecord: BiteRecord) : DashboardUiEvent()
    data object ScanClicked : DashboardUiEvent()
    data object ChatClicked : DashboardUiEvent()
    data object RefreshRequested : DashboardUiEvent()
    data class AnalysisModeChanged(val mode: AnalysisMode) : DashboardUiEvent()
    data class TabSelected(val tab: Int) : DashboardUiEvent()
    data object LaunchCameraRequested : DashboardUiEvent()
}

sealed class DashboardUiEffect {
    data class LaunchCamera(val uri: Uri) : DashboardUiEffect()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BiteHistoryRepository,
    private val cameraHandler: CameraHandler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _uiEffect = Channel<DashboardUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()
    
    val currentPhotoUri: StateFlow<Uri?> = cameraHandler.currentPhotoUri
    
    init {
        loadRecentBites()
    }
    
    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.BiteClicked -> {
                // Handle bite click - will be propagated to parent
            }
            DashboardUiEvent.ScanClicked -> {
                // Handle scan click - will be propagated to parent
            }
            DashboardUiEvent.ChatClicked -> {
                // Handle chat click - will be propagated to parent
            }
            DashboardUiEvent.RefreshRequested -> {
                loadRecentBites()
            }
            is DashboardUiEvent.AnalysisModeChanged -> {
                _uiState.update { it.copy(selectedAnalysisMode = event.mode) }
            }
            is DashboardUiEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }
            DashboardUiEvent.LaunchCameraRequested -> {
                val uri = cameraHandler.createPhotoUri()
                viewModelScope.launch {
                    _uiEffect.send(DashboardUiEffect.LaunchCamera(uri))
                }
            }
        }
    }
    
    private fun loadRecentBites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                repository.biteHistory.collect { bites ->
                    _uiState.update { 
                        it.copy(
                            recentBites = bites,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load recent bites"
                    )
                }
            }
        }
    }
}
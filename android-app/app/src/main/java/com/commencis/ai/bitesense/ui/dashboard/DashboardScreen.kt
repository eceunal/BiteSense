package com.commencis.ai.bitesense.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commencis.ai.bitesense.R
import com.commencis.ai.bitesense.data.BiteRecord
import com.commencis.ai.bitesense.ui.components.AnalysisModeSelector
import com.commencis.ai.bitesense.ui.components.QuickActionCard
import com.commencis.ai.bitesense.ui.components.RecentBiteCard
import com.commencis.ai.bitesense.ui.theme.BackgroundGray
import com.commencis.ai.bitesense.ui.theme.TertiarySurface
import com.commencis.ai.bitesense.ui.theme.TextPrimary
import com.commencis.ai.bitesense.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    onTabSelected: (Int) -> Unit,
    onScanClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onBiteClick: (BiteRecord) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundGray,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = BackgroundGray,
                            spotColor = BackgroundGray
                        )
                        .background(
                            color = TertiarySurface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(32.dp)
                        ),
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (uiState.selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        selected = uiState.selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            unselectedIconColor = TextPrimary.copy(alpha = 0.6f),
                            selectedTextColor = TextPrimary,
                            unselectedTextColor = TextPrimary.copy(alpha = 0.6f),
                            indicatorColor = BackgroundGray.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (uiState.selectedTab == 1) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                                contentDescription = "Scan"
                            )
                        },
                        label = { Text("Scan") },
                        selected = uiState.selectedTab == 1,
                        onClick = {
                            onTabSelected(1)
                            onScanClick()
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            unselectedIconColor = TextPrimary.copy(alpha = 0.6f),
                            selectedTextColor = TextPrimary,
                            unselectedTextColor = TextPrimary.copy(alpha = 0.6f),
                            indicatorColor = BackgroundGray.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (uiState.selectedTab == 2) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = "Chat"
                            )
                        },
                        label = { Text("Chat") },
                        selected = uiState.selectedTab == 2,
                        onClick = {
                            onTabSelected(2)
                            onChatClick()
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            unselectedIconColor = TextPrimary.copy(alpha = 0.6f),
                            selectedTextColor = TextPrimary,
                            unselectedTextColor = TextPrimary.copy(alpha = 0.6f),
                            indicatorColor = BackgroundGray.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            // Analysis Mode Selector
            AnalysisModeSelector(
                selectedMode = uiState.selectedAnalysisMode,
                onModeSelected = { mode ->
                    viewModel.onEvent(DashboardUiEvent.AnalysisModeChanged(mode))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
            
            // How can I help you? section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How can i help you?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                QuickActionCard(
                    title = "Scan",
                    subtitle = "Take a photo for instant AI identification",
                    imageRes = R.drawable.ic_camera,
                    onClick = {
                        viewModel.onEvent(DashboardUiEvent.ScanClicked)
                        onScanClick()
                    }
                )

                QuickActionCard(
                    title = "Chat",
                    subtitle = "Tell us about your bite symptoms",
                    imageRes = R.drawable.ic_chat,
                    onClick = {
                        viewModel.onEvent(DashboardUiEvent.ChatClicked)
                        onChatClick()
                    }
                )
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Recent Bites section
            if (uiState.recentBites.isNotEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Recent Bites",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    uiState.recentBites.take(3).forEach { biteRecord ->
                        RecentBiteCard(
                            biteRecord = biteRecord,
                            onClick = {
                                viewModel.onEvent(DashboardUiEvent.BiteClicked(biteRecord))
                                onBiteClick(biteRecord)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

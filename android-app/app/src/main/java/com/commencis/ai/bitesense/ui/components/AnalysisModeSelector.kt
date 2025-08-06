package com.commencis.ai.bitesense.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commencis.ai.bitesense.ui.theme.SurfaceWhite
import com.commencis.ai.bitesense.ui.theme.TextPrimary
import com.commencis.ai.bitesense.ui.theme.TextSecondary

enum class AnalysisMode(val displayName: String, val icon: ImageVector) {
    NETWORK("Network", Icons.Default.Cloud),
    LOCAL_LLM("On-device Gemma 3n", Icons.Default.Memory)
}

@Composable
fun AnalysisModeSelector(
    selectedMode: AnalysisMode,
    onModeSelected: (AnalysisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dropdown_rotation"
    )

    Column(modifier = modifier) {
        // Main selector button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (expanded) 8.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode icon with background
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (selectedMode) {
                                    AnalysisMode.NETWORK -> Color(0xFFE3F2FD) // Light blue
                                    AnalysisMode.LOCAL_LLM -> Color(0xFFF3E5F5) // Light purple
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedMode.icon,
                            contentDescription = null,
                            tint = when (selectedMode) {
                                AnalysisMode.NETWORK -> Color(0xFF1976D2) // Blue
                                AnalysisMode.LOCAL_LLM -> Color(0xFF7B1FA2) // Purple
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Analysis Mode",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = selectedMode.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand dropdown",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }
        }

        // Dropdown menu
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    AnalysisMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            mode = mode,
                            isSelected = mode == selectedMode,
                            onClick = {
                                onModeSelected(mode)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownMenuItem(
    mode: AnalysisMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    when (mode) {
                        AnalysisMode.NETWORK -> Color(0xFFE3F2FD).copy(alpha = 0.5f)
                        AnalysisMode.LOCAL_LLM -> Color(0xFFF3E5F5).copy(alpha = 0.5f)
                    }
                } else Color.Transparent
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = when (mode) {
                    AnalysisMode.NETWORK -> Color(0xFF1976D2)
                    AnalysisMode.LOCAL_LLM -> Color(0xFF7B1FA2)
                },
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = when (mode) {
                        AnalysisMode.NETWORK -> "Cloud inference (MediaPipe doesn't support finetuned Gemma3n models)"
                        AnalysisMode.LOCAL_LLM -> "Private Gemma 3n processing"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (mode) {
                                AnalysisMode.NETWORK -> Color(0xFF1976D2)
                                AnalysisMode.LOCAL_LLM -> Color(0xFF7B1FA2)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
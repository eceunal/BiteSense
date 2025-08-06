package com.commencis.ai.bitesense.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commencis.ai.bitesense.R
import com.commencis.ai.bitesense.ui.components.ShimmerBox
import com.commencis.ai.bitesense.ui.theme.SurfaceWhite
import com.commencis.ai.bitesense.ui.theme.TertiarySurface
import com.commencis.ai.bitesense.ui.theme.TextPrimary
import com.commencis.ai.bitesense.ui.theme.TextSecondary

private fun getBugDrawableResource(bugType: String): Int {
    return when (bugType.lowercase()) {
        "mosquito", "mosquitos" -> R.drawable.ic_mosquito
        "bed bug", "bed bugs", "bedbug", "bedbugs", "bed_bugs", "bed_bug" -> R.drawable.ic_bedbug
        "chigger", "chiggers" -> R.drawable.ic_chigger
        "spider", "spiders" -> R.drawable.ic_spider
        "flea", "fleas" -> R.drawable.ic_flea
        "tick", "ticks" -> R.drawable.ic_tick
        "ant", "ants" -> R.drawable.ic_ants
        else -> R.drawable.ic_mosquito // Default fallback
    }
}

@Composable
fun ResultScreen(
    onAskQuestion: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: ResultUiState
) {
    // Show loading state if analyzing
    if (uiState.isAnalyzing) {
        LoadingScreen(
            modifier = modifier,
            message = uiState.loadingMessage,
            detectedInsectType = uiState.detectedInsectType,
            showInsectDetection = uiState.showInsectDetection
        )
        return
    }

    // Show no bites detected screen
    if (uiState.detectedInsectType == "no_bites") {
        NoBitesDetectedScreen(
            onClose = onClose,
            onAskQuestion = onAskQuestion,
            modifier = modifier
        )
        return
    }

    val analysis = uiState.biteAnalysis ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TertiarySurface)
            .systemBarsPadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Results",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(
                onClick = {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Bug Image
            Image(
                painter = painterResource(id = getBugDrawableResource(analysis.biteName)),
                contentDescription = "${analysis.biteName} illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp),
                contentScale = ContentScale.FillWidth,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 220.dp)
                    .background(
                        SurfaceWhite,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(16.dp)
            ) {
                // Bite identification card
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = analysis.biteName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (analysis.severity.isEmpty()) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(32.dp),
                            cornerRadius = 20.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = analysis.severityColor.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = analysis.severity,
                                color = TextPrimary.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expected Duration
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Expected Duration",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (analysis.expectedDuration.isEmpty()) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(20.dp)
                        )
                    } else {
                        Text(
                            text = analysis.expectedDuration,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bite Characteristics
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Bite Characteristics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (analysis.characteristics.isEmpty()) {
                        // Show shimmer when no characteristics yet
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(16.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(16.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(16.dp)
                            )
                        }
                    } else {
                        // Show characteristics
                        analysis.characteristics.forEach { characteristic ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("• ", color = TextSecondary)
                                Text(
                                    text = characteristic,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        // Show shimmer at the end if section is not complete
                        if (!uiState.isCharacteristicsComplete) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recommended Treatment
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Recommended Treatment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (analysis.treatments.isEmpty()) {
                        // Show shimmer when no treatments yet
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(16.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(16.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(16.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(16.dp)
                            )
                        }
                    } else {
                        // Show treatments
                        analysis.treatments.forEach { treatment ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("• ", color = TextSecondary)
                                Text(
                                    text = treatment,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        // Show shimmer at the end if section is not complete
                        if (!uiState.isTreatmentsComplete) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progression Timeline
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Progression Timeline",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (analysis.timeline.isEmpty()) {
                        // Show shimmer when no timeline yet
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(3) {
                                Column {
                                    ShimmerBox(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ShimmerBox(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Show timeline entries
                        analysis.timeline.forEach { (day, description) ->
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row {
                                    Text("• ", color = TextSecondary)
                                    Text(
                                        text = description,
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        // Show shimmer at the end if section is not complete
                        if (!uiState.isTimelineComplete) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ShimmerBox(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // AI Disclaimer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0) // Light amber background
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF57C00), // Orange accent
                            modifier = Modifier.size(20.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Important Notice",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100) // Dark orange
                            )
                            Text(
                                text = "This analysis is generated by AI and is for informational purposes only. It should not replace professional medical advice. Please consult a healthcare provider for proper diagnosis and treatment, especially if symptoms persist or worsen.",
                                fontSize = 12.sp,
                                color = Color(0xFF795548), // Brown text
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ask Question Button
                Button(
                    onClick = {
                        onAskQuestion()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        contentColor = SurfaceWhite
                    )
                ) {
                    Text(
                        text = "Ask Question",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoBitesDetectedScreen(
    onClose: () -> Unit,
    onAskQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TertiarySurface)
            .systemBarsPadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Analysis Complete",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Green
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main message
            Text(
                text = "No Insect Bites Detected",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Explanation
            Text(
                text = "Good news! Our AI analysis did not detect any insect bites in the provided image.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9) // Light green background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What this means:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32) // Dark green
                    )
                    Text(
                        text = "• The mark or irritation may not be from an insect bite",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• It could be a skin condition, allergic reaction, or other cause",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Consider consulting a healthcare provider if symptoms persist",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Ask Question button
            Button(
                onClick = onAskQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = SurfaceWhite
                )
            ) {
                Text(
                    text = "Ask a Question",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Try Again button (outline style)
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = TextPrimary
                )
            ) {
                Text(
                    text = "Try Another Image",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp
                )
            }
        }
    }
}
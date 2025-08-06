package com.commencis.ai.bitesense.ui.result

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commencis.ai.bitesense.R
import com.commencis.ai.bitesense.ui.theme.TextPrimary
import com.commencis.ai.bitesense.ui.theme.TextSecondary

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Analyzing...",
    detectedInsectType: String? = null,
    showInsectDetection: Boolean = false
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)), // Light gray background matching the design
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Animated content based on state
            AnimatedContent(
                targetState = showInsectDetection to detectedInsectType,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.8f, animationSpec = tween(300))) togetherWith
                            (fadeOut(animationSpec = tween(200)) +
                                    scaleOut(targetScale = 0.8f, animationSpec = tween(200)))
                },
                label = "loading_animation"
            ) { (showDetection, insectType) ->
                if (showDetection && insectType != null) {
                    InsectDetectionContent(insectType, message)
                } else {
                    InitialLoadingContent(message)
                }
            }
        }
    }
}

@Composable
private fun InsectDetectionContent(insectType: String, message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Insect image with white background
        Image(
            painter = painterResource(id = getInsectImageResource(insectType)),
            contentDescription = insectType,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .blur(12.dp),
            contentScale = ContentScale.FillWidth
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Message from ViewModel
        Text(
            text = message,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round,
            color = TextPrimary
        )
    }
}

@Composable
private fun InitialLoadingContent(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Our AI is carefully examining your photo to provide accurate identification and treatment recommendations.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// Helper function to get insect image resource
private fun getInsectImageResource(insectType: String): Int {
    return when (insectType.lowercase()) {
        "mosquitos" -> R.drawable.ic_mosquito
        "bed bugs" -> R.drawable.ic_bedbug
        "chiggers" -> R.drawable.ic_chigger
        "spider" -> R.drawable.ic_spider
        "fleas" -> R.drawable.ic_flea
        "tick" -> R.drawable.ic_tick
        else -> R.drawable.ic_mosquito
    }
}
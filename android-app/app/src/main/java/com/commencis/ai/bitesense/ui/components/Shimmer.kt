/*
 * Copyright 2025 Commencis. All Rights Reserved.
 *
 * Save to the extent permitted by law, you may not use, copy, modify,
 * distribute or create derivative works of this material or any part
 * of it without the prior written consent of Commencis.
 * Any reproduction of this material must contain this notice.
 */

package com.commencis.ai.bitesense.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.commencis.ai.bitesense.ui.theme.BiteSenseAITheme
import kotlinx.coroutines.launch

val shimmerColorPrimary: Color
    @Composable get() = Color(0xFFE0E0E0) // Light gray color for primary shimmer

val shimmerColorSecondary: Color
    @Composable get() = Color(0xFFF5F5F5) // Lighter gray for secondary shimmer

@Suppress("ModifierReused")
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    isCentered: Boolean = false,
    cornerRadius: Dp = 4.dp,
    color: Color = shimmerColorPrimary,
) {
    val cornerShape = RoundedCornerShape(cornerRadius)
    if (isCentered) {
        CenteredBox(widthFraction, cornerShape, color, modifier)
        return
    }
    Box(
        modifier = modifier
            .fillMaxWidth(fraction = widthFraction)
            .clip(cornerShape)
            .shimmerEffect(color, shimmerColorSecondary) // Secondary color for background
    )
}

@Composable
fun ShimmerBoxRow(
    modifier: Modifier = Modifier,
    weights: List<Float>,
    cornerRadius: Dp = 4.dp,
    color: Color = shimmerColorPrimary,
) {
    val roundedCornerShape = RoundedCornerShape(cornerRadius)
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        weights.forEachIndexed { index, weight ->
            val isOdd = index.rem(2) == 1
            if (isOdd) {
                Box(
                    modifier = Modifier.weight(weight),
                )
                return@forEachIndexed
            }
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(roundedCornerShape)
                    .shimmerEffect(color, shimmerColorSecondary),
            )
        }
    }
}

@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    color: Color = shimmerColorPrimary,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .shimmerEffect(color, shimmerColorSecondary)
    )
}

@Composable
fun ShimmerPageIndicator(
    modifier: Modifier = Modifier,
    count: Int = 2,
    color: Color = shimmerColorPrimary,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        repeat(count) {
            ShimmerBox(
                modifier = Modifier.size(width = 18.dp, height = 4.dp),
                color = color,
                cornerRadius = 14.dp,
            )
        }
    }
}

@Composable
private fun CenteredBox(
    widthFraction: Float,
    cornerShape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        val sideWeight = Modifier.weight((1 - widthFraction).div(2f))
        Box(modifier = sideWeight)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(widthFraction)
                .clip(cornerShape)
                .shimmerEffect(color, Color(0xFFF5F5F5)),
        )
        Box(modifier = sideWeight)
    }
}

fun Modifier.shimmerEffect(
    primaryColor: Color,
    secondaryColor: Color
): Modifier {
    return this then ShimmerEffectModifierNodeElement(primaryColor, secondaryColor)
}

private data class ShimmerEffectModifierNodeElement(
    val primaryColor: Color,
    val secondaryColor: Color
) : ModifierNodeElement<ShimmerEffectModifierNode>() {
    override fun create() = ShimmerEffectModifierNode(primaryColor, secondaryColor)
    override fun update(node: ShimmerEffectModifierNode) {
        node.primaryColor = primaryColor
        node.secondaryColor = secondaryColor
    }

    override fun InspectorInfo.inspectableProperties() {
        properties["primaryColor"] = primaryColor
        properties["secondaryColor"] = secondaryColor
    }
}

private class ShimmerEffectModifierNode(
    var primaryColor: Color,
    var secondaryColor: Color
) : Modifier.Node(),
    DrawModifierNode {
    private var sizeState by mutableStateOf(IntSize.Zero)
    private val startOffsetX = Animatable(initialValue = 0f)

    override fun onAttach() {
        updateShimmerEffect()
    }

    override fun ContentDrawScope.draw() {
        val colors = listOf(
            primaryColor,
            secondaryColor,
            primaryColor,
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = colors,
                startX = startOffsetX.value - sizeState.width,
                endX = startOffsetX.value,
            )
        )
        drawContent()
        sizeState = size.toIntSize()
    }

    private fun updateShimmerEffect() {
        coroutineScope.launch {
            startOffsetX.animateTo(
                sizeState.width.toFloat() * 2,
                infiniteRepeatable(animation = tween(durationMillis = 1500))
            )
        }
    }
}

@Preview
@Composable
private fun ShimmerPreview() {
    Column {
        BiteSenseAITheme {
            Column(
                modifier = Modifier
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp),
            ) {
                ShimmerBox(
                    modifier = Modifier.height(16.dp),
                    widthFraction = 0.35f,
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                ShimmerBox(
                    modifier = Modifier.height(16.dp),
                    widthFraction = 0.35f,
                    isCentered = true,
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                ShimmerCircle(
                    modifier = Modifier.size(16.dp),
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                ShimmerBoxRow(
                    modifier = Modifier.height(20.dp),
                    weights = listOf(0.2f, 0.1f, 0.3f, 0.2f, 0.1f, 0.1f),
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                ShimmerPageIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

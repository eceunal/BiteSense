package com.commencis.ai.bitesense.ai

import android.net.Uri

/**
 * Interface for analyzing insect bites from images.
 * Implementations can use either local LLM or network-based analysis.
 */
interface BiteAnalyzer {
    /**
     * Analyzes an insect bite image and returns detailed information.
     * 
     * @param imageUri The URI of the image to analyze
     * @return BiteAnalysisResult containing insect type, severity, treatments, etc.
     *         Returns null if analysis fails.
     */
    suspend fun analyzeBiteImage(imageUri: Uri): BiteAnalysisResult?
}
package com.commencis.ai.bitesense.ai

import com.commencis.ai.bitesense.ui.components.AnalysisMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory class to provide the appropriate BiteAnalyzer based on the selected analysis mode.
 */
@Singleton
class BiteAnalyzerFactory @Inject constructor(
    private val localBiteAnalyzer: LocalBiteAnalyzer,
    private val networkBiteAnalyzer: NetworkBiteAnalyzer
) {
    /**
     * Returns the appropriate analyzer based on the selected mode
     */
    fun getAnalyzer(mode: AnalysisMode): BiteAnalyzer {
        return when (mode) {
            AnalysisMode.LOCAL_LLM -> localBiteAnalyzer
            AnalysisMode.NETWORK -> networkBiteAnalyzer
        }
    }
}
package com.commencis.ai.bitesense.ai

import android.util.Log

/**
 * Data class for partial bite analysis results during streaming.
 * Fields are nullable/mutable to allow progressive updates as JSON streams in.
 */
data class PartialBiteAnalysis(
    val biteName: String, // We know this from the start
    var severity: String? = null,
    var expectedDuration: String? = null,
    val characteristics: MutableList<String> = mutableListOf(),
    val treatments: MutableList<String> = mutableListOf(),
    val timeline: MutableMap<String, String> = mutableMapOf(),
    var isComplete: Boolean = false,
    // Track completion status for each section
    var isCharacteristicsComplete: Boolean = false,
    var isTreatmentsComplete: Boolean = false,
    var isTimelineComplete: Boolean = false
) {

    /**
     * Create a copy with updated fields (for immutable state updates)
     */
    fun copy(): PartialBiteAnalysis {
        return PartialBiteAnalysis(
            biteName = biteName,
            severity = severity,
            expectedDuration = expectedDuration,
            characteristics = characteristics.toMutableList(),
            treatments = treatments.toMutableList(),
            timeline = timeline.toMutableMap(),
            isComplete = isComplete,
            isCharacteristicsComplete = isCharacteristicsComplete,
            isTreatmentsComplete = isTreatmentsComplete,
            isTimelineComplete = isTimelineComplete
        )
    }
}

/**
 * Parser for streaming JSON responses from the LLM.
 * Designed specifically for our strict JSON schema.
 */
class StreamingBiteAnalysisParser(
    private val insectType: String
) {
    companion object {
        private const val TAG = "StreamingParser"
    }
    
    private val buffer = StringBuilder()
    private val partial = PartialBiteAnalysis(biteName = insectType)
    
    // Track what we've already parsed to avoid re-parsing
    private var hasParsedSeverity = false
    private var hasParsedDuration = false
    private var lastCharacteristicCount = 0
    private var lastTreatmentCount = 0
    private var lastTimelineCount = 0
    
    /**
     * Parse a new chunk of streaming response.
     * Returns true if any new data was parsed.
     */
    fun parseChunk(chunk: String): Boolean {
        buffer.append(chunk)
        val content = buffer.toString()
        
        var hasUpdates = false
        
        // Parse fields in order of importance for UI
        if (!hasParsedSeverity && parseSeverity(content)) {
            hasUpdates = true
            hasParsedSeverity = true
        }
        
        if (!hasParsedDuration && parseExpectedDuration(content)) {
            hasUpdates = true
            hasParsedDuration = true
        }
        
        // Parse arrays - these can have multiple items streaming in
        if (parseCharacteristics(content)) hasUpdates = true
        if (parseTreatments(content)) hasUpdates = true
        if (parseTimeline(content)) hasUpdates = true
        
        // Check if JSON is complete
        if (content.contains("}") && isJsonComplete(content)) {
            partial.isComplete = true
            hasUpdates = true
        }
        
        return hasUpdates
    }
    
    /**
     * Get current partial analysis
     */
    fun getPartialAnalysis(): PartialBiteAnalysis = partial.copy()
    
    private fun parseSeverity(content: String): Boolean {
        val match = """"severity"\s*:\s*"([^"]+)"""".toRegex().find(content)
        return match?.let {
            partial.severity = it.groupValues[1]
            Log.d(TAG, "Parsed severity: ${partial.severity}")
            true
        } ?: false
    }
    
    private fun parseExpectedDuration(content: String): Boolean {
        val match = """"expectedDuration"\s*:\s*"([^"]+)"""".toRegex().find(content)
        return match?.let {
            partial.expectedDuration = it.groupValues[1]
            Log.d(TAG, "Parsed duration: ${partial.expectedDuration}")
            true
        } ?: false
    }
    
    private fun parseCharacteristics(content: String): Boolean {
        // Look for the characteristics array
        val arrayMatch = """"characteristics"\s*:\s*\[(.*?)(?:\]|$)""".toRegex(RegexOption.DOT_MATCHES_ALL).find(content)
        return arrayMatch?.let { match ->
            val arrayContent = match.groupValues[1]
            val items = """"([^"]+)"""".toRegex().findAll(arrayContent).toList()
            
            var hasUpdates = false
            
            if (items.size > lastCharacteristicCount) {
                // New items found
                items.drop(lastCharacteristicCount).forEach { item ->
                    partial.characteristics.add(item.groupValues[1])
                    Log.d(TAG, "Added characteristic: ${item.groupValues[1]}")
                }
                lastCharacteristicCount = items.size
                hasUpdates = true
            }
            
            // Check if array is complete (has closing bracket)
            if (!partial.isCharacteristicsComplete && content.contains(""""characteristics"\s*:\s*\[[^\]]*\]""".toRegex())) {
                partial.isCharacteristicsComplete = true
                Log.d(TAG, "Characteristics array complete")
                hasUpdates = true
            }
            
            hasUpdates
        } ?: false
    }
    
    private fun parseTreatments(content: String): Boolean {
        val arrayMatch = """"treatments"\s*:\s*\[(.*?)(?:\]|$)""".toRegex(RegexOption.DOT_MATCHES_ALL).find(content)
        return arrayMatch?.let { match ->
            val arrayContent = match.groupValues[1]
            val items = """"([^"]+)"""".toRegex().findAll(arrayContent).toList()
            
            var hasUpdates = false
            
            if (items.size > lastTreatmentCount) {
                items.drop(lastTreatmentCount).forEach { item ->
                    partial.treatments.add(item.groupValues[1])
                    Log.d(TAG, "Added treatment: ${item.groupValues[1]}")
                }
                lastTreatmentCount = items.size
                hasUpdates = true
            }
            
            // Check if array is complete (has closing bracket)
            if (!partial.isTreatmentsComplete && content.contains(""""treatments"\s*:\s*\[[^\]]*\]""".toRegex())) {
                partial.isTreatmentsComplete = true
                Log.d(TAG, "Treatments array complete")
                hasUpdates = true
            }
            
            hasUpdates
        } ?: false
    }
    
    private fun parseTimeline(content: String): Boolean {
        // Timeline is more complex as it's a nested object
        val timelineMatch = """"timeline"\s*:\s*\{(.*?)(?:\}|$)""".toRegex(RegexOption.DOT_MATCHES_ALL).find(content)
        return timelineMatch?.let { match ->
            val timelineContent = match.groupValues[1]
            val entries = """"([^"]+)"\s*:\s*"([^"]+)"""".toRegex().findAll(timelineContent).toList()
            
            var hasUpdates = false
            
            if (entries.size > lastTimelineCount) {
                entries.drop(lastTimelineCount).forEach { entry ->
                    val day = entry.groupValues[1]
                    val description = entry.groupValues[2]
                    partial.timeline[day] = description
                    Log.d(TAG, "Added timeline: $day -> $description")
                }
                lastTimelineCount = entries.size
                hasUpdates = true
            }
            
            // Check if object is complete (has closing brace after timeline)
            if (!partial.isTimelineComplete && content.contains(""""timeline"\s*:\s*\{[^}]*\}""".toRegex())) {
                partial.isTimelineComplete = true
                Log.d(TAG, "Timeline object complete")
                hasUpdates = true
            }
            
            hasUpdates
        } ?: false
    }
    
    private fun isJsonComplete(content: String): Boolean {
        // Simple check: count braces
        val openBraces = content.count { it == '{' }
        val closeBraces = content.count { it == '}' }
        return openBraces > 0 && openBraces == closeBraces
    }
}
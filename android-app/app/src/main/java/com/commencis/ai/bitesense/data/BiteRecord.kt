package com.commencis.ai.bitesense.data

import kotlinx.serialization.Serializable

@Serializable
data class BiteRecord(
    val id: String,
    val imageUri: String,
    val biteName: String,
    val severity: String,
    val timestamp: Long,
    val expectedDuration: String,
    val characteristics: List<String>,
    val treatments: List<String>,
    val timeline: Map<String, String>,
)
package com.commencis.ai.bitesense.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization
 */

@Serializable
object DashboardRoute

@Serializable
data class ResultRoute(
    val imageUri: String,
    val biteRecordId: String? = null,
    val analysisMode: String = "NETWORK" // Default to network mode
)

@Serializable
data class ChatRoute(
    val imageUri: String? = null,
    val biteRecordId: String? = null  // Reference to bite record instead of passing all data
)
package com.commencis.ai.bitesense.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class BiteHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val BITE_HISTORY_KEY = stringPreferencesKey("bite_history")
    }

    val biteHistory: Flow<List<BiteRecord>> = dataStore.data
        .map { preferences ->
            val historyJson = preferences[BITE_HISTORY_KEY] ?: "[]"
            try {
                json.decodeFromString<List<BiteRecord>>(historyJson)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun addBiteRecord(
        imageUri: String,
        biteName: String,
        severity: String,
        expectedDuration: String,
        characteristics: List<String>,
        treatments: List<String>,
        timeline: Map<String, String>
    ): String {
        val id = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            val currentHistoryJson = preferences[BITE_HISTORY_KEY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<BiteRecord>>(currentHistoryJson)
            } catch (e: Exception) {
                emptyList()
            }

            val newRecord = BiteRecord(
                id = id,
                imageUri = imageUri,
                biteName = biteName,
                severity = severity,
                timestamp = System.currentTimeMillis(),
                expectedDuration = expectedDuration,
                characteristics = characteristics,
                treatments = treatments,
                timeline = timeline
            )

            // Add new record at the beginning and limit to 10 recent bites
            val updatedHistory = listOf(newRecord) + currentHistory.take(9)
            preferences[BITE_HISTORY_KEY] = json.encodeToString(updatedHistory)
        }

        return id
    }

    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences[BITE_HISTORY_KEY] = "[]"
        }
    }
}
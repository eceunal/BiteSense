package com.commencis.ai.bitesense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.commencis.ai.bitesense.navigation.BiteSenseNavHost
import com.commencis.ai.bitesense.ui.theme.BiteSenseAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiteSenseAITheme {
                val navController = rememberNavController()
                BiteSenseNavHost(navController = navController)
            }
        }
    }
}

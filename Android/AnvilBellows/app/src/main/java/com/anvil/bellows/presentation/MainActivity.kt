package com.anvil.bellows.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anvil.bellows.presentation.navigation.AppNavHost
import com.anvil.bellows.presentation.theme.AnvilBellowsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnvilBellowsTheme {
                AppNavHost()
            }
        }
    }
}

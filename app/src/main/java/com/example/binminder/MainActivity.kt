package com.example.binminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.ui.navigation.MainScreen
import com.example.binminder.ui.theme.BinMinderTheme

/**
 * Main activity for the BinMinder app.
 * 
 * Sets up edge to edge display, initialises application dependency container,
 * and hosts the root Jetpack Compose navigation UI. Right proper!
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as BinMinderApplication).container
        val repository = appContainer.binRepository

        setContent {
            val themeMode by repository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            BinMinderTheme(darkTheme = darkTheme) {
                MainScreen(
                    appContainer = appContainer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

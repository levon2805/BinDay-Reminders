package com.example.binminder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        try {
            enableEdgeToEdge()
        } catch (exception: Exception) {
            Log.e("BinDay", "Error enabling edge to edge in MainActivity", exception)
        }

        val appContainer = try {
            (application as BinMinderApplication).container
        } catch (exception: Exception) {
            Log.e("BinDay", "Error retrieving AppContainer in MainActivity", exception)
            throw exception
        }

        val repository = appContainer.binRepository

        setContent {
            val themeMode by repository.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val systemInDark = isSystemInDarkTheme()
            val darkTheme = try {
                when (themeMode) {
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                    AppThemeMode.SYSTEM -> systemInDark
                }
            } catch (exception: Exception) {
                Log.e("BinDay", "Error determining theme mode in MainActivity", exception)
                false
            }

            BinMinderTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        appContainer = appContainer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

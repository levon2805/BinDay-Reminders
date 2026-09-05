package com.example.binminder.data.model

/**
 * Represents the theme preference options for the application appearance.
 * 
 * Allows switching between light, dark, or matching the system default.
 */
enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

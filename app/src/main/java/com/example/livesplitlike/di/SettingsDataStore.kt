package com.example.livesplitlike.di

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "app_settings"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object SettingsKeys {
    val DARK_THEME = booleanPreferencesKey("dark_theme")
}

class SettingsDataStore(private val context: Context) {

    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SettingsKeys.DARK_THEME] ?: true } // default: dark (según tu Theme actual)

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.DARK_THEME] = enabled
        }
    }
}

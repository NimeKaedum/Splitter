
package com.example.livesplitlike.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings_prefs")

object ButtonMappingStore {
    private val KEY_XBOX_BUTTON = intPreferencesKey("xbox_button_keycode")

    suspend fun saveMapping(context: Context, keyCode: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_XBOX_BUTTON] = keyCode
        }
    }

    suspend fun clearMapping(context: Context) {
        context.dataStore.edit { prefs -> prefs.remove(KEY_XBOX_BUTTON) }
    }

    // devuelve null si no hay mapping
    suspend fun getMapping(context: Context): Int? {
        val prefsValue = context.dataStore.data.map { it[KEY_XBOX_BUTTON] }.first()
        return prefsValue
    }

    // flujo para observar cambios (útil si quieres un Flow en la VM)
    fun observeMapping(context: Context) = context.dataStore.data.map { it[KEY_XBOX_BUTTON] }
}

package io.github.rytixz.tbtbeep

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import io.github.rytixz.tbtbeep.ui.dataStore

private val settingsKey = stringPreferencesKey("settings_v1")

class TbtSettingsService(
    private val context: Context,
) {
    val settings: Flow<TbtSettings> = context.dataStore.data
        .map { preferences ->
            try {
                jsonWithUnknownKeys.decodeFromString<TbtSettings>(
                    preferences[settingsKey] ?: TbtSettings.defaultSettings,
                )
            } catch (error: Throwable) {
                Log.e(KarooTbtExtension.TAG, "Failed to read preferences", error)
                TbtSettings()
            }
        }
        .distinctUntilChanged()

    suspend fun save(settings: TbtSettings) {
        context.dataStore.edit { preferences ->
            preferences[settingsKey] = jsonWithUnknownKeys.encodeToString(settings)
        }
    }
}

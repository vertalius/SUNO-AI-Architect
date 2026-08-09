package com.vibyproduction.sunoaiarchitect.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibyproduction.sunoaiarchitect.domain.AppSettings
import com.vibyproduction.sunoaiarchitect.domain.GeminiModel
import com.vibyproduction.sunoaiarchitect.domain.SunoVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "suno_architect_settings")

/**
 * Settings repository for SUNO AI Architect
 * Version: 0.1 alpha | Developer: ViBy Production | Author: Vitalii Bychkov
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val MODEL = stringPreferencesKey("gemini_model")
        val SUNO_VERSION = stringPreferencesKey("suno_version")
        val MAX_MODE = booleanPreferencesKey("max_mode_default")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            geminiApiKey = prefs[Keys.API_KEY] ?: "",
            selectedModel = GeminiModel.entries.find { it.name == prefs[Keys.MODEL] } ?: GeminiModel.PRO,
            sunoVersion = SunoVersion.entries.find { it.name == prefs[Keys.SUNO_VERSION] } ?: SunoVersion.V5,
            maxModeDefault = prefs[Keys.MAX_MODE] ?: true
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = settings.geminiApiKey
            prefs[Keys.MODEL] = settings.selectedModel.name
            prefs[Keys.SUNO_VERSION] = settings.sunoVersion.name
            prefs[Keys.MAX_MODE] = settings.maxModeDefault
        }
    }
}

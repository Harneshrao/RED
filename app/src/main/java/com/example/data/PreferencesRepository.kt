package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.partlyDataStore by preferencesDataStore(name = "partly_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    val onboardingCompleted: Flow<Boolean> = context.partlyDataStore.data
        .map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    val themePreference: Flow<ThemePreference> = context.partlyDataStore.data
        .map { prefs -> ThemePreference.fromRaw(prefs[Keys.THEME_PREFERENCE]) }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.partlyDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.partlyDataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = theme.name
        }
    }
}

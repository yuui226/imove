package io.github.imove.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TARGET_DIRECTORY = stringPreferencesKey("target_directory")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_MODE = stringPreferencesKey("dark_mode")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            targetDirectory = prefs[Keys.TARGET_DIRECTORY] ?: "",
            gridColumns = prefs[Keys.GRID_COLUMNS] ?: 3,
            language = prefs[Keys.LANGUAGE] ?: if (java.util.Locale.getDefault().language.startsWith("zh")) "zh" else "en",
            darkMode = prefs[Keys.DARK_MODE] ?: "system"
        )
    }

    suspend fun updateTargetDirectory(path: String) {
        context.dataStore.edit { it[Keys.TARGET_DIRECTORY] = path }
    }

    suspend fun updateGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = columns }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun updateDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }
}

package io.github.imove.domain.repository

import io.github.imove.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun updateTargetDirectory(path: String)
    suspend fun updateGridColumns(columns: Int)
    suspend fun updateLanguage(language: String)
    suspend fun updateDarkMode(enabled: Boolean)
}

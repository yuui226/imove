package io.github.imove.data.repository

import io.github.imove.data.local.datastore.UserPreferencesDataSource
import io.github.imove.domain.model.UserPreferences
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataSource: UserPreferencesDataSource
) : UserPreferencesRepository {

    override fun getPreferences(): Flow<UserPreferences> = dataSource.preferences

    override suspend fun updateTargetDirectory(path: String) {
        dataSource.updateTargetDirectory(path)
    }

    override suspend fun updateGridColumns(columns: Int) {
        dataSource.updateGridColumns(columns)
    }

    override suspend fun updateLanguage(language: String) {
        dataSource.updateLanguage(language)
    }

    override suspend fun updateDarkMode(enabled: String) {
        dataSource.updateDarkMode(enabled)
    }
}

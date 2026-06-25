package io.github.imove.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.domain.model.UserPreferences
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.getPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun updateTargetDirectory(path: String) {
        viewModelScope.launch { preferencesRepository.updateTargetDirectory(path) }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch { preferencesRepository.updateGridColumns(columns) }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.updateLanguage(language)
            val localeList = when (language) {
                "zh" -> LocaleListCompat.forLanguageTags("zh")
                else -> LocaleListCompat.forLanguageTags("en")
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun updateDarkMode(mode: String) {
        // The Compose theme observes this preference directly (see IMoveTheme); no AppCompat night-mode needed.
        viewModelScope.launch { preferencesRepository.updateDarkMode(mode) }
    }
}

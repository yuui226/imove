package io.github.imove

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.imove.data.usb.StorageAccessManager
import io.github.imove.domain.repository.UserPreferencesRepository
import io.github.imove.ui.navigation.NavGraph
import io.github.imove.ui.theme.IMoveTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var storageAccessManager: StorageAccessManager
    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val prefs = preferencesRepository.getPreferences().first()
            val localeList = when (prefs.language) {
                "zh" -> LocaleListCompat.forLanguageTags("zh")
                else -> LocaleListCompat.forLanguageTags("en")
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
        setContent {
            IMoveTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController, storageAccessManager = storageAccessManager)
            }
        }
    }
}

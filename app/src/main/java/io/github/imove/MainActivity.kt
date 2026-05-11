package io.github.imove

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.imove.data.usb.StorageAccessManager
import io.github.imove.ui.navigation.NavGraph
import io.github.imove.ui.theme.IMoveTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var storageAccessManager: StorageAccessManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IMoveTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController, storageAccessManager = storageAccessManager)
            }
        }
    }
}

package io.github.imove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.imove.ui.navigation.NavGraph
import io.github.imove.ui.theme.IMoveTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IMoveTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}

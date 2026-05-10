package io.github.imove.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun IMoveTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

package io.github.imove.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Transfer : Screen("transfer/{mode}/{startDate}/{endDate}") {
        fun createRoute(mode: String, startDate: Long = 0L, endDate: Long = 0L) =
            "transfer/$mode/$startDate/$endDate"
    }
    data object Preview : Screen("preview/{fileId}") {
        fun createRoute(fileId: String) = "preview/${android.net.Uri.encode(fileId)}"
    }
    data object Settings : Screen("settings")
}

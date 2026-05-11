package io.github.imove.domain.model

data class UserPreferences(
    val targetDirectory: String = "",
    val gridColumns: Int = 3,
    val language: String = if (java.util.Locale.getDefault().language.startsWith("zh")) "zh" else "en",
    val darkMode: String = "system"
)

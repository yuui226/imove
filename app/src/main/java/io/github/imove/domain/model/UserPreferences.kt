package io.github.imove.domain.model

data class UserPreferences(
    val targetDirectory: String = "Pictures/iMove/",
    val gridColumns: Int = 3,
    val language: String = "system",
    val darkMode: String = "system"
)

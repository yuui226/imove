package io.github.imove.viewmodel

/** Mutually-exclusive states the home screen can be in, computed by [HomeViewModel]. */
sealed interface HomeUiState {
    /** A USB device is connected and we're restoring its saved source folder. */
    data object Restoring : HomeUiState

    /** A USB device is connected but no source folder has been picked yet. */
    data class PickSource(val deviceLabel: String) : HomeUiState

    /** Source and save folders are both set — show the transfer-mode cards. */
    data class Ready(
        val isLocalSource: Boolean,
        val sourcePath: String,
        val volumeLabel: String
    ) : HomeUiState

    /** A USB source is set but the save folder is still missing. */
    data object NeedTarget : HomeUiState

    /** No USB device: unified setup screen to pick a local source and/or a save folder. */
    data class Setup(
        val localSourcePath: String?,
        val targetDirectory: String
    ) : HomeUiState
}

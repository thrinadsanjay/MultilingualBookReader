package com.multilingualbookreader.update

/** Which step of the update flow failed, so the retry action can resume from the right place. */
enum class UpdateStage { CHECK, DOWNLOAD, INSTALL }

/**
 * The single source of truth for the update flow. One explicit phase replaces the old set of
 * booleans, which could contradict each other (for example downloading and up to date at once).
 */
sealed interface UpdatePhase {
    /** Nothing has been checked yet in this session. */
    data object Idle : UpdatePhase

    data object Checking : UpdatePhase

    /** The installed build is the newest one published. */
    data object UpToDate : UpdatePhase

    data class Available(val update: AvailableUpdate) : UpdatePhase

    data class Downloading(val update: AvailableUpdate, val percent: Int) : UpdatePhase

    data class Downloaded(val update: AvailableUpdate) : UpdatePhase

    /** The system installer has been handed the update and has not returned a result yet. */
    data class Installing(val update: AvailableUpdate) : UpdatePhase

    data class Failed(
        val stage: UpdateStage,
        val update: AvailableUpdate?,
        val message: String,
    ) : UpdatePhase
}

object UpdateMessages {
    const val OFFLINE = "You're offline."
    const val CHECK_FAILED = "Couldn't check for updates."
    const val RATE_LIMITED = "Too many update checks. Try again in a few minutes."
    const val NOT_PUBLISHED = "No test builds are published yet."
    const val DOWNLOAD_FAILED = "Download failed."
    const val INSTALL_FAILED = "Installation couldn't be completed."
}

package com.multilingualbookreader.update

enum class UpdateActionKind { CHECK, DOWNLOAD, INSTALL, OPEN_PLAY, GRANT_PERMISSION, CANCEL, BUSY }

enum class UpdateActionIcon { REFRESH, DOWNLOAD, INSTALL, PLAY, NONE }

/** Everything the one update button needs to draw itself for the current phase. */
data class UpdateAction(
    val label: String,
    val kind: UpdateActionKind,
    val icon: UpdateActionIcon = UpdateActionIcon.NONE,
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val progressPercent: Int? = null,
)

data class UpdateHeadline(
    val title: String,
    val detail: String? = null,
    val isError: Boolean = false,
)

/**
 * Maps a phase to the copy and the single primary action shown to the user.
 *
 * Nothing here mentions APKs, file paths, or package installers: the download happens to be an APK,
 * but the user only ever sees check, download, and install.
 */
object UpdateCopy {
    fun primaryAction(phase: UpdatePhase, channel: InstallChannel): UpdateAction {
        // Play-managed and policy-blocked installs can never run our installer, so the only useful
        // action is opening the store, whatever the phase says.
        if (channel == InstallChannel.PLAY) {
            return UpdateAction("Open Google Play", UpdateActionKind.OPEN_PLAY, UpdateActionIcon.PLAY)
        }
        if (channel == InstallChannel.BLOCKED) {
            return UpdateAction("Get update from Google Play", UpdateActionKind.OPEN_PLAY, UpdateActionIcon.PLAY)
        }
        return when (phase) {
            UpdatePhase.Idle ->
                UpdateAction("Check for updates", UpdateActionKind.CHECK, UpdateActionIcon.REFRESH)

            UpdatePhase.Checking ->
                UpdateAction("Checking for updates…", UpdateActionKind.BUSY, enabled = false, busy = true)

            UpdatePhase.UpToDate ->
                UpdateAction("Check again", UpdateActionKind.CHECK, UpdateActionIcon.REFRESH)

            is UpdatePhase.Available ->
                UpdateAction(
                    "Download update ${phase.update.versionName}",
                    UpdateActionKind.DOWNLOAD,
                    UpdateActionIcon.DOWNLOAD,
                )

            is UpdatePhase.Downloading -> UpdateAction(
                label = if (phase.percent > 0) "Downloading… ${phase.percent}%" else "Downloading…",
                kind = UpdateActionKind.BUSY,
                enabled = false,
                busy = true,
                progressPercent = phase.percent.takeIf { it > 0 },
            )

            is UpdatePhase.Downloaded ->
                if (channel == InstallChannel.NEEDS_PERMISSION) {
                    UpdateAction("Allow installs to continue", UpdateActionKind.GRANT_PERMISSION, UpdateActionIcon.INSTALL)
                } else {
                    UpdateAction("Install update", UpdateActionKind.INSTALL, UpdateActionIcon.INSTALL)
                }

            is UpdatePhase.Installing ->
                UpdateAction("Installing…", UpdateActionKind.BUSY, enabled = false, busy = true)

            is UpdatePhase.Failed -> when (phase.stage) {
                UpdateStage.CHECK ->
                    UpdateAction("Try again", UpdateActionKind.CHECK, UpdateActionIcon.REFRESH)

                UpdateStage.DOWNLOAD -> UpdateAction(
                    label = phase.update?.let { "Retry download ${it.versionName}" } ?: "Try again",
                    kind = if (phase.update != null) UpdateActionKind.DOWNLOAD else UpdateActionKind.CHECK,
                    icon = if (phase.update != null) UpdateActionIcon.DOWNLOAD else UpdateActionIcon.REFRESH,
                )

                UpdateStage.INSTALL ->
                    UpdateAction("Try again", UpdateActionKind.INSTALL, UpdateActionIcon.INSTALL)
            }
        }
    }

    /** An optional escape hatch shown beside the primary action; null when there is nothing useful. */
    fun secondaryAction(phase: UpdatePhase, channel: InstallChannel): UpdateAction? = when {
        channel == InstallChannel.BLOCKED && phase !is UpdatePhase.Checking ->
            UpdateAction("Check for updates", UpdateActionKind.CHECK)

        channel == InstallChannel.PLAY -> null

        phase is UpdatePhase.Downloading -> UpdateAction("Cancel", UpdateActionKind.CANCEL)

        phase is UpdatePhase.Failed && phase.stage != UpdateStage.CHECK ->
            UpdateAction("Cancel", UpdateActionKind.CANCEL)

        else -> null
    }

    fun headline(phase: UpdatePhase): UpdateHeadline? = when (phase) {
        UpdatePhase.Idle -> null
        UpdatePhase.Checking -> UpdateHeadline("Checking for updates…")
        UpdatePhase.UpToDate -> UpdateHeadline("You're up to date")
        is UpdatePhase.Available -> UpdateHeadline("New update available", phase.update.versionName)
        is UpdatePhase.Downloading -> UpdateHeadline("Downloading update…", phase.update.versionName)
        is UpdatePhase.Downloaded -> UpdateHeadline("Ready to install", "Update ${phase.update.versionName} is ready to install.")
        is UpdatePhase.Installing -> UpdateHeadline("Installing update…", phase.update.versionName)
        is UpdatePhase.Failed -> UpdateHeadline(phase.message, isError = true)
    }

    /** Release notes trimmed to a few plain lines; internal metadata lines are dropped. */
    fun releaseNotes(phase: UpdatePhase, maxLines: Int = 3): List<String> {
        val update = when (phase) {
            is UpdatePhase.Available -> phase.update
            is UpdatePhase.Downloaded -> phase.update
            else -> null
        } ?: return emptyList()
        return update.notes
            .lineSequence()
            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("versionCode", ignoreCase = true) }
            .filterNot { it.startsWith("File:", ignoreCase = true) }
            .filterNot { it.endsWith(".apk", ignoreCase = true) }
            .take(maxLines)
            .toList()
    }
}

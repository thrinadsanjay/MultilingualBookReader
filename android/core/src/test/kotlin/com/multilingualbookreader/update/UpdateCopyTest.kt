package com.multilingualbookreader.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateCopyTest {
    private val update = AvailableUpdate(
        versionCode = 9,
        versionName = "v0.1.8",
        apkUrl = "https://example.test/BookReader-9-debug.apk",
        apkName = "BookReader-9-debug.apk",
        notes = "versionCode=9\nImproved OCR\nImproved voice processing\nFile: BookReader-9-debug.apk",
    )

    private fun label(phase: UpdatePhase, channel: InstallChannel = InstallChannel.DIRECT) =
        UpdateCopy.primaryAction(phase, channel).label

    @Test
    fun oneButtonWalksTheWholeFlow() {
        assertThat(label(UpdatePhase.Idle)).isEqualTo("Check for updates")
        assertThat(label(UpdatePhase.Checking)).isEqualTo("Checking for updates…")
        assertThat(label(UpdatePhase.Available(update))).isEqualTo("Download update v0.1.8")
        assertThat(label(UpdatePhase.Downloading(update, 45))).isEqualTo("Downloading… 45%")
        assertThat(label(UpdatePhase.Downloaded(update))).isEqualTo("Install update")
        assertThat(label(UpdatePhase.Installing(update))).isEqualTo("Installing…")
        assertThat(label(UpdatePhase.UpToDate)).isEqualTo("Check again")
    }

    @Test
    fun busyPhasesCannotBeTappedTwice() {
        assertThat(UpdateCopy.primaryAction(UpdatePhase.Checking, InstallChannel.DIRECT).enabled).isFalse()
        assertThat(UpdateCopy.primaryAction(UpdatePhase.Downloading(update, 10), InstallChannel.DIRECT).enabled).isFalse()
        assertThat(UpdateCopy.primaryAction(UpdatePhase.Installing(update), InstallChannel.DIRECT).enabled).isFalse()
    }

    @Test
    fun downloadingWithoutProgressStillReadsAsDownloading() {
        assertThat(label(UpdatePhase.Downloading(update, 0))).isEqualTo("Downloading…")
    }

    @Test
    fun eachFailureRetriesItsOwnStage() {
        val checkFailed = UpdatePhase.Failed(UpdateStage.CHECK, null, UpdateMessages.OFFLINE)
        assertThat(UpdateCopy.primaryAction(checkFailed, InstallChannel.DIRECT).kind).isEqualTo(UpdateActionKind.CHECK)
        assertThat(label(checkFailed)).isEqualTo("Try again")

        val downloadFailed = UpdatePhase.Failed(UpdateStage.DOWNLOAD, update, UpdateMessages.DOWNLOAD_FAILED)
        assertThat(UpdateCopy.primaryAction(downloadFailed, InstallChannel.DIRECT).kind).isEqualTo(UpdateActionKind.DOWNLOAD)
        assertThat(label(downloadFailed)).isEqualTo("Retry download v0.1.8")

        val installFailed = UpdatePhase.Failed(UpdateStage.INSTALL, update, UpdateMessages.INSTALL_FAILED)
        assertThat(UpdateCopy.primaryAction(installFailed, InstallChannel.DIRECT).kind).isEqualTo(UpdateActionKind.INSTALL)
    }

    @Test
    fun offlineFailureKeepsTheButtonUsable() {
        val offline = UpdatePhase.Failed(UpdateStage.CHECK, null, UpdateMessages.OFFLINE)
        val action = UpdateCopy.primaryAction(offline, InstallChannel.DIRECT)
        assertThat(action.enabled).isTrue()
        assertThat(action.busy).isFalse()
        assertThat(UpdateCopy.headline(offline)?.title).isEqualTo("You're offline.")
    }

    @Test
    fun blockedAndPlayInstallsPointAtTheStore() {
        assertThat(UpdateCopy.primaryAction(UpdatePhase.Idle, InstallChannel.PLAY).kind)
            .isEqualTo(UpdateActionKind.OPEN_PLAY)
        assertThat(UpdateCopy.primaryAction(UpdatePhase.Available(update), InstallChannel.BLOCKED).kind)
            .isEqualTo(UpdateActionKind.OPEN_PLAY)
    }

    @Test
    fun blockedInstallsCanStillCheckFromTheSecondaryAction() {
        val secondary = UpdateCopy.secondaryAction(UpdatePhase.Idle, InstallChannel.BLOCKED)
        assertThat(secondary?.kind).isEqualTo(UpdateActionKind.CHECK)
    }

    @Test
    fun downloadingCanBeCancelled() {
        val secondary = UpdateCopy.secondaryAction(UpdatePhase.Downloading(update, 20), InstallChannel.DIRECT)
        assertThat(secondary?.kind).isEqualTo(UpdateActionKind.CANCEL)
    }

    @Test
    fun missingInstallPermissionIsRequestedBeforeInstalling() {
        val action = UpdateCopy.primaryAction(UpdatePhase.Downloaded(update), InstallChannel.NEEDS_PERMISSION)
        assertThat(action.kind).isEqualTo(UpdateActionKind.GRANT_PERMISSION)
    }

    @Test
    fun releaseNotesDropFileAndVersionCodeLines() {
        val notes = UpdateCopy.releaseNotes(UpdatePhase.Available(update))
        assertThat(notes).containsExactly("Improved OCR", "Improved voice processing").inOrder()
    }

    @Test
    fun noUserFacingStringMentionsApks() {
        val phases = listOf(
            UpdatePhase.Idle,
            UpdatePhase.Checking,
            UpdatePhase.UpToDate,
            UpdatePhase.Available(update),
            UpdatePhase.Downloading(update, 45),
            UpdatePhase.Downloaded(update),
            UpdatePhase.Installing(update),
            UpdatePhase.Failed(UpdateStage.CHECK, null, UpdateMessages.CHECK_FAILED),
            UpdatePhase.Failed(UpdateStage.DOWNLOAD, update, UpdateMessages.DOWNLOAD_FAILED),
            UpdatePhase.Failed(UpdateStage.INSTALL, update, UpdateMessages.INSTALL_FAILED),
        )
        val channels = InstallChannel.entries
        val strings = phases.flatMap { phase ->
            channels.flatMap { channel ->
                listOfNotNull(
                    UpdateCopy.primaryAction(phase, channel).label,
                    UpdateCopy.secondaryAction(phase, channel)?.label,
                    UpdateCopy.headline(phase)?.title,
                    UpdateCopy.headline(phase)?.detail,
                ) + UpdateCopy.releaseNotes(phase)
            }
        }
        strings.forEach { assertThat(it.lowercase()).doesNotContain("apk") }
    }
}

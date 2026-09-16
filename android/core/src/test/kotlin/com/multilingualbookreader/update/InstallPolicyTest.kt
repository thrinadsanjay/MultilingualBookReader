package com.multilingualbookreader.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InstallPolicyTest {
    @Test
    fun playInstallsUpdateThroughPlay() {
        val channel = InstallPolicy.channel(
            installerPackage = InstallPolicy.PLAY_STORE_PACKAGE,
            canRequestPackageInstalls = false,
            unknownSourcesRestricted = true,
        )
        assertThat(channel).isEqualTo(InstallChannel.PLAY)
    }

    @Test
    fun advancedProtectionBlocksEverySideload() {
        val channel = InstallPolicy.channel(
            installerPackage = null,
            canRequestPackageInstalls = false,
            unknownSourcesRestricted = true,
        )
        assertThat(channel).isEqualTo(InstallChannel.BLOCKED)
    }

    @Test
    fun missingPermissionCanStillBeGranted() {
        val channel = InstallPolicy.channel(
            installerPackage = null,
            canRequestPackageInstalls = false,
            unknownSourcesRestricted = false,
        )
        assertThat(channel).isEqualTo(InstallChannel.NEEDS_PERMISSION)
    }

    @Test
    fun allowsDirectInstallWhenThePhonePermitsIt() {
        val channel = InstallPolicy.channel(
            installerPackage = "com.android.shell",
            canRequestPackageInstalls = true,
            unknownSourcesRestricted = false,
        )
        assertThat(channel).isEqualTo(InstallChannel.DIRECT)
    }

    @Test
    fun releaseBuildsWithoutTheInstallPermissionDeferToPlay() {
        val channel = InstallPolicy.channel(
            installerPackage = null,
            canRequestPackageInstalls = false,
            unknownSourcesRestricted = false,
            selfInstallSupported = false,
        )
        assertThat(channel).isEqualTo(InstallChannel.PLAY)
    }

    @Test
    fun onlySideloadChannelsDownloadAnApk() {
        assertThat(InstallPolicy.downloadIsPointless(InstallChannel.PLAY)).isTrue()
        assertThat(InstallPolicy.downloadIsPointless(InstallChannel.BLOCKED)).isTrue()
        assertThat(InstallPolicy.downloadIsPointless(InstallChannel.DIRECT)).isFalse()
        assertThat(InstallPolicy.downloadIsPointless(InstallChannel.NEEDS_PERMISSION)).isFalse()
    }
}

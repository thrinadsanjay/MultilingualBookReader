package com.multilingualbookreader.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InstallPolicyTest {
    @Test
    fun usesBrowserWhenUnknownSourcesAreRestricted() {
        assertThat(InstallPolicy.prefersBrowserInstall(canRequestPackageInstalls = true, unknownSourcesRestricted = true)).isTrue()
    }

    @Test
    fun usesBrowserWhenThisAppCannotInstall() {
        assertThat(InstallPolicy.prefersBrowserInstall(canRequestPackageInstalls = false, unknownSourcesRestricted = false)).isTrue()
    }

    @Test
    fun allowsDirectInstallWhenThePhonePermitsIt() {
        assertThat(InstallPolicy.prefersBrowserInstall(canRequestPackageInstalls = true, unknownSourcesRestricted = false)).isFalse()
    }
}

package com.multilingualbookreader.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignatureCheckTest {
    @Test
    fun theSameSignerInstallsOverTheTop() {
        val verdict = SignatureCheck.compare(setOf("cert-a"), setOf("cert-a"))
        assertThat(verdict).isEqualTo(SignatureCheck.Verdict.MATCHES)
    }

    @Test
    fun aDifferentSignerIsCaughtBeforeTheSystemInstallerComplains() {
        val verdict = SignatureCheck.compare(setOf("cert-a"), setOf("cert-b"))
        assertThat(verdict).isEqualTo(SignatureCheck.Verdict.DIFFERENT)
    }

    @Test
    fun rotatedKeysThatStillShareASignerAreAccepted() {
        val verdict = SignatureCheck.compare(setOf("cert-a"), setOf("cert-a", "cert-b"))
        assertThat(verdict).isEqualTo(SignatureCheck.Verdict.MATCHES)
    }

    @Test
    fun unreadableSignersDeferToTheSystemInstaller() {
        assertThat(SignatureCheck.compare(emptySet(), setOf("cert-a"))).isEqualTo(SignatureCheck.Verdict.UNKNOWN)
        assertThat(SignatureCheck.compare(setOf("cert-a"), emptySet())).isEqualTo(SignatureCheck.Verdict.UNKNOWN)
    }

    @Test
    fun theExplanationTellsTheUserWhatToDo() {
        assertThat(SignatureCheck.DIFFERENT_SIGNER_MESSAGE).contains("Uninstall")
    }
}

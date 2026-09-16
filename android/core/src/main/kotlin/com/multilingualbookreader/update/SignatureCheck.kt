package com.multilingualbookreader.update

/**
 * Android refuses to replace an installed app when the new file is signed by a different key, and
 * reports it only as "package conflicts with an existing package". Comparing signers first lets the
 * app explain that an uninstall is needed instead of handing the user that message.
 */
object SignatureCheck {
    enum class Verdict {
        /** Same signer: the update will install over the top. */
        MATCHES,

        /** Different signer: Android will reject the install until the old copy is removed. */
        DIFFERENT,

        /** Signers could not be read, so let the system installer decide. */
        UNKNOWN,
    }

    fun compare(installedSigners: Set<String>, updateSigners: Set<String>): Verdict = when {
        installedSigners.isEmpty() || updateSigners.isEmpty() -> Verdict.UNKNOWN
        installedSigners == updateSigners -> Verdict.MATCHES
        installedSigners.intersect(updateSigners).isNotEmpty() -> Verdict.MATCHES
        else -> Verdict.DIFFERENT
    }

    const val DIFFERENT_SIGNER_MESSAGE =
        "This update was signed with a different key, so Android will not install it over the " +
            "current version. Uninstall Svara, then install the update. Books and notes on this " +
            "phone are removed with it."
}

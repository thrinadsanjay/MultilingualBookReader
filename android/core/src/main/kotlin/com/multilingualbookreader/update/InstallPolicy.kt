package com.multilingualbookreader.update

object InstallPolicy {
    fun prefersBrowserInstall(
        canRequestPackageInstalls: Boolean,
        unknownSourcesRestricted: Boolean,
    ): Boolean = unknownSourcesRestricted || !canRequestPackageInstalls
}

package com.multilingualbookreader.update

/**
 * How this install can receive its next version.
 *
 * Android's Advanced Protection revokes REQUEST_INSTALL_PACKAGES for every app and also refuses
 * `adb install`, so a blocked device has no sideload path at all and must update through Google Play.
 */
enum class InstallChannel {
    /** Installed by Play, so Play delivers updates and nothing in the app has to install an APK. */
    PLAY,

    /** Sideloading is permitted; the app can download an APK and hand it to the system installer. */
    DIRECT,

    /** Sideloading is permitted but the user has not granted this app the install permission yet. */
    NEEDS_PERMISSION,

    /** Device policy (typically Advanced Protection) forbids installing APKs from anywhere. */
    BLOCKED,
}

object InstallPolicy {
    const val PLAY_STORE_PACKAGE = "com.android.vending"

    /**
     * @param selfInstallSupported whether this build ships the install permission at all. Release
     * builds do not, because sideloading test builds is a debug-only affordance and Play treats
     * REQUEST_INSTALL_PACKAGES as sensitive.
     */
    fun channel(
        installerPackage: String?,
        canRequestPackageInstalls: Boolean,
        unknownSourcesRestricted: Boolean,
        selfInstallSupported: Boolean = true,
    ): InstallChannel = when {
        installerPackage == PLAY_STORE_PACKAGE -> InstallChannel.PLAY
        !selfInstallSupported -> InstallChannel.PLAY
        unknownSourcesRestricted -> InstallChannel.BLOCKED
        canRequestPackageInstalls -> InstallChannel.DIRECT
        else -> InstallChannel.NEEDS_PERMISSION
    }

    /** True when the app must not download an APK, because it could never be installed. */
    fun downloadIsPointless(channel: InstallChannel): Boolean =
        channel == InstallChannel.PLAY || channel == InstallChannel.BLOCKED
}

package com.multilingualbookreader.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.multilingualbookreader.BuildConfig
import com.multilingualbookreader.common.AppLog
import com.multilingualbookreader.network.ConnectivityObserver
import com.multilingualbookreader.network.PlainHttp
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException

data class UpdateUiState(
    val installedVersionName: String = BuildConfig.VERSION_NAME,
    val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    val phase: UpdatePhase = UpdatePhase.Idle,
) {
    val installedLabel: String = "v$installedVersionName ($installedVersionCode)"
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GitHubReleaseApi,
    private val connectivity: ConnectivityObserver,
    @PlainHttp private val http: OkHttpClient,
) {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state

    private var downloadedApk: File? = null

    private fun moveTo(phase: UpdatePhase) {
        _state.value = _state.value.copy(phase = phase)
    }

    suspend fun check() {
        if (_state.value.phase is UpdatePhase.Checking) return
        if (!connectivity.isOnline) {
            moveTo(UpdatePhase.Failed(UpdateStage.CHECK, null, UpdateMessages.OFFLINE))
            return
        }
        moveTo(UpdatePhase.Checking)
        runCatching {
            val releases = api.releases(BuildConfig.UPDATE_OWNER, BuildConfig.UPDATE_REPO)
            AppUpdateParser.chooseNewest(
                currentVersionCode = BuildConfig.VERSION_CODE,
                releases = releases.map { release ->
                    AppUpdateParser.ReleaseRef(
                        tag = release.tagName,
                        name = release.name.orEmpty(),
                        body = release.body,
                        draft = release.draft,
                        assets = release.assets.map { AppUpdateParser.AssetRef(it.name, it.browserDownloadUrl) },
                    )
                },
            )
        }.onSuccess { newest ->
            downloadedApk = null
            moveTo(if (newest == null) UpdatePhase.UpToDate else UpdatePhase.Available(newest))
        }.onFailure { error ->
            AppLog.w("update_check_failed")
            val message = when ((error as? HttpException)?.code()) {
                403, 429 -> UpdateMessages.RATE_LIMITED
                404 -> UpdateMessages.NOT_PUBLISHED
                else -> UpdateMessages.CHECK_FAILED
            }
            moveTo(UpdatePhase.Failed(UpdateStage.CHECK, null, message))
        }
    }

    /** Downloads the pending update. The transport is an APK, but the UI only ever says "update". */
    suspend fun download() {
        val update = pendingUpdate() ?: return
        if (!connectivity.isOnline) {
            moveTo(UpdatePhase.Failed(UpdateStage.DOWNLOAD, update, UpdateMessages.OFFLINE))
            return
        }
        moveTo(UpdatePhase.Downloading(update, 0))
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(update.apkUrl).build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("download failed")
                    val body = response.body ?: error("empty body")
                    val total = body.contentLength()
                    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                    val file = File(dir, "Svara-update.apk")
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                output.write(buffer, 0, n)
                                read += n
                                if (total > 0) {
                                    moveTo(UpdatePhase.Downloading(update, ((read * 100) / total).toInt()))
                                }
                            }
                        }
                    }
                    file
                }
            }.onSuccess { file ->
                downloadedApk = file
                moveTo(UpdatePhase.Downloaded(update))
            }.onFailure { error ->
                if (error is CancellationException) throw error
                AppLog.w("update_download_failed")
                downloadedApk = null
                moveTo(UpdatePhase.Failed(UpdateStage.DOWNLOAD, update, UpdateMessages.DOWNLOAD_FAILED))
            }
        }
    }

    /** Hands the downloaded update to the system installer. */
    fun startInstall() {
        val update = pendingUpdate() ?: return
        val file = downloadedApk
        if (file == null || !file.exists()) {
            moveTo(UpdatePhase.Failed(UpdateStage.DOWNLOAD, update, UpdateMessages.DOWNLOAD_FAILED))
            return
        }
        if (signerVerdict(file) == SignatureCheck.Verdict.DIFFERENT) {
            AppLog.w("update_signer_mismatch")
            moveTo(UpdatePhase.Failed(UpdateStage.INSTALL, update, SignatureCheck.DIFFERENT_SIGNER_MESSAGE))
            return
        }
        moveTo(UpdatePhase.Installing(update))
        runCatching { context.startActivity(installIntent(file)) }.onFailure {
            AppLog.w("update_install_launch_failed")
            moveTo(UpdatePhase.Failed(UpdateStage.INSTALL, update, UpdateMessages.INSTALL_FAILED))
        }
    }

    /** Reads both signers so a doomed install is explained rather than attempted. */
    private fun signerVerdict(file: File): SignatureCheck.Verdict = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return SignatureCheck.Verdict.UNKNOWN
        val manager = context.packageManager
        val flag = PackageManager.GET_SIGNING_CERTIFICATES
        val update = manager.getPackageArchiveInfo(file.absolutePath, flag)?.signers()
        val installed = manager.getPackageInfo(context.packageName, flag).signers()
        SignatureCheck.compare(installed, update.orEmpty())
    }.getOrDefault(SignatureCheck.Verdict.UNKNOWN)

    private fun PackageInfo.signers(): Set<String> =
        signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()

    /**
     * A successful install replaces this process, so coming back still in [UpdatePhase.Installing]
     * means the user cancelled or the installer refused it.
     */
    fun installerReturned() {
        val phase = _state.value.phase
        if (phase is UpdatePhase.Installing) {
            moveTo(UpdatePhase.Failed(UpdateStage.INSTALL, phase.update, UpdateMessages.INSTALL_FAILED))
        }
    }

    fun cancelDownload() {
        val update = pendingUpdate()
        downloadedApk = null
        moveTo(if (update == null) UpdatePhase.Idle else UpdatePhase.Available(update))
    }

    /** Clears an error without throwing away a download that already succeeded. */
    fun dismissFailure() {
        val phase = _state.value.phase as? UpdatePhase.Failed ?: return
        val update = phase.update ?: return moveTo(UpdatePhase.Idle)
        moveTo(
            if (downloadedApk?.exists() == true) UpdatePhase.Downloaded(update) else UpdatePhase.Available(update),
        )
    }

    private fun pendingUpdate(): AvailableUpdate? = when (val phase = _state.value.phase) {
        is UpdatePhase.Available -> phase.update
        is UpdatePhase.Downloading -> phase.update
        is UpdatePhase.Downloaded -> phase.update
        is UpdatePhase.Installing -> phase.update
        is UpdatePhase.Failed -> phase.update
        else -> null
    }

    fun unknownSourcesRestricted(): Boolean {
        if (advancedProtectionEnabled()) return true
        val users = context.getSystemService(UserManager::class.java) ?: return false
        return users.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES) ||
            users.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY) ||
            users.hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)
    }

    /**
     * Android 16 exposes Advanced Protection through a system service that is newer than our
     * compileSdk, so it is read reflectively and treated as off when the class is missing.
     */
    private fun advancedProtectionEnabled(): Boolean = runCatching {
        val service = context.getSystemService("advanced_protection") ?: return false
        val method = service.javaClass.getMethod("isAdvancedProtectionEnabled")
        method.invoke(service) as? Boolean ?: false
    }.getOrDefault(false)

    private fun installerPackage(): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()

    fun installChannel(): InstallChannel = InstallPolicy.channel(
        installerPackage = installerPackage(),
        canRequestPackageInstalls = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false),
        unknownSourcesRestricted = unknownSourcesRestricted(),
        selfInstallSupported = BuildConfig.SELF_INSTALL_SUPPORTED,
    )

    /** Opens this app's Play listing, where an update installs even while Advanced Protection is on. */
    fun openPlayStore() {
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(Intent.ACTION_VIEW, "$PLAY_WEB_URL${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(market) }
            .recoverCatching { context.startActivity(web) }
            .onFailure { AppLog.w("play_store_unavailable") }
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure { AppLog.w("install_settings_unavailable") }
    }

    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private companion object {
        const val PLAY_WEB_URL = "https://play.google.com/store/apps/details?id="
    }
}

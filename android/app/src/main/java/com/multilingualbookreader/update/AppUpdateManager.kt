package com.multilingualbookreader.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.multilingualbookreader.BuildConfig
import com.multilingualbookreader.common.AppLog
import com.multilingualbookreader.network.PlainHttp
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException

data class UpdateUiState(
    val currentVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val progressPercent: Int = 0,
    val available: AvailableUpdate? = null,
    val downloadedFile: File? = null,
    val message: String? = null,
)

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GitHubReleaseApi,
    @PlainHttp private val http: OkHttpClient,
) {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state

    suspend fun check() {
        if (installChannel() == InstallChannel.PLAY) {
            _state.value = _state.value.copy(
                checking = false,
                available = null,
                message = "Google Play delivers updates for this install. Open Play to get the newest build.",
            )
            return
        }
        _state.value = _state.value.copy(checking = true, message = "Checking for an update…")
        runCatching {
            val releases = api.releases(BuildConfig.UPDATE_OWNER, BuildConfig.UPDATE_REPO)
            val newest = AppUpdateParser.chooseNewest(
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
            if (newest == null) {
                _state.value = _state.value.copy(
                    checking = false,
                    available = null,
                    message = "You already have the latest test build.",
                )
            } else {
                _state.value = _state.value.copy(
                    checking = false,
                    available = newest,
                    downloadedFile = null,
                    message = "Version ${newest.versionName} is ready to install.",
                )
            }
        }.onFailure {
            AppLog.w("update_check_failed")
            val message = when ((it as? HttpException)?.code()) {
                403, 429 -> "GitHub is rate-limiting update checks. Try again in a few minutes."
                404 -> "Could not find published test builds for this app."
                else -> "Could not check for updates. Connect to the internet and try again."
            }
            _state.value = _state.value.copy(checking = false, message = message)
        }
    }

    suspend fun download(): File? {
        val update = _state.value.available ?: return null
        _state.value = _state.value.copy(downloading = true, progressPercent = 0, message = "Downloading update…")
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(update.apkUrl).build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("download failed")
                    val body = response.body ?: error("empty apk")
                    val total = body.contentLength()
                    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                    val file = File(dir, "BookReader-update.apk")
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
                                    _state.value = _state.value.copy(progressPercent = ((read * 100) / total).toInt())
                                }
                            }
                        }
                    }
                    _state.value = _state.value.copy(
                        downloading = false,
                        downloadedFile = file,
                        progressPercent = 100,
                        message = "Download finished. Tap Install to update.",
                    )
                    file
                }
            }.getOrElse {
                AppLog.w("update_download_failed")
                _state.value = _state.value.copy(
                    downloading = false,
                    message = "The update could not be downloaded.",
                )
                null
            }
        }
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
    )

    fun canInstallFromThisApp(): Boolean = installChannel() == InstallChannel.DIRECT

    fun browserDownloadIntent(url: String? = _state.value.available?.apkUrl): Intent? {
        val target = url ?: return null
        return Intent(Intent.ACTION_VIEW, target.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

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

    fun installPermissionIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
        }
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

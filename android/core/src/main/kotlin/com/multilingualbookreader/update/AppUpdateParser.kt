package com.multilingualbookreader.update

data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkName: String,
    val notes: String,
)

object AppUpdateParser {
    // Builds published before the rename are still named BookReader-<code>-debug.apk.
    private val fileVersion = Regex("""(?:Svara|BookReader)-(\d+)(?:-debug)?\.apk""", RegexOption.IGNORE_CASE)
    private val bodyVersion = Regex("""versionCode\s*=\s*(\d+)""")

    fun versionFromAssetName(name: String): Int? = fileVersion.find(name)?.groupValues?.get(1)?.toInt()

    fun versionFromBody(body: String?): Int? = body?.let { bodyVersion.find(it)?.groupValues?.get(1)?.toInt() }

    fun chooseNewest(
        currentVersionCode: Int,
        releases: List<ReleaseRef>,
    ): AvailableUpdate? {
        return releases
            .asSequence()
            .filterNot { it.draft }
            .flatMap { release ->
                release.assets.mapNotNull { asset ->
                    if (!asset.name.endsWith(".apk", ignoreCase = true)) return@mapNotNull null
                    val code = versionFromAssetName(asset.name) ?: versionFromBody(release.body) ?: return@mapNotNull null
                    AvailableUpdate(
                        versionCode = code,
                        versionName = release.name.ifBlank { release.tag },
                        apkUrl = asset.url,
                        apkName = asset.name,
                        notes = release.body.orEmpty(),
                    )
                }
            }
            .filter { it.versionCode > currentVersionCode }
            .maxByOrNull { it.versionCode }
    }

    data class ReleaseRef(
        val tag: String,
        val name: String,
        val body: String?,
        val draft: Boolean,
        val assets: List<AssetRef>,
    )

    data class AssetRef(
        val name: String,
        val url: String,
    )
}

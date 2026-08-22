package com.multilingualbookreader.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppUpdateParserTest {
    @Test
    fun readsVersionFromFileName() {
        assertThat(AppUpdateParser.versionFromAssetName("BookReader-3-debug.apk")).isEqualTo(3)
    }

    @Test
    fun choosesNewerReleaseOnly() {
        val newest = AppUpdateParser.chooseNewest(
            currentVersionCode = 2,
            releases = listOf(
                AppUpdateParser.ReleaseRef(
                    tag = "v1",
                    name = "old",
                    body = "versionCode=2",
                    draft = false,
                    assets = listOf(AppUpdateParser.AssetRef("BookReader-2-debug.apk", "https://example.com/2.apk")),
                ),
                AppUpdateParser.ReleaseRef(
                    tag = "v2",
                    name = "newer",
                    body = "versionCode=4",
                    draft = false,
                    assets = listOf(AppUpdateParser.AssetRef("BookReader-4-debug.apk", "https://example.com/4.apk")),
                ),
            ),
        )
        assertThat(newest?.versionCode).isEqualTo(4)
        assertThat(newest?.apkUrl).isEqualTo("https://example.com/4.apk")
    }

    @Test
    fun readsVersionFromReleaseBodyWhenFileNameHasNoCode() {
        val newest = AppUpdateParser.chooseNewest(
            currentVersionCode = 1,
            releases = listOf(
                AppUpdateParser.ReleaseRef(
                    tag = "testing-latest",
                    name = "Testing build",
                    body = "versionCode=3\nInstall from the app.",
                    draft = false,
                    assets = listOf(AppUpdateParser.AssetRef("BookReader-debug.apk", "https://example.com/latest.apk")),
                ),
            ),
        )
        assertThat(newest?.versionCode).isEqualTo(3)
        assertThat(newest?.apkUrl).isEqualTo("https://example.com/latest.apk")
    }

    @Test
    fun ignoresDraftsAndOlderBuilds() {
        val newest = AppUpdateParser.chooseNewest(
            currentVersionCode = 5,
            releases = listOf(
                AppUpdateParser.ReleaseRef(
                    tag = "draft",
                    name = "draft",
                    body = "versionCode=9",
                    draft = true,
                    assets = listOf(AppUpdateParser.AssetRef("BookReader-9-debug.apk", "https://example.com/9.apk")),
                ),
            ),
        )
        assertThat(newest).isNull()
    }
}

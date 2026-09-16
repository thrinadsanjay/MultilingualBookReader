package com.multilingualbookreader.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.multilingualbookreader.R
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rasterises the adaptive icon layers so the launcher mark can be reviewed, and writes the
 * 512 x 512 PNG that the Play listing requires.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LauncherIconRenderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun writesStoreIcon() {
        val icon = render(size = 512, masked = false)
        File("build/screenshots").mkdirs()
        write(icon, "launcher-icon-512.png")

        // A blank canvas would mean the vector failed to inflate.
        assertThat(distinctColours(icon)).isGreaterThan(8)
    }

    @Test
    fun writesLauncherPreview() {
        write(render(size = 256, masked = true), "launcher-icon-masked.png")
    }

    private fun render(size: Int, masked: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_launcher_background, null)!!
        val foreground = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_launcher_foreground, null)!!
        // Launchers show the middle 72 of the 108 viewport, so scale up to preview the crop.
        val scale = if (masked) size * 108f / 72f else size.toFloat()
        val offset = ((size - scale) / 2f).toInt()
        val edge = offset + scale.toInt()
        listOf(background, foreground).forEach { drawable ->
            drawable.setBounds(offset, offset, edge, edge)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun distinctColours(bitmap: Bitmap): Int {
        val colours = mutableSetOf<Int>()
        for (x in 0 until bitmap.width step 8) {
            for (y in 0 until bitmap.height step 8) {
                colours += bitmap.getPixel(x, y)
            }
        }
        return colours.size
    }

    private fun write(bitmap: Bitmap, name: String) {
        File("build/screenshots").mkdirs()
        File("build/screenshots/$name").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

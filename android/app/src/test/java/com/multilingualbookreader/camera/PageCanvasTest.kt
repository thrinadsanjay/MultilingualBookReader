package com.multilingualbookreader.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * PDF pages are rendered into a bitmap and then compressed to JPEG for recognition. JPEG has no
 * alpha channel, so anything left transparent flattens to black and hides the text.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PageCanvasTest {
    @Test
    fun pageCanvasStartsOpaqueWhite() {
        val bitmap = PageImageProcessor.newPageCanvas(40, 40)
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.WHITE)
        assertThat(bitmap.getPixel(39, 39)).isEqualTo(Color.WHITE)
    }

    @Test
    fun textStaysReadableAfterTheJpegRoundTrip() {
        val bitmap = PageImageProcessor.newPageCanvas(40, 40)
        Canvas(bitmap).drawRect(10f, 10f, 30f, 30f, Paint().apply { color = Color.BLACK })

        val decoded = jpegRoundTrip(bitmap)

        assertThat(decoded.getPixel(2, 2)).isEqualTo(Color.WHITE)
        assertThat(decoded.getPixel(20, 20)).isEqualTo(Color.BLACK)
    }

    @Test
    fun galleryPhotosKeepTheFullPageInsteadOfTheCameraFrame() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        Canvas(source).drawRect(0f, 0f, 6f, 100f, Paint().apply { color = Color.RED })
        val jpeg = ByteArrayOutputStream().apply { source.compress(Bitmap.CompressFormat.JPEG, 100, this) }.toByteArray()

        val camera = PageImageProcessor.prepareForOcr(jpeg, cropToCameraFrame = true)
        val gallery = PageImageProcessor.prepareForOcr(jpeg, cropToCameraFrame = false)

        assertThat(camera.bitmap.width).isLessThan(gallery.bitmap.width)
        assertThat(camera.bitmap.height).isLessThan(gallery.bitmap.height)
        assertThat(gallery.bitmap.width).isEqualTo(100)
        assertThat(gallery.bitmap.height).isEqualTo(100)
        // The red strip is in the camera overlay margin, so a live capture must drop it and a
        // gallery photo must keep the whole page.
        assertThat(Color.red(gallery.bitmap.getPixel(2, 50))).isGreaterThan(150)
        assertThat(Color.red(camera.bitmap.getPixel(2, 50))).isLessThan(80)
    }

    @Test
    fun anUnerasedCanvasLosesTheTextToABlackBackground() {
        val transparent = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        Canvas(transparent).drawRect(10f, 10f, 30f, 30f, Paint().apply { color = Color.BLACK })

        val decoded = jpegRoundTrip(transparent)

        // Background and glyphs both end up black, which is exactly why imported pages read as empty.
        assertThat(decoded.getPixel(2, 2)).isEqualTo(Color.BLACK)
        assertThat(decoded.getPixel(20, 20)).isEqualTo(Color.BLACK)
    }

    private fun jpegRoundTrip(bitmap: Bitmap): Bitmap {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

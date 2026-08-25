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

package com.multilingualbookreader.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
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
        source.setPixel(1, 1, Color.RED)
        val jpeg = ByteArrayOutputStream().apply { source.compress(Bitmap.CompressFormat.JPEG, 100, this) }.toByteArray()

        val camera = PageImageProcessor.prepareForOcr(jpeg, cropToCameraFrame = true)
        val gallery = PageImageProcessor.prepareForOcr(jpeg, cropToCameraFrame = false)

        assertThat(camera.bitmap.width).isEqualTo(90)
        assertThat(camera.bitmap.height).isEqualTo(90)
        assertThat(gallery.bitmap.width).isEqualTo(100)
        assertThat(gallery.bitmap.height).isEqualTo(100)
        val croppedDirect = PageImageProcessor.cropToFrame(source, 0.05f, 0.05f, 0.95f, 0.95f)
        assertThat(croppedDirect.getPixel(0, 0)).isEqualTo(Color.WHITE)
        assertThat(source.getPixel(1, 1)).isEqualTo(Color.RED)
    }

    @Test
    fun aDraftKeepsTheRotationTheReaderPicked() {
        val upright = pageWithHorizontalLines(width = 48, height = 80)
        val rendered = PageImageProcessor.renderDraft(upright, rotationDegrees = 90, cropInset = 0f, enhance = false)
        assertThat(rendered.width).isEqualTo(80)
        assertThat(rendered.height).isEqualTo(48)
        val cropped = PageImageProcessor.renderDraft(upright, rotationDegrees = 0, cropInset = 0.10f, enhance = false)
        assertThat(cropped.width).isEqualTo(39)
        assertThat(cropped.height).isEqualTo(64)
    }

    @Test
    fun aSidewaysPageIsRotatedUntilTheLinesRunAcross() {
        val upright = pageWithHorizontalLines(width = 48, height = 80)
        val sideways = PageImageProcessor.rotate(upright, 90f)

        val restored = PageImageProcessor.uprightPage(sideways)

        assertThat(restored.width).isEqualTo(48)
        assertThat(restored.height).isEqualTo(80)
        assertThat(PageImageProcessor.lineScore(restored))
            .isGreaterThan(PageImageProcessor.lineScore(sideways))
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

    @Test
    fun decodePageFileReadsASavedScan() {
        val source = PageImageProcessor.newPageCanvas(80, 120)
        val file = File.createTempFile("page", ".jpg")
        file.writeBytes(PageImageProcessor.toJpeg(source))
        val loaded = PageImageProcessor.decodePageFile(file.absolutePath)!!
        assertThat(loaded.width).isGreaterThan(0)
        assertThat(loaded.height).isGreaterThan(0)
    }

    @Test
    fun decodePageFileIgnoresAMissingFile() {
        assertThat(PageImageProcessor.decodePageFile("/no/such/page.jpg")).isNull()
    }

    private fun pageWithHorizontalLines(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }
        var y = 12
        while (y < height - 8) {
            canvas.drawLine(4f, y.toFloat(), (width - 4).toFloat(), y.toFloat(), paint)
            y += 6
        }
        return bitmap
    }

    private fun jpegRoundTrip(bitmap: Bitmap): Bitmap {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

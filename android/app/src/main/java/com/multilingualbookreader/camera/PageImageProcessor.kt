package com.multilingualbookreader.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import com.multilingualbookreader.image.ImageOrientation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max

object PageImageProcessor {
    /** Decodes a camera JPEG upright; BitmapFactory alone ignores the EXIF rotation tag. */
    fun decode(bytes: ByteArray, maxWidth: Int = 1600): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = sampleSize(max(bounds.outWidth, bounds.outHeight), maxWidth)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return rotate(decoded, exifDegrees(bytes))
    }

    fun exifDegrees(bytes: ByteArray): Float {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ImageOrientation.NORMAL)
        }.getOrDefault(ImageOrientation.NORMAL)
        return ImageOrientation.degreesFor(orientation)
    }

    /** A page canvas must start opaque white: PDF pages render onto transparency, and JPEG has no
     *  alpha channel, so an un-erased bitmap flattens to black text on black. */
    fun newPageCanvas(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }

    /** Camera captures are framed by the on-screen overlay; gallery photos are already the page. */
    fun prepareForOcr(bytes: ByteArray, cropToCameraFrame: Boolean): PreparedPage {
        val decoded = decode(bytes)
        val framed = if (cropToCameraFrame) cropToFrame(decoded, 0.08f, 0.12f, 0.92f, 0.88f) else decoded
        val upright = uprightPage(framed)
        val enhanced = enhanceContrast(upright)
        val jpeg = ByteArrayOutputStream().apply { enhanced.compress(Bitmap.CompressFormat.JPEG, 90, this) }.toByteArray()
        return PreparedPage(bitmap = enhanced, jpeg = jpeg, blurry = blurScore(enhanced) < 6.0)
    }

    /**
     * Printed lines are horizontal. A gallery photo of a book is often stored sideways; EXIF does
     * not fix that. Score each right angle and keep the one that looks most like a page of lines.
     */
    fun uprightPage(source: Bitmap): Bitmap {
        val scored = listOf(0f, 90f, 180f, 270f).map { degrees ->
            val rotated = rotate(source, degrees)
            Triple(degrees, rotated, lineScore(rotated))
        }
        val original = scored.first { it.first == 0f }
        val best = scored.maxBy { it.third }
        // A sideways page jumps in score; a nearly uniform photo should stay as EXIF left it.
        val pool = if (best.third > original.third * 1.12) {
            scored.filter { it.third >= best.third * 0.92f }
        } else {
            listOf(original)
        }
        return pool.maxBy { topMarginWhiteness(it.second) }.second
    }

    fun lineScore(bitmap: Bitmap): Double {
        val sample = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        val rows = DoubleArray(sample.height)
        for (y in 0 until sample.height) {
            var sum = 0.0
            for (x in 0 until sample.width) {
                val p = sample.getPixel(x, y)
                sum += ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3.0
            }
            rows[y] = sum / sample.width
        }
        if (sample !== bitmap) sample.recycle()
        val mean = rows.average()
        return rows.sumOf { value ->
            val d = value - mean
            d * d
        } / rows.size
    }

    fun topMarginWhiteness(bitmap: Bitmap): Double {
        val band = (bitmap.height * 0.12f).toInt().coerceAtLeast(2)
        var sum = 0.0
        var count = 0
        val yEnd = band.coerceAtMost(bitmap.height)
        for (y in 0 until yEnd) {
            for (x in 0 until bitmap.width step 2) {
                val p = bitmap.getPixel(x, y)
                sum += ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3.0
                count++
            }
        }
        return if (count == 0) 0.0 else sum / count
    }

    fun cropToFrame(source: Bitmap, leftRatio: Float, topRatio: Float, rightRatio: Float, bottomRatio: Float): Bitmap {
        val left = (source.width * leftRatio).toInt().coerceIn(0, source.width - 1)
        val top = (source.height * topRatio).toInt().coerceIn(0, source.height - 1)
        val right = (source.width * rightRatio).toInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * bottomRatio).toInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    fun enhanceContrast(source: Bitmap): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(out.width * out.height)
        out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = ((c shr 16) and 0xFF)
            val g = ((c shr 8) and 0xFF)
            val b = (c and 0xFF)
            val contrast = 1.15f
            fun channel(v: Int): Int {
                val scaled = (((v / 255f - 0.5f) * contrast) + 0.5f) * 255f
                return scaled.toInt().coerceIn(0, 255)
            }
            pixels[i] = (c and 0xFF000000.toInt()) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
        }
        out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        return out
    }

    fun blurScore(bitmap: Bitmap): Double {
        val sample = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
        var prev = 0
        var sum = 0.0
        var count = 0
        for (y in 1 until sample.height - 1) {
            for (x in 1 until sample.width - 1) {
                val p = sample.getPixel(x, y)
                val gray = ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3
                sum += abs(gray - prev)
                prev = gray
                count++
            }
        }
        if (sample !== bitmap) sample.recycle()
        return if (count == 0) 0.0 else sum / count
    }

    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        if (abs(degrees) < 1f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    data class PreparedPage(
        val bitmap: Bitmap,
        val jpeg: ByteArray,
        val blurry: Boolean,
    )

    private fun sampleSize(longest: Int, max: Int): Int {
        var sample = 1
        while (longest / sample > max * 2) sample *= 2
        return sample
    }
}

package com.multilingualbookreader.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlin.math.abs
import kotlin.math.max

object PageImageProcessor {
    fun decode(bytes: ByteArray, maxWidth: Int = 1600): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = sampleSize(max(bounds.outWidth, bounds.outHeight), maxWidth)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
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

    private fun sampleSize(longest: Int, max: Int): Int {
        var sample = 1
        while (longest / sample > max * 2) sample *= 2
        return sample
    }
}

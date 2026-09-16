package com.multilingualbookreader.image

/**
 * EXIF orientation handling for camera captures.
 *
 * BitmapFactory ignores the EXIF tag, so a photo taken in portrait decodes sideways. Text rotated
 * 90 degrees is largely unreadable to the recognisers, which is why captures came back empty.
 */
object ImageOrientation {
    const val NORMAL = 1
    const val ROTATE_180 = 3
    const val ROTATE_90 = 6
    const val ROTATE_270 = 8
    const val TRANSPOSE = 5
    const val TRANSVERSE = 7
    const val FLIP_HORIZONTAL = 2
    const val FLIP_VERTICAL = 4

    /** Clockwise degrees needed to view the image upright. */
    fun degreesFor(exifOrientation: Int): Float = when (exifOrientation) {
        ROTATE_90, TRANSPOSE -> 90f
        ROTATE_180 -> 180f
        ROTATE_270, TRANSVERSE -> 270f
        else -> 0f
    }
}

package com.multilingualbookreader.image

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImageOrientationTest {
    @Test
    fun portraitCapturesAreRotatedUpright() {
        assertThat(ImageOrientation.degreesFor(ImageOrientation.ROTATE_90)).isEqualTo(90f)
        assertThat(ImageOrientation.degreesFor(ImageOrientation.ROTATE_180)).isEqualTo(180f)
        assertThat(ImageOrientation.degreesFor(ImageOrientation.ROTATE_270)).isEqualTo(270f)
    }

    @Test
    fun transposedCapturesUseTheirRotationComponent() {
        assertThat(ImageOrientation.degreesFor(ImageOrientation.TRANSPOSE)).isEqualTo(90f)
        assertThat(ImageOrientation.degreesFor(ImageOrientation.TRANSVERSE)).isEqualTo(270f)
    }

    @Test
    fun uprightAndUnknownTagsAreLeftAlone() {
        assertThat(ImageOrientation.degreesFor(ImageOrientation.NORMAL)).isEqualTo(0f)
        assertThat(ImageOrientation.degreesFor(ImageOrientation.FLIP_HORIZONTAL)).isEqualTo(0f)
        assertThat(ImageOrientation.degreesFor(0)).isEqualTo(0f)
        assertThat(ImageOrientation.degreesFor(99)).isEqualTo(0f)
    }
}

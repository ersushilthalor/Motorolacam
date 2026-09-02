package com.example

import com.example.camera.model.CameraResolution
import com.example.camera.model.LensInfo
import com.example.camera.model.LensType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCameraResolutionCalculations() {
        val res43 = CameraResolution(4000, 3000)
        assertEquals(12.0f, res43.megapixels, 0.01f)
        assertEquals("4:3", res43.aspectRatioLabel)

        val res169 = CameraResolution(3840, 2160)
        assertEquals(8.29f, res169.megapixels, 0.02f)
        assertEquals("16:9", res169.aspectRatioLabel)

        val res209 = CameraResolution(2400, 1080)
        assertEquals("20:9", res209.aspectRatioLabel)
    }

    @Test
    fun testLensInfoTypes() {
        val ultraWide = LensInfo(
            cameraId = "2",
            facing = android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK,
            lensType = LensType.ULTRAWIDE,
            displayName = "0.5x Ultra Wide",
            focalLengthMm = 1.94f,
            maxAperture = 2.2f,
            isPhysical = true,
            isHiddenAux = true,
            fovDegrees = 118f
        )
        assertEquals("0.5x", ultraWide.lensType.shortLabel)
        assertEquals("Ultra Wide", ultraWide.lensType.fullLabel)
        assertTrue(ultraWide.isPhysical)
        assertTrue(ultraWide.isHiddenAux)
    }
}

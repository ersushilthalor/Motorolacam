package com.example.camera.model

import android.graphics.ImageFormat
import android.util.Size

enum class CameraMode {
    PHOTO,
    VIDEO
}

enum class FlashMode(val title: String) {
    OFF("Off"),
    AUTO("Auto"),
    ON("On"),
    TORCH("Torch")
}

enum class TimerMode(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_5(5, "5s"),
    SEC_10(10, "10s")
}

enum class GridType(val title: String) {
    NONE("Off"),
    THIRDS("3x3 Rule"),
    GOLDEN("Golden Ratio"),
    SQUARE("1:1 Box"),
    LEVEL("Level Horizon")
}

enum class WhiteBalanceMode(val title: String, val camera2Mode: Int) {
    AUTO("Auto", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO),
    INCANDESCENT("Incandescent", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("Fluorescent", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT),
    WARM_FLUORESCENT("Warm Fluor", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT),
    DAYLIGHT("Daylight", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("Cloudy", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    TWILIGHT("Twilight", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_TWILIGHT),
    SHADE("Shade", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_SHADE)
}

enum class FocusMode(val title: String, val camera2Mode: Int) {
    CONTINUOUS("AF-C", android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE),
    AUTO("AF-S", android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO),
    MACRO("Macro", android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_MACRO),
    MANUAL("Manual", android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF)
}

enum class VideoProfileQuality(val title: String, val width: Int, val height: Int) {
    UHD_4K("4K UHD", 3840, 2160),
    FHD_1080P("1080p FHD", 1920, 1080),
    HD_720P("720p HD", 1280, 720),
    SD_480P("480p SD", 720, 480)
}

enum class VideoBitrateOption(val title: String, val bps: Int) {
    AUTO("Auto (Default)", 0),
    STANDARD("Standard (16 Mbps)", 16_000_000),
    HIGH("High (30 Mbps)", 30_000_000),
    MAX("Max (50 Mbps)", 50_000_000)
}

enum class ColorProfile(val title: String, val isFlat: Boolean) {
    STANDARD("Standard", false),
    VIBRANT("Vibrant", false),
    NATURAL("Natural", false),
    FLAT_LOG("Flat / Log", true),
    MONOCHROME("B&W Monochrome", false)
}

data class CameraResolution(
    val width: Int,
    val height: Int,
    val format: Int = ImageFormat.JPEG,
    val isRaw: Boolean = false
) {
    val megapixels: Float
        get() = (width * height) / 1_000_000f

    val aspectRatioLabel: String
        get() {
            val gcd = gcd(width, height)
            val w = width / gcd
            val h = height / gcd
            return when {
                (w == 4 && h == 3) || (w == 3 && h == 4) -> "4:3"
                (w == 16 && h == 9) || (w == 9 && h == 16) -> "16:9"
                (w == 1 && h == 1) -> "1:1"
                (w == 20 && h == 9) || (w == 9 && h == 20) -> "20:9"
                else -> {
                    val ratio = width.toFloat() / height.toFloat()
                    when {
                        kotlin.math.abs(ratio - (4f / 3f)) < 0.05f || kotlin.math.abs(ratio - (3f / 4f)) < 0.05f -> "4:3"
                        kotlin.math.abs(ratio - (16f / 9f)) < 0.05f || kotlin.math.abs(ratio - (9f / 16f)) < 0.05f -> "16:9"
                        kotlin.math.abs(ratio - (20f / 9f)) < 0.05f || kotlin.math.abs(ratio - (9f / 20f)) < 0.05f -> "20:9"
                        else -> "$width x $height"
                    }
                }
            }
        }

    val displayLabel: String
        get() = if (isRaw) {
            "RAW %.1f MP (%dx%d)".format(megapixels, width, height)
        } else {
            "%.1f MP (%s · %dx%d)".format(megapixels, aspectRatioLabel, width, height)
        }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

data class LensInfo(
    val cameraId: String,
    val facing: Int, // CameraCharacteristics.LENS_FACING_BACK, etc.
    val lensType: LensType,
    val displayName: String,
    val focalLengthMm: Float,
    val maxAperture: Float,
    val isPhysical: Boolean = false
)

enum class LensType(val shortLabel: String, val fullLabel: String) {
    ULTRAWIDE("0.5x", "Ultra Wide"),
    WIDE("1x", "Main Wide"),
    TELEPHOTO("2x", "Telephoto"),
    TELEPHOTO_3X("3x", "3x Telephoto"),
    MACRO("Macro", "Macro Lens"),
    FRONT("1x", "Front Selfie")
}

data class HardwareCapabilities(
    val supportsManualSensor: Boolean = false,
    val supportsRaw: Boolean = false,
    val supportsOis: Boolean = false,
    val supportsEis: Boolean = false,
    val supportsFlash: Boolean = false,
    val minIso: Int = 100,
    val maxIso: Int = 3200,
    val minExposureTimeNs: Long = 100_000L, // 0.1ms
    val maxExposureTimeNs: Long = 1_000_000_000L, // 1s
    val minExposureCompensation: Int = -4,
    val maxExposureCompensation: Int = 4,
    val exposureCompensationStep: Float = 0.333f,
    val minFocusDistance: Float = 10f, // diopters
    val supportedAwbModes: List<WhiteBalanceMode> = emptyList(),
    val supportedAfModes: List<FocusMode> = emptyList(),
    val supportedPhotoResolutions: List<CameraResolution> = emptyList(),
    val supportedRawResolutions: List<CameraResolution> = emptyList(),
    val supportedVideoResolutions: List<CameraResolution> = emptyList(),
    val supportedFpsRanges: List<Int> = listOf(30, 60),
    val supportsTonemapCurve: Boolean = false,
    val maxZoom: Float = 8f
)

data class StorageStats(
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val freeGb: Float = 0f,
    val estimatedPhotos: Int = 0,
    val estimatedVideoMinutes: Int = 0
)

data class CapturedMedia(
    val uri: android.net.Uri,
    val isVideo: Boolean,
    val timestamp: Long,
    val displayName: String
)

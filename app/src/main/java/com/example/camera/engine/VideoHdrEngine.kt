package com.example.camera.engine

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.TonemapCurve
import android.os.Build
import android.util.Log
import com.example.camera.model.VideoHdrMode
import com.example.camera.model.VideoHdrState
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Real-Time Video HDR & Adaptive Noise Reduction Engine for Camera2.
 *
 * Capabilities:
 * 1. Intelligently analyzes scene lighting, photometric EV, ISO, and motion per frame.
 * 2. Computes dynamic S-curve tonemapping: lifting deep shadows, preserving midtone contrast,
 *    and compressing specular highlights to prevent clipping.
 * 3. Applies aggressive live adaptive spatial + temporal noise reduction in dark / high-ISO scenes.
 * 4. Motion-aware processing prevents ghosting and smearing on moving subjects.
 * 5. Multi-frame exponential temporal smoothing prevents brightness pumping, flickering,
 *    or contrast fluttering.
 * 6. Directly applies hardware ISP controls (TonemapCurve, Gamma, Noise Reduction, Edge Mode,
 *    Scene Mode) to the Camera2 CaptureRequest, affecting BOTH the live viewfinder and recorded video.
 */
class VideoHdrEngine {

    companion object {
        private const val TAG = "VideoHdrEngine"
        private const val CURVE_POINTS = 64
        private const val TEMPORAL_SMOOTHING_ALPHA = 0.08f // Multi-frame EMA factor for anti-pumping
        private const val MOTION_THRESHOLD_DELTA_ISO = 250
        private const val MOTION_THRESHOLD_FOCUS_DELTA = 0.8f
    }

    // Configuration
    var mode: VideoHdrMode = VideoHdrMode.AUTO
        set(value) {
            field = value
            if (value == VideoHdrMode.OFF) {
                smoothedShadowLift = 0f
                smoothedHighlightProtect = 0f
                smoothedContrast = 1.0f
                smoothedNoiseReduction = 0f
            }
            updateState()
        }

    var manualIntensity: Int = 50 // 0 to 100
        set(value) {
            field = value.coerceIn(0, 100)
            updateState()
        }

    // Current smoothed parameters (for anti-flicker stability)
    private var smoothedShadowLift: Float = 0.35f
    private var smoothedHighlightProtect: Float = 0.40f
    private var smoothedContrast: Float = 1.10f
    private var smoothedNoiseReduction: Float = 0.30f
    private var smoothedEv: Float = 10f
    private var smoothedIso: Float = 200f

    // Consecutive frame tracking for motion-aware temporal processing
    private var lastIso: Int = 200
    private var lastExposureTimeNs: Long = 10_000_000L
    private var lastFocusDistance: Float = 0f
    private var lastTimestampNs: Long = 0L
    private var isMotionDetected: Boolean = false

    // Hardware capability cache
    private var supportsContrastCurve: Boolean = false
    private var supportsGammaValue: Boolean = false
    private var supportsSceneHdr: Boolean = false
    private var supportsHighQualityNr: Boolean = false
    private var supportsHighQualityEdge: Boolean = false
    private var tonemapMaxPoints: Int = CURVE_POINTS

    // Pre-allocated curve buffers for zero garbage collection during video recording
    private val curveRed = FloatArray(CURVE_POINTS * 2)
    private val curveGreen = FloatArray(CURVE_POINTS * 2)
    private val curveBlue = FloatArray(CURVE_POINTS * 2)

    // Public State for UI observation
    var currentState: VideoHdrState = VideoHdrState()
        private set

    var onStateChangedListener: ((VideoHdrState) -> Unit)? = null

    /**
     * Inspect CameraCharacteristics to determine supported hardware ISP features.
     */
    fun onCameraConfigured(chars: CameraCharacteristics) {
        val tonemapModes = chars.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES) ?: intArrayOf()
        supportsContrastCurve = tonemapModes.contains(CameraCharacteristics.TONEMAP_MODE_CONTRAST_CURVE)
        supportsGammaValue = tonemapModes.contains(CameraCharacteristics.TONEMAP_MODE_GAMMA_VALUE)
        tonemapMaxPoints = chars.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS) ?: CURVE_POINTS

        val sceneModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) ?: intArrayOf()
        supportsSceneHdr = sceneModes.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR)

        val nrModes = chars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES) ?: intArrayOf()
        supportsHighQualityNr = nrModes.contains(CameraCharacteristics.NOISE_REDUCTION_MODE_HIGH_QUALITY)

        val edgeModes = chars.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES) ?: intArrayOf()
        supportsHighQualityEdge = edgeModes.contains(CameraCharacteristics.EDGE_MODE_HIGH_QUALITY)

        Log.d(TAG, "Configured: contrastCurve=$supportsContrastCurve, gamma=$supportsGammaValue, " +
                "sceneHdr=$supportsSceneHdr, hqNr=$supportsHighQualityNr, hqEdge=$supportsHighQualityEdge")
        updateState()
    }

    /**
     * Process TotalCaptureResult from onCaptureCompleted for real-time scene analysis.
     * Returns true if request settings should be updated.
     */
    fun onFrameCaptured(result: TotalCaptureResult): Boolean {
        if (mode == VideoHdrMode.OFF) {
            return false
        }

        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: 200
        val exposureNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 10_000_000L
        val aperture = result.get(CaptureResult.LENS_APERTURE) ?: 1.8f
        val focusDist = result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: 0f
        val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: System.nanoTime()

        return processFrameValues(iso, exposureNs, aperture, focusDist, timestamp)
    }

    /**
     * Process raw sensor values for frame-to-frame analysis and testing.
     */
    fun processFrameValues(
        iso: Int,
        exposureNs: Long,
        aperture: Float,
        focusDist: Float,
        timestamp: Long = System.nanoTime()
    ): Boolean {
        if (mode == VideoHdrMode.OFF) {
            updateState()
            return false
        }

        // 1. Motion-Aware Analysis across consecutive frames
        val deltaIso = abs(iso - lastIso)
        val deltaFocus = abs(focusDist - lastFocusDistance)
        val deltaExposure = abs(exposureNs - lastExposureTimeNs)

        // If sudden large changes occur between consecutive frames, scene or camera is in rapid motion
        isMotionDetected = (deltaIso > MOTION_THRESHOLD_DELTA_ISO) ||
                (deltaFocus > MOTION_THRESHOLD_FOCUS_DELTA) ||
                (deltaExposure > (lastExposureTimeNs / 2))

        lastIso = iso
        lastExposureTimeNs = exposureNs
        lastFocusDistance = focusDist
        lastTimestampNs = timestamp

        // 2. Photometric EV Calculation:
        // EV = log2(N^2 / t) - log2(ISO / 100)
        val exposureSeconds = (exposureNs / 1_000_000_000.0).coerceAtLeast(0.00001)
        val log2 = { v: Double -> ln(v) / ln(2.0) }
        val ev = (log2((aperture * aperture) / exposureSeconds) - log2((iso / 100.0).coerceAtLeast(0.1))).toFloat()

        // 3. Temporal Smoothing (Anti-pumping & Anti-flicker EMA filter)
        // Adapt alpha slightly if rapid scene change is detected, but keep low to prevent flicker
        val alpha = if (isMotionDetected) 0.14f else TEMPORAL_SMOOTHING_ALPHA
        smoothedEv = smoothedEv * (1f - alpha) + ev * alpha
        smoothedIso = smoothedIso * (1f - alpha) + iso.toFloat() * alpha

        // 4. Scene-Adaptive Dynamic Range & Noise Reduction targets
        val targetShadowLift: Float
        val targetHighlightProtect: Float
        val targetContrast: Float
        val targetNoiseReduction: Float

        when (mode) {
            VideoHdrMode.AUTO -> {
                // Determine parameters according to lighting condition and ISO
                when {
                    // Dark / Low-Light / High-ISO (EV < 4 or ISO > 800)
                    smoothedIso > 800 || smoothedEv < 4f -> {
                        val lowLightFactor = ((smoothedIso - 400f) / 2800f).coerceIn(0f, 1f)
                        targetShadowLift = 0.45f + (lowLightFactor * 0.35f) // 0.45 .. 0.80
                        targetHighlightProtect = 0.30f
                        targetContrast = 1.05f + (lowLightFactor * 0.10f)
                        // Aggressive live adaptive noise reduction in dark/high-ISO scenes
                        targetNoiseReduction = 0.50f + (lowLightFactor * 0.50f) // up to 1.0f
                    }
                    // Bright daylight / High Dynamic Range scenes (EV > 11)
                    smoothedEv > 11f -> {
                        targetShadowLift = 0.40f
                        targetHighlightProtect = 0.60f // strong highlight compression for skies & sunlight
                        targetContrast = 1.18f
                        targetNoiseReduction = 0.15f // keep sharp, noise is very low
                    }
                    // Balanced indoor / overcast (EV 4 .. 11)
                    else -> {
                        targetShadowLift = 0.35f
                        targetHighlightProtect = 0.40f
                        targetContrast = 1.10f
                        targetNoiseReduction = 0.25f
                    }
                }
            }
            VideoHdrMode.MANUAL -> {
                // User-controlled dynamic range intensity (0 to 100)
                val normIntensity = manualIntensity / 100f
                targetShadowLift = normIntensity * 0.75f
                targetHighlightProtect = normIntensity * 0.65f
                targetContrast = 1.0f + (normIntensity * 0.25f)
                // Noise reduction remains independently adaptive to actual ISO levels
                val isoNoiseBase = ((smoothedIso - 200f) / 3000f).coerceIn(0.1f, 1.0f)
                targetNoiseReduction = isoNoiseBase
            }
            VideoHdrMode.OFF -> {
                targetShadowLift = 0f
                targetHighlightProtect = 0f
                targetContrast = 1.0f
                targetNoiseReduction = 0f
            }
        }

        // Apply smooth temporal blending to parameters
        smoothedShadowLift = smoothedShadowLift * (1f - alpha) + targetShadowLift * alpha
        smoothedHighlightProtect = smoothedHighlightProtect * (1f - alpha) + targetHighlightProtect * alpha
        smoothedContrast = smoothedContrast * (1f - alpha) + targetContrast * alpha
        smoothedNoiseReduction = smoothedNoiseReduction * (1f - alpha) + targetNoiseReduction * alpha

        updateState()
        return true
    }

    private fun updateState() {
        val desc = when (mode) {
            VideoHdrMode.OFF -> "HDR: OFF"
            VideoHdrMode.AUTO -> {
                val condition = when {
                    smoothedIso > 800 || smoothedEv < 4f -> "Night/Low-Light Boost"
                    smoothedEv > 11f -> "High Dynamic Range"
                    else -> "Balanced Tone"
                }
                "HDR Auto · $condition (Shadow +${(smoothedShadowLift * 100).toInt()}%)"
            }
            VideoHdrMode.MANUAL -> "HDR Manual ($manualIntensity%) · Shadow +${(smoothedShadowLift * 100).toInt()}%"
        }

        currentState = VideoHdrState(
            mode = mode,
            manualIntensity = manualIntensity,
            isHdrActive = mode != VideoHdrMode.OFF,
            currentStrength = if (mode == VideoHdrMode.OFF) 0f else if (mode == VideoHdrMode.MANUAL) manualIntensity / 100f else smoothedShadowLift,
            shadowLift = if (mode == VideoHdrMode.OFF) 0f else smoothedShadowLift,
            highlightProtection = if (mode == VideoHdrMode.OFF) 0f else smoothedHighlightProtect,
            contrastFactor = if (mode == VideoHdrMode.OFF) 1.0f else smoothedContrast,
            noiseReductionStrength = if (mode == VideoHdrMode.OFF) 0f else smoothedNoiseReduction,
            currentIso = smoothedIso.toInt(),
            estimatedEv = smoothedEv,
            isMotionDetected = isMotionDetected,
            statusDescription = desc
        )
        onStateChangedListener?.invoke(currentState)
    }

    /**
     * Apply real-time HDR and Noise Reduction controls to Camera2 CaptureRequest.Builder.
     * These settings are processed directly by the camera ISP on ALL target surfaces
     * (viewfinder SurfaceTexture + MediaRecorder surface).
     */
    fun applyToCaptureRequest(builder: CaptureRequest.Builder) {
        if (mode == VideoHdrMode.OFF) {
            // Restore default linear tonemapping and fast noise reduction
            builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST)
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST)
            builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST)
            if (supportsSceneHdr) {
                builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED)
            }
            return
        }

        // 1. Hardware Scene Mode HDR (if supported by device)
        if (supportsSceneHdr) {
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR)
        }

        // 2. Dynamic HDR Tonemap S-Curve Synthesis
        if (supportsContrastCurve) {
            val tonemapCurve = generateHdrTonemapCurve(
                shadowLift = smoothedShadowLift,
                highlightProtect = smoothedHighlightProtect,
                contrast = smoothedContrast
            )
            builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)
            builder.set(CaptureRequest.TONEMAP_CURVE, tonemapCurve)
        } else if (supportsGammaValue) {
            // Adaptive gamma curve fallback: lifting shadows naturally without washing out blacks
            val adaptiveGamma = (2.2f - (smoothedShadowLift * 0.6f)).coerceIn(1.6f, 2.4f)
            builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_GAMMA_VALUE)
            builder.set(CaptureRequest.TONEMAP_GAMMA, adaptiveGamma)
        } else {
            builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_HIGH_QUALITY)
        }

        // 3. Aggressive Live Adaptive Noise Reduction (Spatial + Temporal)
        // When ISO > 600 or noise reduction target is high, switch to High-Quality spatial + temporal ISP filter
        if (smoothedNoiseReduction > 0.35f && supportsHighQualityNr) {
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
            builder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY)
            builder.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_HIGH_QUALITY)

            // Motion-aware edge processing:
            // If rapid motion is occurring, use FAST edge mode to prevent ghosting or smearing artifacts.
            // If camera/subject is stable, use HIGH_QUALITY edge mode for crisp detail retention.
            if (isMotionDetected) {
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST)
            } else if (supportsHighQualityEdge) {
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            }
        } else {
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST)
            if (supportsHighQualityEdge) {
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            }
        }
    }

    /**
     * Synthesizes a high-precision 64-point S-curve tonemapping curve for Camera2.
     * Monotonically smooth:
     * - Deep shadows lifted naturally
     * - Midtones contrast preserved
     * - Highlights softly compressed to eliminate blown-out clipping
     */
    private fun generateHdrTonemapCurve(
        shadowLift: Float,
        highlightProtect: Float,
        contrast: Float
    ): TonemapCurve {
        val points = CURVE_POINTS
        var prevOut = 0f

        for (i in 0 until points) {
            val inVal = i.toFloat() / (points - 1).toFloat()

            // S-curve synthesis:
            // 1. Shadow lifting function (power-bezier taper)
            val shadowBoost = shadowLift * inVal * (1f - inVal).pow(2f) * 2.0f
            val baseVal = (inVal + shadowBoost).coerceIn(0f, 1f)

            // 2. Midtone contrast response (filmic power curve)
            val p = contrast.coerceIn(0.9f, 1.4f)
            val vPow = baseVal.pow(p)
            val midVal = if (baseVal <= 0f) 0f else vPow / (vPow + (1f - baseVal).pow(p))

            // 3. Highlight compression shoulder (protect specular highlights from clipping)
            val shoulder = 1f + (highlightProtect * 0.75f)
            val finalVal = (1f - (1f - midVal).pow(shoulder)).coerceIn(0f, 1f)

            // Ensure strictly monotonic non-decreasing output
            val monotonicOut = max(prevOut, finalVal).coerceIn(0f, 1f)
            prevOut = monotonicOut

            val idx = i * 2
            curveRed[idx] = inVal
            curveRed[idx + 1] = monotonicOut

            curveGreen[idx] = inVal
            curveGreen[idx + 1] = monotonicOut

            curveBlue[idx] = inVal
            curveBlue[idx + 1] = monotonicOut
        }

        return TonemapCurve(curveRed, curveGreen, curveBlue)
    }
}

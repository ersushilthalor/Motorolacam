package com.example.camera.engine

import android.graphics.Rect
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.Face
import com.example.camera.model.DollyDirection
import com.example.camera.model.DollyZoomState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Real Dolly Zoom (Vertigo Effect) Computation Engine.
 *
 * Automatically compensates digital/optical zoom as camera distance to subject changes,
 * keeping the foreground subject at a constant framing scale while dynamically expanding
 * or compressing the background perspective.
 */
class DollyZoomEngine {

    private val _dollyState = MutableStateFlow(DollyZoomState())
    val dollyState: StateFlow<DollyZoomState> = _dollyState.asStateFlow()

    // Calibration references
    private var referenceSubjectScale: Float = 0f
    private var referenceDistanceMeters: Float = 1.0f
    private var referenceZoom: Float = 1.0f
    private var currentSmoothedZoom: Float = 1.0f

    // Smoothing factor (EMA) to ensure continuous, silky transitions with zero jumps
    private val smoothingFactor = 0.15f

    /**
     * Locks and calibrates on current subject scale and distance as baseline.
     */
    fun calibrate(currentZoom: Float, currentFace: Face?, lensFocusDiopters: Float, sensorRect: Rect?) {
        referenceZoom = currentZoom.coerceIn(1.0f, 8.0f)
        currentSmoothedZoom = referenceZoom

        val dist = if (lensFocusDiopters > 0.01f) {
            (1.0f / lensFocusDiopters).coerceIn(0.2f, 15.0f)
        } else {
            1.2f
        }
        referenceDistanceMeters = dist

        if (currentFace != null && sensorRect != null && sensorRect.width() > 0) {
            referenceSubjectScale = currentFace.bounds.width().toFloat() / sensorRect.width().toFloat()
        } else {
            referenceSubjectScale = 0.25f
        }

        _dollyState.value = _dollyState.value.copy(
            isCalibrated = true,
            targetDistanceMeters = referenceDistanceMeters,
            initialZoom = referenceZoom,
            currentDistanceMeters = referenceDistanceMeters,
            targetZoom = referenceZoom,
            smoothedZoom = currentSmoothedZoom,
            statusPrompt = "Subject Locked · Walk forward or backward slowly"
        )
    }

    /**
     * Resets calibration.
     */
    fun reset() {
        referenceSubjectScale = 0f
        referenceDistanceMeters = 1.0f
        referenceZoom = 1.0f
        _dollyState.value = DollyZoomState()
    }

    fun setDirection(direction: DollyDirection) {
        _dollyState.value = _dollyState.value.copy(direction = direction)
    }

    /**
     * Called on each Camera2 TotalCaptureResult frame.
     * Returns the new smoothed zoom ratio to apply to CaptureRequest, or null if uncalibrated.
     */
    fun processFrame(
        result: CaptureResult,
        sensorRect: Rect?,
        maxAvailableZoom: Float = 8.0f
    ): Float? {
        val state = _dollyState.value
        if (!state.isCalibrated || !state.isTracking) return null

        val diopters = result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: 0f
        val currentDist = if (diopters > 0.01f) {
            (1.0f / diopters).coerceIn(0.2f, 15.0f)
        } else {
            state.currentDistanceMeters
        }

        val faces = result.get(CaptureResult.STATISTICS_FACES)
        val primaryFace = faces?.firstOrNull()

        // Compute target zoom based on face size or lens distance
        val targetZoomRaw: Float = if (primaryFace != null && sensorRect != null && sensorRect.width() > 0 && referenceSubjectScale > 0.01f) {
            val currentScale = primaryFace.bounds.width().toFloat() / sensorRect.width().toFloat()
            // To keep subject constant: if currentScale increases (closer), zoom must decrease
            referenceZoom * (referenceSubjectScale / max(currentScale, 0.01f))
        } else {
            // Lens distance fallback: if distance shrinks, zoom decreases
            referenceZoom * (currentDist / max(referenceDistanceMeters, 0.1f))
        }

        val clampedTarget = targetZoomRaw.coerceIn(1.0f, maxAvailableZoom)

        // Exponential Moving Average for smooth zoom without sudden jumps
        currentSmoothedZoom += (clampedTarget - currentSmoothedZoom) * smoothingFactor

        val prompt = when {
            currentDist < referenceDistanceMeters * 0.85f -> "Push-In Active · Background expanding"
            currentDist > referenceDistanceMeters * 1.15f -> "Pull-Out Active · Background compressing"
            else -> "Tracking Subject · Keep moving smoothly"
        }

        _dollyState.value = state.copy(
            currentDistanceMeters = currentDist,
            targetZoom = clampedTarget,
            smoothedZoom = currentSmoothedZoom,
            statusPrompt = prompt
        )

        return currentSmoothedZoom
    }
}

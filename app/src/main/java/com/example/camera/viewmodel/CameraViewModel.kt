package com.example.camera.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.engine.Camera2Engine
import com.example.camera.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProControlTab(val label: String) {
    EXPOSURE("EV"),
    ISO("ISO"),
    SHUTTER("SEC"),
    WB("WB"),
    FOCUS("FOCUS"),
    TONE("TONE")
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val engine = Camera2Engine(application.applicationContext)

    // Mode
    private val _cameraMode = MutableStateFlow(CameraMode.PHOTO)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    // Timer
    private val _timerMode = MutableStateFlow(TimerMode.OFF)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _activeTimerCountdown = MutableStateFlow<Int?>(null)
    val activeTimerCountdown: StateFlow<Int?> = _activeTimerCountdown.asStateFlow()

    // Grid
    private val _gridType = MutableStateFlow(GridType.NONE)
    val gridType: StateFlow<GridType> = _gridType.asStateFlow()

    // Flash
    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    // Manual Pro controls drawer / bar
    private val _isManualProOpen = MutableStateFlow(false)
    val isManualProOpen: StateFlow<Boolean> = _isManualProOpen.asStateFlow()

    private val _activeProTab = MutableStateFlow(ProControlTab.EXPOSURE)
    val activeProTab: StateFlow<ProControlTab> = _activeProTab.asStateFlow()

    // Settings drawer & media viewer
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isMediaViewerOpen = MutableStateFlow(false)
    val isMediaViewerOpen: StateFlow<Boolean> = _isMediaViewerOpen.asStateFlow()

    // Focus indicator ring
    private val _focusRingPoint = MutableStateFlow<Offset?>(null)
    val focusRingPoint: StateFlow<Offset?> = _focusRingPoint.asStateFlow()

    // Toast/Feedback banner
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Parameters
    private val _exposureCompensation = MutableStateFlow(0)
    val exposureCompensation: StateFlow<Int> = _exposureCompensation.asStateFlow()

    private val _manualIso = MutableStateFlow<Int?>(null)
    val manualIso: StateFlow<Int?> = _manualIso.asStateFlow()

    private val _manualShutterSpeedNs = MutableStateFlow<Long?>(null)
    val manualShutterSpeedNs: StateFlow<Long?> = _manualShutterSpeedNs.asStateFlow()

    private val _whiteBalance = MutableStateFlow(WhiteBalanceMode.AUTO)
    val whiteBalance: StateFlow<WhiteBalanceMode> = _whiteBalance.asStateFlow()

    private val _focusMode = MutableStateFlow(FocusMode.CONTINUOUS)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    private val _manualFocusDistance = MutableStateFlow(0f)
    val manualFocusDistance: StateFlow<Float> = _manualFocusDistance.asStateFlow()

    private val _isAeLocked = MutableStateFlow(false)
    val isAeLocked: StateFlow<Boolean> = _isAeLocked.asStateFlow()

    private val _isAfLocked = MutableStateFlow(false)
    val isAfLocked: StateFlow<Boolean> = _isAfLocked.asStateFlow()

    private val _isRawCaptureEnabled = MutableStateFlow(false)
    val isRawCaptureEnabled: StateFlow<Boolean> = _isRawCaptureEnabled.asStateFlow()

    private val _isVideoStabilizationEnabled = MutableStateFlow(true)
    val isVideoStabilizationEnabled: StateFlow<Boolean> = _isVideoStabilizationEnabled.asStateFlow()

    private val _videoBitrateOption = MutableStateFlow(VideoBitrateOption.AUTO)
    val videoBitrateOption: StateFlow<VideoBitrateOption> = _videoBitrateOption.asStateFlow()

    private val _videoFps = MutableStateFlow(30)
    val videoFps: StateFlow<Int> = _videoFps.asStateFlow()

    private val _colorProfile = MutableStateFlow(ColorProfile.STANDARD)
    val colorProfile: StateFlow<ColorProfile> = _colorProfile.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom.asStateFlow()

    private var timerJob: Job? = null
    private var focusDismissJob: Job? = null
    private var toastDismissJob: Job? = null

    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
        engine.setMode(mode)
    }

    fun selectLens(lens: LensInfo) {
        engine.selectLens(lens)
    }

    fun toggleCameraFacing() {
        val currentLens = engine.selectedLens.value ?: return
        val targetFacing = if (currentLens.facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
            android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
        } else {
            android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
        }
        val targetLens = engine.availableLenses.value.firstOrNull { it.facing == targetFacing }
        if (targetLens != null) {
            engine.selectLens(targetLens)
        }
    }

    fun cycleFlashMode() {
        val nextMode = when (_flashMode.value) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.TORCH
            FlashMode.TORCH -> FlashMode.OFF
        }
        _flashMode.value = nextMode
        engine.flashMode = nextMode
        engine.updatePreviewSettings()
        showToast("Flash: ${nextMode.title}")
    }

    fun cycleTimerMode() {
        val nextMode = when (_timerMode.value) {
            TimerMode.OFF -> TimerMode.SEC_3
            TimerMode.SEC_3 -> TimerMode.SEC_5
            TimerMode.SEC_5 -> TimerMode.SEC_10
            TimerMode.SEC_10 -> TimerMode.OFF
        }
        _timerMode.value = nextMode
        showToast("Timer: ${nextMode.label}")
    }

    fun cycleGridType() {
        val nextGrid = when (_gridType.value) {
            GridType.NONE -> GridType.THIRDS
            GridType.THIRDS -> GridType.GOLDEN
            GridType.GOLDEN -> GridType.SQUARE
            GridType.SQUARE -> GridType.LEVEL
            GridType.LEVEL -> GridType.NONE
        }
        _gridType.value = nextGrid
        showToast("Grid: ${nextGrid.title}")
    }

    fun toggleRawCapture() {
        val caps = engine.capabilities.value
        if (!caps.supportsRaw) {
            showToast("RAW format not supported by this sensor")
            return
        }
        val next = !_isRawCaptureEnabled.value
        _isRawCaptureEnabled.value = next
        engine.isRawCaptureEnabled = next
        engine.restartCamera()
        showToast(if (next) "RAW (DNG + JPEG) Enabled" else "RAW Disabled")
    }

    fun toggleManualPro() {
        _isManualProOpen.value = !_isManualProOpen.value
    }

    fun setActiveProTab(tab: ProControlTab) {
        _activeProTab.value = tab
    }

    fun setExposureCompensation(value: Int) {
        _exposureCompensation.value = value
        engine.exposureCompensationIndex = value
        engine.updatePreviewSettings()
    }

    fun setManualIso(iso: Int?) {
        _manualIso.value = iso
        engine.manualIso = iso
        engine.updatePreviewSettings()
    }

    fun setManualShutterSpeedNs(ns: Long?) {
        _manualShutterSpeedNs.value = ns
        engine.manualExposureTimeNs = ns
        engine.updatePreviewSettings()
    }

    fun setWhiteBalance(wb: WhiteBalanceMode) {
        _whiteBalance.value = wb
        engine.whiteBalanceMode = wb
        engine.updatePreviewSettings()
    }

    fun setFocusMode(mode: FocusMode) {
        _focusMode.value = mode
        engine.focusMode = mode
        engine.updatePreviewSettings()
    }

    fun setManualFocusDistance(distance: Float) {
        _manualFocusDistance.value = distance
        engine.manualFocusDistance = distance
        engine.updatePreviewSettings()
    }

    fun toggleAeLock() {
        val next = !_isAeLocked.value
        _isAeLocked.value = next
        engine.isAeLocked = next
        engine.updatePreviewSettings()
        showToast(if (next) "Exposure Locked" else "Exposure Unlocked")
    }

    fun toggleAfLock() {
        val next = !_isAfLocked.value
        _isAfLocked.value = next
        engine.isAfLocked = next
        engine.updatePreviewSettings()
        showToast(if (next) "Focus Locked" else "Focus Unlocked")
    }

    fun setZoom(zoom: Float) {
        _currentZoom.value = zoom
        engine.setZoom(zoom)
    }

    fun setVideoStabilization(enabled: Boolean) {
        val caps = engine.capabilities.value
        if (!caps.supportsEis && !caps.supportsOis) {
            showToast("Stabilization not supported by hardware")
            return
        }
        _isVideoStabilizationEnabled.value = enabled
        engine.isVideoStabilizationEnabled = enabled
        engine.updatePreviewSettings()
        showToast(if (enabled) "Stabilization Enabled" else "Stabilization Disabled")
    }

    fun setVideoBitrate(bitrate: VideoBitrateOption) {
        _videoBitrateOption.value = bitrate
        engine.videoBitrateOption = bitrate
        showToast("Bitrate: ${bitrate.title}")
    }

    fun setVideoFps(fps: Int) {
        _videoFps.value = fps
        engine.videoFps = fps
        showToast("Frame Rate: ${fps} FPS")
    }

    fun setColorProfile(profile: ColorProfile) {
        _colorProfile.value = profile
        engine.colorProfile = profile
        engine.updatePreviewSettings()
        showToast("Profile: ${profile.title}")
    }

    fun toggleAudio() {
        val next = !_isAudioEnabled.value
        _isAudioEnabled.value = next
        engine.isAudioEnabled = next
        showToast(if (next) "Audio Recording On" else "Audio Muted")
    }

    fun selectPhotoResolution(res: CameraResolution) {
        engine.selectPhotoResolution(res)
        showToast("Photo Resolution: ${res.displayLabel}")
    }

    fun selectVideoResolution(res: CameraResolution) {
        engine.selectVideoResolution(res)
        showToast("Video Resolution: ${res.displayLabel}")
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setMediaViewerOpen(open: Boolean) {
        _isMediaViewerOpen.value = open
    }

    fun onTapToFocus(point: Offset, normX: Float, normY: Float) {
        _focusRingPoint.value = point
        engine.triggerFocusAndMeter(normX, normY)

        focusDismissJob?.cancel()
        focusDismissJob = viewModelScope.launch {
            delay(2000)
            _focusRingPoint.value = null
        }
    }

    fun onMainActionButtonClick() {
        if (_cameraMode.value == CameraMode.PHOTO) {
            triggerPhotoCapture()
        } else {
            triggerVideoCapture()
        }
    }

    private fun triggerPhotoCapture() {
        if (engine.isCapturing.value) return

        val timerSeconds = _timerMode.value.seconds
        if (timerSeconds > 0) {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                for (remaining in timerSeconds downTo 1) {
                    _activeTimerCountdown.value = remaining
                    delay(1000)
                }
                _activeTimerCountdown.value = null
                executePhotoCapture()
            }
        } else {
            executePhotoCapture()
        }
    }

    private fun executePhotoCapture() {
        engine.takePhoto { uri ->
            if (uri != null) {
                showToast("Saved to DCIM/Camera")
            } else {
                showToast("Failed to save photo")
            }
        }
    }

    private fun triggerVideoCapture() {
        if (engine.isRecordingVideo.value) {
            engine.stopVideoRecording()
            showToast("Video saved to DCIM/Camera")
        } else {
            engine.startVideoRecording { error ->
                showToast("Recording error: $error")
            }
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
        toastDismissJob?.cancel()
        toastDismissJob = viewModelScope.launch {
            delay(2500)
            _toastMessage.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}

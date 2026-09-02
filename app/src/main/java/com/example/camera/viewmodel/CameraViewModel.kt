package com.example.camera.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.data.CameraPreferences
import com.example.camera.engine.Camera2Engine
import com.example.camera.engine.PortraitProcessor
import com.example.camera.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    private val portraitProcessor by lazy { PortraitProcessor(application.applicationContext) }
    private val preferences = CameraPreferences(application.applicationContext)

    // Mode
    private val _cameraMode = MutableStateFlow(preferences.cameraMode)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    // Portrait Mode Controls & Pipeline State
    private val _portraitConfig = MutableStateFlow(
        PortraitConfig(
            blurStrength = preferences.portraitBlurStrength,
            simulatedAperture = preferences.portraitAperture
        )
    )
    val portraitConfig: StateFlow<PortraitConfig> = _portraitConfig.asStateFlow()

    private val _portraitProcessingState = MutableStateFlow(PortraitProcessingState())
    val portraitProcessingState: StateFlow<PortraitProcessingState> = _portraitProcessingState.asStateFlow()

    // Filtered lenses strictly adhering to current facing:
    // When on Back Camera -> ONLY back lenses (0.5x, 1x, 2x, etc.)
    // When on Front Camera -> ONLY front selfie lens
    val displayedLenses: StateFlow<List<LensInfo>> = combine(
        engine.availableLenses,
        engine.selectedLens
    ) { lenses, selected ->
        val currentFacing = selected?.facing ?: android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
        lenses.filter { it.facing == currentFacing }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Timer
    private val _timerMode = MutableStateFlow(preferences.timerMode)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _activeTimerCountdown = MutableStateFlow<Int?>(null)
    val activeTimerCountdown: StateFlow<Int?> = _activeTimerCountdown.asStateFlow()

    // Grid
    private val _gridType = MutableStateFlow(preferences.gridType)
    val gridType: StateFlow<GridType> = _gridType.asStateFlow()

    // Flash
    private val _flashMode = MutableStateFlow(preferences.flashMode)
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

    private val _whiteBalance = MutableStateFlow(preferences.whiteBalance)
    val whiteBalance: StateFlow<WhiteBalanceMode> = _whiteBalance.asStateFlow()

    private val _focusMode = MutableStateFlow(preferences.focusMode)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    private val _manualFocusDistance = MutableStateFlow(0f)
    val manualFocusDistance: StateFlow<Float> = _manualFocusDistance.asStateFlow()

    private val _isAeLocked = MutableStateFlow(false)
    val isAeLocked: StateFlow<Boolean> = _isAeLocked.asStateFlow()

    private val _isAfLocked = MutableStateFlow(false)
    val isAfLocked: StateFlow<Boolean> = _isAfLocked.asStateFlow()

    private val _isRawCaptureEnabled = MutableStateFlow(preferences.isRawEnabled)
    val isRawCaptureEnabled: StateFlow<Boolean> = _isRawCaptureEnabled.asStateFlow()

    private val _isVideoStabilizationEnabled = MutableStateFlow(preferences.isVideoStabilizationEnabled)
    val isVideoStabilizationEnabled: StateFlow<Boolean> = _isVideoStabilizationEnabled.asStateFlow()

    private val _videoBitrateOption = MutableStateFlow(preferences.videoBitrate)
    val videoBitrateOption: StateFlow<VideoBitrateOption> = _videoBitrateOption.asStateFlow()

    private val _videoFps = MutableStateFlow(preferences.videoFps)
    val videoFps: StateFlow<Int> = _videoFps.asStateFlow()

    private val _colorProfile = MutableStateFlow(preferences.colorProfile)
    val colorProfile: StateFlow<ColorProfile> = _colorProfile.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(preferences.isAudioEnabled)
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom.asStateFlow()

    // Active Video Quality (4K 30, 4K 60, 1080p 30, 1080p 60, 720p 30)
    val currentVideoQuality: StateFlow<VideoQualityOption> = combine(
        engine.selectedVideoResolution,
        _videoFps
    ) { res, fps ->
        when {
            res?.width == 3840 && fps == 60 -> VideoQualityOption.UHD_4K_60
            res?.width == 3840 -> VideoQualityOption.UHD_4K_30
            res?.width == 1920 && fps == 60 -> VideoQualityOption.FHD_1080_60
            res?.width == 1920 -> VideoQualityOption.FHD_1080_30
            res?.width == 1280 -> VideoQualityOption.HD_720_30
            else -> VideoQualityOption.UHD_4K_30
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VideoQualityOption.UHD_4K_30)

    private var timerJob: Job? = null
    private var focusDismissJob: Job? = null
    private var toastDismissJob: Job? = null

    init {
        // Apply restored preferences into engine
        engine.flashMode = preferences.flashMode
        engine.isRawCaptureEnabled = preferences.isRawEnabled
        engine.isVideoStabilizationEnabled = preferences.isVideoStabilizationEnabled
        engine.videoBitrateOption = preferences.videoBitrate
        engine.videoFps = preferences.videoFps
        engine.colorProfile = preferences.colorProfile
        engine.isAudioEnabled = preferences.isAudioEnabled
        engine.whiteBalanceMode = preferences.whiteBalance
        engine.focusMode = preferences.focusMode
        engine.setMode(preferences.cameraMode)
        engine.selectVideoResolution(CameraResolution(preferences.videoWidth, preferences.videoHeight))
    }

    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
        preferences.cameraMode = mode
        engine.setMode(mode)
    }

    fun selectLens(lens: LensInfo) {
        engine.selectLens(lens)
        preferences.lastFacing = lens.facing
    }

    fun forceDeepScanLenses() {
        val count = engine.detectHardwareLenses(forceDeepScan = true)
        showToast("Deep scan found $count hardware & aux lenses")
    }

    fun toggleCameraFacing() {
        val currentLens = engine.selectedLens.value ?: return
        val targetFacing = if (currentLens.facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
            android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
        } else {
            android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
        }
        val targetLens = engine.availableLenses.value.firstOrNull {
            it.facing == targetFacing && (it.lensType == LensType.WIDE || it.lensType == LensType.FRONT) && !it.isZoomPreset
        } ?: engine.availableLenses.value.firstOrNull { it.facing == targetFacing }

        if (targetLens != null) {
            engine.selectLens(targetLens)
            preferences.lastFacing = targetFacing
            val label = if (targetFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) "Front Camera" else "Rear Camera"
            showToast("Switched to $label")
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
        preferences.flashMode = nextMode
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
        preferences.timerMode = nextMode
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
        preferences.gridType = nextGrid
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
        preferences.isRawEnabled = next
        engine.isRawCaptureEnabled = next
        engine.restartCamera()
        showToast(if (next) "RAW (DNG + JPEG) Enabled" else "RAW Disabled")
    }

    fun cycleVideoQuality() {
        val supportedQualities = listOf(
            VideoQualityOption.UHD_4K_30,
            VideoQualityOption.UHD_4K_60,
            VideoQualityOption.FHD_1080_30,
            VideoQualityOption.FHD_1080_60,
            VideoQualityOption.HD_720_30
        )
        val currentIndex = supportedQualities.indexOf(currentVideoQuality.value).let { if (it >= 0) it else 0 }
        val nextQuality = supportedQualities[(currentIndex + 1) % supportedQualities.size]
        setVideoQuality(nextQuality)
    }

    fun setVideoQuality(quality: VideoQualityOption) {
        engine.selectVideoResolution(quality.resolution)
        engine.videoFps = quality.fps
        _videoFps.value = quality.fps
        preferences.videoWidth = quality.width
        preferences.videoHeight = quality.height
        preferences.videoFps = quality.fps
        showToast("Video Quality: ${quality.fullLabel}")
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
        preferences.whiteBalance = wb
        engine.whiteBalanceMode = wb
        engine.updatePreviewSettings()
        showToast("WB: ${wb.title}")
    }

    fun setFocusMode(mode: FocusMode) {
        _focusMode.value = mode
        preferences.focusMode = mode
        engine.focusMode = mode
        engine.updatePreviewSettings()
        showToast("Focus: ${mode.title}")
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
        preferences.isVideoStabilizationEnabled = enabled
        engine.isVideoStabilizationEnabled = enabled
        engine.updatePreviewSettings()
        showToast(if (enabled) "Stabilization Enabled" else "Stabilization Disabled")
    }

    fun setVideoBitrate(bitrate: VideoBitrateOption) {
        _videoBitrateOption.value = bitrate
        preferences.videoBitrate = bitrate
        engine.videoBitrateOption = bitrate
        showToast("Bitrate: ${bitrate.title}")
    }

    fun setVideoFps(fps: Int) {
        _videoFps.value = fps
        preferences.videoFps = fps
        engine.videoFps = fps
        showToast("Frame Rate: ${fps} FPS")
    }

    fun setColorProfile(profile: ColorProfile) {
        _colorProfile.value = profile
        preferences.colorProfile = profile
        engine.colorProfile = profile
        engine.updatePreviewSettings()
        showToast("Profile: ${profile.title}")
    }

    fun toggleAudio() {
        val next = !_isAudioEnabled.value
        _isAudioEnabled.value = next
        preferences.isAudioEnabled = next
        engine.isAudioEnabled = next
        showToast(if (next) "Audio Recording On" else "Audio Muted")
    }

    fun selectPhotoResolution(res: CameraResolution) {
        engine.selectPhotoResolution(res)
        showToast("Photo Resolution: ${res.displayLabel}")
    }

    fun selectVideoResolution(res: CameraResolution) {
        engine.selectVideoResolution(res)
        preferences.videoWidth = res.width
        preferences.videoHeight = res.height
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

    fun setPortraitBlurStrength(strength: Float) {
        _portraitConfig.update { it.copy(blurStrength = strength) }
        preferences.portraitBlurStrength = strength
    }

    fun setPortraitAperture(aperture: String) {
        _portraitConfig.update { it.copy(simulatedAperture = aperture) }
        preferences.portraitAperture = aperture
        showToast("Aperture: $aperture")
    }

    fun setPortraitBokehStyle(style: BokehStyle) {
        _portraitConfig.update { it.copy(bokehStyle = style) }
        showToast("Bokeh: ${style.label}")
    }

    fun togglePortraitFaceEnhancement() {
        val next = !_portraitConfig.value.faceEnhancement
        _portraitConfig.update { it.copy(faceEnhancement = next) }
        showToast("Face Enhancement: ${if (next) "ON" else "OFF"}")
    }

    fun togglePortraitSkinTone() {
        val next = !_portraitConfig.value.skinToneCorrection
        _portraitConfig.update { it.copy(skinToneCorrection = next) }
        showToast("Skin Tone Correction: ${if (next) "ON" else "OFF"}")
    }

    fun onMainActionButtonClick() {
        when (_cameraMode.value) {
            CameraMode.PHOTO -> triggerPhotoCapture()
            CameraMode.PORTRAIT -> triggerPortraitCapture()
            CameraMode.VIDEO -> triggerVideoCapture()
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

    private fun triggerPortraitCapture() {
        if (engine.isCapturing.value || _portraitProcessingState.value.isProcessing) return

        val timerSeconds = _timerMode.value.seconds
        if (timerSeconds > 0) {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                for (remaining in timerSeconds downTo 1) {
                    _activeTimerCountdown.value = remaining
                    delay(1000)
                }
                _activeTimerCountdown.value = null
                executePortraitCapture()
            }
        } else {
            executePortraitCapture()
        }
    }

    private fun executePortraitCapture() {
        _portraitProcessingState.value = PortraitProcessingState(
            isProcessing = true,
            progress = 0.05f,
            statusText = "Capturing full-resolution frame..."
        )

        engine.captureStillBitmap { capturedBitmap ->
            if (capturedBitmap == null) {
                _portraitProcessingState.value = PortraitProcessingState(isProcessing = false)
                showToast("Portrait capture failed")
                return@captureStillBitmap
            }

            viewModelScope.launch {
                val config = _portraitConfig.value
                val uri = portraitProcessor.processAndSavePortrait(
                    orientedBitmap = capturedBitmap,
                    config = config,
                    onProgress = { progress, status ->
                        _portraitProcessingState.value = PortraitProcessingState(
                            isProcessing = true,
                            progress = progress,
                            statusText = status
                        )
                    }
                )

                _portraitProcessingState.value = PortraitProcessingState(isProcessing = false)
                if (uri != null) {
                    showToast("Portrait saved to DCIM/Camera")
                    engine.updateStorageStats()
                } else {
                    showToast("Portrait processing failed")
                }
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

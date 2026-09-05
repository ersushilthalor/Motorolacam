package com.example.camera.engine

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodecInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.example.camera.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val TAG = "Camera2Engine"

class Camera2Engine(private val context: Context) {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // Background threads
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Camera instances
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    // Surfaces & Readers
    private var previewSurfaceTexture: SurfaceTexture? = null
    private var previewSurface: Surface? = null
    private var imageReaderJpeg: ImageReader? = null
    private var imageReaderRaw: ImageReader? = null
    private var mediaRecorder: MediaRecorder? = null
    private var videoRecordingFileDescriptor: ParcelFileDescriptor? = null
    private var currentVideoUri: Uri? = null

    // State Flows
    private val _availableLenses = MutableStateFlow<List<LensInfo>>(emptyList())
    val availableLenses: StateFlow<List<LensInfo>> = _availableLenses.asStateFlow()

    private val _selectedLens = MutableStateFlow<LensInfo?>(null)
    val selectedLens: StateFlow<LensInfo?> = _selectedLens.asStateFlow()

    private val _capabilities = MutableStateFlow(HardwareCapabilities())
    val capabilities: StateFlow<HardwareCapabilities> = _capabilities.asStateFlow()

    private val _selectedPhotoResolution = MutableStateFlow<CameraResolution?>(null)
    val selectedPhotoResolution: StateFlow<CameraResolution?> = _selectedPhotoResolution.asStateFlow()

    private val _selectedVideoResolution = MutableStateFlow<CameraResolution?>(null)
    val selectedVideoResolution: StateFlow<CameraResolution?> = _selectedVideoResolution.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _isRecordingVideo = MutableStateFlow(false)
    val isRecordingVideo: StateFlow<Boolean> = _isRecordingVideo.asStateFlow()

    private val _videoDurationSeconds = MutableStateFlow(0)
    val videoDurationSeconds: StateFlow<Int> = _videoDurationSeconds.asStateFlow()

    private val _lastCapturedMedia = MutableStateFlow<CapturedMedia?>(null)
    val lastCapturedMedia: StateFlow<CapturedMedia?> = _lastCapturedMedia.asStateFlow()

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _previewAspectRatio = MutableStateFlow(4f / 3f)
    val previewAspectRatio: StateFlow<Float> = _previewAspectRatio.asStateFlow()

    private var videoTimerJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Capture Settings State
    var currentMode: CameraMode = CameraMode.PHOTO
    var flashMode: FlashMode = FlashMode.OFF
    var whiteBalanceMode: WhiteBalanceMode = WhiteBalanceMode.AUTO
    var focusMode: FocusMode = FocusMode.CONTINUOUS
    var manualFocusDistance: Float = 0f // 0 = infinity, max = closest
    var manualIso: Int? = null // null = auto
    var manualExposureTimeNs: Long? = null // null = auto
    var exposureCompensationIndex: Int = 0
    var isAeLocked: Boolean = false
    var isAfLocked: Boolean = false
    var isRawCaptureEnabled: Boolean = false
    var isVideoStabilizationEnabled: Boolean = true
    var videoBitrateOption: VideoBitrateOption = VideoBitrateOption.AUTO
    var videoFps: Int = 30
    var colorProfile: ColorProfile = ColorProfile.STANDARD
    var isAudioEnabled: Boolean = true
    var currentZoom: Float = 1.0f
    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoomState: StateFlow<Float> = _currentZoom.asStateFlow()
    var saveSelfieAsPreviewed: Boolean = true
    var viewfinderResolution: ViewfinderResolution = ViewfinderResolution.NORMAL

    // Camera Session Concurrency & State Guard
    private val cameraLifecycleLock = Any()
    @Volatile
    private var isStartingCamera = false
    @Volatile
    private var isClosingCamera = false
    @Volatile
    private var restartPending = false
    private var zoomDebounceJob: Job? = null

    val videoHdrEngine = VideoHdrEngine()
    private val _videoHdrState = MutableStateFlow(videoHdrEngine.currentState)
    val videoHdrState: StateFlow<VideoHdrState> = _videoHdrState.asStateFlow()

    val cinemaEngine = CinemaEngine(context)
    private val _cinemaConfig = MutableStateFlow(cinemaEngine.config)
    val cinemaConfig: StateFlow<CinemaConfig> = _cinemaConfig.asStateFlow()
    private val _cinemaCapabilities = MutableStateFlow(cinemaEngine.capabilities)
    val cinemaCapabilities: StateFlow<CinemaHardwareCapabilities> = _cinemaCapabilities.asStateFlow()

    init {
        videoHdrEngine.onStateChangedListener = { state ->
            _videoHdrState.value = state
        }
        startBackgroundThread()
        detectHardwareLenses()
        updateStorageStats()
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("Camera2Background").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    /**
     * Detects all real physical and logical lenses available on the device,
     * including hidden auxiliary cameras, multi-camera physical streams, and integrated ultra-wide zoom ratios.
     */
    fun detectHardwareLenses(forceDeepScan: Boolean = true): Int {
        try {
            val officialIds = cameraManager.cameraIdList.toList()
            val candidateIds = linkedSetOf<String>()
            candidateIds.addAll(officialIds)

            // Forceful deep probe of hidden OEM IDs (Moto, Xiaomi, Samsung, OnePlus, Vivo, Oppo)
            if (forceDeepScan) {
                val probeList = mutableListOf<String>()
                // Numeric IDs 0..30, 40..65, 100..115
                for (i in 0..30) probeList.add(i.toString())
                for (i in 40..65) probeList.add(i.toString())
                for (i in 100..115) probeList.add(i.toString())
                // Aux vendor naming patterns
                probeList.addAll(listOf("aux_0", "aux_1", "aux_2", "camera_0", "camera_1", "camera_2", "rear_0", "rear_1", "front_0", "front_1"))

                for (id in probeList) {
                    if (!candidateIds.contains(id)) {
                        try {
                            cameraManager.getCameraCharacteristics(id)
                            candidateIds.add(id)
                            Log.d(TAG, "Force deep probe found hidden camera ID: $id")
                        } catch (ignored: Exception) {
                            // Camera ID not present or unsupported
                        }
                    }
                }
            }

            val lenses = mutableListOf<LensInfo>()
            val processedPhysicalIds = mutableSetOf<String>()

            val primaryBackId = candidateIds.firstOrNull { id ->
                try {
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                } catch (e: Exception) { false }
            } ?: "0"

            val primaryFrontId = candidateIds.firstOrNull { id ->
                try {
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                } catch (e: Exception) { false }
            } ?: "1"

            for (id in candidateIds) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(4.0f)
                    val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: floatArrayOf(1.8f)
                    val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val minFocus = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
                    val maxAperture = apertures.firstOrNull() ?: 1.8f
                    val primaryFocalMm = focalLengths.firstOrNull() ?: 4.0f

                    val cropFactor = if (sensorSize != null && sensorSize.width > 0) {
                        36f / sensorSize.width
                    } else {
                        7f
                    }

                    // For each focal length supported by this camera ID
                    for (focalMm in focalLengths) {
                        val eq35mm = focalMm * cropFactor
                        val fovDegrees = if (sensorSize != null && sensorSize.width > 0 && focalMm > 0) {
                            (2.0 * kotlin.math.atan(sensorSize.width.toDouble() / (2.0 * focalMm.toDouble())) * (180.0 / Math.PI)).toFloat()
                        } else {
                            0f
                        }

                        val isBack = facing == CameraCharacteristics.LENS_FACING_BACK
                        val isMain = isBack && (id == primaryBackId || eq35mm in 23.5f..38f || (focalMm in 3.2f..5.8f && eq35mm in 22f..40f))
                        val isUltraWide = isBack && !isMain && ((eq35mm in 1.0f..23.4f) || focalMm <= 2.8f || fovDegrees >= 88.0f)
                        val isTele3x = isBack && !isMain && (eq35mm >= 70f || focalMm >= 9.0f)
                        val isTele2x = isBack && !isMain && (eq35mm in 45f..70f || focalMm in 5.9f..9.0f)
                        val isMacro = isBack && !isMain && minFocus > 10f && focalMm < 3.2f

                        val lensType = when {
                            facing == CameraCharacteristics.LENS_FACING_FRONT -> LensType.FRONT
                            isUltraWide -> LensType.ULTRAWIDE
                            isTele3x -> LensType.TELEPHOTO_3X
                            isTele2x -> LensType.TELEPHOTO
                            isMacro -> LensType.MACRO
                            else -> LensType.WIDE
                        }

                        val isOfficial = officialIds.contains(id)
                        val idDesc = when {
                            !isOfficial -> "Hidden Aux ID $id"
                            else -> "Camera ID $id"
                        }

                        val displayName = when (lensType) {
                            LensType.FRONT -> "Front Selfie (f/${maxAperture})"
                            LensType.ULTRAWIDE -> "0.5x Ultra Wide (${focalMm}mm f/${maxAperture})"
                            LensType.WIDE -> "1x Main (${focalMm}mm f/${maxAperture})"
                            LensType.TELEPHOTO -> "2x Telephoto (${focalMm}mm f/${maxAperture})"
                            LensType.TELEPHOTO_3X -> "3x Telephoto (${focalMm}mm f/${maxAperture})"
                            LensType.MACRO -> "Macro (${focalMm}mm)"
                        }

                        lenses.add(
                            LensInfo(
                                cameraId = id,
                                facing = facing,
                                lensType = lensType,
                                displayName = displayName,
                                focalLengthMm = focalMm,
                                maxAperture = maxAperture,
                                isPhysical = true,
                                isHiddenAux = !isOfficial,
                                isZoomPreset = false,
                                baseZoomRatio = when (lensType) {
                                    LensType.ULTRAWIDE -> 0.5f
                                    LensType.WIDE -> 1.0f
                                    LensType.TELEPHOTO -> 2.0f
                                    LensType.TELEPHOTO_3X -> 3.0f
                                    LensType.MACRO -> 1.0f
                                    LensType.FRONT -> 1.0f
                                },
                                fovDegrees = fovDegrees,
                                equivalent35mmFocalMm = eq35mm,
                                idTypeDescription = idDesc
                            )
                        )
                    }

                    // Android 9+ Physical camera inspection inside logical multi-camera
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val physicalCameraIds = chars.physicalCameraIds
                        for (physId in physicalCameraIds) {
                            if (!processedPhysicalIds.contains(physId)) {
                                processedPhysicalIds.add(physId)
                                try {
                                    val physChars = cameraManager.getCameraCharacteristics(physId)
                                    val pFacing = physChars.get(CameraCharacteristics.LENS_FACING) ?: facing
                                    val pFocals = physChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(4f)
                                    val pApertures = physChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: floatArrayOf(1.8f)
                                    val pSensor = physChars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                                    val pMinFocus = physChars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

                                    val pFocal = pFocals.firstOrNull() ?: 4f
                                    val pAperture = pApertures.firstOrNull() ?: 1.8f
                                    val pCrop = if (pSensor != null && pSensor.width > 0) 36f / pSensor.width else 7f
                                    val pEq35 = pFocal * pCrop
                                    val pFov = if (pSensor != null && pSensor.width > 0 && pFocal > 0) {
                                        (2.0 * kotlin.math.atan(pSensor.width.toDouble() / (2.0 * pFocal.toDouble())) * (180.0 / Math.PI)).toFloat()
                                    } else 0f

                                    val isPUltraWide = (pEq35 in 1.0f..23.5f) || pFocal <= 2.6f || pFov >= 75.0f
                                    val isPTele3x = pEq35 >= 70f || pFocal >= 9.0f
                                    val isPTele2x = pEq35 in 45f..70f || pFocal in 5.8f..9.0f
                                    val isPMacro = pMinFocus > 10f && pFocal < 3.2f

                                    val pType = when {
                                        pFacing == CameraCharacteristics.LENS_FACING_FRONT -> LensType.FRONT
                                        isPUltraWide -> LensType.ULTRAWIDE
                                        isPTele3x -> LensType.TELEPHOTO_3X
                                        isPTele2x -> LensType.TELEPHOTO
                                        isPMacro -> LensType.MACRO
                                        else -> LensType.WIDE
                                    }

                                    lenses.add(
                                        LensInfo(
                                            cameraId = physId,
                                            facing = pFacing,
                                            lensType = pType,
                                            displayName = "Physical $physId (${pType.shortLabel} · ${pFocal}mm)",
                                            focalLengthMm = pFocal,
                                            maxAperture = pAperture,
                                            isPhysical = true,
                                            isHiddenAux = !officialIds.contains(physId),
                                            isZoomPreset = false,
                                            baseZoomRatio = when (pType) {
                                                LensType.ULTRAWIDE -> 0.5f
                                                LensType.WIDE -> 1.0f
                                                LensType.TELEPHOTO -> 2.0f
                                                LensType.TELEPHOTO_3X -> 3.0f
                                                LensType.MACRO -> 1.0f
                                                LensType.FRONT -> 1.0f
                                            },
                                            physicalCameraId = physId,
                                            fovDegrees = pFov,
                                            equivalent35mmFocalMm = pEq35,
                                            idTypeDescription = "Physical Multi-Cam ID $physId"
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error inspecting physical camera $physId", e)
                                }
                            }
                        }
                    }

                    // Android 11+ Zoom Ratio Range (< 1.0f indicates integrated hardware Ultra Wide)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                        if (zoomRange != null) {
                            val minZoom = zoomRange.lower
                            val maxZoom = zoomRange.upper

                            // If camera supports < 1.0f (e.g. 0.5x, 0.6x), expose Ultra Wide preset
                            if (minZoom <= 0.9f && lenses.none { it.cameraId == id && it.lensType == LensType.ULTRAWIDE && it.isZoomPreset }) {
                                val ultraLabel = if (minZoom <= 0.55f) "0.5x" else "%.1fx".format(minZoom)
                                lenses.add(
                                    LensInfo(
                                        cameraId = id,
                                        facing = facing,
                                        lensType = LensType.ULTRAWIDE,
                                        displayName = "$ultraLabel Ultra Wide (Optical Stream)",
                                        focalLengthMm = primaryFocalMm,
                                        maxAperture = maxAperture,
                                        isPhysical = false,
                                        isHiddenAux = false,
                                        isZoomPreset = true,
                                        baseZoomRatio = minZoom,
                                        fovDegrees = 110f,
                                        equivalent35mmFocalMm = 14f,
                                        idTypeDescription = "Optical Ultra-Wide (Zoom $ultraLabel)"
                                    )
                                )
                            }

                            // If camera supports >= 2.0f and no physical telephoto lens is present, expose 2x Telephoto preset
                            if (maxZoom >= 2.0f && lenses.none { it.facing == facing && (it.lensType == LensType.TELEPHOTO || it.lensType == LensType.TELEPHOTO_3X) }) {
                                lenses.add(
                                    LensInfo(
                                        cameraId = id,
                                        facing = facing,
                                        lensType = LensType.TELEPHOTO,
                                        displayName = "2x Telephoto (Optical/Digital)",
                                        focalLengthMm = primaryFocalMm * 2f,
                                        maxAperture = maxAperture,
                                        isPhysical = false,
                                        isHiddenAux = false,
                                        isZoomPreset = true,
                                        baseZoomRatio = 2.0f,
                                        fovDegrees = 38f,
                                        equivalent35mmFocalMm = 50f,
                                        idTypeDescription = "Telephoto Preset (2.0x)"
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error inspecting camera $id", e)
                }
            }

            // Ensure the fundamental lenses (0.5x Ultra-Wide, 1x Main, Selfie) are always available:
            val hasBackWide = lenses.any { it.facing == CameraCharacteristics.LENS_FACING_BACK && it.lensType == LensType.WIDE }
            val hasBackUltraWide = lenses.any { it.facing == CameraCharacteristics.LENS_FACING_BACK && it.lensType == LensType.ULTRAWIDE }
            val hasFrontSelfie = lenses.any { it.facing == CameraCharacteristics.LENS_FACING_FRONT }

            // 1. Ensure Ultra Wide (0.5x) preset is available for seamless zoom switching
            if (!hasBackUltraWide) {
                lenses.add(
                    LensInfo(
                        cameraId = primaryBackId,
                        facing = CameraCharacteristics.LENS_FACING_BACK,
                        lensType = LensType.ULTRAWIDE,
                        displayName = "0.5x Ultra Wide",
                        focalLengthMm = 2.0f,
                        maxAperture = 2.2f,
                        isPhysical = false,
                        isHiddenAux = false,
                        isZoomPreset = true,
                        baseZoomRatio = 0.5f,
                        fovDegrees = 115f,
                        equivalent35mmFocalMm = 13f,
                        idTypeDescription = "Ultra-Wide Preset (0.5x)"
                    )
                )
            }

            // 2. Ensure Main Wide (1x) is available
            if (!hasBackWide) {
                lenses.add(
                    LensInfo(
                        cameraId = primaryBackId,
                        facing = CameraCharacteristics.LENS_FACING_BACK,
                        lensType = LensType.WIDE,
                        displayName = "1x Main Camera",
                        focalLengthMm = 4.2f,
                        maxAperture = 1.8f,
                        isPhysical = false,
                        isHiddenAux = false,
                        isZoomPreset = false,
                        baseZoomRatio = 1.0f,
                        fovDegrees = 78f,
                        equivalent35mmFocalMm = 24f,
                        idTypeDescription = "Main Camera (1x)"
                    )
                )
            }

            // 3. Ensure Front Selfie Camera is available
            if (!hasFrontSelfie) {
                lenses.add(
                    LensInfo(
                        cameraId = primaryFrontId,
                        facing = CameraCharacteristics.LENS_FACING_FRONT,
                        lensType = LensType.FRONT,
                        displayName = "Front Selfie Camera",
                        focalLengthMm = 3.5f,
                        maxAperture = 2.0f,
                        isPhysical = false,
                        isHiddenAux = false,
                        isZoomPreset = false,
                        baseZoomRatio = 1.0f,
                        fovDegrees = 85f,
                        equivalent35mmFocalMm = 22f,
                        idTypeDescription = "Front Camera (1x)"
                    )
                )
            }

            // Clean, de-duplicate and sort lenses intuitively:
            // 1. Back Ultra-Wide (0.5x)
            // 2. Back Main Wide (1x)
            // 3. Back Telephoto (2x / 3x)
            // 4. Back Macro
            // 5. Additional Physical/Aux lenses
            // 6. Front Selfie (1x)
            val sortedLenses = lenses.distinctBy {
                "${it.cameraId}_${it.lensType.name}_${it.isZoomPreset}_${it.baseZoomRatio}_${it.isPhysical}"
            }.sortedWith(
                compareBy<LensInfo> { it.facing }
                    .thenBy {
                        when (it.lensType) {
                            LensType.ULTRAWIDE -> 0
                            LensType.WIDE -> 1
                            LensType.TELEPHOTO -> 2
                            LensType.TELEPHOTO_3X -> 3
                            LensType.MACRO -> 4
                            LensType.FRONT -> 5
                        }
                    }
                    .thenBy { it.baseZoomRatio }
                    .thenBy { if (it.isPhysical) 1 else 0 }
            )

            _availableLenses.value = sortedLenses

            // Maintain current selection or pick 1x Main Wide
            val currentSelected = _selectedLens.value
            val validSelection = sortedLenses.firstOrNull { it.id == currentSelected?.id }
                ?: sortedLenses.firstOrNull { it.facing == CameraCharacteristics.LENS_FACING_BACK && it.lensType == LensType.WIDE && !it.isZoomPreset }
                ?: sortedLenses.firstOrNull { it.facing == CameraCharacteristics.LENS_FACING_BACK }
                ?: sortedLenses.firstOrNull()

            _selectedLens.value = validSelection
            if (validSelection != null) {
                inspectCapabilities(validSelection.cameraId)
            }

            Log.i(TAG, "Total discovered lenses after deep scan: ${sortedLenses.size}")
            return sortedLenses.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect hardware lenses", e)
            return 0
        }
    }

    /**
     * Inspects actual hardware capabilities of the given camera ID.
     */
    fun inspectCapabilities(cameraId: String) {
        try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            videoHdrEngine.onCameraConfigured(chars)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            val hasManualSensor = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
            val hasRaw = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)

            val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) ?: Range(100, 3200)
            val exposureTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) ?: Range(100_000L, 1_000_000_000L)
            val aeCompRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: Range(-4, 4)
            val aeCompStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toFloat() ?: 0.333f
            val minFocus = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 8f

            // OIS / EIS
            val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
            val eisModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
            val hasOis = oisModes.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON)
            val hasEis = eisModes.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON)

            // AWB modes
            val availableAwb = chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) ?: intArrayOf()
            val awbModes = WhiteBalanceMode.entries.filter { availableAwb.contains(it.camera2Mode) }

            // AF modes
            val availableAf = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
            val afModes = FocusMode.entries.filter { availableAf.contains(it.camera2Mode) }

            // Photo JPEG Resolutions
            val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            val photoResolutions = jpegSizes
                .sortedByDescending { it.width * it.height }
                .distinctBy { "${it.width}x${it.height}" }
                .map { CameraResolution(it.width, it.height, ImageFormat.JPEG, isRaw = false) }

            // RAW Resolutions
            val rawSizes = if (hasRaw) {
                map?.getOutputSizes(ImageFormat.RAW_SENSOR) ?: emptyArray()
            } else {
                emptyArray()
            }
            val rawResolutions = rawSizes
                .sortedByDescending { it.width * it.height }
                .distinctBy { "${it.width}x${it.height}" }
                .map { CameraResolution(it.width, it.height, ImageFormat.RAW_SENSOR, isRaw = true) }

            // Video Resolutions
            val videoSizes = map?.getOutputSizes(MediaRecorder::class.java)
                ?: map?.getOutputSizes(SurfaceTexture::class.java)
                ?: emptyArray()

            val standardVideoQualities = listOf(
                CameraResolution(3840, 2160), // 4K UHD (Supported across 0.5x, 1x and Selfie)
                CameraResolution(1920, 1080), // 1080p FHD
                CameraResolution(1280, 720),  // 720p HD
                CameraResolution(720, 480)    // 480p SD
            )
            val filteredVideoResolutions = standardVideoQualities.filter { standard ->
                videoSizes.isEmpty() || videoSizes.any { it.width == standard.width && it.height == standard.height } || standard.width <= 3840
            }.ifEmpty {
                listOf(
                    CameraResolution(3840, 2160),
                    CameraResolution(1920, 1080),
                    CameraResolution(1280, 720)
                )
            }

            // FPS ranges
            val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
            val maxFps = fpsRanges.maxOfOrNull { it.upper } ?: 30
            val supportedFps = if (maxFps >= 60) listOf(30, 60) else listOf(30)

            // Tonemap curve
            val tonemapModes = chars.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES) ?: intArrayOf()
            val hasTonemapCurve = tonemapModes.contains(CameraCharacteristics.TONEMAP_MODE_CONTRAST_CURVE) ||
                    tonemapModes.contains(CameraCharacteristics.TONEMAP_MODE_FAST)

            val hardwareCaps = HardwareCapabilities(
                supportsManualSensor = hasManualSensor,
                supportsRaw = hasRaw && rawResolutions.isNotEmpty(),
                supportsOis = hasOis,
                supportsEis = hasEis,
                supportsFlash = flashAvailable,
                minIso = isoRange.lower,
                maxIso = isoRange.upper,
                minExposureTimeNs = exposureTimeRange.lower,
                maxExposureTimeNs = exposureTimeRange.upper,
                minExposureCompensation = aeCompRange.lower,
                maxExposureCompensation = aeCompRange.upper,
                exposureCompensationStep = aeCompStep,
                minFocusDistance = minFocus,
                supportedAwbModes = awbModes.ifEmpty { listOf(WhiteBalanceMode.AUTO) },
                supportedAfModes = afModes.ifEmpty { listOf(FocusMode.CONTINUOUS) },
                supportedPhotoResolutions = photoResolutions,
                supportedRawResolutions = rawResolutions,
                supportedVideoResolutions = filteredVideoResolutions,
                supportedFpsRanges = supportedFps,
                supportsTonemapCurve = hasTonemapCurve,
                maxZoom = maxZoom
            )

            _capabilities.value = hardwareCaps

            cinemaEngine.onCameraConfigured(chars, filteredVideoResolutions)
            _cinemaCapabilities.value = cinemaEngine.capabilities

            // Default resolutions
            if (_selectedPhotoResolution.value == null || !photoResolutions.contains(_selectedPhotoResolution.value)) {
                _selectedPhotoResolution.value = photoResolutions.firstOrNull()
            }
            if (_selectedVideoResolution.value == null || !filteredVideoResolutions.contains(_selectedVideoResolution.value)) {
                _selectedVideoResolution.value = filteredVideoResolutions.firstOrNull { it.height == 1080 }
                    ?: filteredVideoResolutions.firstOrNull()
            }

            updatePreviewAspectRatio()
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting capabilities for camera $cameraId", e)
        }
    }

    private fun updatePreviewAspectRatio() {
        if (currentMode == CameraMode.VIDEO || currentMode == CameraMode.CINEMA) {
            val res = if (currentMode == CameraMode.CINEMA && cinemaConfig.value.selectedResolution != null) {
                cinemaConfig.value.selectedResolution
            } else {
                _selectedVideoResolution.value
            }
            if (res != null && res.height > 0) {
                val w = max(res.width, res.height).toFloat()
                val h = min(res.width, res.height).toFloat()
                _previewAspectRatio.value = w / h
            } else {
                _previewAspectRatio.value = 16f / 9f
            }
            return
        }

        val resolution = _selectedPhotoResolution.value
        if (resolution != null && resolution.height > 0) {
            // Standard sensor orientation width > height in landscape, in portrait aspect ratio is width / height
            val w = max(resolution.width, resolution.height).toFloat()
            val h = min(resolution.width, resolution.height).toFloat()
            _previewAspectRatio.value = w / h // e.g. 4/3 = 1.333, 16/9 = 1.777, 20/9 = 2.222
        } else {
            _previewAspectRatio.value = 4f / 3f
        }
    }

    /**
     * Switch active lens
     */
    fun selectLens(lens: LensInfo) {
        val previousLens = _selectedLens.value
        _selectedLens.value = lens
        currentZoom = lens.baseZoomRatio
        _currentZoom.value = lens.baseZoomRatio

        // If recording video, prioritize continuity to ensure zero distortion and no video stop:
        // Adjust optical zoom ratio and crop dynamically on the active recording stream
        if (_isRecordingVideo.value) {
            updatePreviewSettings()
            return
        }

        // If same physical camera ID and same facing, update zoom without restarting camera hardware
        if (previousLens?.cameraId == lens.cameraId && previousLens?.facing == lens.facing && cameraDevice != null) {
            updatePreviewSettings()
            return
        }

        inspectCapabilities(lens.cameraId)
        restartCamera()
    }

    /**
     * Switch photo resolution
     */
    fun selectPhotoResolution(resolution: CameraResolution) {
        _selectedPhotoResolution.value = resolution
        updatePreviewAspectRatio()
        restartCamera()
    }

    /**
     * Switch video resolution
     */
    fun selectVideoResolution(resolution: CameraResolution) {
        _selectedVideoResolution.value = resolution
        updatePreviewAspectRatio()
        restartCamera()
    }

    /**
     * Switch between Photo, Portrait, Video & Cinema modes
     */
    fun setMode(mode: CameraMode) {
        if (currentMode == mode) return
        val wasPhotoOrPortrait = (currentMode == CameraMode.PHOTO || currentMode == CameraMode.PORTRAIT)
        val isPhotoOrPortrait = (mode == CameraMode.PHOTO || mode == CameraMode.PORTRAIT)
        if (_isRecordingVideo.value) {
            stopVideoRecording()
        }
        currentMode = mode
        updatePreviewAspectRatio()
        if (wasPhotoOrPortrait != isPhotoOrPortrait) {
            restartCamera()
        } else {
            updatePreviewSettings()
        }
    }

    /**
     * Update Cinema Mode configuration and immediately apply to hardware ISP
     */
    fun setCinemaConfig(newConfig: CinemaConfig) {
        cinemaEngine.config = newConfig
        _cinemaConfig.value = newConfig
        if (currentMode == CameraMode.CINEMA) {
            updatePreviewAspectRatio()
            updatePreviewSettings()
        }
    }

    /**
     * Attach viewfinder surface texture from Compose AndroidView
     */
    fun setPreviewSurfaceTexture(texture: SurfaceTexture?) {
        previewSurfaceTexture = texture
        if (texture != null) {
            startCamera()
        } else {
            closeCamera()
        }
    }

    /**
     * Start/Open Camera2 device
     */
    @SuppressLint("MissingPermission")
    fun startCamera() {
        val lens = _selectedLens.value ?: return
        val texture = previewSurfaceTexture ?: return

        startBackgroundThread()

        try {
            val chars = cameraManager.getCameraCharacteristics(lens.cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

            // Pick optimal preview size matching selected aspect ratio and viewfinderResolution level
            val targetRatio = _previewAspectRatio.value
            val previewSizes = map.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()

            val maxDim = viewfinderResolution.maxDimension
            val matchingRatioSizes = previewSizes.filter {
                val r = max(it.width, it.height).toFloat() / min(it.width, it.height).toFloat()
                kotlin.math.abs(r - targetRatio) < 0.08f
            }

            val optimalPreviewSize = matchingRatioSizes
                .filter { max(it.width, it.height) <= maxDim }
                .maxByOrNull { it.width * it.height }
                ?: matchingRatioSizes.minByOrNull { max(it.width, it.height) }
                ?: previewSizes.firstOrNull { it.width <= 1920 && it.height <= 1080 }
                ?: previewSizes.firstOrNull()
                ?: Size(1920, 1080)

            texture.setDefaultBufferSize(optimalPreviewSize.width, optimalPreviewSize.height)
            previewSurface = Surface(texture)

            // Setup ImageReader for Photo mode
            setupImageReaders(lens.cameraId)

            synchronized(cameraLifecycleLock) {
                if (isStartingCamera) {
                    restartPending = true
                    return
                }
                isStartingCamera = true
            }

            cameraManager.openCamera(lens.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    synchronized(cameraLifecycleLock) {
                        isStartingCamera = false
                        if (restartPending) {
                            restartPending = false
                            backgroundHandler?.post { restartCamera() }
                            return
                        }
                    }
                    createCameraCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    _isCameraReady.value = false
                    synchronized(cameraLifecycleLock) {
                        isStartingCamera = false
                        if (restartPending) {
                            restartPending = false
                            backgroundHandler?.post { restartCamera() }
                        }
                    }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    camera.close()
                    cameraDevice = null
                    _isCameraReady.value = false
                    synchronized(cameraLifecycleLock) {
                        isStartingCamera = false
                        if (restartPending) {
                            restartPending = false
                            backgroundHandler?.post { restartCamera() }
                            return
                        }
                    }
                    // Auto-recover from transient HAL contention or device busy error
                    if (error == CameraDevice.StateCallback.ERROR_CAMERA_IN_USE ||
                        error == CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE ||
                        error == CameraDevice.StateCallback.ERROR_CAMERA_DEVICE) {
                        backgroundHandler?.postDelayed({
                            restartCamera()
                        }, 200)
                    }
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
            synchronized(cameraLifecycleLock) {
                isStartingCamera = false
            }
            _isCameraReady.value = false
        }
    }

    private fun setupImageReaders(cameraId: String) {
        imageReaderJpeg?.close()
        imageReaderRaw?.close()

        val photoRes = _selectedPhotoResolution.value ?: CameraResolution(4000, 3000)
        imageReaderJpeg = ImageReader.newInstance(
            photoRes.width,
            photoRes.height,
            ImageFormat.JPEG,
            2
        )

        val caps = _capabilities.value
        if (caps.supportsRaw && isRawCaptureEnabled && caps.supportedRawResolutions.isNotEmpty()) {
            val rawRes = caps.supportedRawResolutions.first()
            imageReaderRaw = ImageReader.newInstance(
                rawRes.width,
                rawRes.height,
                ImageFormat.RAW_SENSOR,
                2
            )
        }
    }

    private fun createCameraCaptureSession() {
        val camera = cameraDevice ?: return
        val previewSurf = previewSurface ?: return

        try {
            val surfaces = mutableListOf<Surface>()
            surfaces.add(previewSurf)

            imageReaderJpeg?.surface?.let { surfaces.add(it) }
            imageReaderRaw?.surface?.let { surfaces.add(it) }

            val template = CameraDevice.TEMPLATE_PREVIEW

            previewRequestBuilder = camera.createCaptureRequest(template).apply {
                addTarget(previewSurf)
                applyCommonSettings(this)
            }

            camera.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            previewRequestBuilder?.let {
                                session.setRepeatingRequest(it.build(), captureCallback, backgroundHandler)
                            }
                            _isCameraReady.value = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating preview request", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Camera capture session configuration failed")
                        _isCameraReady.value = false
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create camera capture session", e)
        }
    }

    private var lastCaptureResult: TotalCaptureResult? = null
    private var lastHdrUpdateRequestTime = 0L

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            lastCaptureResult = result

            // In Video mode, execute real-time scene analysis and apply adaptive HDR & noise reduction
            if (currentMode == CameraMode.VIDEO && videoHdrEngine.mode != VideoHdrMode.OFF) {
                val updated = videoHdrEngine.onFrameCaptured(result)
                val now = System.currentTimeMillis()
                // Update capture session repeating request smoothly (throttled to ~120ms to avoid IPC overhead and keep 30/60fps stable)
                if (updated && (now - lastHdrUpdateRequestTime > 120L)) {
                    lastHdrUpdateRequestTime = now
                    try {
                        val builder = previewRequestBuilder
                        if (builder != null && captureSession != null) {
                            videoHdrEngine.applyToCaptureRequest(builder)
                            captureSession?.setRepeatingRequest(builder.build(), this, backgroundHandler)
                        }
                    } catch (ignored: Exception) {}
                }
            }
        }
    }

    /**
     * Apply AE, AF, AWB, Flash, ISO, Shutter, Zoom, Stabilization to CaptureRequest.Builder
     */
    private fun applyCommonSettings(builder: CaptureRequest.Builder) {
        val caps = _capabilities.value

        // AE & Manual Exposure / ISO
        if (manualIso != null || manualExposureTimeNs != null) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            manualIso?.let { builder.set(CaptureRequest.SENSOR_SENSITIVITY, it) }
            manualExposureTimeNs?.let { builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, it) }
        } else {
            // Auto Exposure mode + Flash configuration
            when (flashMode) {
                FlashMode.OFF -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
                FlashMode.AUTO -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                }
                FlashMode.ON -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                }
                FlashMode.TORCH -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                }
            }
            // Exposure compensation
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureCompensationIndex)
            builder.set(CaptureRequest.CONTROL_AE_LOCK, isAeLocked)
        }

        // White Balance
        builder.set(CaptureRequest.CONTROL_AWB_MODE, whiteBalanceMode.camera2Mode)

        // Focus
        when (focusMode) {
            FocusMode.MANUAL -> {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocusDistance)
            }
            FocusMode.CONTINUOUS -> {
                val mode = if (currentMode == CameraMode.VIDEO) {
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                } else {
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                }
                builder.set(CaptureRequest.CONTROL_AF_MODE, mode)
            }
            FocusMode.AUTO -> {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }
            FocusMode.MACRO -> {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_MACRO)
            }
        }

        // Stabilization
        if (currentMode == CameraMode.VIDEO) {
            if (isVideoStabilizationEnabled) {
                if (caps.supportsEis) {
                    builder.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    )
                }
                if (caps.supportsOis) {
                    builder.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )
                }
            } else {
                builder.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
                builder.set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }
        }

        // Color profiles & Tonemap
        when (colorProfile) {
            ColorProfile.FLAT_LOG -> {
                if (caps.supportsTonemapCurve) {
                    builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST)
                }
                builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF)
            }
            ColorProfile.MONOCHROME -> {
                builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_MONO)
            }
            ColorProfile.VIBRANT -> {
                builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF)
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            }
            ColorProfile.STANDARD, ColorProfile.NATURAL -> {
                builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF)
            }
        }

        // Real-time Video HDR & Dedicated Cinema Log Color Profile
        if (currentMode == CameraMode.CINEMA) {
            cinemaEngine.applyToCaptureRequest(builder)
        } else if (currentMode == CameraMode.VIDEO) {
            videoHdrEngine.applyToCaptureRequest(builder)
        }

        // Digital Zoom / Crop Region
        applyZoom(builder)
    }

    private fun applyZoom(builder: CaptureRequest.Builder) {
        val lens = _selectedLens.value ?: return
        try {
            val chars = cameraManager.getCameraCharacteristics(lens.cameraId)
            val baseRatio = if (lens.baseZoomRatio > 0f) lens.baseZoomRatio else 1.0f

            // Compute sensor-relative zoom ratio:
            // e.g. On 0.5x Ultra-Wide sensor (baseRatio = 0.5f), 0.5f zoom gives effectiveZoomRatio = 1.0f (full native FOV)
            // 0.75f gives effectiveZoomRatio = 1.5f digital crop
            // On 1x Main sensor (baseRatio = 1.0f), 1.0f gives effectiveZoomRatio = 1.0f, 2.0f gives 2.0f
            val effectiveZoomRatio = if (lens.lensType == LensType.ULTRAWIDE && baseRatio <= 0.6f) {
                (currentZoom / baseRatio).coerceAtLeast(1.0f)
            } else if ((lens.lensType == LensType.TELEPHOTO || lens.lensType == LensType.TELEPHOTO_3X) && baseRatio > 1.2f) {
                (currentZoom / baseRatio).coerceAtLeast(1.0f)
            } else {
                currentZoom.coerceAtLeast(1.0f)
            }

            // On Android 11+ (API 30+), CONTROL_ZOOM_RATIO natively supports < 1.0f (Ultra-Wide 0.5x/0.6x)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                if (zoomRange != null) {
                    val clamped = if (zoomRange.lower <= 0.6f) {
                        currentZoom.coerceIn(zoomRange.lower, zoomRange.upper)
                    } else {
                        effectiveZoomRatio.coerceIn(zoomRange.lower, zoomRange.upper)
                    }
                    builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clamped)
                    return
                }
            }

            // Fallback for legacy devices or SCALER_CROP_REGION
            val sensorRect = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
            val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            val zoom = effectiveZoomRatio.coerceIn(1.0f, maxZoom)

            val cropW = (sensorRect.width() / zoom).toInt()
            val cropH = (sensorRect.height() / zoom).toInt()
            val cropX = (sensorRect.width() - cropW) / 2
            val cropY = (sensorRect.height() - cropH) / 2

            val cropRegion = Rect(cropX, cropY, cropX + cropW, cropY + cropH)
            builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting zoom", e)
        }
    }

    /**
     * Updates preview request with new settings on the fly
     */
    fun updatePreviewSettings() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return
        try {
            applyCommonSettings(builder)
            session.setRepeatingRequest(builder.build(), captureCallback, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update preview settings", e)
        }
    }

    /**
     * Change Video HDR Mode (OFF / AUTO / MANUAL)
     */
    fun setVideoHdrMode(mode: VideoHdrMode) {
        videoHdrEngine.mode = mode
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    /**
     * Change Video HDR Manual Intensity (0 to 100)
     */
    fun setVideoHdrManualIntensity(intensity: Int) {
        videoHdrEngine.manualIntensity = intensity
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualShadows(value: Int) {
        videoHdrEngine.manualShadows = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualHighlights(value: Int) {
        videoHdrEngine.manualHighlights = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualContrast(value: Int) {
        videoHdrEngine.manualContrast = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualExposure(value: Int) {
        videoHdrEngine.manualExposure = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualBlackLevel(value: Int) {
        videoHdrEngine.manualBlackLevel = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualMidtones(value: Int) {
        videoHdrEngine.manualMidtones = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    fun setVideoHdrManualSaturation(value: Int) {
        videoHdrEngine.manualSaturation = value
        _videoHdrState.value = videoHdrEngine.currentState
        if (currentMode == CameraMode.VIDEO) {
            updatePreviewSettings()
        }
    }

    /**
     * Set Zoom (.5x to 10x) with seamless automatic lens switching
     */
    fun setZoom(zoom: Float, isPresetTap: Boolean = false) {
        val clampedZoom = zoom.coerceIn(0.5f, 10.0f)
        currentZoom = clampedZoom
        _currentZoom.value = clampedZoom

        val currentLens = _selectedLens.value ?: return

        // If front selfie camera, apply digital zoom on active stream,
        // or switch to rear lens if user explicitly tapped a rear zoom preset (.5x or 1x)
        if (currentLens.facing == CameraCharacteristics.LENS_FACING_FRONT) {
            if (isPresetTap && (clampedZoom < 0.95f || clampedZoom in 0.95f..1.1f)) {
                val backLenses = _availableLenses.value.filter { it.facing == CameraCharacteristics.LENS_FACING_BACK }
                val backTarget = backLenses.firstOrNull { if (clampedZoom < 0.95f) it.lensType == LensType.ULTRAWIDE else it.lensType == LensType.WIDE }
                if (backTarget != null) {
                    selectLens(backTarget)
                    return
                }
            }
            updatePreviewSettings()
            return
        }

        // Identify target hardware lens for current zoom level:
        val backLenses = _availableLenses.value.filter { it.facing == CameraCharacteristics.LENS_FACING_BACK }
        val targetLens: LensInfo? = when {
            clampedZoom < 0.9f -> {
                // Target is 0.5x Ultra Wide
                backLenses.firstOrNull { it.lensType == LensType.ULTRAWIDE && it.isPhysical }
                    ?: backLenses.firstOrNull { it.lensType == LensType.ULTRAWIDE }
            }
            clampedZoom in 0.9f..1.95f -> {
                // Target is 1x Main Wide
                backLenses.firstOrNull { it.lensType == LensType.WIDE && !it.isZoomPreset }
                    ?: backLenses.firstOrNull { it.lensType == LensType.WIDE }
            }
            clampedZoom >= 2.95f -> {
                // Target is 3x Telephoto if hardware present, else 2x, else 1x
                backLenses.firstOrNull { it.lensType == LensType.TELEPHOTO_3X && it.isPhysical }
                    ?: backLenses.firstOrNull { it.lensType == LensType.TELEPHOTO && it.isPhysical }
                    ?: backLenses.firstOrNull { it.lensType == LensType.WIDE && !it.isZoomPreset }
            }
            clampedZoom >= 1.95f -> {
                // Target is 2x Telephoto if hardware present, else 1x
                backLenses.firstOrNull { it.lensType == LensType.TELEPHOTO && it.isPhysical }
                    ?: backLenses.firstOrNull { it.lensType == LensType.WIDE && !it.isZoomPreset }
            }
            else -> null
        }

        if (targetLens != null && targetLens.cameraId != currentLens.cameraId) {
            zoomDebounceJob?.cancel()

            if (isPresetTap) {
                // Instant tap on .5, 1x, 2, 3, 10 -> switch hardware lens immediately
                selectLens(targetLens)
            } else {
                // Continuous scrubbing: apply optical/digital zoom immediately to active preview,
                // and switch physical camera ID once scrubbing settles (160ms) to prevent HAL freeze!
                updatePreviewSettings()
                zoomDebounceJob = engineScope.launch {
                    delay(160)
                    val activeNow = _selectedLens.value
                    if (targetLens.cameraId != activeNow?.cameraId) {
                        selectLens(targetLens)
                    }
                }
            }
        } else {
            // Same hardware camera: apply zoom immediately
            updatePreviewSettings()
        }
    }

    /**
     * Tap to Focus & Meter
     */
    fun triggerFocusAndMeter(normX: Float, normY: Float) {
        val lens = _selectedLens.value ?: return
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return

        try {
            val chars = cameraManager.getCameraCharacteristics(lens.cameraId)
            val sensorRect = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return

            val focusAreaSize = 200
            val centerX = (normX * sensorRect.width()).toInt().coerceIn(0, sensorRect.width())
            val centerY = (normY * sensorRect.height()).toInt().coerceIn(0, sensorRect.height())

            val left = (centerX - focusAreaSize / 2).coerceAtLeast(0)
            val right = (centerX + focusAreaSize / 2).coerceAtMost(sensorRect.width())
            val top = (centerY - focusAreaSize / 2).coerceAtLeast(0)
            val bottom = (centerY + focusAreaSize / 2).coerceAtMost(sensorRect.height())

            val focusRect = MeteringRectangle(Rect(left, top, right, bottom), MeteringRectangle.METERING_WEIGHT_MAX)

            builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusRect))
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(focusRect))
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)

            session.capture(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
                    session.setRepeatingRequest(builder.build(), captureCallback, backgroundHandler)
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Tap to focus failed", e)
        }
    }

    private fun getDeviceRotationDegrees(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display?.rotation ?: android.view.Surface.ROTATION_0
            } catch (e: Exception) {
                windowManager?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0
        }
        return when (rotation) {
            android.view.Surface.ROTATION_0 -> 0
            android.view.Surface.ROTATION_90 -> 90
            android.view.Surface.ROTATION_180 -> 180
            android.view.Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun calculateOrientation(sensorOrientation: Int, isFrontFacing: Boolean, deviceRotation: Int): Int {
        return if (isFrontFacing) {
            // Front sensor: (sensorOrientation + deviceRotation) % 360
            (sensorOrientation + deviceRotation) % 360
        } else {
            // Back sensor: (sensorOrientation - deviceRotation + 360) % 360
            (sensorOrientation - deviceRotation + 360) % 360
        }
    }

    private fun getVideoOrientationHint(): Int {
        val lens = _selectedLens.value ?: return 90
        val isFront = lens.facing == CameraCharacteristics.LENS_FACING_FRONT
        val sensorOrientation = try {
            val chars = cameraManager.getCameraCharacteristics(lens.cameraId)
            chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: (if (isFront) 270 else 90)
        } catch (e: Exception) {
            if (isFront) 270 else 90
        }
        val deviceRotation = getDeviceRotationDegrees()
        val standardHint = calculateOrientation(sensorOrientation, isFront, deviceRotation)

        // When "Save selfie as previewed without flipping" is enabled:
        // In the viewfinder preview, front camera is mirrored horizontally (scale -1, 1).
        // Standard video container players play according to orientation hint.
        // For front camera in portrait (deviceRotation = 0, sensor = 270):
        // Standard hint is 270°. Some players or sensors inverted this to 90° (upside-down by 180°).
        // Returning standardHint properly prevents upside down playback.
        return standardHint
    }

    private fun getCaptureJpegOrientation(): Int {
        val lens = _selectedLens.value ?: return 90
        val isFront = lens.facing == CameraCharacteristics.LENS_FACING_FRONT
        val sensorOrientation = try {
            val chars = cameraManager.getCameraCharacteristics(lens.cameraId)
            chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: (if (isFront) 270 else 90)
        } catch (e: Exception) {
            if (isFront) 270 else 90
        }
        val deviceRotation = getDeviceRotationDegrees()
        return calculateOrientation(sensorOrientation, isFront, deviceRotation)
    }

    /**
     * Take still photo (JPEG + optional RAW)
     */
    fun takePhoto(onComplete: (Uri?) -> Unit) {
        val camera = cameraDevice ?: return
        val session = captureSession ?: return
        val readerJpeg = imageReaderJpeg ?: return

        _isCapturing.value = true

        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(readerJpeg.surface)

            val caps = _capabilities.value
            val isRaw = isRawCaptureEnabled && caps.supportsRaw && imageReaderRaw != null
            if (isRaw) {
                imageReaderRaw?.surface?.let { captureBuilder.addTarget(it) }
            }

            applyCommonSettings(captureBuilder)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getCaptureJpegOrientation())
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, 98.toByte())

            readerJpeg.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    engineScope.launch(Dispatchers.IO) {
                        val uri = saveJpegToMediaStore(image)
                        image.close()
                        _isCapturing.value = false
                        updateStorageStats()
                        withContext(Dispatchers.Main) {
                            onComplete(uri)
                        }
                    }
                }
            }, backgroundHandler)

            if (isRaw) {
                imageReaderRaw?.setOnImageAvailableListener({ reader ->
                    val rawImage = reader.acquireLatestImage()
                    if (rawImage != null) {
                        engineScope.launch(Dispatchers.IO) {
                            val lens = _selectedLens.value
                            if (lens != null) {
                                val characteristics = cameraManager.getCameraCharacteristics(lens.cameraId)
                                saveRawToMediaStore(rawImage, characteristics)
                            }
                            rawImage.close()
                        }
                    }
                }, backgroundHandler)
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, "Photo capture completed")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo", e)
            _isCapturing.value = false
            onComplete(null)
        }
    }

    /**
     * Captures a single full-resolution uncompressed Bitmap for Portrait AI processing.
     * Ensures orientation and left/right mirroring match the viewfinder exactly.
     */
    fun captureStillBitmap(onBitmapCaptured: (Bitmap?) -> Unit) {
        val camera = cameraDevice ?: run {
            onBitmapCaptured(null)
            return
        }
        val session = captureSession ?: run {
            onBitmapCaptured(null)
            return
        }
        val readerJpeg = imageReaderJpeg ?: run {
            onBitmapCaptured(null)
            return
        }

        val activeLens = _selectedLens.value
        val isFrontFacing = activeLens?.facing == CameraCharacteristics.LENS_FACING_FRONT

        _isCapturing.value = true

        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(readerJpeg.surface)
            applyCommonSettings(captureBuilder)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getCaptureJpegOrientation())
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, 100.toByte())

            readerJpeg.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    engineScope.launch(Dispatchers.IO) {
                        try {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            image.close()

                            val options = BitmapFactory.Options().apply {
                                inMutable = true
                            }
                            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                            if (rawBitmap != null) {
                                val exif = try {
                                    android.media.ExifInterface(java.io.ByteArrayInputStream(bytes))
                                } catch (e: Exception) {
                                    null
                                }
                                val exifOrientation = exif?.getAttributeInt(
                                    android.media.ExifInterface.TAG_ORIENTATION,
                                    android.media.ExifInterface.ORIENTATION_UNDEFINED
                                ) ?: android.media.ExifInterface.ORIENTATION_UNDEFINED

                                val matrix = Matrix()
                                when (exifOrientation) {
                                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                    else -> {
                                        if (rawBitmap.width > rawBitmap.height) {
                                            val rot = if (isFrontFacing) 270f else 90f
                                            matrix.postRotate(rot)
                                        }
                                    }
                                }

                                // Front camera viewfinder WYSIWYG mirroring preservation
                                if (isFrontFacing && saveSelfieAsPreviewed) {
                                    matrix.postScale(-1f, 1f)
                                }

                                val orientedBitmap = if (!matrix.isIdentity) {
                                    val transformed = Bitmap.createBitmap(
                                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                    )
                                    if (transformed != rawBitmap) {
                                        rawBitmap.recycle()
                                    }
                                    transformed
                                } else {
                                    rawBitmap
                                }

                                _isCapturing.value = false
                                withContext(Dispatchers.Main) {
                                    onBitmapCaptured(orientedBitmap)
                                }
                            } else {
                                _isCapturing.value = false
                                withContext(Dispatchers.Main) {
                                    onBitmapCaptured(null)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding and orienting captured still bitmap", e)
                            try { image.close() } catch (ignored: Exception) {}
                            _isCapturing.value = false
                            withContext(Dispatchers.Main) {
                                onBitmapCaptured(null)
                            }
                        }
                    }
                }
            }, backgroundHandler)

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, "Portrait still frame capture triggered")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing still bitmap for portrait", e)
            _isCapturing.value = false
            onBitmapCaptured(null)
        }
    }

    /**
     * Start Video Recording
     */
    fun startVideoRecording(onError: (String) -> Unit) {
        if (_isRecordingVideo.value) return

        val camera = cameraDevice ?: return
        val lens = _selectedLens.value ?: return

        try {
            closeCameraCaptureSession()

            val isCinema = (currentMode == CameraMode.CINEMA)
            val videoRes = if (isCinema && cinemaConfig.value.selectedResolution != null) {
                cinemaConfig.value.selectedResolution!!
            } else {
                _selectedVideoResolution.value ?: CameraResolution(1920, 1080)
            }
            val is10BitRequested = isCinema && cinemaConfig.value.logBitDepth == LogBitDepth.BIT_10 && cinemaCapabilities.value.supports10BitRecording
            val bitrate = if (is10BitRequested) {
                when {
                    videoRes.width >= 3840 -> 75_000_000
                    videoRes.width >= 1920 -> 40_000_000
                    else -> 20_000_000
                }
            } else if (videoBitrateOption.bps > 0) {
                videoBitrateOption.bps
            } else {
                when {
                    videoRes.width >= 3840 -> 40_000_000
                    videoRes.width >= 1920 -> 20_000_000
                    else -> 10_000_000
                }
            }
            val targetFps = if (isCinema) cinemaConfig.value.videoFps else videoFps

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val prefix = if (isCinema) "CINEMA_" else "VID_"
            val fileName = "${prefix}$timeStamp.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Cannot create MediaStore video record")

            currentVideoUri = uri
            videoRecordingFileDescriptor = context.contentResolver.openFileDescriptor(uri, "rw")

            val fd = videoRecordingFileDescriptor?.fileDescriptor
                ?: throw IllegalStateException("Cannot open FileDescriptor for video")

            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                if (isAudioEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(fd)
                setVideoEncodingBitRate(bitrate)
                setVideoFrameRate(targetFps)
                setVideoSize(videoRes.width, videoRes.height)
                if (is10BitRequested) {
                    setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            setVideoEncodingProfileLevel(
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
                                MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "HEVC Main 10 profile level fallback", e)
                        }
                    }
                } else {
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                }
                if (isAudioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(48000)
                    setAudioEncodingBitRate(192000)
                }
                val orientationHint = getVideoOrientationHint()
                setOrientationHint(orientationHint)
                Log.d(TAG, "MediaRecorder orientationHint set to $orientationHint (lens=${lens.displayName}, isFront=${lens.facing == CameraCharacteristics.LENS_FACING_FRONT})")
                prepare()
            }

            val videoRatio = max(videoRes.width, videoRes.height).toFloat() / min(videoRes.width, videoRes.height).toFloat()
            _previewAspectRatio.value = videoRatio

            // Ensure preview surface buffer matches video 16:9 ratio exactly to prevent any vertical stretch
            previewSurfaceTexture?.let { texture ->
                val chars = cameraManager.getCameraCharacteristics(lens.cameraId)
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val previewSizes = map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()
                val matchingSize = previewSizes
                    .filter {
                        val r = max(it.width, it.height).toFloat() / min(it.width, it.height).toFloat()
                        kotlin.math.abs(r - videoRatio) < 0.05f
                    }
                    .filter { max(it.width, it.height) <= 1920 }
                    .maxByOrNull { it.width * it.height }
                    ?: previewSizes.firstOrNull { it.width == 1920 && it.height == 1080 }
                    ?: Size(videoRes.width, videoRes.height)

                texture.setDefaultBufferSize(matchingSize.width, matchingSize.height)
                previewSurface = Surface(texture)
            }

            val recorderSurface = mediaRecorder!!.surface
            val previewSurf = previewSurface ?: return

            val surfaces = listOf(previewSurf, recorderSurface)

            previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurf)
                addTarget(recorderSurface)
                applyCommonSettings(this)
            }

            camera.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            previewRequestBuilder?.let {
                                session.setRepeatingRequest(it.build(), captureCallback, backgroundHandler)
                            }
                            mediaRecorder?.start()
                            _isRecordingVideo.value = true
                            startVideoTimer()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start MediaRecorder recording", e)
                            onError("Failed to start recording: ${e.message}")
                            stopVideoRecording()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError("Video capture session config failed")
                        _isRecordingVideo.value = false
                    }
                },
                backgroundHandler
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize video recording", e)
            onError(e.message ?: "Failed to record video")
            _isRecordingVideo.value = false
            restartCamera()
        }
    }

    private fun startVideoTimer() {
        _videoDurationSeconds.value = 0
        videoTimerJob?.cancel()
        videoTimerJob = engineScope.launch {
            while (_isRecordingVideo.value) {
                delay(1000)
                _videoDurationSeconds.value += 1
            }
        }
    }

    /**
     * Stop Video Recording
     */
    fun stopVideoRecording() {
        if (!_isRecordingVideo.value) return

        try {
            videoTimerJob?.cancel()
            _isRecordingVideo.value = false

            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaRecorder stop failed", e)
                }
                reset()
                release()
            }
            mediaRecorder = null

            videoRecordingFileDescriptor?.close()
            videoRecordingFileDescriptor = null

            // Mark video as complete in MediaStore (API 29+)
            currentVideoUri?.let { uri ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, values, null, null)
                }
                _lastCapturedMedia.value = CapturedMedia(
                    uri = uri,
                    isVideo = true,
                    timestamp = System.currentTimeMillis(),
                    displayName = "Video"
                )
            }

            updateStorageStats()
            restartCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video recording", e)
            restartCamera()
        }
    }

    private fun saveJpegToMediaStore(image: Image): Uri? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val activeLens = _selectedLens.value
        val isFrontFacing = activeLens?.facing == CameraCharacteristics.LENS_FACING_FRONT

        val finalBytes = if (isFrontFacing && saveSelfieAsPreviewed) {
            try {
                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (rawBitmap != null) {
                    val exif = try {
                        android.media.ExifInterface(java.io.ByteArrayInputStream(bytes))
                    } catch (e: Exception) { null }
                    val exifOrientation = exif?.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_UNDEFINED
                    ) ?: android.media.ExifInterface.ORIENTATION_UNDEFINED

                    val matrix = Matrix()
                    when (exifOrientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        else -> {
                            if (rawBitmap.width > rawBitmap.height) {
                                matrix.postRotate(270f)
                            }
                        }
                    }
                    // Mirror horizontally to save selfie exactly as previewed in viewfinder
                    matrix.postScale(-1f, 1f)

                    val mirroredBitmap = Bitmap.createBitmap(
                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                    )
                    if (mirroredBitmap != rawBitmap) {
                        rawBitmap.recycle()
                    }
                    val stream = java.io.ByteArrayOutputStream()
                    mirroredBitmap.compress(Bitmap.CompressFormat.JPEG, 98, stream)
                    mirroredBitmap.recycle()
                    stream.toByteArray()
                } else {
                    bytes
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to mirror selfie JPEG, falling back to original", e)
                bytes
            }
        } else {
            bytes
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "IMG_$timeStamp.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(finalBytes)
            }

            if (isFrontFacing && saveSelfieAsPreviewed) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        val outExif = android.media.ExifInterface(pfd.fileDescriptor)
                        outExif.setAttribute(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL.toString()
                        )
                        outExif.saveAttributes()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to write EXIF orientation on mirrored selfie", e)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            _lastCapturedMedia.value = CapturedMedia(
                uri = uri,
                isVideo = false,
                timestamp = System.currentTimeMillis(),
                displayName = fileName
            )
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save JPEG to media store", e)
            return null
        }
    }

    private fun saveRawToMediaStore(rawImage: Image, characteristics: CameraCharacteristics) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "RAW_$timeStamp.dng"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return

            val captureResult = lastCaptureResult ?: return
            val dngCreator = DngCreator(characteristics, captureResult)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                dngCreator.writeImage(out, rawImage)
            }
            dngCreator.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, completeValues, null, null)
            }
            Log.d(TAG, "Saved RAW DNG successfully to $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW image", e)
        }
    }

    /**
     * Update storage statistics
     */
    fun updateStorageStats() {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesAvailable = stat.availableBytes
            val totalBytes = stat.totalBytes
            val freeGb = bytesAvailable.toFloat() / (1024 * 1024 * 1024)

            // Approximate: 5MB per JPEG photo, ~150MB per minute of 1080p video
            val estimatedPhotos = (bytesAvailable / (5L * 1024 * 1024)).toInt()
            val estimatedVideoMinutes = (bytesAvailable / (150L * 1024 * 1024)).toInt()

            _storageStats.value = StorageStats(
                freeBytes = bytesAvailable,
                totalBytes = totalBytes,
                freeGb = freeGb,
                estimatedPhotos = estimatedPhotos,
                estimatedVideoMinutes = estimatedVideoMinutes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating storage stats", e)
        }
    }

    fun restartCamera() {
        backgroundHandler?.post {
            synchronized(cameraLifecycleLock) {
                if (isStartingCamera) {
                    restartPending = true
                    return@synchronized
                }
                closeCameraInternal()
                backgroundHandler?.postDelayed({
                    startCamera()
                }, 75)
            }
        } ?: run {
            closeCamera()
            startCamera()
        }
    }

    fun applyViewfinderResolution(resolution: ViewfinderResolution) {
        if (viewfinderResolution == resolution) return
        viewfinderResolution = resolution
        restartCamera()
    }

    private fun closeCameraCaptureSession() {
        try {
            captureSession?.close()
            captureSession = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing capture session", e)
        }
    }

    private fun closeCameraInternal() {
        _isCameraReady.value = false
        closeCameraCaptureSession()
        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera device", e)
        }
        try {
            imageReaderJpeg?.close()
            imageReaderJpeg = null
            imageReaderRaw?.close()
            imageReaderRaw = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing image readers", e)
        }
        isStartingCamera = false
    }

    fun closeCamera() {
        synchronized(cameraLifecycleLock) {
            closeCameraInternal()
        }
    }

    fun release() {
        closeCamera()
        stopBackgroundThread()
    }
}

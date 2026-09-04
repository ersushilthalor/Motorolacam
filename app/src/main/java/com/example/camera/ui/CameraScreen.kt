package com.example.camera.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.model.CameraMode
import com.example.camera.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionPrompt(
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            },
            modifier = modifier
        )
        return
    }

    // Engine & VM States
    val cameraMode by viewModel.cameraMode.collectAsStateWithLifecycle()
    val displayedLenses by viewModel.displayedLenses.collectAsStateWithLifecycle()
    val selectedLens by viewModel.engine.selectedLens.collectAsStateWithLifecycle()
    val capabilities by viewModel.engine.capabilities.collectAsStateWithLifecycle()
    val selectedPhotoResolution by viewModel.engine.selectedPhotoResolution.collectAsStateWithLifecycle()
    val selectedVideoResolution by viewModel.engine.selectedVideoResolution.collectAsStateWithLifecycle()
    val previewAspectRatio by viewModel.engine.previewAspectRatio.collectAsStateWithLifecycle()
    val storageStats by viewModel.engine.storageStats.collectAsStateWithLifecycle()
    val isRecordingVideo by viewModel.engine.isRecordingVideo.collectAsStateWithLifecycle()
    val videoDurationSeconds by viewModel.engine.videoDurationSeconds.collectAsStateWithLifecycle()
    val isCapturing by viewModel.engine.isCapturing.collectAsStateWithLifecycle()
    val lastCapturedMedia by viewModel.engine.lastCapturedMedia.collectAsStateWithLifecycle()

    val portraitConfig by viewModel.portraitConfig.collectAsStateWithLifecycle()
    val portraitProcessingState by viewModel.portraitProcessingState.collectAsStateWithLifecycle()
    val isPortraitSettingsOpen by viewModel.isPortraitSettingsOpen.collectAsStateWithLifecycle()
    val saveSelfieAsPreviewed by viewModel.saveSelfieAsPreviewed.collectAsStateWithLifecycle()
    val videoHdrState by viewModel.videoHdrState.collectAsStateWithLifecycle()
    val isVideoHdrPanelOpen by viewModel.isVideoHdrPanelOpen.collectAsStateWithLifecycle()

    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val timerMode by viewModel.timerMode.collectAsStateWithLifecycle()
    val activeTimerCountdown by viewModel.activeTimerCountdown.collectAsStateWithLifecycle()
    val gridType by viewModel.gridType.collectAsStateWithLifecycle()
    val isManualProOpen by viewModel.isManualProOpen.collectAsStateWithLifecycle()
    val activeProTab by viewModel.activeProTab.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isMediaViewerOpen by viewModel.isMediaViewerOpen.collectAsStateWithLifecycle()
    val focusRingPoint by viewModel.focusRingPoint.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val exposureCompensation by viewModel.exposureCompensation.collectAsStateWithLifecycle()
    val manualIso by viewModel.manualIso.collectAsStateWithLifecycle()
    val manualShutterSpeedNs by viewModel.manualShutterSpeedNs.collectAsStateWithLifecycle()
    val whiteBalance by viewModel.whiteBalance.collectAsStateWithLifecycle()
    val focusMode by viewModel.focusMode.collectAsStateWithLifecycle()
    val manualFocusDistance by viewModel.manualFocusDistance.collectAsStateWithLifecycle()
    val isAeLocked by viewModel.isAeLocked.collectAsStateWithLifecycle()
    val isAfLocked by viewModel.isAfLocked.collectAsStateWithLifecycle()
    val isRawEnabled by viewModel.isRawCaptureEnabled.collectAsStateWithLifecycle()
    val isVideoStabilizationEnabled by viewModel.isVideoStabilizationEnabled.collectAsStateWithLifecycle()
    val videoBitrate by viewModel.videoBitrateOption.collectAsStateWithLifecycle()
    val videoFps by viewModel.videoFps.collectAsStateWithLifecycle()
    val colorProfile by viewModel.colorProfile.collectAsStateWithLifecycle()
    val isAudioEnabled by viewModel.isAudioEnabled.collectAsStateWithLifecycle()
    val currentVideoQuality by viewModel.currentVideoQuality.collectAsStateWithLifecycle()
    val viewfinderResolution by viewModel.viewfinderResolution.collectAsStateWithLifecycle()
    val currentZoom by viewModel.currentZoom.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_main_screen")
    ) {
        // 1. Viewfinder layer preserving exact aspect ratio without distortion
        Viewfinder(
            aspectRatio = previewAspectRatio,
            gridType = gridType,
            focusRingPoint = focusRingPoint,
            isAeLocked = isAeLocked,
            isAfLocked = isAfLocked,
            onSurfaceTextureAvailable = { texture ->
                viewModel.engine.setPreviewSurfaceTexture(texture)
            },
            onTapToFocus = { point, normX, normY ->
                viewModel.onTapToFocus(point, normX, normY)
            },
            onZoomChange = { zoom ->
                viewModel.setZoom(zoom)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Controls
        TopControlBar(
            cameraMode = cameraMode,
            flashMode = flashMode,
            timerMode = timerMode,
            gridType = gridType,
            isRawEnabled = isRawEnabled,
            supportsRaw = capabilities.supportsRaw,
            storageStats = storageStats,
            videoQuality = currentVideoQuality,
            hdrState = videoHdrState,
            onVideoQualityClick = { viewModel.cycleVideoQuality() },
            onHdrClick = { viewModel.toggleVideoHdrPanel() },
            onFlashClick = { viewModel.cycleFlashMode() },
            onTimerClick = { viewModel.cycleTimerMode() },
            onGridClick = { viewModel.cycleGridType() },
            onRawClick = { viewModel.toggleRawCapture() },
            onSettingsClick = { viewModel.setSettingsOpen(true) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Manual Pro Control Bar (Slide-up above bottom controls in Photo/Video modes)
        if (cameraMode != CameraMode.PORTRAIT) {
            ManualProControlBar(
                isOpen = isManualProOpen,
                activeTab = activeProTab,
                capabilities = capabilities,
                exposureCompensation = exposureCompensation,
                manualIso = manualIso,
                manualShutterSpeedNs = manualShutterSpeedNs,
                whiteBalance = whiteBalance,
                focusMode = focusMode,
                manualFocusDistance = manualFocusDistance,
                colorProfile = colorProfile,
                isAeLocked = isAeLocked,
                isAfLocked = isAfLocked,
                onTabSelected = { viewModel.setActiveProTab(it) },
                onExposureChange = { viewModel.setExposureCompensation(it) },
                onIsoChange = { viewModel.setManualIso(it) },
                onShutterChange = { viewModel.setManualShutterSpeedNs(it) },
                onWbChange = { viewModel.setWhiteBalance(it) },
                onFocusModeChange = { viewModel.setFocusMode(it) },
                onFocusDistanceChange = { viewModel.setManualFocusDistance(it) },
                onColorProfileChange = { viewModel.setColorProfile(it) },
                onToggleAeLock = { viewModel.toggleAeLock() },
                onToggleAfLock = { viewModel.toggleAfLock() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
            )
        }

        // 3b. Dedicated Portrait Mode AI Controls Panel
        AnimatedVisibility(
            visible = cameraMode == CameraMode.PORTRAIT && isPortraitSettingsOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 190.dp)
        ) {
            PortraitControlBar(
                config = portraitConfig,
                processingState = portraitProcessingState,
                onBlurStrengthChanged = { viewModel.setPortraitBlurStrength(it) },
                onApertureSelected = { viewModel.setPortraitAperture(it) },
                onBokehStyleSelected = { viewModel.setPortraitBokehStyle(it) },
                onToggleFaceEnhancement = { viewModel.togglePortraitFaceEnhancement() },
                onToggleSkinTone = { viewModel.togglePortraitSkinTone() },
                onClose = { viewModel.setPortraitSettingsOpen(false) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Floating button to open Aperture Setting in Portrait Mode (only 'f' icon)
        if (cameraMode == CameraMode.PORTRAIT && !isPortraitSettingsOpen) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF141724).copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.setPortraitSettingsOpen(true) }
                    .testTag("open_portrait_settings_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "f",
                        color = Color(0xFFFFD54F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                }
            }
        }

        // 3c. Dedicated Video Mode Real-Time HDR Controls Panel
        AnimatedVisibility(
            visible = cameraMode == CameraMode.VIDEO && isVideoHdrPanelOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 190.dp)
        ) {
            VideoHdrControlBar(
                hdrState = videoHdrState,
                onModeChanged = { viewModel.setVideoHdrMode(it) },
                onIntensityChanged = { viewModel.setVideoHdrManualIntensity(it) },
                onShadowsChanged = { viewModel.setVideoHdrManualShadows(it) },
                onHighlightsChanged = { viewModel.setVideoHdrManualHighlights(it) },
                onContrastChanged = { viewModel.setVideoHdrManualContrast(it) },
                onExposureChanged = { viewModel.setVideoHdrManualExposure(it) },
                onBlackLevelChanged = { viewModel.setVideoHdrManualBlackLevel(it) },
                onMidtonesChanged = { viewModel.setVideoHdrManualMidtones(it) },
                onSaturationChanged = { viewModel.setVideoHdrManualSaturation(it) },
                onClose = { viewModel.setVideoHdrPanelOpen(false) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Floating button to reopen Video HDR Controls when closed
        if (cameraMode == CameraMode.VIDEO && !isVideoHdrPanelOpen) {
            VideoHdrFloatingButton(
                hdrState = videoHdrState,
                onClick = { viewModel.setVideoHdrPanelOpen(true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
            )
        }

        // 4. Toast Notification Overlay
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            toastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("camera_toast")
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 5. Bottom Controls (Uses displayedLenses filtered strictly by active lens facing)
        BottomControlBar(
            cameraMode = cameraMode,
            currentZoom = currentZoom,
            onZoomChange = { zoom -> viewModel.setZoom(zoom) },
            availableLenses = displayedLenses,
            selectedLens = selectedLens,
            isRecordingVideo = isRecordingVideo,
            videoDurationSeconds = videoDurationSeconds,
            isCapturing = isCapturing,
            isManualProOpen = isManualProOpen,
            lastCapturedMedia = lastCapturedMedia,
            activeTimerCountdown = activeTimerCountdown,
            onLensSelected = { viewModel.selectLens(it) },
            onModeSelected = { viewModel.setCameraMode(it) },
            onShutterClick = { viewModel.onMainActionButtonClick() },
            onFlipCameraClick = { viewModel.toggleCameraFacing() },
            onToggleProClick = { viewModel.toggleManualPro() },
            onGalleryClick = {
                if (lastCapturedMedia != null) {
                    viewModel.setMediaViewerOpen(true)
                } else {
                    viewModel.showToast("No recent photos yet")
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 6. Settings Bottom Sheet
        SettingsDrawer(
            isOpen = isSettingsOpen,
            cameraMode = cameraMode,
            capabilities = capabilities,
            availableLenses = displayedLenses,
            selectedLens = selectedLens,
            selectedPhotoResolution = selectedPhotoResolution,
            selectedVideoResolution = selectedVideoResolution,
            videoFps = videoFps,
            videoBitrate = videoBitrate,
            isVideoStabilizationEnabled = isVideoStabilizationEnabled,
            isAudioEnabled = isAudioEnabled,
            isRawEnabled = isRawEnabled,
            saveSelfieAsPreviewed = saveSelfieAsPreviewed,
            hdrState = videoHdrState,
            viewfinderResolution = viewfinderResolution,
            onLensSelected = { viewModel.selectLens(it) },
            onForceDeepScan = { viewModel.forceDeepScanLenses() },
            onPhotoResolutionSelected = { viewModel.selectPhotoResolution(it) },
            onVideoResolutionSelected = { viewModel.selectVideoResolution(it) },
            onViewfinderResolutionSelected = { viewModel.setViewfinderResolution(it) },
            onVideoFpsSelected = { viewModel.setVideoFps(it) },
            onVideoBitrateSelected = { viewModel.setVideoBitrate(it) },
            onStabilizationToggle = { viewModel.setVideoStabilization(it) },
            onAudioToggle = { viewModel.toggleAudio() },
            onRawToggle = { viewModel.toggleRawCapture() },
            onSaveSelfieAsPreviewedToggle = { viewModel.setSaveSelfieAsPreviewed(it) },
            onHdrModeSelected = { viewModel.setVideoHdrMode(it) },
            onHdrIntensityChanged = { viewModel.setVideoHdrManualIntensity(it) },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )

        // 7. Full-Screen Media Viewer Dialog
        if (isMediaViewerOpen) {
            MediaViewerDialog(
                media = lastCapturedMedia,
                onDismiss = { viewModel.setMediaViewerOpen(false) }
            )
        }
    }
}

@Composable
fun CameraPermissionPrompt(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101012))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                    .border(2.dp, Color(0xFFFFD54F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Camera & Audio Access",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "To capture high-resolution photos and record crisp video with genuine Camera2 manual controls, grant camera and microphone permissions.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_permissions_button")
            ) {
                Text(
                    text = "Grant Permissions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.example.camera.ui

import android.hardware.camera2.CameraCharacteristics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.camera.model.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Master Bottom Control Bar matching the reference UI design.
 * Structure:
 * 1. Floating Master Zoom Capsule directly over the viewfinder: [0.5] [(1x)] [2] [3] [5] [10]
 * 2. Solid Pure Black Bottom Panel:
 *    - Shutter row: [Gallery]  [Shutter Button]  [Flip Camera]
 *    - Mode carousel: [PHOTO ●]  [PORTRAIT]  [VIDEO]  [CINEMA]  [MORE]
 */
@Composable
fun BottomControlBar(
    cameraMode: CameraMode,
    currentZoom: Float = 1.0f,
    displayedLenses: List<LensInfo> = emptyList(),
    selectedLens: LensInfo? = null,
    onLensSelected: (LensInfo) -> Unit = {},
    onZoomChange: (Float) -> Unit = {},
    onZoomPresetTap: (Float) -> Unit = onZoomChange,
    isRecordingVideo: Boolean,
    videoDurationSeconds: Int,
    isCapturing: Boolean,
    isManualProOpen: Boolean = false,
    lastCapturedMedia: CapturedMedia?,
    activeTimerCountdown: Int?,
    onModeSelected: (CameraMode) -> Unit,
    onShutterClick: () -> Unit,
    onFlipCameraClick: () -> Unit,
    onToggleProClick: () -> Unit = {},
    onGalleryClick: () -> Unit,
    onCinemaModeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("master_bottom_control_bar"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Floating Master Zoom Capsule (0.5, 1x, 2, 3, 5, 10)
        MasterZoomCapsule(
            currentZoom = currentZoom,
            displayedLenses = displayedLenses,
            selectedLens = selectedLens,
            onLensSelected = onLensSelected,
            onZoomChange = onZoomChange,
            onZoomPresetTap = onZoomPresetTap,
            modifier = Modifier
                .padding(bottom = 14.dp)
                .testTag("master_zoom_capsule")
        )

        // 2. Solid Pure Black Bottom Control Area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 16.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Active Countdown Badge (Timer)
                AnimatedVisibility(visible = activeTimerCountdown != null) {
                    activeTimerCountdown?.let { count ->
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD54F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                color = Color.Black,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Active Video Recording Timer Badge
                AnimatedVisibility(visible = isRecordingVideo) {
                    val minutes = videoDurationSeconds / 60
                    val seconds = videoDurationSeconds % 60
                    val timeFormatted = "%02d:%02d".format(minutes, seconds)

                    val infiniteTransition = rememberInfiniteTransition(label = "recDotPulse")
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )

                    Row(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Red.copy(alpha = 0.25f))
                            .border(1.dp, Color.Red, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = dotAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC $timeFormatted",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Shutter & Primary Action Controls Row (Gallery - Shutter - Flip)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Gallery Thumbnail
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable { onGalleryClick() }
                            .testTag("gallery_thumbnail_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lastCapturedMedia != null) {
                            AsyncImage(
                                model = lastCapturedMedia.uri,
                                contentDescription = "Last captured media",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Center: Double-Ring Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, Color.White, CircleShape)
                            .clickable { onShutterClick() }
                            .testTag("main_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isCapturing) 0.85f else 1.0f,
                            label = "shutterScale"
                        )

                        when (cameraMode) {
                            CameraMode.PHOTO, CameraMode.MORE -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(buttonScale)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                            CameraMode.NIGHT -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(buttonScale)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(3.dp, Color(0xFFFFB300), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFB300))
                                    )
                                }
                            }
                            CameraMode.PORTRAIT -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(buttonScale)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.5.dp, Color(0xFFFFD54F), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD54F))
                                    )
                                }
                            }
                            CameraMode.VIDEO, CameraMode.CINEMA, CameraMode.DOLLY_ZOOM, CameraMode.DUAL_VIDEO -> {
                                if (isRecordingVideo) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE53935))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE53935))
                                    )
                                }
                            }
                        }
                    }

                    // Right: Camera Switcher / Flip Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xB21E1E24))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(enabled = !isRecordingVideo) { onFlipCameraClick() }
                            .testTag("flip_camera_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Mode Carousel (PHOTO · PORTRAIT · VIDEO · CINEMA · MORE)
                if (!isRecordingVideo) {
                    val modeScrollState = rememberScrollState()

                    LaunchedEffect(cameraMode) {
                        val index = CameraMode.entries.indexOf(cameraMode)
                        if (index >= 0) {
                            val itemEstimatedWidthPx = 200
                            val targetScroll = (index * itemEstimatedWidthPx - 160).coerceAtLeast(0)
                            modeScrollState.animateScrollTo(targetScroll)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(modeScrollState)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CameraMode.entries.forEach { mode ->
                            val isSelected = cameraMode == mode
                            val textColor by animateColorAsState(
                                if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.65f),
                                label = "modeTextColor"
                            )

                            Column(
                                modifier = Modifier
                                    .clickable {
                                        if (isSelected && mode == CameraMode.CINEMA) {
                                            onCinemaModeClick?.invoke()
                                        } else {
                                            onModeSelected(mode)
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .testTag("mode_${mode.name.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.name,
                                    color = textColor,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD54F))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Master Zoom Capsule matching the reference screenshot:
 * Dark frosted pill floating above the bottom controls, featuring:
 * 0.5   [1x] (with golden yellow circle border)   2   3   5   10
 * Supports direct tapping and horizontal drag scrubbing for fine zoom control.
 */
@Composable
fun MasterZoomCapsule(
    currentZoom: Float,
    displayedLenses: List<LensInfo>,
    selectedLens: LensInfo?,
    onLensSelected: (LensInfo) -> Unit,
    onZoomChange: (Float) -> Unit,
    onZoomPresetTap: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFrontCamera = selectedLens?.facing == CameraCharacteristics.LENS_FACING_FRONT
    val presets = remember(isFrontCamera) {
        if (isFrontCamera) {
            listOf(1.0f)
        } else {
            listOf(0.5f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
        }
    }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9141418))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .pointerInput(currentZoom) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 0.022f
                        val newZoom = (currentZoom + dragAmount * sensitivity).coerceIn(0.5f, 10.0f)
                        val rounded = (newZoom * 10).roundToInt() / 10f
                        onZoomChange(rounded)
                    }
                )
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isClosest = presets.minByOrNull { (it - currentZoom).absoluteValue } == preset
                val isExactMatch = (currentZoom - preset).absoluteValue < 0.2f
                val isActive = isExactMatch || (isClosest && !isDragging)

                val label = when (preset) {
                    0.5f -> "0.5"
                    1.0f -> "1x"
                    2.0f -> "2"
                    3.0f -> "3"
                    5.0f -> "5"
                    10.0f -> "10"
                    else -> "${preset}x"
                }

                val displayText = if (isActive && (currentZoom - preset).absoluteValue >= 0.25f) {
                    "%.1fx".format(currentZoom)
                } else {
                    label
                }

                // Physical lens mapping
                val targetLens = when (preset) {
                    0.5f -> displayedLenses.firstOrNull { it.lensType == LensType.ULTRAWIDE && it.isPhysical }
                        ?: displayedLenses.firstOrNull { it.lensType == LensType.ULTRAWIDE }
                    1.0f -> displayedLenses.firstOrNull { it.lensType == LensType.WIDE && it.isPhysical && !it.isZoomPreset }
                        ?: displayedLenses.firstOrNull { it.lensType == LensType.WIDE && !it.isZoomPreset }
                    2.0f -> displayedLenses.firstOrNull { (it.lensType == LensType.TELEPHOTO || it.lensType == LensType.TELEPHOTO_3X) && it.isPhysical }
                        ?: displayedLenses.firstOrNull { it.lensType == LensType.TELEPHOTO || it.lensType == LensType.TELEPHOTO_3X }
                    3.0f -> displayedLenses.firstOrNull { it.lensType == LensType.TELEPHOTO_3X && it.isPhysical }
                        ?: displayedLenses.firstOrNull { it.lensType == LensType.TELEPHOTO_3X }
                        ?: displayedLenses.firstOrNull { it.lensType == LensType.TELEPHOTO && it.isPhysical }
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0xFF26210A) else Color.Transparent)
                        .border(
                            width = if (isActive) 1.5.dp else 0.dp,
                            color = if (isActive) Color(0xFFFFD54F) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            if (targetLens != null) {
                                onLensSelected(targetLens)
                            } else {
                                onZoomPresetTap(preset)
                            }
                        }
                        .testTag("zoom_preset_${(preset * 10).roundToInt()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = if (isActive) Color(0xFFFFD54F) else Color.White,
                        fontSize = if (displayText.length >= 4) 10.5.sp else 12.5.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}


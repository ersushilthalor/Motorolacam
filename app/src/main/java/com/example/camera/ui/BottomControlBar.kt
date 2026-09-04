package com.example.camera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

@Composable
fun BottomControlBar(
    cameraMode: CameraMode,
    currentZoom: Float = 1.0f,
    onZoomChange: (Float) -> Unit = {},
    onZoomPresetTap: (Float) -> Unit = onZoomChange,
    isRecordingVideo: Boolean,
    videoDurationSeconds: Int,
    isCapturing: Boolean,
    isManualProOpen: Boolean,
    lastCapturedMedia: CapturedMedia?,
    activeTimerCountdown: Int?,
    onModeSelected: (CameraMode) -> Unit,
    onShutterClick: () -> Unit,
    onFlipCameraClick: () -> Unit,
    onToggleProClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.85f),
                        Color.Black
                    )
                )
            )
            .navigationBarsPadding()
            .padding(bottom = 12.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Timer Countdown Badge
        AnimatedVisibility(visible = activeTimerCountdown != null) {
            activeTimerCountdown?.let { count ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        color = Color.Black,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Active Video Recording Timer
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
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Red.copy(alpha = 0.25f))
                    .border(1.dp, Color.Red, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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

        // Horizontally Scrolling Liquid Transparent Zoom Bar (.5x to 10x, ultra-smooth)
        HorizontalScrollingZoomBar(
            currentZoom = currentZoom,
            onZoomChange = onZoomChange,
            onZoomPresetTap = onZoomPresetTap,
            modifier = Modifier.testTag("horizontal_scrolling_zoom_bar_container")
        )

        // Bottom Mode Switcher (PHOTO / VIDEO)
        if (!isRecordingVideo) {
            Row(
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraMode.entries.forEach { mode ->
                    val isSelected = cameraMode == mode
                    val textColor by animateColorAsState(
                        if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f),
                        label = "modeTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModeSelected(mode) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Primary Action Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gallery Thumbnail / Viewer Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center: Big Shutter / Record Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { onShutterClick() }
                    .testTag("main_shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                val buttonScale by animateFloatAsState(
                    targetValue = if (isCapturing) 0.85f else 1.0f,
                    label = "shutterScale"
                )

                when (cameraMode) {
                    CameraMode.PHOTO -> {
                        // Photo Shutter
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .scale(buttonScale)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    CameraMode.PORTRAIT -> {
                        // Portrait Mode Photo Shutter with Warm Golden Ring
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .scale(buttonScale)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, Color(0xFFFFD54F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD54F))
                            )
                        }
                    }
                    CameraMode.VIDEO -> {
                        // Video Record Button
                        if (isRecordingVideo) {
                            // Stop square
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Red)
                            )
                        } else {
                            // Record red circle
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }

            // Right: Flip Camera / Pro Toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Flip camera button
                IconButton(
                    onClick = onFlipCameraClick,
                    enabled = !isRecordingVideo,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Pro toggle small pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isManualProOpen) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.15f))
                        .clickable { onToggleProClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("pro_toggle_button")
                ) {
                    Text(
                        text = "PRO",
                        color = if (isManualProOpen) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * Horizontal Scrolling Liquid Transparent Zoom Bar (.5x to 10x, ultra-smooth)
 * Styled exactly to the reference screenshot:
 * Sleek frosted glass capsule, solid white circular active badge, black bold text, 3 vertical divider dots.
 */
@Composable
fun HorizontalScrollingZoomBar(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onZoomPresetTap: (Float) -> Unit = onZoomChange,
    modifier: Modifier = Modifier
) {
    val presets = remember { listOf(0.5f, 1.0f, 2.0f, 3.0f, 10.0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0xB21A1A1E))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.20f),
                shape = RoundedCornerShape(23.dp)
            )
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
            .testTag("horizontal_scrolling_zoom_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEachIndexed { index, preset ->
                val isClosest = presets.minByOrNull { (it - currentZoom).absoluteValue } == preset
                val isExactMatch = (currentZoom - preset).absoluteValue < 0.18f
                val isSelected = isExactMatch || (isClosest && !isDragging)

                val label = when (preset) {
                    0.5f -> "0.5"
                    1.0f -> "1x"
                    2.0f -> "2"
                    3.0f -> "3"
                    10.0f -> "10"
                    else -> "${preset}x"
                }

                val displayText = if (isClosest && (currentZoom - preset).absoluteValue >= 0.2f) {
                    "%.1f".format(currentZoom)
                } else {
                    label
                }

                // Preset button: Active displays as solid white circle with bold black text
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onZoomPresetTap(preset) }
                        .testTag("zoom_preset_${preset}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = if (displayText.length >= 4) 11.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }

                // Between presets: 3 vertical divider dots
                if (index < presets.size - 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(width = 1.5.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }
    }
}
